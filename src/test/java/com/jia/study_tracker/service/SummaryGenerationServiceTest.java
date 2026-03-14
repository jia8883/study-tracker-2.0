package com.jia.study_tracker.service;

import com.jia.study_tracker.domain.StudyLog;
import com.jia.study_tracker.domain.Summary;
import com.jia.study_tracker.domain.SummaryType;
import com.jia.study_tracker.domain.User;
import com.jia.study_tracker.dto.SummaryRetryRequest;
import com.jia.study_tracker.exception.InvalidOpenAIResponseException;
import com.jia.study_tracker.exception.OpenAIClientException;
import com.jia.study_tracker.repository.UserRepository;
import com.jia.study_tracker.service.dto.SummaryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * SummaryGenerationService 테스트
 *
 * 목표:
 * - generateSummaries 메서드가 상황별로 정상 동작하는지 검증한다.
 *
 * 테스트 시나리오:
 * 1. 학습 로그가 없는 사용자는 GPT 요약 생성을 스킵하고 저장하지 않는다.
 * 2. 학습 로그가 있는 사용자는 GPT 요약을 생성하고 저장하고 사용자에게 전송한다.
 * 3. OpenAI 응답이 유효하지 않은 경우 (InvalidOpenAIResponseException 발생),
 *  시스템은 슬랙으로 오류 메시지를 전송하고, 실패한 요청을 Redis 큐에 등록한다.
 * 4. OpenAI 호출이 실패하는 경우 (OpenAIClientException 발생),
 *  실패한 요청을 Redis 큐에 등록한다.
 */
@ExtendWith(MockitoExtension.class)
class SummaryGenerationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyLogQueryService studyLogQueryService;

    @Mock
    private OpenAIClient openAIClient;

    @Mock
    private SlackNotificationService slackNotificationService;

    @Mock
    private SummarySaver summarySaver;

    @Mock
    private RedisTemplate<String, SummaryRetryRequest> redisTemplate;

    @Mock
    private ListOperations<String, SummaryRetryRequest> listOperations;

    @InjectMocks
    private SummaryGenerationService summaryGenerationService;

    private User user;
    private LocalDate date;
    private SummaryType type;

    @BeforeEach
    void setUp() {
        user = new User("U123456", "jia");
        date = LocalDate.of(2025, 5, 2);
        type = SummaryType.DAILY;
    }

    @Test
    @DisplayName("사용자에게 학습 로그가 없으면 요약을 건너뛴다")
    void shouldSkipSummaryWhenNoLogsExist() {
        // given
        given(userRepository.findAll()).willReturn(List.of(user));
        given(studyLogQueryService.getLogs(user.getSlackUserId(), date, type)).willReturn(List.of());

        // when
        summaryGenerationService.generateSummaries(date, type);

        // then
        verify(openAIClient, never()).generateSummaryAndFeedback(any());
        verify(summarySaver, never()).save(any());
    }

    @Test
    @DisplayName("학습 로그가 존재하면 GPT를 호출하고 결과를 저장하고 알림을 보낸다")
    void shouldGenerateAndSaveSummaryWhenLogsExist() {
        // given
        List<StudyLog> logs = List.of(new StudyLog("공부 내용", LocalDateTime.now(), user));
        SummaryResult result = new SummaryResult("요약", "피드백");

        given(userRepository.findAll()).willReturn(List.of(user));
        given(studyLogQueryService.getLogs(user.getSlackUserId(), date, type)).willReturn(logs);
        given(openAIClient.generateSummaryAndFeedback(logs)).willReturn(result);

        // when
        summaryGenerationService.generateSummaries(date, type);

        // then
        verify(openAIClient).generateSummaryAndFeedback(logs);
        verify(summarySaver).save(any(Summary.class));
        verify(slackNotificationService).sendSummaryToUser(eq(user), any(Summary.class));
    }

    @Test
    @DisplayName("InvalidOpenAIResponseException 발생 시 슬랙에 오류 메시지를 보내고 큐에 요청을 등록한다")
    void shouldNotifyErrorAndQueueWhenInvalidOpenAIResponse() {
        // given
        List<StudyLog> logs = List.of(new StudyLog("공부 내용", LocalDateTime.now(), user));
        SummaryRetryRequest expectedRequest = new SummaryRetryRequest(
                user.getSlackUserId(),
                user.getSlackUsername(),
                type.name(),
                date.toString(),
                0
        );

        given(userRepository.findAll()).willReturn(List.of(user));
        given(studyLogQueryService.getLogs(user.getSlackUserId(), date, type)).willReturn(logs);
        given(openAIClient.generateSummaryAndFeedback(logs))
                .willThrow(new InvalidOpenAIResponseException("응답 이상"));
        given(redisTemplate.opsForList()).willReturn(listOperations);

        // when
        summaryGenerationService.generateSummaries(date, type);

        // then
        verify(slackNotificationService).sendErrorNotice(eq(user), eq(date), eq(type));
        verify(listOperations).rightPush(eq("summary-retry-queue"), refEq(expectedRequest));
    }


    @Test
    @DisplayName("OpenAIClientException 발생 시 큐에 요청을 등록한다")
    void shouldEnqueueRequestOnOpenAIClientFailure() {
        // given
        List<StudyLog> logs = List.of(new StudyLog("공부 내용", LocalDateTime.now(), user));
        SummaryRetryRequest expectedRequest = new SummaryRetryRequest(
                user.getSlackUserId(),
                user.getSlackUsername(),
                type.name(),
                date.toString(),
                0
        );

        given(userRepository.findAll()).willReturn(List.of(user));
        given(studyLogQueryService.getLogs(user.getSlackUserId(), date, type)).willReturn(logs);
        given(openAIClient.generateSummaryAndFeedback(logs))
                .willThrow(new OpenAIClientException("서버 오류", new RuntimeException("internal")));
        given(redisTemplate.opsForList()).willReturn(listOperations);

        // when
        summaryGenerationService.generateSummaries(date, type);

        // then
        verify(listOperations).rightPush(eq("summary-retry-queue"), refEq(expectedRequest));
    }

}

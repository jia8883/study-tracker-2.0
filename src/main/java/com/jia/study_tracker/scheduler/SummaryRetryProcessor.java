package com.jia.study_tracker.scheduler;

import com.jia.study_tracker.domain.StudyLog;
import com.jia.study_tracker.domain.Summary;
import com.jia.study_tracker.domain.SummaryType;
import com.jia.study_tracker.domain.User;
import com.jia.study_tracker.dto.SummaryRetryRequest;
import com.jia.study_tracker.repository.UserRepository;
import com.jia.study_tracker.service.OpenAIClient;
import com.jia.study_tracker.service.SlackNotificationService;
import com.jia.study_tracker.service.StudyLogQueryService;
import com.jia.study_tracker.service.SummarySaver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Redis 큐에 등록된 실패한 요약 요청을 주기적으로 꺼내어 재시도 처리하는 컴포넌트
 *
 * 주요 책임:
 * - Redis 리스트(summary-retry-queue)에서 실패 요청을 하나씩 꺼냄
 * - 해당 유저의 StudyLog를 조회하고, OpenAI를 통해 요약을 재생성
 * - 성공 시 Summary 저장 및 사용자에게 Slack으로 요약 전송
 * - 실패 시 다시 큐에 넣어 재시도 기회를 유지
 *
 * 신뢰성 보장:
 * - SummaryGenerationService에서 실패한 요청을 보존하고,
 *   일정 간격으로 재시도함으로써 최소 1회 이상 요약 생성 보장 (at-least-once)
 * - 다만 재시도는 무한루프 방지를 위해 5회 미만으로 제한함
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SummaryRetryProcessor {

    private final RedisTemplate<String, SummaryRetryRequest> redisTemplate;
    private final UserRepository userRepository;
    private final StudyLogQueryService studyLogQueryService;
    private final OpenAIClient openAIClient;
    private final SlackNotificationService slackNotificationService;
    private final SummarySaver summarySaver;

    private static final int MAX_RETRY_COUNT = 5;

    @Scheduled(fixedDelay = 5 * 60 * 1000) // 5분마다 실행
    public void processRetryQueue() {
        while (true) {
            SummaryRetryRequest request = redisTemplate.opsForList().leftPop("summary-retry-queue");
            if (request == null) break;

            if (request.getRetryCount() >= MAX_RETRY_COUNT) {
                log.error("❌ 최대 재시도 초과 - 폐기됨: {} (type: {}, date: {})",
                        request.getSlackUserId(), request.getSummaryType(), request.getTargetDate());
                continue;
            }

            log.info("Redis 재시도 처리 시작: {} (retryCount: {})",
                    request.getSlackUserId(), request.getRetryCount());

            try {
                User user = userRepository.findById(request.getSlackUserId())
                        .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getSlackUserId()));

                LocalDate date = LocalDate.parse(request.getTargetDate());
                SummaryType type = SummaryType.valueOf(request.getSummaryType());

                List<StudyLog> logs = studyLogQueryService.getLogs(user.getSlackUserId(), date, type);
                if (logs.isEmpty()) {
                    log.debug("[{}] {} 로그 없음 - 요약 생략 (재시도)", user.getSlackUsername(), type);
                    continue;
                }

                var result = openAIClient.generateSummaryAndFeedback(logs);
                Summary summary = new Summary(
                        date,
                        result.getSummary(),
                        result.getFeedback(),
                        true,
                        null,
                        user,
                        type
                );
                summarySaver.save(summary);
                slackNotificationService.sendSummaryToUser(user, summary);
                log.info("✅ 재시도 성공: {}", user.getSlackUserId());

            } catch (Exception e) {
                log.error("❌ 재시도 실패 → 다시 큐에 넣음: {} - {}", request.getSlackUserId(), e.getMessage());

                // retryCount 증가 후 재등록
                SummaryRetryRequest retry = new SummaryRetryRequest(
                        request.getSlackUserId(),
                        request.getSlackUsername(),
                        request.getSummaryType(),
                        request.getTargetDate(),
                        request.getRetryCount() + 1
                );
                redisTemplate.opsForList().rightPush("summary-retry-queue", retry);
            }
        }
    }
}

package com.jia.study_tracker.service;

import com.jia.study_tracker.domain.StudyLog;
import com.jia.study_tracker.domain.SummaryType;
import com.jia.study_tracker.domain.User;
import com.jia.study_tracker.messaging.dto.SummaryRequestEvent;
import com.jia.study_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.jia.study_tracker.dto.SummaryRetryRequest;
import org.springframework.data.redis.core.RedisTemplate;
import com.jia.study_tracker.messaging.producer.SummaryEventProducer;
import java.time.LocalDate;
import java.util.List;

/**
 * SummaryGenerationService는 매일/매주/매월 스케줄러에 의해 호출되어
 * 각 사용자에 대해 해당 기간의 StudyLog를 조회하고, AI 요약 생성을 요청하는 서비스
 * 주요 책임:
 * - 사용자 목록 조회 및 반복 처리
 * - 각 사용자에 대해 StudyLog 조회 및 요약 생성 요청 전송 (Kafka)
 * - Kafka 전송 실패 시 Redis 큐에 재시도 요청 등록
 * - OpenAI 호출은 직접 수행하지 않고, Kafka로 메시지를 전송하여 별도의 워커(SummaryWorker)가 처리하도록 설계
 * - Kafka로 메시지 전송 시 예외 발생 시 Redis에 재시도 요청 등록하여 최소 1회 이상 요약 생성 보장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryGenerationService {

    private final UserRepository userRepository;
    private final StudyLogQueryService studyLogQueryService;
    private final RedisTemplate<String, SummaryRetryRequest> redisTemplate;
    private final SummaryEventProducer summaryEventProducer;

    public void generateSummaries(LocalDate date, SummaryType type) {
        log.info("요약 생성 시작 - date: {}, type: {}", date, type);
        long userCount = userRepository.count();
        log.info("전체 사용자 수: {}", userCount);

        userRepository.findAll().forEach(user -> {
            log.info("사용자 처리 시작 - {} ({})", user.getSlackUsername(), user.getSlackUserId());
            try {
                processOneUser(user, date, type);
            } catch (Exception e) {
                log.error("[{}] 요약 처리 중 예외 발생", user.getSlackUsername(), e);
            }
        });
    }

    /**
     * 한 명의 유저에 대해 로그 조회 → AI 요약 생성 → 저장 → 슬랙 전송 흐름을 처리
     */
    private void processOneUser(User user, LocalDate date, SummaryType type) {
        log.debug("[{}] {} 요약 시작", user.getSlackUsername(), type);

        List<StudyLog> logs = studyLogQueryService.getLogs(user.getSlackUserId(), date, type);
        log.debug("[{}] 로그 수: {}", user.getSlackUsername(), logs.size());

        if (logs.isEmpty()) {
            log.debug("[{}] {} 로그 없음 - 요약 생략", user.getSlackUsername(), type);
            return;
        }

        // OpenAI 호출 대신 Kafka로 메시지 전송
        try {
            summaryEventProducer.sendSummaryRequest(
                    new SummaryRequestEvent(
                            user.getSlackUserId(),
                            date.toString(),
                            type.name()
                    )
            );
            log.info("[{}] Kafka로 요약 요청 전송 완료", user.getSlackUsername());
        } catch (Exception e) {
            log.error("[{}] Kafka 전송 실패", user.getSlackUsername(), e);
            registerRetry(user, date, type);
        }
    }

    private void registerRetry(User user, LocalDate date, SummaryType type) {
        SummaryRetryRequest retryRequest = new SummaryRetryRequest(
                user.getSlackUserId(),
                user.getSlackUsername(),
                type.name(),
                date.toString(),
                0
        );
        redisTemplate.opsForList().rightPush("summary-retry-queue", retryRequest);
    }
}

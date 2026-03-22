package com.jia.study_tracker.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jia.study_tracker.domain.Summary;
import com.jia.study_tracker.messaging.dto.SummaryResultEvent;
import com.jia.study_tracker.messaging.topic.KafkaTopics;
import com.jia.study_tracker.repository.SummaryRepository;
import com.jia.study_tracker.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/*
 * 1. KafkaTopics.SUMMARY_RESULT 토픽에서 메시지를 수신
 * 2. Summary ID를 기반으로 DB에서 Summary 조회
 * 3. SlackNotificationService를 사용하여 사용자에게 요약 결과 전송
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlackSenderWorker {

    private final SlackNotificationService slackNotificationService;
    private final SummaryRepository summaryRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.SUMMARY_RESULT)
    public void send(SummaryResultEvent event) {

        try {
            Long summaryId = event.getSummaryId();

            Summary summary = summaryRepository.findById(summaryId)
                    .orElseThrow();

            slackNotificationService.sendSummaryToUser(
                    summary.getUser(),
                    summary
            );

        } catch (Exception e) {
            log.error("Slack send error - event: {}", event, e);
        }
    }
}

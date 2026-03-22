package com.jia.study_tracker.worker;


import com.jia.study_tracker.messaging.dto.SummaryRequestEvent;
import com.jia.study_tracker.messaging.dto.SummaryResultEvent;
import com.jia.study_tracker.messaging.topic.KafkaTopics;
import com.jia.study_tracker.service.StudyLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.jia.study_tracker.domain.StudyLog;
import com.jia.study_tracker.domain.Summary;
import com.jia.study_tracker.domain.SummaryType;
import com.jia.study_tracker.domain.User;
import com.jia.study_tracker.repository.UserRepository;
import com.jia.study_tracker.service.OpenAIClient;
import com.jia.study_tracker.service.SummarySaver;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;

/*
 * 1. KafkaTopics.SUMMARY_REQUEST 토픽에서 메시지를 수신
 * 2. OpenAI를 사용하여 요약 생성
 * 3. 요약을 DB에 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryWorker {

    private final OpenAIClient openAIClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserRepository userRepository;
    private final SummarySaver summarySaver;
    private final StudyLogQueryService studyLogQueryService;

    @KafkaListener(topics = KafkaTopics.SUMMARY_REQUEST)
    public void process(SummaryRequestEvent event) {

        try {
            String userId = event.getUserId();
            LocalDate date = LocalDate.parse(event.getDate());
            SummaryType type = SummaryType.valueOf(event.getType());

            User user = userRepository.findById(userId)
                    .orElseThrow();

            List<StudyLog> logs =
                    studyLogQueryService.getLogs(userId, date, type);

            if (logs.isEmpty()) {
                return;
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

            kafkaTemplate.send(
                    KafkaTopics.SUMMARY_RESULT,
                    new SummaryResultEvent(userId, summary.getId())
            );

        } catch (Exception e) {
            log.error("Summary worker error - event: {}", event, e);
        }
    }
}

package com.jia.study_tracker.worker;


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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserRepository userRepository;
    private final SummarySaver summarySaver;
    private final ObjectMapper objectMapper;
    private final StudyLogQueryService studyLogQueryService;

    @KafkaListener(topics = KafkaTopics.SUMMARY_REQUEST)
    public void process(String message) {

        try {
            Map<String, Object> data = objectMapper.readValue(message, Map.class);

            String userId = (String) data.get("userId");
            LocalDate date = LocalDate.parse((String) data.get("date"));
            SummaryType type = SummaryType.valueOf((String) data.get("type"));

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
                    objectMapper.writeValueAsString(Map.of(
                            "userId", userId,
                            "summaryId", summary.getId()
                    ))
            );

        } catch (Exception e) {
            log.error("Summary worker error - message: {}", message, e);
        }
    }
}

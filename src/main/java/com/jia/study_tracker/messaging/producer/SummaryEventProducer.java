package com.jia.study_tracker.messaging.producer;

import com.jia.study_tracker.messaging.dto.SummaryRequestEvent;
import com.jia.study_tracker.messaging.topic.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/*
 * 1. KafkaTopics.SUMMARY_REQUEST 토픽에 메시지 전송
 * 2. SummaryWorker가 메시지를 수신하여 처리
 */
@Service
@RequiredArgsConstructor
public class SummaryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendSummaryRequest(SummaryRequestEvent message) {

        kafkaTemplate.send(KafkaTopics.SUMMARY_REQUEST, message);

    }
}

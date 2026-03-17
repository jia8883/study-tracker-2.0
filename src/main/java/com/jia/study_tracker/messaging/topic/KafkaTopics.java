package com.jia.study_tracker.messaging.topic;

/**
 * SUMMARY_REQUEST : SummaryEventProducer가 SummaryWorker에게 요약 요청 메시지를 보낼 때 사용하는 토픽
 * SUMMARY_RESULT : SummaryWorker가 SummaryEventProducer에게 요약 결과 메시지를 보낼 때 사용하는 토픽
 */
public class KafkaTopics {
    public static final String SUMMARY_REQUEST = "summary-request";
    public static final String SUMMARY_RESULT = "summary-result";
}

package com.jia.study_tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 실패한 요약 요청을 Redis 큐에 저장하기 위한 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryRetryRequest implements Serializable {
    private String slackUserId;
    private String slackUsername;
    private String summaryType;
    private String targetDate;
    private int retryCount; // 재시도 제한에 활용
}

package com.jia.study_tracker.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OpenAI로부터 받은 요약 및 피드백 결과를 담는 DTO 클래스
 *
 * 역할: OpenAIClient에서 생성한 summary, feedback을 전달
 */
@Getter
@AllArgsConstructor
public class SummaryResult {
    private String summary; // 학습 내용 요약
    private String feedback; // 동기부여 피드백
}

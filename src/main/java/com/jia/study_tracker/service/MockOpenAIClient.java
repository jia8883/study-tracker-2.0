package com.jia.study_tracker.service;

import com.jia.study_tracker.domain.StudyLog;
import com.jia.study_tracker.domain.SummaryType;
import com.jia.study_tracker.exception.InvalidOpenAIResponseException;
import com.jia.study_tracker.service.dto.SummaryResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * OpenAIClient를 대체하는 모킹 버전
 * 실제 API 호출 없이 고정된 응답을 리턴
 */
@Component
@Profile("mock-openai")
public class MockOpenAIClient implements OpenAIClient {

    @Override
    public SummaryResult generateSummaryAndFeedback(List<StudyLog> logs) {
        return new SummaryResult(
                "[MOCK 요약] 공부 열심히 했어요",
                "[MOCK 피드백] 화이팅!"
        );
    }

}

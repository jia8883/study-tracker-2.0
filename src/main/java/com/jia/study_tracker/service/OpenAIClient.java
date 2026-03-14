package com.jia.study_tracker.service;

import com.jia.study_tracker.domain.StudyLog;
import com.jia.study_tracker.service.dto.SummaryResult;

import java.util.List;

public interface OpenAIClient {
    SummaryResult generateSummaryAndFeedback(List<StudyLog> logs);
}

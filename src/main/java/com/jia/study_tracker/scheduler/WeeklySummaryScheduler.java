package com.jia.study_tracker.scheduler;

import com.jia.study_tracker.domain.SummaryType;
import com.jia.study_tracker.service.SummaryGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 매주 일요일 밤 9시 스케줄러 작동
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklySummaryScheduler {

    private final SummaryGenerationService summaryGenerationService;

    @Scheduled(cron = "0 0 21 * * SUN")
    public void generateWeeklySummaries() {
        LocalDate today = LocalDate.now();
        summaryGenerationService.generateSummaries(today, SummaryType.WEEKLY);
    }
}


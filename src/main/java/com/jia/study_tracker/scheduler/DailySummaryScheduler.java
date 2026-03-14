package com.jia.study_tracker.scheduler;

import com.jia.study_tracker.domain.SummaryType;
import com.jia.study_tracker.service.SummaryGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 매일 밤 10시 스케줄러 작동
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailySummaryScheduler {

    private final SummaryGenerationService summaryGenerationService;

    @Scheduled(cron = "0 0 22 * * *")
    public void generateDailySummaries() {
        summaryGenerationService.generateSummaries(LocalDate.now(), SummaryType.DAILY);
    }
}

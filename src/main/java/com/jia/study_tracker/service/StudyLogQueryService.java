package com.jia.study_tracker.service;

import com.jia.study_tracker.domain.StudyLog;
import com.jia.study_tracker.domain.SummaryType;
import com.jia.study_tracker.domain.User;
import com.jia.study_tracker.repository.StudyLogRepository;
import com.jia.study_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 사용자 ID(slackUserId)를 기반으로
 * 일/주/월 단위의 학습 기록(StudyLog)을 조회하는 기능을 제공
 */

@Service
@RequiredArgsConstructor
public class StudyLogQueryService {

    private final StudyLogRepository studyLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<StudyLog> getLogs(String slackUserId, LocalDate baseDate, SummaryType type) {
        User user = userRepository.findById(slackUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저"));

        LocalDateTime start;
        LocalDateTime end;

        switch (type) {
            case DAILY -> {
                start = baseDate.atStartOfDay();
                end = baseDate.plusDays(1).atStartOfDay();
            }
            case WEEKLY -> {
                // baseDate: 일요일(주 시작 기준)
                start = baseDate.atStartOfDay();
                end = baseDate.plusDays(7).atStartOfDay();
            }
            case MONTHLY -> {
                YearMonth month = YearMonth.from(baseDate);
                start = month.atDay(1).atStartOfDay();
                end = month.plusMonths(1).atDay(1).atStartOfDay();
            }
            default -> throw new IllegalArgumentException("지원하지 않는 SummaryType");
        }

        return studyLogRepository.findByUserAndTimestampBetween(user, start, end);
    }
}

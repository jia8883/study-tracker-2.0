package com.jia.study_tracker.repository;

import com.jia.study_tracker.domain.StudyLog;
import com.jia.study_tracker.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StudyLogRepository extends JpaRepository<StudyLog, Long> {

    List<StudyLog> findByUserAndTimestampBetween(User user, LocalDateTime start, LocalDateTime end);
}

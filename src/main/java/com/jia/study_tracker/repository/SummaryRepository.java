package com.jia.study_tracker.repository;

import com.jia.study_tracker.domain.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SummaryRepository extends JpaRepository<Summary, Long> {

}
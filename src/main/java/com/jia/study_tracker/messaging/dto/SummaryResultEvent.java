package com.jia.study_tracker.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResultEvent {

    private String userId;
    private Long summaryId;

}

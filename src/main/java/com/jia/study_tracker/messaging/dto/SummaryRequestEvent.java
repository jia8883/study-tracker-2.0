package com.jia.study_tracker.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryRequestEvent {

    private String userId;
    private String date;
    private String type;

}
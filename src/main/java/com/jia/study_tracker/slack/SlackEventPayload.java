package com.jia.study_tracker.slack;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class SlackEventPayload {
    private SlackEventType type;
    private String challenge;
    private Event event;

    @Getter
    @AllArgsConstructor
    public static class Event {
        private String type;
        private String user;
        private String text;
        private String bot_id;
    }
}

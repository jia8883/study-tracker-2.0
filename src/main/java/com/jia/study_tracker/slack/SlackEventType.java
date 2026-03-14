package com.jia.study_tracker.slack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SlackEventType {
    URL_VERIFICATION("url_verification"),
    EVENT_CALLBACK("event_callback");

    private final String type;

    SlackEventType(String type) {
        this.type = type;
    }

    @JsonValue
    public String getType() {
        return type;
    }

    @JsonCreator
    public static SlackEventType from(String type) {
        for (SlackEventType value : values()) {
            if (value.type.equalsIgnoreCase(type)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown SlackEventType: " + type);
    }
}

package com.jia.study_tracker.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private String slackUserId;

    private String slackUsername;

    public User(String slackUserId, String slackUsername) {
        this.slackUserId = slackUserId;
        this.slackUsername = slackUsername;
    }

}

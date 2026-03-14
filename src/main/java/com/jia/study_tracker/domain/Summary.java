package com.jia.study_tracker.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;



@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    // 수천 자 이내 요약 기준으로 설정 (너무 길어지면 @Lob으로 교체 고려)
    @Column(length = 5000)
    private String summary;

    // 수천 자 이내 요약 기준으로 설정 (너무 길어지면 @Lob으로 교체 고려)
    @Column(length = 5000)
    private String feedback;

    private boolean success;

    private String failureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    private SummaryType type;

    public Summary(LocalDate date, String summary, String feedback, boolean success, String failureReason, User user, SummaryType type) {
        this.date = date;
        this.summary = summary;
        this.feedback = feedback;
        this.success = success;
        this.failureReason = failureReason;
        this.user = user;
        this.type = type;
    }
}

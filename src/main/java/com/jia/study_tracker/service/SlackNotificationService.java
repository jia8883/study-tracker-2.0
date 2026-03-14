package com.jia.study_tracker.service;

import com.jia.study_tracker.domain.Summary;
import com.jia.study_tracker.domain.SummaryType;
import com.jia.study_tracker.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

/**
 * 요약 메시지를 Slack으로 전송하며 오류 발생 시 자동 재시도
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlackNotificationService {

    private final WebClient slackWebClient;

    @Value("${slack.enabled:true}") // 기본값 true
    private boolean slackEnabled;

    // 테스트에서 직접 주입할 수 있게 setter 추가
    public void setSlackEnabled(boolean slackEnabled) {
        this.slackEnabled = slackEnabled;
    }

    /**
     * 정상 요약 메시지를 슬랙으로 전송 (비동기)
     */
    public void sendSummaryToUser(User user, Summary summary) {
        if (!slackEnabled) return;

        sendMessage(user.getSlackUserId(), formatSummaryMessage(summary))
                .doOnSuccess(resp -> log.debug("✅ Slack 전송 완료: userId={}", user.getSlackUserId()))
                .doOnError(err -> log.warn("❌ Slack 전송 실패: userId={}, reason={}", user.getSlackUserId(), err.getMessage()))
                .subscribe();
    }

    /**
     * 정상 요약 메시지를 슬랙으로 전송 (동기, 테스트용)
     */
    public void sendSummaryToUserSync(User user, Summary summary) {
        if (!slackEnabled) return;

        sendMessage(user.getSlackUserId(), formatSummaryMessage(summary))
                .doOnSuccess(resp -> log.debug("✅ Slack 전송 완료: userId={}", user.getSlackUserId()))
                .doOnError(err -> log.warn("❌ Slack 전송 실패: userId={}, reason={}", user.getSlackUserId(), err.getMessage()))
                .block();
    }

    /**
     * 요약 생성 실패 시 사용자에게 관리자 문의 안내 메시지를 전송 (비동기)
     */
    public void sendErrorNotice(User user, LocalDate date, SummaryType type) {
        if (!slackEnabled) return;

        sendMessage(user.getSlackUserId(), formatErrorMessage(user, date, type))
                .doOnSuccess(resp -> log.debug("✅ 관리자 문의 안내 전송 완료: userId={}", user.getSlackUserId()))
                .doOnError(err -> log.warn("❌ 관리자 문의 안내 전송 실패: userId={}, reason={}", user.getSlackUserId(), err.getMessage()))
                .subscribe();
    }

    /**
     * 요약 생성 실패 시 사용자에게 관리자 문의 안내 메시지를 전송 (동기, 테스트용)
     */
    public void sendErrorNoticeSync(User user, LocalDate date, SummaryType type) {
        if (!slackEnabled) return;

        sendMessage(user.getSlackUserId(), formatErrorMessage(user, date, type))
                .doOnSuccess(resp -> log.debug("✅ 관리자 문의 안내 전송 완료: userId={}", user.getSlackUserId()))
                .doOnError(err -> log.warn("❌ 관리자 문의 안내 전송 실패: userId={}, reason={}", user.getSlackUserId(), err.getMessage()))
                .block();
    }

    /**
     * 실제 WebClient 요청을 생성하여 반환 (공통 처리)
     */
    private Mono<String> sendMessage(String channel, String message) {
        return slackWebClient.post()
                .uri("/chat.postMessage")
                .body(BodyInserters.fromValue(Map.of("channel", channel, "text", message)))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)));
    }

    /**
     * 요약 메시지 포맷 구성
     */
    private String formatSummaryMessage(Summary summary) {
        return String.format(
                "[%s 요약 📚]\n%s\n\n🌟 피드백:\n%s",
                summary.getType(),
                summary.getSummary(),
                summary.getFeedback() != null ? summary.getFeedback() : "피드백 없음"
        );
    }

    /**
     * 에러 안내 메시지 포맷 구성
     */
    private String formatErrorMessage(User user, LocalDate date, SummaryType type) {
        return String.format("""
                [%s 요약 ⚠️]
                %s님의 %s 요약 생성 중 오류가 발생했습니다.
                재시도 중이니 잠시만 기다려 주세요.
                반복적으로 실패하면 관리자에게 문의해주세요.
                """, type, user.getSlackUsername(), date);
    }
}

package com.jia.study_tracker.slack;

import com.jia.study_tracker.exception.HmacCalculationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class SlackRequestVerifier {

    @Value("${slack.signing-secret}")
    private String signingSecret;

    @Value("${slack.verify-signature:true}") // 기본값 true
    private boolean verifySignature;

    private static final long MAX_REQUEST_AGE_IN_SECONDS = 5L * 60; // 5분

    /**
     * 요청이 유효한지 검증하는 메서드
     * @param signature Slack에서 전달한 서명
     * @param timestamp Slack에서 전달한 타임스탬프
     * @param body 요청 본문
     * @return 유효한 요청인지 여부
     */
    public boolean isValid(String signature, String timestamp, String body) {
        // 인증 우회 조건
        if (!verifySignature) {
            return true;
        }

        try {
            // 타임스탬프가 너무 오래된 요청은 거부 (5분 초과)
            if (!isTimestampValid(timestamp)) {
                return false;
            }

            // Slack 서명 검증
            String baseString = "v0:" + timestamp + ":" + body;
            String mySignature = "v0=" + calculateHmacSHA256(baseString, signingSecret);
            return mySignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 타임스탬프가 유효한지 검사
     * @param timestamp Slack에서 전달한 타임스탬프
     * @return 타임스탬프가 유효한지 여부
     */
    private boolean isTimestampValid(String timestamp) {
        long requestTs = Long.parseLong(timestamp);
        long nowTs = System.currentTimeMillis() / 1000;

        // 타임스탬프가 5분 초과하면 거부
        return Math.abs(nowTs - requestTs) <= MAX_REQUEST_AGE_IN_SECONDS;
    }

    /**
     * HMAC-SHA256 방식으로 서명을 계산하는 메서드
     * @param data 서명에 사용할 데이터
     * @param secret Slack Signing Secret
     * @return 계산된 서명
     */
    private String calculateHmacSHA256(String data, String secret) {
        try {
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmac);
        } catch (Exception e) {
            throw new HmacCalculationException("HMAC-SHA256 계산 중 오류 발생", e);
        }
    }

    // 테스트할 때 signingSecret 직접 주입할 수 있게 만든 메소드
    public void setSigningSecret(String signingSecret) {
        this.signingSecret = signingSecret;
    }
    // 테스트할 때를 위한 메소드
    public void setVerifySignature(boolean verifySignature) {
        this.verifySignature = verifySignature;
    }

}

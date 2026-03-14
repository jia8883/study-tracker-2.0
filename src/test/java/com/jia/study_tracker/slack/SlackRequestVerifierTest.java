package com.jia.study_tracker.slack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SlackRequestVerifier 테스트
 *
 * 목표:
 * - Slack 서명 검증 기능이 올바르게 동작하는지 확인한다.
 *
 * 테스트 시나리오:
 * - 유효한 서명이 전달되었을 때 검증이 성공해야 한다.
 * - 오래된 timestamp가 전달되면 실패해야 한다.
 * - 서명이 잘못된 경우 실패해야 한다.
 *
 * 참고:
 * - 서명 검증은 Signing Secret을 기반으로 HMAC-SHA256 방식으로 이루어진다.
 * - 이 테스트에서는 Signing Secret을 dummy 값으로 설정하고,
 *   signature 및 timestamp를 제어해 검증한다.
 */
@ExtendWith(MockitoExtension.class)
public class SlackRequestVerifierTest {

    private SlackRequestVerifier slackRequestVerifier;

    private static final String DUMMY_SIGNING_SECRET = "dummy_secret";

    @BeforeEach
    void setUp() {
        slackRequestVerifier = new SlackRequestVerifier();
        slackRequestVerifier.setSigningSecret(DUMMY_SIGNING_SECRET);
        slackRequestVerifier.setVerifySignature(true);
    }

    // 유효한 슬랙 요청이 들어오면 true를 반환해야 한다.
    @Test
    void isValid_validRequest_returnsTrue() throws Exception {
        // Given
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000); // 현재 시간
        String body = "test_body";
        String baseString = "v0:" + timestamp + ":" + body;
        String validSignature = "v0=" + calculateHmacSHA256(baseString, DUMMY_SIGNING_SECRET);

        // When
        boolean isValid = slackRequestVerifier.isValid(validSignature, timestamp, body);

        // Then
        assertTrue(isValid);
    }

    // 오래된 timestamp가 전달되면 false를 반환해야 한다.
    @Test
    void isValid_invalidTimestamp_returnsFalse() throws Exception {
        // Given
        String oldTimestamp = "1234567890";
        String body = "test_body";
        String baseString = "v0:" + oldTimestamp + ":" + body;
        String validSignature = "v0=" + calculateHmacSHA256(baseString, DUMMY_SIGNING_SECRET);

        // When
        boolean isValid = slackRequestVerifier.isValid(validSignature, oldTimestamp, body);

        // Then
        assertFalse(isValid);
    }

    // 잘못된 서명이 전달되면 false를 반환해야 한다.
    @Test
    void isValid_invalidSignature_returnsFalse() {
        // Given
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String body = "test_body";
        String invalidSignature = "v0=invalidsignature";

        // When
        boolean isValid = slackRequestVerifier.isValid(invalidSignature, timestamp, body);

        // Then
        assertFalse(isValid);
    }

    // 테스트용 HMAC-SHA256 서명 생성 메서드
    private String calculateHmacSHA256(String data, String secret) throws Exception {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hmac);
    }
}
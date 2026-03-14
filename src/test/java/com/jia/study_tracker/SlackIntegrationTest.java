package com.jia.study_tracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jia.study_tracker.repository.StudyLogRepository;
import com.jia.study_tracker.repository.UserRepository;
import com.jia.study_tracker.slack.SlackEventPayload;
import com.jia.study_tracker.slack.SlackEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slack 요청, DB 저장 기능에 대한 통합 테스트
 */

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SlackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyLogRepository studyLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${slack.signing-secret}")
    private String signingSecret;

    @Test
    void slackMessageEvent_createsUserAndStudyLog() throws Exception {
        // Given
        String slackUserId = "user123";
        String message = "스프링을 공부했다";
        SlackEventPayload.Event event = new SlackEventPayload.Event("message", slackUserId, message, null);
        SlackEventPayload payload = new SlackEventPayload(SlackEventType.EVENT_CALLBACK, null, event);

        String requestBody = objectMapper.writeValueAsString(payload);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = generateSlackSignature(timestamp, requestBody);

        // When
        mockMvc.perform(post("/slack/events")
                        .header("X-Slack-Signature", signature)
                        .header("X-Slack-Request-Timestamp", timestamp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Then
        assertTrue(userRepository.findById(slackUserId).isPresent());
        assertFalse(studyLogRepository.findAll().isEmpty());
    }

    // 테스트용 HMAC-SHA256 서명 생성 메서드
    private String generateSlackSignature(String timestamp, String body) throws Exception {
        String baseString = "v0:" + timestamp + ":" + body;
        SecretKeySpec key = new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        byte[] hmac = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
        return "v0=" + HexFormat.of().formatHex(hmac);
    }

}

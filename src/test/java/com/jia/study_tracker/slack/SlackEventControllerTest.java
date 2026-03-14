package com.jia.study_tracker.slack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SlackEventController 테스트
 *
 * 목표: HTTP 요청이 잘 처리되는지 확인한다.
 *
 * 테스트 시나리오:
 * 1. 유효한 슬랙 요청이 들어오면 200 OK 응답을 반환한다.
 * 2. 유효하지 않은 슬랙 요청이 들어오면 401 Unauthorized 응답을 반환한다.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(SlackEventController.class)
public class SlackEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SlackEventService slackEventService;

    @MockBean
    private SlackRequestVerifier slackRequestVerifier;

    // 유효한 슬랙 요청이 들어오면 200 OK 응답을 반환하는지 테스트
    @Test
    void receiveEvent_validRequest_returnsOk() throws Exception {
        // Given
        String signature = "validSignature";
        String timestamp = "validTimestamp";
        String body = "{}";

        Mockito.when(slackRequestVerifier.isValid(signature, timestamp, body)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/slack/events")
                        .header("X-Slack-Signature", signature)
                        .header("X-Slack-Request-Timestamp", timestamp)
                        .content(body))
                .andExpect(status().isOk());
    }

    // 유효하지 않은 슬랙 요청이 들어오면 401 Unauthorized 응답을 반환하는지 테스트
    @Test
    void receiveEvent_invalidRequest_returnsUnauthorized() throws Exception {
        // Given
        String signature = "invalidSignature";
        String timestamp = "invalidTimestamp";
        String body = "{}";

        Mockito.when(slackRequestVerifier.isValid(signature, timestamp, body)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/slack/events")
                        .header("X-Slack-Signature", signature)
                        .header("X-Slack-Request-Timestamp", timestamp)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}

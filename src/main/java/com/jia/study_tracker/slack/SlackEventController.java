package com.jia.study_tracker.slack;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
@Slf4j
public class SlackEventController {

    private final SlackEventService slackEventService;
    private final SlackRequestVerifier slackRequestVerifier;
    private final ObjectMapper objectMapper;


    @PostMapping("/events")
    public ResponseEntity<String> receiveEvent(
            @RequestHeader("X-Slack-Signature") String signature,
            @RequestHeader("X-Slack-Request-Timestamp") String timestamp,
            @RequestBody String body) {

        if (!slackRequestVerifier.isValid(signature, timestamp, body)) {
            log.warn("유효하지 않은 슬랙 요청입니다. Signature: {}, Timestamp: {}", signature, timestamp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid request signature");
        }

        try {
            SlackEventPayload payload = objectMapper.readValue(body, SlackEventPayload.class);
            String result = slackEventService.handleEvent(payload);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("페이로드 처리 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }
    }


}


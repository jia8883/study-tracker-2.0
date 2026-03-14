package com.jia.study_tracker.slack;

import com.jia.study_tracker.domain.StudyLog;
import com.jia.study_tracker.domain.User;
import com.jia.study_tracker.filter.StudyMessageFilter;
import com.jia.study_tracker.service.UserService;
import com.jia.study_tracker.repository.StudyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Slack 이벤트를 처리하는 서비스 클래스
 *
 * 기능:
 * - url_verification 요청 응답
 * - message 이벤트 감지 및 StudyLog 저장
 * - 트랜잭션으로 사용자 등록과 로그 저장을 하나의 단위로 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlackEventService {

    private final UserService userService;
    private final StudyLogRepository studyLogRepository;
    private final StudyMessageFilter studyMessageFilter;

    @Transactional
    public String handleEvent(SlackEventPayload payload) {
        return switch (payload.getType()) {
            case URL_VERIFICATION -> handleUrlVerification(payload);
            case EVENT_CALLBACK -> handleEventCallback(payload);
        };
    }

    private String handleUrlVerification(SlackEventPayload payload) {
        return payload.getChallenge();
    }

    private String handleEventCallback(SlackEventPayload payload) {
        SlackEventPayload.Event event = payload.getEvent();

        // 봇 메시지라면 무시
        if (event.getBot_id() != null) {
            log.debug("봇 메시지는 무시됩니다: {}", event.getText());
            return "ok";
        }

        if ("message".equals(event.getType())) {
            return handleMessageEvent(event);
        }

        return "ok";
    }

    private String handleMessageEvent(SlackEventPayload.Event event) {
        String slackUserId = event.getUser();
        String text = event.getText();

        if (!studyMessageFilter.isStudyRelated(text)) {
            log.info("🚫 저장되지 않은 메시지: {}", text);
            return "학습과 관련 없는 메시지는 저장되지 않습니다.";
        }

        User user = userService.findOrCreateUser(slackUserId, "unknown");
        StudyLog studyLog = new StudyLog(text, LocalDateTime.now(), user);
        studyLogRepository.save(studyLog);

        log.info("💾 저장된 메시지: {}", text);
        return "ok";
    }
}


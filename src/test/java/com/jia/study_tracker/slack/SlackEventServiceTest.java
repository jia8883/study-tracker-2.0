package com.jia.study_tracker.slack;

import com.jia.study_tracker.domain.StudyLog;
import com.jia.study_tracker.domain.User;
import com.jia.study_tracker.filter.StudyMessageFilter;
import com.jia.study_tracker.repository.StudyLogRepository;
import com.jia.study_tracker.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SlackEventService 테스트
 *
 * 목표:
 * - 슬랙 이벤트가 잘 처리되는지 검증한다.
 *
 * 테스트 시나리오:
 * 1. 슬랙에서 메시지가 오면 StudyLog에 저장되어야 한다.
 * 2. 슬랙에서 url_verification 이벤트가 오면 challenge를 반환해야 한다.
 */
@ExtendWith(MockitoExtension.class)
public class SlackEventServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private StudyLogRepository studyLogRepository;

    @Mock
    private StudyMessageFilter studyMessageFilter;

    @InjectMocks
    private SlackEventService slackEventService;

    // 슬랙 메시지 이벤트를 처리해서 StudyLog에 저장하는지 검증
    @Test
    void handleEvent_messageEvent_savesStudyLog() {
        // Given
        String slackUserId = "user123";
        String message = "스프링을 공부했다";
        SlackEventPayload.Event event = new SlackEventPayload.Event("message", slackUserId, message, null);
        SlackEventPayload payload = new SlackEventPayload(SlackEventType.EVENT_CALLBACK, null, event);

        User user = new User(slackUserId, "testUser");
        Mockito.when(userService.findOrCreateUser(slackUserId, "unknown")).thenReturn(user);
        Mockito.when(studyMessageFilter.isStudyRelated(Mockito.anyString())).thenReturn(true);

        // When
        String response = slackEventService.handleEvent(payload);

        // Then
        Mockito.verify(studyLogRepository).save(Mockito.any(StudyLog.class));
        assertEquals("ok", response);
    }

    // 슬랙 url_verification 이벤트가 오면 challenge 값을 반환하는지 검증
    @Test
    void handleEvent_urlVerification_returnsChallenge() {
        // Given
        SlackEventPayload payload = new SlackEventPayload(SlackEventType.URL_VERIFICATION, "testChallenge", null);

        // When
        String response = slackEventService.handleEvent(payload);

        // Then
        assertEquals("testChallenge", response);
    }

}

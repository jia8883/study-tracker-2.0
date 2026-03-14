package com.jia.study_tracker.service;

import com.jia.study_tracker.domain.User;
import com.jia.study_tracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


/**
 * UserService 테스트
 *
 * 목표:
 * - findOrCreateUser 메서드가 정상 동작하는지 검증한다.
 *
 * 테스트 시나리오:
 * 1. 사용자가 DB에 존재하는 경우, 해당 사용자를 반환해야 한다.
 * 2. 사용자가 DB에 존재하지 않는 경우, 새로운 사용자를 생성하여 반환해야 한다.
 *
 * 주의사항:
 * - 동시성 테스트는 별도의 추가 테스트 케이스로 다룰 수 있으며,
 *   여기서는 기본 동작만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // 기존 유저가 존재하는 경우, 해당 유저를 반환하는지 테스트
    @Test
    void findOrCreateUser_existingUser_returnsUser() {
        // Given
        String slackUserId = "user123";
        String slackUsername = "testUser";
        User existingUser = new User(slackUserId, slackUsername);
        Mockito.when(userRepository.findById(slackUserId)).thenReturn(Optional.of(existingUser));

        // When
        User user = userService.findOrCreateUser(slackUserId, slackUsername);

        // Then
        assertEquals(slackUserId, user.getSlackUserId());
        assertEquals(slackUsername, user.getSlackUsername());
    }

    // 새로운 유저가 없는 경우, 새로 생성하고 반환하는지 테스트
    @Test
    void findOrCreateUser_newUser_createsAndReturnsUser() {
        // Given
        String slackUserId = "user123";
        String slackUsername = "newUser";
        Mockito.when(userRepository.findById(slackUserId)).thenReturn(Optional.empty());

        // save 메서드를 목(mock)으로 처리
        User newUser = new User(slackUserId, slackUsername);
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(newUser);

        // When
        User user = userService.findOrCreateUser(slackUserId, slackUsername);

        // Then
        assertNotNull(user);
        assertEquals(slackUserId, user.getSlackUserId());
        assertEquals(slackUsername, user.getSlackUsername());
    }

}

package com.jia.study_tracker.service;

import com.jia.study_tracker.domain.User;
import com.jia.study_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 유저가 존재하지 않으면 새로 생성해서 리턴합니다.
     * 동시에 여러 요청이 들어올 수 있으므로 예외 재시도로 처리합니다.
     */

    @Transactional
    public User findOrCreateUser(String slackUserId, String slackUsername) {
        return userRepository.findById(slackUserId)
                .orElseGet(() -> tryCreateUser(slackUserId, slackUsername));
    }

    private User tryCreateUser(String slackUserId, String slackUsername) {
        try {
            return userRepository.save(new User(slackUserId, slackUsername));
        } catch (DataIntegrityViolationException e) {
            return userRepository.findById(slackUserId)
                    .orElseThrow(() -> new IllegalStateException("동시성 문제로 사용자 생성 실패", e));
        }
    }

}

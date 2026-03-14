package com.jia.study_tracker.exception;

/**
 * 네트워크 오류, 인증 실패 등 OpenAI API 호출 자체가 실패했을 때 발생하는 예외
 */
public class OpenAIClientException extends RuntimeException {
    public OpenAIClientException(String message, Throwable cause) {
        super(message, cause);
    }
}


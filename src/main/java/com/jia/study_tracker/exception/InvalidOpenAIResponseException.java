package com.jia.study_tracker.exception;

/**
 * OpenAI API 호출이 됐으나 응답이 비어 있거나, 예상한 형식(요약:, 피드백:)이 아닐 때 발생하는 예외
 */
public class InvalidOpenAIResponseException extends RuntimeException {
  public InvalidOpenAIResponseException(String message) {
    super(message);
  }
}
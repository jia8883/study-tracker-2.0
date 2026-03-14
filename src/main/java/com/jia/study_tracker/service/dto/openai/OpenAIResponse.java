package com.jia.study_tracker.service.dto.openai;

import java.util.List;

/**
 * OpenAI API의 응답을 담는 DTO.
 * 응답의 주요 내용은 Choice 객체 내부의 Message에 포함됨.
 */
public record OpenAIResponse(List<Choice> choices) {

    // 실제 메시지는 내부 message 필드에 존재.
    public record Choice(Message message) {}
}

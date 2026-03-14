package com.jia.study_tracker.service.dto.openai;

import java.util.List;

// OpenAI API에 보낼 요청 데이터를 담는 DTO. (모델명 + 메시지 리스트)
public record OpenAIRequest(String model, List<Message> messages) {}

package com.jia.study_tracker.service.dto.openai;

// OpenAI API에 전달하거나 응답받은 메시지를 나타내는 DTO
// 역할(role): user (사용자), system (지시문), assistant (AI 응답) / 내용(content): 메시지 내용
public record Message(String role, String content) {}

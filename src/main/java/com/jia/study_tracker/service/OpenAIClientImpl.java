package com.jia.study_tracker.service;

import com.jia.study_tracker.domain.StudyLog;
import com.jia.study_tracker.exception.InvalidOpenAIResponseException;
import com.jia.study_tracker.exception.OpenAIClientException;
import com.jia.study_tracker.service.dto.SummaryResult;
import com.jia.study_tracker.service.dto.openai.Message;
import com.jia.study_tracker.service.dto.openai.OpenAIRequest;
import com.jia.study_tracker.service.dto.openai.OpenAIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자의 학습 로그를 OpenAI API에 전달하여 요약 및 피드백을 생성하는 컴포넌트
 *
 * 역할:
 * - StudyLog 리스트를 문자열로 변환 후 프롬프트 형태로 구성
 * - OpenAI의 Chat Completion API에 요청을 보내고 응답을 파싱
 * - 요약 및 피드백을 추출하여 SummaryResult 객체로 반환
 */
@Component
@Profile("!mock-openai")
@RequiredArgsConstructor
@Slf4j
public class OpenAIClientImpl implements OpenAIClient {

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final WebClient openAIWebClient;

    /**
     * 학습 로그 리스트를 받아 OpenAI에 요청하고 요약 및 피드백을 생성
     */
    public SummaryResult generateSummaryAndFeedback(List<StudyLog> logs) {
        // StudyLog의 content만 추출하여 한 개의 문자열로 결합
        String joinedContent = logs.stream()
                .map(StudyLog::getContent)
                .collect(Collectors.joining("\n"));

        // OpenAI에게 전달할 프롬프트 구성
        String prompt = String.format("""
                다음은 사용자의 학습 로그입니다:
                ---
                %s
                ---

                위 내용을 3~4문장 이내로 요약해주세요. 그리고 학습을 응원하는 동기부여 성격의 짧은 피드백을 함께 작성해주세요.
                출력 형식은 다음과 같이 해주세요:

                요약: ~~~
                피드백: ~~~
                """, joinedContent);

        // DTO 기반 요청 생성
        OpenAIRequest request = new OpenAIRequest(
                model,
                List.of(
                        new Message("system", "나는 친절한 학습 요약 봇이야."),
                        new Message("user", prompt)
                )
        );

        try {
            // DTO 기반 응답 처리
            OpenAIResponse response = openAIWebClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenAIResponse.class)
                    .block();

            if (response.choices().isEmpty()) {
                throw new InvalidOpenAIResponseException("OpenAI 응답은 왔지만 내용(choices)이 비어 있음");
            }

            // 응답에서 content 추출
            String content = response.choices().get(0).message().content();
            if (!content.contains("요약:") || !content.contains("피드백:")) {
                throw new InvalidOpenAIResponseException("응답 형식이 올바르지 않음");
            }
            // content 문자열에서 요약과 피드백 분리
            String[] parts = content.split("피드백:");

            String summary = parts[0].replace("요약:", "").trim();
            String feedback = parts.length > 1 ? parts[1].trim() : "피드백 없음";

            return new SummaryResult(summary, feedback);

        } catch (InvalidOpenAIResponseException e) {
            log.warn("OpenAI 응답 파싱 실패", e);
            throw e; // 다시 던져서 SummaryGenerationService에서 처리 가능하게
        } catch (WebClientException e) {
            log.error("WebClient 오류로 OpenAI API 호출 실패", e);
            throw new OpenAIClientException("WebClient 오류로 OpenAI API 호출 실패", e);
        } catch (Exception e) {
            log.error("OpenAIClient 내부 처리 중 알 수 없는 예외 발생 - 요청 프롬프트 처리 또는 응답 파싱 과정 문제일 수 있음", e);
            throw new OpenAIClientException("알 수 없는 오류로 OpenAI 호출 실패", e);
        }
    }
}

package com.sa.baff.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Anthropic API 클라이언트
 * - 나만그래 AiApiServiceImpl 패턴 참조
 * - Haiku + Sonnet 병렬 호출 지원
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnthropicApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.claude.api-key:}")
    private String apiKey;

    @Value("${ai.claude.base-url:https://api.anthropic.com/v1}")
    private String baseUrl;

    @Value("${ai.claude.model-haiku:claude-haiku-4-5-20251001}")
    private String haikuModel;

    @Value("${ai.claude.model-sonnet:claude-sonnet-4-5-20241022}")
    private String sonnetModel;

    /** Haiku + Sonnet 병렬 호출, 두 결과를 함께 반환 */
    public DualAnalysisResult analyzeDual(String systemPrompt, String userPrompt) {
        CompletableFuture<String> haikuFuture = CompletableFuture.supplyAsync(
                () -> callApi(haikuModel, systemPrompt, userPrompt));
        CompletableFuture<String> sonnetFuture = CompletableFuture.supplyAsync(
                () -> callApi(sonnetModel, systemPrompt, userPrompt));

        try {
            CompletableFuture.allOf(haikuFuture, sonnetFuture).join();
            return new DualAnalysisResult(haikuFuture.get(), sonnetFuture.get());
        } catch (Exception e) {
            log.error("[AnthropicApi] 병렬 호출 실패: {}", e.getMessage(), e);
            // 개별 결과라도 반환
            String haiku = null;
            String sonnet = null;
            try { haiku = haikuFuture.get(); } catch (Exception ignored) {}
            try { sonnet = sonnetFuture.get(); } catch (Exception ignored) {}
            return new DualAnalysisResult(haiku, sonnet);
        }
    }

    private String callApi(String model, String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", 1024,
                    "system", systemPrompt,
                    "messages", List.of(
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            String responseJson = webClient.post()
                    .uri(baseUrl + "/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode contentArray = root.get("content");
            if (contentArray != null && contentArray.isArray() && !contentArray.isEmpty()) {
                return contentArray.get(0).get("text").asText();
            }
            return null;
        } catch (Exception e) {
            log.error("[AnthropicApi] {} 호출 실패: {}", model, e.getMessage(), e);
            return null;
        }
    }

    @Getter
    @Setter
    public static class DualAnalysisResult {
        private String haikuResult;
        private String sonnetResult;

        public DualAnalysisResult(String haikuResult, String sonnetResult) {
            this.haikuResult = haikuResult;
            this.sonnetResult = sonnetResult;
        }
    }
}

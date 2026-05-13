//package com.z.finance.tracker.client;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.time.Duration;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class DeepseekClient {
//
//    @Value("${deepseek-ai.api-key}")
//    private String apiKey;
//
//    @Value("${deepseek-ai.model}")
//    private String model;
//
//    private final HttpClient httpClient = HttpClient.newBuilder()
//        .connectTimeout(Duration.ofSeconds(30))
//        .build();
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    public String chat(List<Map<String, String>> messages) throws Exception {
//        Map<String, Object> requestBody = Map.of(
//            "model", model,
//            "messages", messages,
//            "stream", false,
//            "max_tokens", 1024
//        );
//
//        String json = objectMapper.writeValueAsString(requestBody);
//
//        HttpRequest request = HttpRequest.newBuilder()
//            .uri(URI.create("https://api.deepseek.com/chat/completions"))
//            .header("Content-Type", "application/json")
//            .header("Authorization", "Bearer " + apiKey)
//            .timeout(Duration.ofSeconds(60))
//            .POST(HttpRequest.BodyPublishers.ofString(json))
//            .build();
//
//        HttpResponse<String> response = httpClient.send(
//            request,
//            HttpResponse.BodyHandlers.ofString()
//        );
//
//        if (response.statusCode() != 200) {
//            throw new Exception("Deepseek API error: " + response.statusCode() + " " + response.body());
//        }
//
//        // Deepseek uses OpenAI format:
//        // { "choices": [ { "message": { "content": "..." } } ] }
//        Map<?, ?> parsed = objectMapper.readValue(response.body(), Map.class);
//        List<?> choices = (List<?>) parsed.get("choices");
//        Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
//        return (String) message.get("content");
//    }
//}
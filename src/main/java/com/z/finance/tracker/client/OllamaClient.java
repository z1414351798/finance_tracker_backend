package com.z.finance.tracker.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class OllamaClient {

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.model}")
    private String model;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Send a conversation (list of messages) to Ollama.
     * Each message is a Map with "role" and "content".
     * Returns the assistant reply as a plain String.
     */
    public String chat(List<Map<String, String>> messages) throws Exception {
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", messages,
            "stream", false
        );

        String json = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/chat"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString()
        );

        // Parse: { "message": { "content": "..." } }
        Map<?, ?> parsed = objectMapper.readValue(response.body(), Map.class);
        Map<?, ?> message = (Map<?, ?>) parsed.get("message");
        return (String) message.get("content");
    }
}
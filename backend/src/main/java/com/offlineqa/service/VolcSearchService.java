package com.offlineqa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class VolcSearchService {

    private final WebClient webClient;
    private final String enabled;
    private final String apiKey;

    public VolcSearchService(@Value("${app.volc.search-enabled:false}") String enabled,
                             @Value("${app.volc.base-url:}") String baseUrl,
                             @Value("${app.volc.api-key:}") String apiKey) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public String factCheck(String question) {
        if (!Boolean.parseBoolean(enabled) || apiKey == null || apiKey.isBlank()) {
            return "";
        }
        try {
            Map<?, ?> resp = webClient.post()
                    .uri("/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(Map.of("query", question, "topK", 3))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return resp == null ? "" : resp.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}

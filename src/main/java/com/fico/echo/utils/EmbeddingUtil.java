package com.fico.echo.utils;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmbeddingUtil {
    private static final String LLAMA_EMBED_URL = "http://localhost:11434/api/embeddings";

    public static List<Double> getEmbedding(String text) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama3");
        body.put("prompt", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(LLAMA_EMBED_URL, HttpMethod.POST, request, Map.class);
        return (List<Double>) response.getBody().get("embedding");
    }

    public static double cosineSimilarity(List<Double> a, List<Double> b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += Math.pow(a.get(i), 2);
            normB += Math.pow(b.get(i), 2);
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}


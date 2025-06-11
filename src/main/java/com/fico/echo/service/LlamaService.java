package com.fico.echo.service;

import com.fico.echo.model.ChunkData;
import com.fico.echo.utils.EmbeddingUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.fico.echo.utils.PDFReader.loadAllPdfChunks;

@Service
public class LlamaService {

    private static final String LLAMA_API_URL = "http://localhost:11434/api/generate";
    private final List<String> pdfChunks = new ArrayList<>();
    private List<ChunkData> embeddedChunks = new ArrayList<>();

    @PostConstruct
    public void init() throws IOException {
        loadAllPdfChunks(pdfChunks);
    }

    public String askLlama(String userQuestion) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", "llama3");
        request.put("prompt", buildPrompt(userQuestion));
        request.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map> response = new RestTemplate().postForEntity(LLAMA_API_URL, entity, Map.class);
        return Objects.requireNonNull(response.getBody()).get("response").toString();
    }


    private String buildPrompt(String question) {
        StringBuilder prompt = new StringBuilder("Use the following product documentation to answer:\n\n");
        for (String chunk : pdfChunks) {
            prompt.append(chunk).append("\n\n");
        }
        prompt.append("Q: ").append(question);
        return prompt.toString();
    }




//    public String askQuestion(String question) {
//        List<Double> questionEmbedding = EmbeddingUtil.getEmbedding(question);
//
//        // Calculate similarity scores for each chunk
//        List<Map.Entry<ChunkData, Double>> scoredChunks = embeddedChunks.stream()
//                .map(chunk -> Map.entry(chunk, EmbeddingUtil.cosineSimilarity(questionEmbedding, chunk.getEmbedding())))
//                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
//                .collect(Collectors.toList());
//
//        // Set similarity threshold
//        double threshold = 0.75;
//        boolean isRelevant = !scoredChunks.isEmpty() && scoredChunks.get(0).getValue() >= threshold;
//
//        String finalPrompt;
//
//        if (isRelevant) {
//            List<String> topChunkTexts = scoredChunks.stream()
//                    .limit(4)
//                    .map(e -> e.getKey().getText())
//                    .collect(Collectors.toList());
//            finalPrompt = buildPrompt(question, topChunkTexts);
//        } else {
//            // Fallback to general LLaMA response
//            finalPrompt = question;
//        }
//
//        // Call LLaMA API
//        RestTemplate restTemplate = new RestTemplate();
//        String ollamaUrl = "http://localhost:11434/api/generate";
//
//        Map<String, Object> body = new HashMap<>();
//        body.put("model", "llama3");
//        body.put("prompt", finalPrompt);
//        body.put("stream", false);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
//
//        ResponseEntity<Map> response = restTemplate.exchange(ollamaUrl, HttpMethod.POST, request, Map.class);
//        return response.getBody().get("response").toString();
//    }
//
//    private String buildPrompt(String question, List<String> topChunks) {
//        StringBuilder prompt = new StringBuilder("Use the following product documentation to answer:\n\n");
//        for (String chunk : topChunks) {
//            prompt.append(chunk).append("\n\n");
//        }
//        prompt.append("Q: ").append(question);
//        return prompt.toString();
//    }
}

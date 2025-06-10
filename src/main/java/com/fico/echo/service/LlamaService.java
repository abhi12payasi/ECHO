package com.fico.echo.service;

import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlamaService {

    private final RestTemplate restTemplate = new RestTemplate();
    private List<String> pdfChunks = new ArrayList<>();
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";

    @PostConstruct
    public void init() throws IOException {
        File pdfFile = new ClassPathResource("docs/SmartHealth360_Architecture.pdf").getFile();
        this.pdfChunks = extractChunksFromPdf(pdfFile, 1000);
    }


    public String askLlama(String userQuestion) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", "llama3");
        request.put("prompt", buildPrompt(userQuestion));
        request.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(OLLAMA_API_URL, entity, Map.class);
        String resp =  response.getBody().get("response").toString();
        return prettifyResponse(resp);
    }

    private List<String> extractChunksFromPdf(File file, int chunkSize) throws IOException {
        PDDocument document = PDDocument.load(file);
        PDFTextStripper stripper = new PDFTextStripper();
        String fullText = stripper.getText(document);
        document.close();

        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < fullText.length(); i += chunkSize) {
            int end = Math.min(fullText.length(), i + chunkSize);
            chunks.add(fullText.substring(i, end));
        }
        return chunks;
    }

    private String buildPrompt(String question) {
        StringBuilder prompt = new StringBuilder("Use the following product documentation to answer:\n\n");
        for (String chunk : pdfChunks) {
            prompt.append(chunk).append("\n\n");
        }
        prompt.append("Q: ").append(question);
        return prompt.toString();
    }
    private String prettifyResponse(String raw) {
        // Basic trimming and cleanup
        String cleaned = raw.trim();

        // Optional: Add line breaks after periods
        cleaned = cleaned.replaceAll("(?<=[.?!])\\s+", "\n");

        // Optional: Capitalize first letter
        if (!cleaned.isEmpty()) {
            cleaned = cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
        }

        return cleaned;
    }


}

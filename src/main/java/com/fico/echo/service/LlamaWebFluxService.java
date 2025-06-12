package com.fico.echo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fico.echo.model.ChunkData;
import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlamaWebFluxService {

    private final WebClient webClient;
    private static final String LLAMA_API_URL = "http://localhost:11434/api/generate";
    private final List<String> pdfChunks = new ArrayList<>();
    private List<ChunkData> embeddedChunks = new ArrayList<>();

    @PostConstruct
    public void init() throws IOException {
        loadAllPdfChunks(pdfChunks);
    }

    public LlamaWebFluxService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:11434")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String askLlama(String userQuestion) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", "llama3");
        request.put("prompt", buildPrompt(userQuestion));
        request.put("stream", true); // ✅ streaming enabled

        long start = System.currentTimeMillis();
        // Ollama returns NDJSON where each line is a JSON object with partial output
        Flux<String> responseFlux = webClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class);

        StringBuilder completeResponse = new StringBuilder();
        start = System.currentTimeMillis();
        responseFlux
                .map(this::extractTextFromNdjson)
                .doOnNext(completeResponse::append)
                .blockLast(); // blocks until stream ends
        return completeResponse.toString();
    }

    private String extractTextFromNdjson(String line) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(line);
            return node.get("response").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private String buildPrompt(String userQuestion) {
        StringBuilder prompt = new StringBuilder("Use the following product documentation to answer:\n\n");
        for (String chunk : pdfChunks) {
            prompt.append(chunk).append("\n\n");
        }
        prompt.append("Q: ").append(userQuestion);
        return prompt.toString();
    }

    public static void loadAllPdfChunks(List<String> pdfChunks) throws IOException {
        File folder = new ClassPathResource("docs").getFile();
        File[] pdfFiles = folder.listFiles((dir, name) -> name.endsWith(".pdf"));
        assert pdfFiles != null;
        for (File pdfFile : pdfFiles) {
            pdfChunks.addAll(extractChunksFromPdf(pdfFile, 1000));
        }
    }

    private static List<String> extractChunksFromPdf(File file, int chunkSize) throws IOException {
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

    public Flux<String> streamLlamaResponse(String question) {
        WebClient webClient = WebClient.builder().baseUrl("http://localhost:11434").build();

        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "model", "llama3",
                        "prompt", buildPrompt(question),
                        "stream", true
                ))
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::extractTextFromNdjson);
    }

}

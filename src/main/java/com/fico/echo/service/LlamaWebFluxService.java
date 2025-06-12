package com.fico.echo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fico.echo.model.ChunkData;
import com.fico.echo.utils.EmbeddingUtil;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LlamaWebFluxService {

    private final WebClient webClient;
    private static final String LLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final double SIMILARITY_THRESHOLD = 0.3; // Minimum similarity for relevance
    private static final int TOP_K_CHUNKS = 5; // Number of top chunks to retrieve

    private List<ChunkData> embeddedChunks = new ArrayList<>();

    @PostConstruct
    public void init() throws IOException {
        loadAndEmbedAllPdfChunks();
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
        request.put("prompt", buildPromptWithRelevantChunks(userQuestion));
        request.put("stream", true);

        long start = System.currentTimeMillis();
        Flux<String> responseFlux = webClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class);

        StringBuilder completeResponse = new StringBuilder();
        responseFlux
                .map(this::extractTextFromNdjson)
                .doOnNext(completeResponse::append)
                .blockLast();

        System.out.println("Response time: " + (System.currentTimeMillis() - start) + "ms");
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

    private String buildPromptWithRelevantChunks(String userQuestion) {
        List<ChunkData> relevantChunks = findRelevantChunks(userQuestion);

        StringBuilder prompt = new StringBuilder();

        if (relevantChunks.isEmpty()) {
            // No relevant chunks found, let Llama use its own training
            prompt.append("Please answer the following question based on your knowledge:\n\n");
            prompt.append("Q: ").append(userQuestion);
        } else {
            // Use relevant chunks as context
            prompt.append("Use the following relevant documentation to answer the question. ");
            prompt.append("If the documentation doesn't contain the answer, you can use your general knowledge:\n\n");

            for (int i = 0; i < relevantChunks.size(); i++) {
                ChunkData chunk = relevantChunks.get(i);
                prompt.append("").append(i + 1)
                        .append(" (similarity: ").append(String.format("%.3f", chunk.getSimilarity())).append("):\n")
                        .append(chunk.getContent()).append("\n\n");
            }

            prompt.append("Q: ").append(userQuestion);
        }

        return prompt.toString();
    }

    private List<ChunkData> findRelevantChunks(String query) {
        try {
            // Get embedding for the user query
            List<Double> queryEmbedding = EmbeddingUtil.getEmbedding(query);

            // Calculate similarity scores for all chunks
            List<ChunkData> scoredChunks = embeddedChunks.stream()
                    .peek(chunk -> {
                        double similarity = EmbeddingUtil.cosineSimilarity(queryEmbedding, chunk.getEmbedding());
                        chunk.setSimilarity(similarity);
                    })
                    .filter(chunk -> chunk.getSimilarity() >= SIMILARITY_THRESHOLD) // Filter by threshold
                    .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity())) // Sort by similarity desc
                    .limit(TOP_K_CHUNKS) // Take top K
                    .collect(Collectors.toList());

            System.out.println("Found " + scoredChunks.size() + " relevant chunks out of " + embeddedChunks.size() + " total chunks");
            scoredChunks.forEach(chunk ->
                    System.out.println("Chunk similarity: " + String.format("%.3f", chunk.getSimilarity())));

            return scoredChunks;

        } catch (Exception e) {
            System.err.println("Error finding relevant chunks: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void loadAndEmbedAllPdfChunks() throws IOException {
        System.out.println("Loading and embedding PDF chunks...");
        List<String> pdfChunks = new ArrayList<>();
        loadAllPdfChunks(pdfChunks);

        int totalChunks = pdfChunks.size();
        System.out.println("Processing " + totalChunks + " chunks for embedding...");

        for (int i = 0; i < pdfChunks.size(); i++) {
            String chunkText = pdfChunks.get(i);
            try {
                List<Double> embedding = EmbeddingUtil.getEmbedding(chunkText);
                ChunkData chunkData = new ChunkData();
                chunkData.setContent(chunkText);
                chunkData.setEmbedding(embedding);
                embeddedChunks.add(chunkData);

                if ((i + 1) % 10 == 0) {
                    System.out.println("Processed " + (i + 1) + "/" + totalChunks + " chunks");
                }
            } catch (Exception e) {
                System.err.println("Failed to embed chunk " + i + ": " + e.getMessage());
            }
        }

        System.out.println("Successfully embedded " + embeddedChunks.size() + " chunks");
    }

//

    public static void loadAllPdfChunks(List<String> pdfChunks) throws IOException {
        File docsFolder = new ClassPathResource("docs").getFile();
        Path docsPath = docsFolder.toPath();

        try (Stream<Path> paths = Files.walk(docsPath)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                    .forEach(path -> {
                        try {
                            pdfChunks.addAll(extractChunksFromPdf(path.toFile(), 1000));
                        } catch (IOException e) {
                            // You can log or handle individual file errors here
                            e.printStackTrace();
                        }
                    });
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
            String chunk = fullText.substring(i, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    public Flux<String> streamLlamaResponse(String question) {
        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "model", "llama3",
                        "prompt", buildPromptWithRelevantChunks(question),
                        "stream", true
                ))
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::extractTextFromNdjson);
    }

    // Utility methods for debugging and monitoring
    public int getTotalChunksCount() {
        return embeddedChunks.size();
    }

    public void setSimilarityThreshold(double threshold) {
        // This could be made configurable via application properties
        System.setProperty("similarity.threshold", String.valueOf(threshold));
    }
}
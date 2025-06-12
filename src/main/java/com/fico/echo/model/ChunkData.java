package com.fico.echo.model;

import java.util.List;

public class ChunkData {
    private String content;
    private List<Double> embedding;
    private double similarity; // Similarity score for ranking
    private String sourceFile; // Optional: track which PDF this came from
    private int chunkIndex; // Optional: track chunk position in original document

    public ChunkData() {}

    public ChunkData(String content, List<Double> embedding) {
        this.content = content;
        this.embedding = embedding;
    }

    // Getters and setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    @Override
    public String toString() {
        return "ChunkData{" +
                "content='" + (content != null ? content.substring(0, Math.min(100, content.length())) + "..." : null) + '\'' +
                ", similarity=" + similarity +
                ", sourceFile='" + sourceFile + '\'' +
                ", chunkIndex=" + chunkIndex +
                '}';
    }
}
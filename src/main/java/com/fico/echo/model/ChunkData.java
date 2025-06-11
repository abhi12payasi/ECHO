package com.fico.echo.model;

import java.util.List;

public class ChunkData {
    private String text;
    private List<Double> embedding;

    ChunkData(String text, List<Double> embedding) {
        this.text = text;
        this.embedding = embedding;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }
}
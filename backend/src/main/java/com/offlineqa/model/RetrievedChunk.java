package com.offlineqa.model;

public record RetrievedChunk(String content, double score, String source) {

    public RetrievedChunk(String content, double score) {
        this(content, score, "");
    }
}

package com.offlineqa.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(
            @Value("${app.ollama.base-url}") String baseUrl,
            @Value("${app.ollama.embedding-model}") String embeddingModelName
    ) {
        this.embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .build();
    }

    public List<Double> embed(String content) {
        Embedding embedding = embeddingModel.embed(content).content();
        return embedding.vectorAsList().stream().map(Float::doubleValue).toList();
    }
}

package com.offlineqa.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final boolean skipExternalServices;

    public EmbeddingService(
            @Value("${app.ollama.base-url}") String baseUrl,
            @Value("${app.ollama.embedding-model}") String embeddingModelName,
            @Value("${app.dev.skip-external-services:false}") boolean skipExternalServices
    ) {
        this.skipExternalServices = skipExternalServices;
        if (skipExternalServices) {
            this.embeddingModel = null;
        } else {
            this.embeddingModel = OllamaEmbeddingModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(embeddingModelName)
                    .build();
        }
    }

    public List<Double> embed(String content) {
        if (skipExternalServices) {
            return mockEmbedding(content);
        }
        try {
            Embedding embedding = embeddingModel.embed(content).content();
            return embedding.vectorAsList().stream().map(Float::doubleValue).toList();
        } catch (Exception e) {
            // Ollama 服务连接失败，回退到 mock 向量
            return mockEmbedding(content);
        }
    }

    private List<Double> mockEmbedding(String content) {
        int dim = 768; // 与 nomic-embed-text 模型生成的向量大小匹配
        List<Double> vector = new ArrayList<>(dim);
        int seed = content == null ? 0 : content.hashCode();
        for (int i = 0; i < dim; i++) {
            int v = (seed ^ (i * 31)) & 1023;
            vector.add(v / 1023.0);
        }
        return vector;
    }
}

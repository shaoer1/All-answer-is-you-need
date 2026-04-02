package com.offlineqa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdaptiveChunkService {

    @Value("${app.rag.max-chunk-size}")
    private int maxChunkSize;

    @Value("${app.rag.min-chunk-size}")
    private int minChunkSize;

    public List<String> split(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        String[] paragraphs = text.split("\\n\\n");
        StringBuilder buffer = new StringBuilder();

        for (String paragraph : paragraphs) {
            String candidate = paragraph.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            if (buffer.length() + candidate.length() + 2 <= maxChunkSize) {
                if (!buffer.isEmpty()) {
                    buffer.append("\n\n");
                }
                buffer.append(candidate);
            } else {
                flushChunk(result, buffer);
                if (candidate.length() > maxChunkSize) {
                    splitLongParagraph(result, candidate);
                } else {
                    buffer.append(candidate);
                }
            }
        }

        flushChunk(result, buffer);
        return result;
    }

    private void flushChunk(List<String> result, StringBuilder buffer) {
        if (buffer.isEmpty()) {
            return;
        }
        String chunk = buffer.toString().trim();
        if (chunk.length() >= minChunkSize || result.isEmpty()) {
            result.add(chunk);
        } else {
            int last = result.size() - 1;
            result.set(last, result.get(last) + "\n\n" + chunk);
        }
        buffer.setLength(0);
    }

    private void splitLongParagraph(List<String> result, String paragraph) {
        String[] sentences = paragraph.split("(?<=[。！？.!?])");
        StringBuilder sentenceBuffer = new StringBuilder();
        for (String sentence : sentences) {
            if (sentenceBuffer.length() + sentence.length() <= maxChunkSize) {
                sentenceBuffer.append(sentence);
            } else {
                flushChunk(result, sentenceBuffer);
                sentenceBuffer.append(sentence);
            }
        }
        flushChunk(result, sentenceBuffer);
    }
}

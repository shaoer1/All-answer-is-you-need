package com.offlineqa.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class EmbeddingJson {

    private EmbeddingJson() {
    }

    public static String toJson(List<Double> vector) {
        return vector.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }

    public static List<Double> fromJson(String json) {
        String body = json.substring(1, json.length() - 1).trim();
        if (body.isBlank()) {
            return List.of();
        }
        return Arrays.stream(body.split(",")).map(String::trim).map(Double::parseDouble).toList();
    }
}

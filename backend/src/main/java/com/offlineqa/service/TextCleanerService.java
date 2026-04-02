package com.offlineqa.service;

import org.springframework.stereotype.Service;

@Service
public class TextCleanerService {

    public String clean(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String cleaned = raw
                .replaceAll("\\u00A0", " ")
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
        return cleaned;
    }
}

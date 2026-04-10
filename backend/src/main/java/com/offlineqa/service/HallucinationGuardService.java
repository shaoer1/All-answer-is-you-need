package com.offlineqa.service;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class HallucinationGuardService {

    public String enforce(String answer, String ragContext, String webEvidence) {
        if (answer == null || answer.isBlank()) {
            return "抱歉，暂时无法回答该问题";
        }

        String normalized = answer.toLowerCase(Locale.ROOT);
        if (containsRiskPhrase(normalized) && (ragContext == null || ragContext.isBlank()) && (webEvidence == null || webEvidence.isBlank())) {
            return "抱歉，暂时无法回答该问题";
        }

        if ((ragContext == null || ragContext.isBlank()) && (webEvidence == null || webEvidence.isBlank())) {
            return "抱歉，暂时无法回答该问题";
        }

        return answer;
    }

    private boolean containsRiskPhrase(String answer) {
        return answer.contains("一定")
                || answer.contains("必然")
                || answer.contains("100%")
                || answer.contains("唯一")
                || answer.contains("绝对");
    }
}

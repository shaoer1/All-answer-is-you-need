package com.offlineqa.service;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class HallucinationGuardService {

    public String enforce(String answer, String ragContext, String webEvidence) {
        if (answer == null || answer.isBlank()) {
            return "我无法从现有资料得出可靠结论。";
        }

        String normalized = answer.toLowerCase(Locale.ROOT);
        if (containsRiskPhrase(normalized) && (ragContext == null || ragContext.isBlank()) && (webEvidence == null || webEvidence.isBlank())) {
            return "当前检索资料不足，我不能给出无依据结论。";
        }

        if ((ragContext == null || ragContext.isBlank()) && (webEvidence == null || webEvidence.isBlank())) {
            return "我暂时没有检索到足够依据，建议补充语料后再问。";
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

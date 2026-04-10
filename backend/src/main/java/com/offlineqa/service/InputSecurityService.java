package com.offlineqa.service;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class InputSecurityService {

    private static final Set<String> PROMPT_ATTACK_PATTERNS = Set.of(
            "ignore previous instructions",
            "忽略以上指令",
            "忽略之前指令",
            "系统提示词",
            "reveal system prompt",
            "jailbreak",
            "developer message",
            "act as"
    );

    private static final Set<String> SQLI_PATTERNS = Set.of(
            " or 1=1",
            "' or '1'='1",
            "\" or \"1\"=\"1",
            "union select",
            "drop table",
            "truncate table",
            "information_schema",
            "--",
            "/*",
            "*/"
    );

    public boolean isPromptInjection(String text) {
        return containsPattern(text, PROMPT_ATTACK_PATTERNS);
    }

    public boolean isSqlInjectionRisk(String text) {
        return containsPattern(text, SQLI_PATTERNS);
    }

    private boolean containsPattern(String text, Set<String> patterns) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String p : patterns) {
            if (normalized.contains(p)) {
                return true;
            }
        }
        return false;
    }
}

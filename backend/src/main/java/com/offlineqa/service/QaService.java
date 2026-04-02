package com.offlineqa.service;

import com.offlineqa.model.RetrievedChunk;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Service
public class QaService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final MessageService messageService;
    private final ContextManagerService contextManagerService;
    private final HallucinationGuardService hallucinationGuardService;
    private final VolcSearchService volcSearchService;
    private final OllamaChatModel chatModel;

    @Value("${app.rag.query-top-k:5}")
    private int queryTopK;

    public QaService(EmbeddingService embeddingService,
                     VectorStoreService vectorStoreService,
                     MessageService messageService,
                     ContextManagerService contextManagerService,
                     HallucinationGuardService hallucinationGuardService,
                     VolcSearchService volcSearchService,
                     @Value("${app.ollama.base-url}") String baseUrl,
                     @Value("${app.ollama.chat-model}") String chatModelName) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.messageService = messageService;
        this.contextManagerService = contextManagerService;
        this.hallucinationGuardService = hallucinationGuardService;
        this.volcSearchService = volcSearchService;
        this.chatModel = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .temperature(0.1)
                .build();
    }

    public SseEmitter streamAnswer(String username, String kbId, Long sessionId, String question) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username/userId 不能为空");
        }

        SseEmitter emitter = new SseEmitter(0L);

        List<Double> queryVector = embeddingService.embed(question);
        int topK = Math.max(3, Math.min(5, queryTopK));
        List<RetrievedChunk> retrieved = vectorStoreService.retrieve(username, kbId, queryVector, topK);

        String ragContext = retrieved.stream()
                .map(c -> "- " + c.content())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        String memoryContext = contextManagerService.buildContext(username, sessionId);
        String webEvidence = volcSearchService.factCheck(question);

        String prompt = """
                你是企业离线知识助手。
                严格规则：
                1) 只允许依据给定资料回答，不允许编造；
                2) 若资料不足，必须明确说“不知道/资料不足”；
                3) 对时间敏感问题，优先参考联网核验信息；
                4) 回答要简洁、可核验，不输出无依据数字。

                [会话记忆]
                %s

                [RAG检索]
                %s

                [联网核验]
                %s

                [用户问题]
                %s
                """.formatted(memoryContext, ragContext, webEvidence, question);

        messageService.save(sessionId, username, "user", question);

        new Thread(() -> {
            try {
                String rawAnswer = chatModel.generate(prompt);
                String finalAnswer = hallucinationGuardService.enforce(rawAnswer, ragContext, webEvidence);

                for (int i = 0; i < finalAnswer.length(); i += 8) {
                    String token = finalAnswer.substring(i, Math.min(i + 8, finalAnswer.length()));
                    emitter.send(SseEmitter.event().name("token").data(token));
                }

                messageService.save(sessionId, username, "assistant", finalAnswer);
                contextManagerService.maybeRefreshSummary(username, sessionId);

                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}

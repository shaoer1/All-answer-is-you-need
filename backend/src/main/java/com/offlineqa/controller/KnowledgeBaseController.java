package com.offlineqa.controller;

import com.offlineqa.model.KnowledgeBase;
import com.offlineqa.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping("/list")
    public List<KnowledgeBase> list(@RequestParam("userId") Long userId) {
        return knowledgeBaseService.listByUserId(userId);
    }

    @PostMapping("/create")
    public KnowledgeBase create(@RequestBody(required = false) Map<String, Object> body,
                               @RequestParam(value = "userId", required = false) Long userIdParam,
                               @RequestParam(value = "name", required = false) String nameParam,
                               @RequestParam(value = "description", required = false) String descriptionParam) {
        Long userId = userIdParam != null ? userIdParam : toLong(body, "userId");
        String name = hasText(nameParam) ? nameParam : toString(body, "name");
        String description = descriptionParam != null ? descriptionParam : toString(body, "description");
        if (userId == null || !hasText(name)) {
            throw new IllegalArgumentException("创建知识库缺少必要参数: userId/name");
        }
        return knowledgeBaseService.create(userId, name, description);
    }

    @PostMapping("/delete")
    public void delete(@RequestParam("id") Long id) {
        knowledgeBaseService.delete(id);
    }

    @PostMapping("/update")
    public KnowledgeBase update(@RequestBody(required = false) Map<String, Object> body,
                              @RequestParam(value = "id", required = false) Long idParam,
                              @RequestParam(value = "name", required = false) String nameParam,
                              @RequestParam(value = "description", required = false) String descriptionParam) {
        Long id = idParam != null ? idParam : toLong(body, "id");
        String name = hasText(nameParam) ? nameParam : toString(body, "name");
        String description = descriptionParam != null ? descriptionParam : toString(body, "description");
        if (id == null || !hasText(name)) {
            throw new IllegalArgumentException("更新知识库缺少必要参数: id/name");
        }
        return knowledgeBaseService.update(id, name, description);
    }

    @GetMapping("/get")
    public KnowledgeBase get(@RequestParam("id") Long id) {
        return knowledgeBaseService.getById(id);
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String toString(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Long toLong(Map<String, Object> body, String key) {
        String raw = toString(body, key);
        if (!hasText(raw)) return null;
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
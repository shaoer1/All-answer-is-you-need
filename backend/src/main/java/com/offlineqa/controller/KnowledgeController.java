package com.offlineqa.controller;

import com.offlineqa.model.UploadResponse;
import com.offlineqa.service.IngestionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final IngestionService ingestionService;

    public KnowledgeController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam(value = "username", required = false) String username,
                                 @RequestParam(value = "userId", required = false) String userId,
                                 @RequestParam("kbId") Long kbId,
                                 @RequestParam("file") MultipartFile file) {
        String principal = (username != null && !username.isBlank()) ? username : userId;
        return ingestionService.upload(principal, kbId, file);
    }
}
package com.offlineqa.service;

import com.offlineqa.model.KnowledgeBase;
import com.offlineqa.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    public List<KnowledgeBase> listByUserId(Long userId) {
        return knowledgeBaseRepository.findByUserId(userId);
    }

    public KnowledgeBase create(Long userId, String name, String description) {
        return knowledgeBaseRepository.create(userId, name, description);
    }

    public void delete(Long id) {
        knowledgeBaseRepository.delete(id);
    }

    public KnowledgeBase update(Long id, String name, String description) {
        knowledgeBaseRepository.update(id, name, description);
        return knowledgeBaseRepository.findById(id).orElse(null);
    }

    public KnowledgeBase getById(Long id) {
        return knowledgeBaseRepository.findById(id).orElse(null);
    }
}
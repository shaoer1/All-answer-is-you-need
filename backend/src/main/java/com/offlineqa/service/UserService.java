package com.offlineqa.service;

import com.offlineqa.model.SysUser;
import com.offlineqa.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final KnowledgeBaseService knowledgeBaseService;

    public UserService(UserRepository userRepository, KnowledgeBaseService knowledgeBaseService) {
        this.userRepository = userRepository;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    public long initUser(String username) {
        long userId = userRepository.createIfAbsent(username);
        // 检查用户是否有知识库，如果没有，创建一个默认知识库
        var knowledgeBases = knowledgeBaseService.listByUserId(userId);
        if (knowledgeBases.isEmpty()) {
            knowledgeBaseService.create(userId, "默认知识库", "用户的默认知识库");
        }
        return userId;
    }

    public long resolveUserId(String username, String userId) {
        if (username != null && !username.isBlank()) {
            return initUser(username.trim());
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("username/userId 不能为空");
        }
        try {
            long uid = Long.parseLong(userId.trim());
            return userRepository.findById(uid)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + uid))
                    .getId();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("userId 格式错误");
        }
    }

    public SysUser register(String username, String password, String nickname) {
        long userId = userRepository.register(username, password, nickname);
        // 为新注册的用户创建一个默认知识库
        knowledgeBaseService.create(userId, "默认知识库", "用户的默认知识库");
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(username);
        user.setNickname((nickname == null || nickname.isBlank()) ? username : nickname);
        return user;
    }

    public SysUser login(String username, String password) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!password.equals(user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        // 确保用户有知识库
        initUser(username);
        return user;
    }
}

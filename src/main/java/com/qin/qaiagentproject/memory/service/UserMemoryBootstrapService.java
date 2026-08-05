package com.qin.qaiagentproject.memory.service;

import com.qin.qaiagentproject.memory.repository.HotMemoryRepository;
import com.qin.qaiagentproject.memory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 确保 users + hot_memory 行存在（对齐简化方案 initUserMemory）。
 */
@Service
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
@RequiredArgsConstructor
@Slf4j
public class UserMemoryBootstrapService {

    private final UserRepository userRepository;
    private final HotMemoryRepository hotMemoryRepository;

    /**
     * @param userIdHint 可选 UUID 字符串；空则生成匿名用户
     * @return 落库后的 userId
     */
    public UUID ensureUser(String userIdHint) {
        UUID userId = resolveUserId(userIdHint);
        String username = buildUsername(userId, userIdHint);

        if (!userRepository.existsById(userId)) {
            userRepository.insert(userId, username);
            log.info("创建用户: userId={}, username={}", userId, username);
        }
        hotMemoryRepository.insertIfAbsent(userId);
        userRepository.touchLastActive(userId);
        return userId;
    }

    private UUID resolveUserId(String userIdHint) {
        if (!StringUtils.hasText(userIdHint)) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(userIdHint.trim());
        } catch (IllegalArgumentException ex) {
            // 非 UUID：按 username 查找或新建
            return userRepository.findIdByUsername(userIdHint.trim())
                    .orElseGet(() -> {
                        UUID id = UUID.randomUUID();
                        userRepository.insert(id, truncate(userIdHint.trim(), 50));
                        return id;
                    });
        }
    }

    private String buildUsername(UUID userId, String userIdHint) {
        if (StringUtils.hasText(userIdHint) && !isUuid(userIdHint.trim())) {
            return truncate(userIdHint.trim(), 50);
        }
        return "anon-" + userId.toString().substring(0, 8);
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}

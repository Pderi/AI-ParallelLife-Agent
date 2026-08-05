package com.qin.qaiagentproject.memory.service;

import com.qin.qaiagentproject.config.MemoryProperties;
import com.qin.qaiagentproject.exception.BusinessException;
import com.qin.qaiagentproject.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Locale;
import java.util.StringJoiner;

@Service
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final MemoryProperties memoryProperties;

    public float[] embed(String text) {
        Assert.hasText(text, "embed text must not be blank");
        float[] vector = embeddingModel.embed(text);
        int expected = memoryProperties.getEmbeddingDimensions();
        if (vector == null || vector.length == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Embedding 返回空向量");
        }
        if (vector.length != expected) {
            log.warn("Embedding 维度与配置不一致: actual={}, expected={}", vector.length, expected);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Embedding 维度不匹配: actual=" + vector.length + ", expected=" + expected);
        }
        return vector;
    }

    public String toPgVectorLiteral(float[] vector) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float v : vector) {
            joiner.add(String.format(Locale.ROOT, "%.8f", v));
        }
        return joiner.toString();
    }

    public String embedAsPgLiteral(String text) {
        return toPgVectorLiteral(embed(text));
    }
}

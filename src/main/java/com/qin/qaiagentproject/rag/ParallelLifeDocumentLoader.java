package com.qin.qaiagentproject.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
class ParallelLifeDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    ParallelLifeDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            // 加载平行人生模拟器相关的知识库文档
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName == null) continue;
                
                // 提取文档类型（从文件名中提取，例如：职业发展-技术类.md -> 职业发展）
                String category = extractCategory(fileName);
                
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", fileName)
                        .withAdditionalMetadata("category", category)
                        .build();
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(reader.get());
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败", e);
        }
        return allDocuments;
    }

    /**
     * 从文件名中提取类别
     * 例如：职业发展路径-技术类.md -> 职业发展
     */
    private String extractCategory(String fileName) {
        if (fileName.contains("职业发展")) {
            return "职业发展";
        } else if (fileName.contains("决策分析")) {
            return "决策分析";
        } else if (fileName.contains("人生规划")) {
            return "人生规划";
        } else if (fileName.contains("行业数据")) {
            return "行业数据";
        }
        return "其他";
    }
}


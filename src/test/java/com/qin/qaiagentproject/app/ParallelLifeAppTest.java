package com.qin.qaiagentproject.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class ParallelLifeAppTest {

    @Resource
    private ParallelLifeApp parallelLifeApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮：介绍当前情况
        String message = "你好，我今年25岁，是一名程序员，工作2年了";
        String answer = parallelLifeApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        
        // 第二轮：探索人生路径
        message = "我在考虑是否应该转行做产品经理，帮我模拟一下不同选择的结果";
        answer = parallelLifeApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        
        // 第三轮：深入询问
        message = "如果选择转行，我需要做哪些准备？";
        answer = parallelLifeApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "我今年25岁，程序员，工作2年，在考虑是否应该继续做技术还是转行做产品经理，帮我生成一份平行人生报告";
        ParallelLifeApp.ParallelLifeReport report = parallelLifeApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(report);
        Assertions.assertNotNull(report.title());
        Assertions.assertNotNull(report.universes());
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我想了解程序员转行做产品经理的成功概率和需要准备什么";
        String answer = parallelLifeApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索：搜索行业数据
        testMessage("帮我搜索一下2024年程序员转行做产品经理的成功率和薪资水平");

        // 测试网页抓取：获取职业发展信息
        testMessage("帮我抓取一下某个职业规划网站的最新行业趋势分析");

        // 测试文件操作：保存人生规划
        testMessage("保存我的人生规划报告为文件");

        // 测试 PDF 生成
        testMessage("生成一份'平行人生报告'PDF，包含多个宇宙的详细分析和建议");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = parallelLifeApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }
}


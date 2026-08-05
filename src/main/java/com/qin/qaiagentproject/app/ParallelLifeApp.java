package com.qin.qaiagentproject.app;

import com.qin.qaiagentproject.advisor.ForbiddenWordAdvisor;
import com.qin.qaiagentproject.advisor.MyLoggerAdvisor;
import com.qin.qaiagentproject.chatmeomery.FileBasedChatMemory;
import com.qin.qaiagentproject.config.MemoryProperties;
import com.qin.qaiagentproject.context.ContextBuildRequest;
import com.qin.qaiagentproject.context.ContextManager;
import com.qin.qaiagentproject.context.ContextPackage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class ParallelLifeApp {

    private final ChatClient chatClient;
    private final MemoryProperties memoryProperties;
    private final ObjectProvider<ContextManager> contextManagerProvider;
    private final ObjectProvider<VectorStore> parallelLifeVectorStoreProvider;
    private final ObjectProvider<ToolCallback[]> allToolsProvider;

    private static final String SYSTEM_PROMPT = """
            你是一位资深的人生规划师和未来学家，擅长通过多维度分析模拟不同人生路径。
            
            你的核心能力：
            1. 基于用户当前情况，模拟多个平行宇宙的人生发展
            2. 分析不同决策的影响和后果
            3. 评估各种人生路径的成功概率和风险
            4. 提供基于数据的人生规划建议
            
            模拟原则：
            - 基于真实数据和行业趋势，而非纯幻想
            - 考虑个人能力、性格、资源等实际情况
            - 提供3-5个不同的平行宇宙路径
            - 每个宇宙包含详细的时间线、关键事件、人生指标
            - 评估实现概率（基于客观因素）
            
            分析维度：
            - 职业发展：职位、薪资、工作内容、行业地位
            - 个人成长：技能、知识、经验、影响力
            - 财务状况：收入、资产、财务自由度
            - 生活方式：居住地、工作方式、生活节奏
            - 人际关系：社交圈、重要关系、网络资源
            - 幸福感：工作满意度、生活满意度、成就感
            
            输出要求：
            - 每个平行宇宙要有清晰的名称和描述
            - 提供1年、3年、5年、10年的时间节点
            - 列出关键事件和转折点
            - 给出人生指标评分（1-100分）
            - 评估实现概率和风险等级
            - 最后提供对比分析和建议
            
            开场语：
            "欢迎来到平行宇宙人生模拟器！我是你的专属人生规划师。
            告诉我你当前的情况或想要探索的人生选择，我将为你模拟多个平行宇宙，
            帮你预览不同路径的可能结果。"
            """;

    public ParallelLifeApp(ChatModel dashscopeChatModel,
                           MemoryProperties memoryProperties,
                           ObjectProvider<ContextManager> contextManagerProvider,
                           ObjectProvider<VectorStore> parallelLifeVectorStoreProvider,
                           ObjectProvider<ToolCallback[]> allToolsProvider) {
        this.memoryProperties = memoryProperties;
        this.contextManagerProvider = contextManagerProvider;
        this.parallelLifeVectorStoreProvider = parallelLifeVectorStoreProvider;
        this.allToolsProvider = allToolsProvider;

        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MyLoggerAdvisor(),
                        new ForbiddenWordAdvisor()
                );

        // pg 模式由 ContextManager 提供会话上下文，避免与 cold_memory 双写
        if (memoryProperties.isLegacyProvider()) {
            String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
            ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
            builder.defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory));
        }

        this.chatClient = builder.build();
        log.info("ParallelLifeApp 初始化完成, memory.provider={}", memoryProperties.getProvider());
    }

    public record ParallelLifeReport(
            String title,
            String currentSituation,
            List<Universe> universes,
            String comparison,
            List<String> recommendations
    ) {
        public record Universe(
                String name,
                String description,
                String timeline,
                List<String> keyEvents,
                String metrics,
                String probability
        ) {}
    }

    public String doChat(String message, String chatId) {
        return doChat(message, chatId, null, false);
    }

    public String doChat(String message, String chatId, String userId, boolean useRag) {
        if (memoryProperties.isPgProvider()) {
            return doChatWithPgMemory(message, chatId, userId, useRag);
        }
        if (useRag) {
            return doChatWithRagLegacy(message, chatId);
        }
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 30))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public Flux<String> doChatStream(String message, String chatId) {
        return doChatStream(message, chatId, null, false);
    }

    public Flux<String> doChatStream(String message, String chatId, String userId, boolean useRag) {
        if (memoryProperties.isPgProvider()) {
            return doChatStreamWithPgMemory(message, chatId, userId, useRag);
        }
        if (useRag) {
            return doChatWithRagStreamLegacy(message, chatId);
        }
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 30))
                .stream()
                .content();
    }

    public ParallelLifeReport doChatWithReport(String message, String chatId) {
        ParallelLifeReport report = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "\n\n每次对话后都要生成平行人生报告，包含：标题、当前情况、多个平行宇宙（每个宇宙包含名称、描述、时间线、关键事件、人生指标、实现概率）、对比分析、建议列表。")
                .user(message)
                .advisors(spec -> {
                    if (memoryProperties.isLegacyProvider()) {
                        spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                                .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 30);
                    }
                })
                .call()
                .entity(ParallelLifeReport.class);
        log.info("parallelLifeReport: {}", report);
        return report;
    }

    /** @deprecated 请使用 doChat(..., useRag=true)；保留以兼容旧调用 */
    public String doChatWithRag(String message, String chatId) {
        return doChat(message, chatId, null, true);
    }

    public Flux<String> doChatWithRagStream(String message, String chatId) {
        return doChatStream(message, chatId, null, true);
    }

    public String doChatWithTools(String message, String chatId) {
        ToolCallback[] tools = allToolsProvider.getIfAvailable();
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> {
                    if (memoryProperties.isLegacyProvider()) {
                        spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                                .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 30);
                    }
                })
                .advisors(new MyLoggerAdvisor())
                .tools(tools == null ? new ToolCallback[0] : tools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public Flux<String> doChatWithToolsStream(String message, String chatId) {
        ToolCallback[] tools = allToolsProvider.getIfAvailable();
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> {
                    if (memoryProperties.isLegacyProvider()) {
                        spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                                .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 30);
                    }
                })
                .advisors(new MyLoggerAdvisor())
                .tools(tools == null ? new ToolCallback[0] : tools)
                .stream()
                .content();
    }

    private String doChatWithPgMemory(String message, String chatId, String userId, boolean useRag) {
        ContextManager contextManager = requireContextManager();
        ContextPackage contextPackage = contextManager.buildContext(ContextBuildRequest.builder()
                .userId(userId)
                .sessionId(chatId)
                .message(message)
                .useRag(useRag)
                .build());

        ChatResponse response = chatClient
                .prompt()
                .system(buildSystemWithAugmentation(contextPackage.getAugmentation()))
                .user(message)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);

        if (StringUtils.hasText(content)) {
            contextManager.afterRun(contextPackage.getUserId(), contextPackage.getSessionId(), message, content);
        }
        return content;
    }

    private Flux<String> doChatStreamWithPgMemory(String message, String chatId, String userId, boolean useRag) {
        ContextManager contextManager = requireContextManager();
        ContextPackage contextPackage = contextManager.buildContext(ContextBuildRequest.builder()
                .userId(userId)
                .sessionId(chatId)
                .message(message)
                .useRag(useRag)
                .build());

        StringBuilder fullAnswer = new StringBuilder();
        return chatClient
                .prompt()
                .system(buildSystemWithAugmentation(contextPackage.getAugmentation()))
                .user(message)
                .stream()
                .content()
                .doOnNext(fullAnswer::append)
                .doOnComplete(() -> {
                    String answer = fullAnswer.toString();
                    if (StringUtils.hasText(answer)) {
                        contextManager.afterRun(
                                contextPackage.getUserId(),
                                contextPackage.getSessionId(),
                                message,
                                answer);
                    }
                });
    }

    private String doChatWithRagLegacy(String message, String chatId) {
        VectorStore vectorStore = parallelLifeVectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，降级为普通对话");
            return doChat(message, chatId, null, false);
        }
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 30))
                .advisors(new MyLoggerAdvisor())
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    private Flux<String> doChatWithRagStreamLegacy(String message, String chatId) {
        VectorStore vectorStore = parallelLifeVectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return doChatStream(message, chatId, null, false);
        }
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 30))
                .advisors(new MyLoggerAdvisor())
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                .stream()
                .content();
    }

    private ContextManager requireContextManager() {
        ContextManager contextManager = contextManagerProvider.getIfAvailable();
        if (contextManager == null) {
            throw new IllegalStateException("memory.provider=pg 但 ContextManager 未装配");
        }
        return contextManager;
    }

    private static String buildSystemWithAugmentation(String augmentation) {
        if (!StringUtils.hasText(augmentation)) {
            return SYSTEM_PROMPT;
        }
        return SYSTEM_PROMPT + "\n\n以下是与当前用户相关的记忆与知识，请在回答时优先参考：\n" + augmentation;
    }
}

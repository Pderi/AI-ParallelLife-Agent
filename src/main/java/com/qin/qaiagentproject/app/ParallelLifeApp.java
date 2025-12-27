package com.qin.qaiagentproject.app;

import com.qin.qaiagentproject.advisor.MyLoggerAdvisor;
import com.qin.qaiagentproject.advisor.ForbiddenWordAdvisor;
import com.qin.qaiagentproject.chatmeomery.FileBasedChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class ParallelLifeApp {

    private final ChatClient chatClient;

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

    public ParallelLifeApp(ChatModel dashscopeChatModel) {
        // 初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor(),
                        new ForbiddenWordAdvisor()
                )
                .build();
    }

    /**
     * 平行人生报告
     */
    public record ParallelLifeReport(
            String title,
            String currentSituation,
            List<Universe> universes,
            String comparison,
            List<String> recommendations
    ) {
        /**
         * 平行宇宙
         */
        public record Universe(
                String name,
                String description,
                String timeline,
                List<String> keyEvents,
                String metrics,
                String probability
        ) {}
    }

    /**
     * 基础对话：模拟平行人生
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 生成结构化的平行人生报告
     */
    public ParallelLifeReport doChatWithReport(String message, String chatId) {
        ParallelLifeReport report = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "\n\n每次对话后都要生成平行人生报告，包含：标题、当前情况、多个平行宇宙（每个宇宙包含名称、描述、时间线、关键事件、人生指标、实现概率）、对比分析、建议列表。")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(ParallelLifeReport.class);
        log.info("parallelLifeReport: {}", report);
        return report;
    }

    @Resource
    private VectorStore parallelLifeVectorStore;

    /**
     * RAG增强对话：结合知识库进行模拟
     */
    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用知识库问答
                .advisors(new QuestionAnswerAdvisor(parallelLifeVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallback[] allTools;

    /**
     * 工具调用对话：可以搜索行业数据、生成PDF报告等
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}


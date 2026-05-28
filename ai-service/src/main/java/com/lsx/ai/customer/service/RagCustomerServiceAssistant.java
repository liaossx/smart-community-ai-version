package com.lsx.ai.customer.service;

import com.lsx.ai.customer.dto.CustomerServiceAnswerResponse;
import com.lsx.ai.customer.dto.CustomerServiceAskRequest;
import com.lsx.ai.customer.knowledge.HybridCommunityKnowledgeRetriever;
import com.lsx.ai.customer.knowledge.RetrievedKnowledgeDocument;
import com.lsx.ai.observability.model.AiCallLogEntry;
import com.lsx.ai.observability.service.AiCallLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;
//编排rag的流程
@Service
public class RagCustomerServiceAssistant implements CustomerServiceAssistant {
    private static final Logger log = LoggerFactory.getLogger(RagCustomerServiceAssistant.class);

    /*
     * RAG 回答的系统提示词。
     *
     * 核心要求：
     * 1. 只能基于检索到的社区资料回答。
     * 2. 资料不足时不能编造，要设置 cannotAnswer=true。
     * 3. citations 必须引用检索资料的 sourceId。
     */
    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是智能社区客服助手。",
            "你只能根据提供的社区资料回答问题，不能编造公告、制度、费用、时间或工作人员信息。",
            "如果资料不足以回答问题，cannotAnswer 必须为 true，并提示居民联系物业客服人工确认。",
            "回答要简洁、礼貌、可执行，面向居民。",
            "citations 只能填写资料中的 sourceId。",
            "返回结构化对象，字段包括 answer、followUpActions、citations、cannotAnswer、confidence。"
    );

    private final ChatClient chatClient;
    private final HybridCommunityKnowledgeRetriever retriever;
    private final CustomerServiceAnswerNormalizer normalizer;
    private final AiCallLogService aiCallLogService;
    private final String providerVersion;
    private final String model;

    public RagCustomerServiceAssistant(ChatClient.Builder chatClientBuilder,
                                       HybridCommunityKnowledgeRetriever retriever,
                                       CustomerServiceAnswerNormalizer normalizer,
                                       AiCallLogService aiCallLogService,
                                       @Value("${smart-community.ai.customer-service.provider-version:rag-v1}")
                                       String providerVersion,
                                       @Value("${spring.ai.openai.chat.options.model:unknown}")
                                       String model) {
        this.chatClient = chatClientBuilder.build();
        this.retriever = retriever;
        this.normalizer = normalizer;
        this.aiCallLogService = aiCallLogService;
        this.providerVersion = providerVersion;
        this.model = model;
    }

    @Override
    public CustomerServiceAnswerResponse answer(CustomerServiceAskRequest request) {
        AiCallLogEntry callLog = AiCallLogEntry.start("CUSTOMER_SERVICE_RAG")
                .bizKey(request.getCommunityId() == null ? null : "communityId=" + request.getCommunityId())
                .requestSummary(request.getQuestion());
        // 第一步：先检索知识库。RAG 和普通 LLM 调用最大的区别就在这里。
        List<RetrievedKnowledgeDocument> documents = retriever.retrieve(
                request.getQuestion(), request.getCommunityId(), request.getTopK());
        if (documents.isEmpty()) {
            // 没有资料时不调用大模型，直接拒答/转人工，避免模型胡编。
            CustomerServiceAnswerResponse result =
                    normalizer.normalize(noKnowledgeAnswer(), documents, "RAG_RETRIEVAL", providerVersion, model);
            aiCallLogService.record(callLog
                    .provider(result.getProvider(), result.getProviderVersion(), result.getModel())
                    .status("SUCCESS")
                    .confidence(result.getConfidence())
                    .responseSummary(result.getAnswer()));
            return result;
        }

        try {
            // 第二步：把“用户问题 + 检索资料”一起拼进 Prompt，再调用大模型。
            CustomerServiceAnswerResponse response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(user -> user.text(String.join("\n",
                                    "居民问题：{question}",
                                    "社区资料：",
                                    "{context}",
                                    "请只根据社区资料回答，并返回结构化对象。"))
                            .param("question", request.getQuestion())
                            .param("context", buildContext(documents)))
                    .call()
                    .entity(CustomerServiceAnswerResponse.class);
            // 第三步：模型输出后做字段兜底、引用校验和 provider 元数据补全。
            CustomerServiceAnswerResponse result =
                    normalizer.normalize(response, documents, "SPRING_AI_RAG", providerVersion, model);
            aiCallLogService.record(callLog
                    .provider(result.getProvider(), result.getProviderVersion(), result.getModel())
                    .status("SUCCESS")
                    .confidence(result.getConfidence())
                    .retrievedSourceIds(sourceIds(documents))
                    .responseSummary(result.getAnswer()));
            return result;
        } catch (RuntimeException ex) {
            log.warn("RAG customer service model call failed, use retrieval fallback. question={}",
                    request.getQuestion(), ex);
            // 模型不可用时，仍然返回检索结果，让前端/客服知道查到了哪些资料。
            CustomerServiceAnswerResponse result =
                    normalizer.normalize(retrievalFallback(documents), documents, "RAG_RETRIEVAL", providerVersion, model);
            aiCallLogService.record(callLog
                    .provider(result.getProvider(), result.getProviderVersion(), result.getModel())
                    .status("FALLBACK")
                    .confidence(result.getConfidence())
                    .retrievedSourceIds(sourceIds(documents))
                    .responseSummary(result.getAnswer())
                    .error(ex));
            return result;
        }
    }

    private String buildContext(List<RetrievedKnowledgeDocument> documents) {
        // 把检索到的资料转换成带编号的上下文块，供 LLM 引用。
        return IntStream.range(0, documents.size())
                .mapToObj(index -> documents.get(index).getDocument().toContextBlock(index + 1))
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }

    private CustomerServiceAnswerResponse noKnowledgeAnswer() {
        CustomerServiceAnswerResponse response = new CustomerServiceAnswerResponse();
        response.setAnswer("暂未在社区知识库中找到可以回答该问题的资料，请联系物业客服人工确认。");
        response.setCannotAnswer(true);
        response.setConfidence(30);
        return response;
    }

    private CustomerServiceAnswerResponse retrievalFallback(List<RetrievedKnowledgeDocument> documents) {
        CustomerServiceAnswerResponse response = new CustomerServiceAnswerResponse();
        response.setAnswer("已检索到相关社区资料，但大模型暂时不可用，请根据引用资料联系物业客服进一步确认。");
        response.setCannotAnswer(false);
        response.setConfidence(60);
        response.setCitations(documents.stream()
                .map(result -> result.getDocument().getSourceId())
                .limit(2)
                .toList());
        return response;
    }

    private List<String> sourceIds(List<RetrievedKnowledgeDocument> documents) {
        return documents.stream()
                .map(result -> result.getDocument().getSourceId())
                .toList();
    }
}

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
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.stream.IntStream;

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
                                       @Value("${spring.ai.openai.chat.options.model}")
                                       String model) {
        this.chatClient = chatClientBuilder.build();
        this.retriever = retriever;
        this.normalizer = normalizer;
        this.aiCallLogService = aiCallLogService;
        this.providerVersion = providerVersion;
        this.model = model;
    }

    @PostConstruct
    void init() {
        log.info("AI model configured: model={}, providerVersion={}", model, providerVersion);
    }

    @Override
    public CustomerServiceAnswerResponse answer(CustomerServiceAskRequest request) {
        long t0 = System.currentTimeMillis();
        AiCallLogEntry callLog = AiCallLogEntry.start("CUSTOMER_SERVICE_RAG")
                .bizKey(request.getCommunityId() == null ? null : "communityId=" + request.getCommunityId())
                .requestSummary(request.getQuestion());

        // -- 1. retrieve --
        long t1 = System.currentTimeMillis();
        List<RetrievedKnowledgeDocument> documents = retriever.retrieve(
                request.getQuestion(), request.getCommunityId(), request.getTopK());
        long t1cost = System.currentTimeMillis() - t1;
        log.info("[TIMING] (1) retrieve: {} ms, resultCount={}", t1cost, documents.size());

        if (documents.isEmpty()) {
            CustomerServiceAnswerResponse result =
                    normalizer.normalize(noKnowledgeAnswer(), documents, "RAG_RETRIEVAL", providerVersion, model);
            aiCallLogService.record(callLog
                    .provider(result.getProvider(), result.getProviderVersion(), result.getModel())
                    .status("SUCCESS")
                    .confidence(result.getConfidence())
                    .responseSummary(result.getAnswer()));
            log.info("[TIMING] total(noKnowledge): {} ms | (1)retrieve={}",
                    System.currentTimeMillis() - t0, t1cost);
            return result;
        }

        try {
            // -- 2. LLM call --
            long t2 = System.currentTimeMillis();
            String contextText = buildContext(documents);
            CustomerServiceAnswerResponse response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(user -> user.text(String.join("\n",
                                    "居民问题：{question}",
                                    "社区资料：",
                                    "{context}",
                                    "请只根据社区资料回答，并返回结构化对象。"))
                            .param("question", request.getQuestion())
                            .param("context", contextText))
                    .call()
                    .entity(CustomerServiceAnswerResponse.class);
            long t2cost = System.currentTimeMillis() - t2;
            log.info("[TIMING] (2) LLM call: {} ms, contextChars={}", t2cost, contextText.length());

            // -- 3. normalize --
            long t3 = System.currentTimeMillis();
            CustomerServiceAnswerResponse result =
                    normalizer.normalize(response, documents, "SPRING_AI_RAG", providerVersion, model);
            long t3cost = System.currentTimeMillis() - t3;
            log.info("[TIMING] (3) normalize: {} ms", t3cost);

            // -- 4. record call log --
            long t4 = System.currentTimeMillis();
            aiCallLogService.record(callLog
                    .provider(result.getProvider(), result.getProviderVersion(), result.getModel())
                    .status("SUCCESS")
                    .confidence(result.getConfidence())
                    .retrievedSourceIds(sourceIds(documents))
                    .responseSummary(result.getAnswer()));
            long t4cost = System.currentTimeMillis() - t4;
            log.info("[TIMING] (4) callLog record: {} ms", t4cost);

            long totalCost = System.currentTimeMillis() - t0;
            log.info("[TIMING] total: {} ms | (1)retrieve={} (2)llm={} (3)normalize={} (4)log={}",
                    totalCost, t1cost, t2cost, t3cost, t4cost);
            return result;
        } catch (RuntimeException ex) {
            log.warn("RAG customer service model call failed, use retrieval fallback. question={}",
                    request.getQuestion(), ex);
            CustomerServiceAnswerResponse result =
                    normalizer.normalize(retrievalFallback(documents), documents, "RAG_RETRIEVAL", providerVersion, model);
            aiCallLogService.record(callLog
                    .provider(result.getProvider(), result.getProviderVersion(), result.getModel())
                    .status("FALLBACK")
                    .confidence(result.getConfidence())
                    .retrievedSourceIds(sourceIds(documents))
                    .responseSummary(result.getAnswer())
                    .error(ex));
            log.info("[TIMING] total(fallback): {} ms | (1)retrieve={}",
                    System.currentTimeMillis() - t0, t1cost);
            return result;
        }
    }

    private String buildContext(List<RetrievedKnowledgeDocument> documents) {
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

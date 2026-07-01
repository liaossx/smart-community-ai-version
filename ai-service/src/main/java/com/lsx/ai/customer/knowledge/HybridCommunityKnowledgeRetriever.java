package com.lsx.ai.customer.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Primary
public class HybridCommunityKnowledgeRetriever {

    private static final Logger log = LoggerFactory.getLogger(HybridCommunityKnowledgeRetriever.class);

    // ============ 常量说明 ============
    // DEFAULT_TOP_K: 默认返回 3 条最相关结果
    // MAX_TOP_K: 上限 5 条，传给大模型的上下文太长会稀释回答质量
    // HYBRID_MATCH_BONUS: 同一条资料"关键字 + 向量"都命中，额外加 5 分奖励
    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOP_K = 5;
    private static final int HYBRID_MATCH_BONUS = 5;

    // 下面三个常量用于 suppressWeakVectorOnlyTail 降噪逻辑：
    // 当没有关键字命中时，完全靠向量的资料的最低通过分
    // 当有关键字命中作为"锚点"时，纯向量的资料的分水岭
    private static final int MIN_VECTOR_ONLY_SCORE_WITH_KEYWORD_ANCHOR = 50;
    private static final double MIN_VECTOR_ONLY_RATIO_WITH_KEYWORD_ANCHOR = 0.95d;
    private static final int MIN_VECTOR_ONLY_ACCEPT_SCORE_WITHOUT_KEYWORD = 50;

    // 两路检索器：关键字路 + 向量路
    private final KeywordCommunityKnowledgeRetriever keywordRetriever;
    private final VectorCommunityKnowledgeRetriever vectorRetriever;

    // 构造注入。向量检索器用了 ObjectProvider.getIfAvailable()
    // 意思：如果向量检索器没启用（比如没配 Embedding 模型），就返回 null 不报错
    // 这样关键字检索仍然可以独立工作。
    public HybridCommunityKnowledgeRetriever(KeywordCommunityKnowledgeRetriever keywordRetriever,
                                             ObjectProvider<VectorCommunityKnowledgeRetriever> vectorRetrieverProvider) {
        this.keywordRetriever = keywordRetriever;
        this.vectorRetriever = vectorRetrieverProvider.getIfAvailable();
    }

    // =====================================================================
    // 对外调用的入口
    // 1. 空问题保护
    // 2. 关键字检索（第一路）
    // 3. 向量检索（第二路，可能为 null）
    // 4. mergeResults 把两路结果合并排序返回
    // =====================================================================
    public List<RetrievedKnowledgeDocument> retrieve(String question, Long communityId, Integer topK) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        int limit = safeTopK(topK);

        // 关键字 + 向量两路并行检索
        CompletableFuture<List<RetrievedKnowledgeDocument>> keywordFuture =
                CompletableFuture.supplyAsync(() -> {
                    long tk = System.currentTimeMillis();
                    List<RetrievedKnowledgeDocument> results = keywordRetriever.retrieve(question, communityId, limit);
                    log.info("[TIMING]   ①a keyword retrieve: {} ms, count={}",
                            System.currentTimeMillis() - tk, results.size());
                    return results;
                });

        CompletableFuture<List<RetrievedKnowledgeDocument>> vectorFuture =
                CompletableFuture.supplyAsync(() -> {
                    long tv = System.currentTimeMillis();
                    List<RetrievedKnowledgeDocument> results = vectorRetriever == null
                            ? List.of()
                            : vectorRetriever.retrieve(question, communityId, limit);
                    log.info("[TIMING]   ①b vector retrieve: {} ms, count={}",
                            System.currentTimeMillis() - tv, results.size());
                    return results;
                });

        List<RetrievedKnowledgeDocument> keywordResults = keywordFuture.join();
        List<RetrievedKnowledgeDocument> vectorResults = vectorFuture.join();

        // 合并排序
        long tm = System.currentTimeMillis();
        List<RetrievedKnowledgeDocument> merged = mergeResults(keywordResults, vectorResults, limit);
        long tmCost = System.currentTimeMillis() - tm;
        log.info("[TIMING]   ①c merge: {} ms, mergedCount={}", tmCost, merged.size());

        return merged;
    }

    // =====================================================================
    // mergeResults: 混合检索的"灵魂方法"
    //
    // 核心流程：
    //   1. 用一个 Map<documentKey, MergeCandidate> 把两路结果合并（去重）
    //   2. 每个 MergeCandidate 记录关键字得分 + 向量得分
    //   3. finalScore = max(关键字得分, 向量得分 × 0.35) + 双命中加 5 分
    //   4. 排序：总分降序 → 关键字分降序 → 向量分降序
    //   5. suppressWeakVectorOnlyTail：过滤掉语义"碰瓷"的弱相关结果
    //   6. limit 截断
    //
    // 思考点：为什么向量得分要 ×0.35？
    //   关键字精确匹配的置信度更高，所以权重给大。
    //   向量语义起"兜底"作用——0.35 保证它在排序里能露脸，但不会盖过关键字命中。
    // =====================================================================
    static List<RetrievedKnowledgeDocument> mergeResults(List<RetrievedKnowledgeDocument> keywordResults,
                                                         List<RetrievedKnowledgeDocument> vectorResults,
                                                         int limit) {
        // ★ Step 1: 去重合并
        // 用 LinkedHashMap 保持插入顺序，保证排序前结果是稳定的
        Map<String, MergeCandidate> merged = new LinkedHashMap<>();

        // 先处理关键字结果（优先保留关键字命中）
        for (RetrievedKnowledgeDocument result : safeList(keywordResults)) {
            // computeIfAbsent：这个 key（资料）第一次出现 → 新建 MergeCandidate
            // acceptKeyword：记录这条资料的关键字得分（取最大值，防同一条资料被关键字路多次返回）
            merged.computeIfAbsent(documentKey(result.getDocument()), key -> new MergeCandidate(result.getDocument()))
                    .acceptKeyword(result);
        }

        // 再处理向量结果
        for (RetrievedKnowledgeDocument result : safeList(vectorResults)) {
            // 同样方式处理向量结果，如果和前面关键字结果是同一份资料，会合并到同一个 MergeCandidate
            merged.computeIfAbsent(documentKey(result.getDocument()), key -> new MergeCandidate(result.getDocument()))
                    .acceptVector(result);
        }

        // ★ Step 2: 合并打分 + 排序
        // 排序规则：总分降序 > 关键字分降序 > 向量分降序
        // 这意味着：总分相同时，关键字命中的资料排在前面，向量命中的排在后面
        List<RetrievedKnowledgeDocument> results = merged.values().stream()
                .map(MergeCandidate::toResult)
                .filter(result -> result.getScore() > 0)
                .sorted(Comparator.comparingInt(RetrievedKnowledgeDocument::getScore).reversed()
                        .thenComparing(Comparator.comparingInt(RetrievedKnowledgeDocument::getKeywordScore).reversed())
                        .thenComparing(Comparator.comparingInt(RetrievedKnowledgeDocument::getVectorScore).reversed()))
                .collect(Collectors.toList());

        // ★ Step 3: 降噪 + 截断
        // 降噪：删掉"只有向量命中但得分很低"的结果
        // 场景：用户问"物业费"，向量路可能把"小区绿化"的也拉回来
        // 因为"物业"这个词的语义向量跟"绿化"向量距离较近
        // suppressWeakVectorOnlyTail 的作用就是杀掉这种"语义碰瓷"的内容
        // 最后 limit 截断，且确保至少返回 1 条（Math.max(1, limit)）
        return suppressWeakVectorOnlyTail(results).stream()
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    // 安全判空：如果传入 null，返回空列表
    private static List<RetrievedKnowledgeDocument> safeList(List<RetrievedKnowledgeDocument> results) {
        return results == null ? List.of() : results;
    }

    // documentKey 用来在 Map 里判断"两条结果是否是同一份资料"
    // 拼接规则：sourceId + 标题 + 内容的哈希值
    // 为什么哈希 content 而不是直接用 content？
    //   因为 content 可能很长，用 hashCode 的十六进制字符串当 key 更省内存
    // 为什么用 Integer.toHexString？
    //   把 hashCode 转成简短字符串，比直接放 int 当 key toString 更可读
    private static String documentKey(KnowledgeDocument document) {
        return String.join("|",
                document.getSourceId(),
                Objects.toString(document.getTitle(), ""),
                Integer.toHexString(Objects.hashCode(document.getContent())));
    }

    // 安全转 topK：null/0 走默认值 3，超过上限 5 的截到 5
    private int safeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    // =====================================================================
    // suppressWeakVectorOnlyTail: 混合检索的"质检员"
    //
    // 这个函数解决一个问题：
    //   向量检索会捞回一些语义有点相近但实际不相关的内容
    //   这些内容的向量得分低（60 分以下），但又因为没被关键字命中所以排在末尾
    //   如果不过滤，这些"碰瓷"内容会作为纯向量结果排最下面，反而误导大模型
    //
    // 算法分两条路径：
    //
    // 情况 A：有"带关键字得分的锚点"（bestAnchoredVectorScore > 0）
    //   取这些锚点的向量得分最大值作为基准
    //   低于基准 × 0.95 的纯向量结果 → 砍掉
    //   含义：关键字命中的那条资料向量得分如果是 100，纯向量的至少要 ≥ 95
    //
    // 情况 B：完全没有关键字命中
    //   纯向量独苗场景，取最高向量得分
    //   低于最高分 × 0.90 且低于 50 分 → 整批丢掉（说明整个检索都没找到靠谱结果）
    // =====================================================================
    private static List<RetrievedKnowledgeDocument> suppressWeakVectorOnlyTail(List<RetrievedKnowledgeDocument> results) {
        // 找出"既被关键字命中、又有向量得分"的结果中的最高向量得分
        // 这个最高分就是"锚点"——标杆值
        int bestAnchoredVectorScore = results.stream()
                .filter(result -> result.getKeywordScore() > 0)
                .mapToInt(RetrievedKnowledgeDocument::getVectorScore)
                .max()
                .orElse(0);

        if (bestAnchoredVectorScore <= 0) {
            // ★ 情况 B：完全没有关键字命中，纯靠向量
            int bestVectorOnlyScore = results.stream()
                    .mapToInt(RetrievedKnowledgeDocument::getVectorScore)
                    .max()
                    .orElse(0);
            // 如果最高向量分还不到 50，说明整个检索都没产生"勉强可用"的结果
            // 返回空列表，让上层知道"啥都没找到"
            if (bestVectorOnlyScore < MIN_VECTOR_ONLY_ACCEPT_SCORE_WITHOUT_KEYWORD) {
                return List.of();
            }
            // 取 50 分和最高分 × 0.90 中较大的那个
            int minVectorOnlyScore = Math.max(
                    MIN_VECTOR_ONLY_ACCEPT_SCORE_WITHOUT_KEYWORD,
                    (int) Math.ceil(bestVectorOnlyScore * 0.90d)
            );
            // 只保留向量得分 ≥ 门槛的
            return results.stream()
                    .filter(result -> result.getVectorScore() >= minVectorOnlyScore)
                    .collect(Collectors.toList());
        }

        // ★ 情况 A：有带关键字命中的"锚点"
        // 取 50 分和锚点向量分 × 0.95 中较大的那个
        int minVectorOnlyScore = Math.max(
                MIN_VECTOR_ONLY_SCORE_WITH_KEYWORD_ANCHOR,
                (int) Math.ceil(bestAnchoredVectorScore * MIN_VECTOR_ONLY_RATIO_WITH_KEYWORD_ANCHOR)
        );
        // 保留条件：关键字命中了 → 无条件保留；关键字没命中但向量分够高 → 保留
        List<RetrievedKnowledgeDocument> filtered = results.stream()
                .filter(result -> result.getKeywordScore() > 0 || result.getVectorScore() >= minVectorOnlyScore)
                .collect(Collectors.toList());
        // 兜底：如果过滤后列表为空（极少情况），返回原始列表，至少让上层"有点东西"
        return filtered.isEmpty() ? results : filtered;
    }

    // =====================================================================
    // MergeCandidate: 合并关键字和向量得分的"小账本"
    // 每个 MergeCandidate 对应一份知识文档
    //
    // 打个比方：
    //   你问"停水了怎么办"
    //     关键字路搜到资料 A（得分 80）和资料 B（得分 30）
    //     向量路搜到资料 B（得分 75）和资料 C（得分 60）
    //   MergeCandidate 会把同一份资料 B 的关键字 30 和向量 75 合并到一起算分
    // =====================================================================
    private static final class MergeCandidate {
        private final KnowledgeDocument document;
        private int keywordScore;   // 关键字得分（关键词命中越准越高）
        private int vectorScore;    // 向量语义得分（余弦相似度 × 100）

        private MergeCandidate(KnowledgeDocument document) {
            this.document = document;
        }

        // 关键字得分：取所有关键字结果中的最大值
        // 为什么取 max 不是累加？
        //   同一条资料被关键字路多次召回时，取最高分就够了
        //   累加会导致同一条资料的分被重复计算
        private void acceptKeyword(RetrievedKnowledgeDocument result) {
            this.keywordScore = Math.max(this.keywordScore, result.getKeywordScore());
        }

        // 向量得分：同样的取最大值策略
        private void acceptVector(RetrievedKnowledgeDocument result) {
            this.vectorScore = Math.max(this.vectorScore, result.getVectorScore());
        }

        // 把两份得分合并成最终的 RetrievedKnowledgeDocument
        // 核心公式（见 mergedScore 方法）：
        //   finalScore = max(关键字分, 向量分 × 0.35) + (双命中 ? 5 : 0)
        // 同时记录 retrievalMode：HYBRID / KEYWORD / VECTOR
        // 面试官可能问："你这结果是靠什么搜出来的"
        private RetrievedKnowledgeDocument toResult() {
            int finalScore = mergedScore(keywordScore, vectorScore);
            return new RetrievedKnowledgeDocument(
                    document,
                    finalScore,
                    keywordScore,
                    vectorScore,
                    retrievalMode(keywordScore, vectorScore)
            );
        }

        // ★ 核心打分公式
        // vectorContribution = round(vectorScore × 0.35)
        //   → 0.35 是"向量权重"，为什么是 0.35 不是 0.5？
        //     经验值调优出来的——向量权重太高会拉低关键字命中的排序效果
        //     太低了向量几乎不起作用。0.35 是个折中
        //
        // baseScore = max(keywordScore, vectorContribution)
        //   → 两路中取分更高的那个作为底分
        //   → 如果关键字分 80，向量贡献只有 35，baseScore 就是 80
        //   → 如果关键字没命中（0分），向量贡献 42，baseScore 就是 42
        //
        // if (两路都命中) baseScore += 5
        //   → 双命中加 5 分，鼓励"关键字和语义都匹配"的高质量结果排在前面
        private int mergedScore(int keywordScore, int vectorScore) {
            int vectorContribution = Math.round(vectorScore * 0.35f);
            int baseScore = Math.max(keywordScore, vectorContribution);
            if (keywordScore > 0 && vectorScore > 0) {
                baseScore += HYBRID_MATCH_BONUS;
            }
            return baseScore;
        }

        // retrievalMode 标记这条结果的来源：
        //   HYBRID  - 关键字+向量双命中（最可靠）
        //   KEYWORD - 仅关键字命中（精确匹配）
        //   VECTOR  - 仅向量命中（语义兜底，可靠性一般）
        private String retrievalMode(int keywordScore, int vectorScore) {
            if (keywordScore > 0 && vectorScore > 0) {
                return "HYBRID";
            }
            if (keywordScore > 0) {
                return "KEYWORD";
            }
            return "VECTOR";
        }
    }
}

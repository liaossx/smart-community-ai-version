# AI 智能客服面试准备

---

## 面试官会怎么问（逐层深入）

面试官看到这条大概率按这个顺序往下挖：

```
第一层：RAG 是什么，为什么不用直接调大模型？
         └── 追问：检索是怎么做的？

第二层：关键字和向量各自怎么检索？怎么合并的？
         └── 追问：权重怎么定的？

第三层：检索不到资料怎么办？模型编造了引用怎么处理？

第四层：向量存在哪？用的什么 Embedding 模型？

第五层：DeepSeek 调一次多久？token 消耗多少？
```

---

## 一、RAG 是什么？为什么不做直接问答？

**回答（30 秒）**：

> 直接调 DeepSeek 它不知道本社区的信息——维修电话是多少、停水通知发了没——只能胡编。RAG 就是在调模型之前先从 MySQL 知识库检索相关的社区资料，拼进 Prompt 再让模型回答。这样模型有了依据，回答可以追溯——每条回答的 citations 都对应知识库里真实的 sourceId。

---

## 二、混合检索怎么做？权重怎么定的？

**回答（40 秒）**：

> 单靠关键字：精确但僵硬——"天花板往下淌水"匹配不到"漏水"这条资料。单靠向量语义：覆盖面广但可能召回不相关内容。所以两路都搜，加权合并。
>
> 公式是 `finalScore = max(关键字得分, 向量得分 × 0.35) + 双命中加 5 分`。关键字权重高——因为它精确匹配到的资料置信度更高。向量起辅助兜底作用——权重 0.35 让它能在排序里露脸，但不会盖过关键字命中的结果。

**对应代码**：[HybridCommunityKnowledgeRetriever.java](/D:/CCproject/smart-community-ai-version/ai-service/src/main/java/com/lsx/ai/customer/knowledge/HybridCommunityKnowledgeRetriever.java)

---

## 三、检索不到资料怎么办？

**回答（20 秒）**：

> 不调 DeepSeek——直接返回兜底回答"请联系物业人工客服确认"。不调模型既是省钱，也是防止模型在没有资料时编造内容。Lua 脚本执行前也会先检查——documents 为空就直接返回，代码里在前面就短路了。

**对应代码**：[RagCustomerServiceAssistant.java](/D:/CCproject/smart-community-ai-version/ai-service/src/main/java/com/lsx/ai/customer/service/RagCustomerServiceAssistant.java) `if (documents.isEmpty())` 处

---

## 四、模型编造了引用你怎么处理？（抑制幻觉）

**回答（30 秒）**：

> Normalizer 在模型返回后做一层安全校验。每次检索出的资料都有一个 sourceId——Normalizer 先收集这些 sourceId 组成白名单。模型返回的 citations 逐个检查——在白名单里就保留，不在就直接丢弃。另外 confidence 做限幅——模型不能随便写个 999。answer 为空时补一段兜底文案。

**对应代码**：[CustomerServiceAnswerNormalizer.java](/D:/CCproject/smart-community-ai-version/ai-service/src/main/java/com/lsx/ai/customer/service/CustomerServiceAnswerNormalizer.java) `normalizeCitations()` 方法

---

## 五、向量存在哪？为什么不用专门的向量数据库？

**回答（20 秒）**：

> 向量存在 MySQL 的 `ai_knowledge_embedding` 表里。用 MySQL 而不是 Milvus 或 Pinecone 的原因很简单——社区知识库几百条资料，MySQL 的余弦相似度计算完全够用，不需要引入新组件增加运维复杂度。开发阶段用最简单的方案，真正量大了再换。

---

## 六、Embedding 模型用的什么？

**回答（15 秒）**：

> 本地开发阶段用的 Hash Embedding——128 维、本地计算、不用调外部 API。上线可以切 OpenAI 兼容的实现——把 `AI_EMBEDDING_PROVIDER` 环境变量从 `hash` 改成 `openai`，底层自动切到 `text-embedding-3-small`，1536 维。代码不用改。

---

## 七、DeepSeek 调一次多久？token 消费多少？

**回答（20 秒）**：

> 一次调用 10 到 30 秒不等——因为有 thinking mode，模型先内部推理再输出。token 消费看 prompt 长度——system prompt 大概 200 token，加上检索回来的资料和用户问题，一次调用输入在 500-1000 token 左右。输出回答一般 100-200 token。这块我在可观测性日志里记录了每次调用的耗时和成本。

---

## 八、系统提示词怎么设计的？

**回答（20 秒）**：

> system prompt 五个要素：角色设定（你是智能社区客服助手）、能力边界（只能根据提供的资料回答）、兜底规则（资料不足时 cannotAnswer 为 true）、枚举约束（citations 只能填资料中的 sourceId）、输出格式（返回结构化 JSON）。另外 system prompt 写在 static final 常量里，利用了 DeepSeek KV Cache——固定前缀每次都是命中缓存，比动态拼接便宜很多。

**对应代码**：[RagCustomerServiceAssistant.java](/D:/CCproject/smart-community-ai-version/ai-service/src/main/java/com/lsx/ai/customer/service/RagCustomerServiceAssistant.java) `SYSTEM_PROMPT` 常量

---

## 九、你做过这个模块后最大的收获是什么？

**回答（20 秒）**：

> 最大的收获是理解了"大模型不是调一下 API 就行了"。从检索到 Prompt 拼装、到模型调用、到输出校验、到可观测性——每一层都有工程问题要解决。特别是 Normalizer 那一层——模型输出是不可靠的，你必须在系统层面兜底。这个经验对我以后做任何 AI 工程化都有帮助。

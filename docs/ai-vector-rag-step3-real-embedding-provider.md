# 向量 RAG 第三步：真实 Embedding Provider

这一步不是再改检索逻辑，而是把之前演示用的：

```text
HASH embedding
```

升级成：

```text
真实 embedding 模型
```

当前实现保留了两种 provider：

```text
hash    本地演示版，不依赖外部模型服务
openai  真实版，走 Spring AI 的 OpenAI-compatible EmbeddingModel
```

## 这一步做了什么

### 1. 新增真实 Embedding Provider

代码位置：

```text
ai-service/src/main/java/com/lsx/ai/knowledge/embedding/OpenAiCompatibleEmbeddingProvider.java
```

职责：

```text
调用 Spring AI 的 EmbeddingModel.embed(text)
 -> 得到真实 embedding 向量
 -> 转成 EmbeddingVector
 -> 继续复用现有重建向量、向量检索、混合检索链路
```

所以你现在的链路已经变成：

```text
知识入库
 -> chunk 切分
 -> 真实 embedding 入库
 -> query embedding
 -> hybrid retrieval
 -> Prompt
 -> 大模型回答
```

### 2. 配置层支持 embedding 单独走一个模型服务

代码位置：

```text
ai-service/src/main/resources/application.yml
```

新增配置：

```yaml
spring:
  ai:
    openai:
      embedding:
        api-key: ${AI_EMBEDDING_OPENAI_API_KEY:${AI_OPENAI_API_KEY:}}
        base-url: ${AI_EMBEDDING_OPENAI_BASE_URL:${AI_OPENAI_BASE_URL:https://api.openai.com}}
        embeddings-path: ${AI_EMBEDDING_OPENAI_EMBEDDINGS_PATH:/v1/embeddings}
        options:
          model: ${AI_EMBEDDING_OPENAI_MODEL:text-embedding-3-small}
          dimensions: ${AI_EMBEDDING_OPENAI_DIMENSION:1536}

smart-community:
  ai:
    embedding:
      provider: ${AI_EMBEDDING_PROVIDER:hash}
      openai:
        model: ${AI_EMBEDDING_OPENAI_MODEL:text-embedding-3-small}
        dimension: ${AI_EMBEDDING_OPENAI_DIMENSION:1536}
```

## 为什么这样设计

因为你的聊天模型和 embedding 模型不一定是同一个服务。

例如：

```text
chat 继续走 DeepSeek
embedding 单独走一个兼容 OpenAI 的 embedding 服务
```

这样是很常见的工程做法。

## 你怎么切换到真实 provider

把运行配置里的环境变量改成：

```text
AI_EMBEDDING_PROVIDER=openai
AI_EMBEDDING_OPENAI_API_KEY=你的embedding服务key
AI_EMBEDDING_OPENAI_BASE_URL=你的embedding服务base-url
AI_EMBEDDING_OPENAI_EMBEDDINGS_PATH=/v1/embeddings
AI_EMBEDDING_OPENAI_MODEL=你的embedding模型名
AI_EMBEDDING_OPENAI_DIMENSION=1536
```

如果你的聊天继续走 DeepSeek，那么：

```text
AI_OPENAI_API_KEY=你的DeepSeek聊天key
AI_OPENAI_BASE_URL=你的DeepSeek聊天base-url
AI_OPENAI_CHAT_COMPLETIONS_PATH=/chat/completions
AI_OPENAI_CHAT_MODEL=deepseek-v4-flash
```

两套配置可以并存。

## 切换后的操作步骤

1. 重启 `AiServiceApplication`
2. 在前端点击“重建向量”
3. 观察返回里的 provider 和 model
4. 再次测试客服问答

## 如果暂时没有真实 embedding 服务

那就继续保留：

```text
AI_EMBEDDING_PROVIDER=hash
```

这不会影响你当前项目演示。

## 面试里可以怎么讲

你可以这样说：

> 为了先跑通 RAG 核心链路，第一版使用本地 Hash Embedding 完成 chunk 向量化、相似度检索和混合召回。第二版把 Embedding Provider 抽象成可替换接口，再接入 Spring AI 的 OpenAI-compatible EmbeddingModel，使聊天模型与 embedding 模型可以分离配置，既适合演示，也更接近生产中的多 Provider 架构。

# ai-service

Independent Spring Boot 3 / Java 17 service for smart community AI features.

This service is intentionally separate from the existing Java 8 / Spring Boot 2.7 services. It lets the project use Spring AI without forcing a full legacy-service upgrade.

## Work Order Analysis API

```http
POST /api/ai/workorder/analyze
Content-Type: application/json
```

Request:

```json
{
  "repairId": 201011,
  "faultType": "水管",
  "faultDesc": "3栋2单元1801厨房水槽下方漏水，水已经流到客厅。"
}
```

Response uses the same contract as `workorder-service` AI snapshots:

```json
{
  "category": "WATER",
  "priority": 3,
  "priorityDesc": "紧急",
  "urgencyLevel": "HIGH",
  "riskLevel": "MEDIUM",
  "recommendedTeam": "水暖维修组",
  "suggestedAction": "建议先联系住户确认漏水范围...",
  "summary": "住户反馈厨房水槽下方漏水...",
  "extractedLocation": "3栋2单元1801厨房水槽下方",
  "suggestedResponseMinutes": 30,
  "matchedKeywords": ["漏水", "水槽"],
  "safetyTips": ["提醒住户关闭就近阀门"],
  "confidence": 85,
  "manualReviewNeeded": false,
  "provider": "SPRING_AI",
  "providerVersion": "spring-ai-v1",
  "model": "deepseek-v4-flash"
}
```

## Configuration

Use Java 17 before building or running this module.

```powershell
$env:JAVA_HOME="C:\Path\To\jdk-17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

OpenAI-compatible configuration:

```powershell
$env:AI_OPENAI_API_KEY="sk-..."
$env:AI_OPENAI_CHAT_MODEL="gpt-4.1-mini"
```

Optional:

```powershell
$env:AI_OPENAI_BASE_URL="https://api.openai.com"
$env:AI_SERVICE_PORT="8090"
```

DeepSeek example:

```powershell
$env:AI_OPENAI_API_KEY="你的 DeepSeek API Key"
$env:AI_OPENAI_BASE_URL="https://api.deepseek.com"
$env:AI_OPENAI_CHAT_COMPLETIONS_PATH="/chat/completions"
$env:AI_OPENAI_CHAT_MODEL="deepseek-v4-flash"
```

Use `deepseek-v4-flash` first for normal structured extraction. Use `deepseek-v4-pro` later if you want stronger reasoning behavior.

Run tests:

```powershell
mvn -f ai-service/pom.xml test
```

Run service:

```powershell
mvn -f ai-service/pom.xml spring-boot:run
```

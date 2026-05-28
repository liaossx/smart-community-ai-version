# AI Work Order Assistant

Phase one of the AI work order assistant lives in `workorder-service`. It analyzes existing repair reports, stores durable AI analysis snapshots, and helps initialize work order priority without taking over dispatch.

The enhanced rule provider is `rule-v1.1`. It is still deterministic, but its output contract is shaped so a future Spring AI or RAG-backed provider can return the same fields.

This document records the database and API usage that frontend, backend, and operations work should follow.

## Database

The schema is defined in `sql/workorder_ai_analysis_schema.sql`.

### `biz_work_order_ai_analysis`

`biz_work_order_ai_analysis` stores one AI analysis snapshot per analysis run.

Important fields:

| Field | Meaning |
| --- | --- |
| `id` | Snapshot primary key. |
| `repair_id` | Related repair report id from `biz_repair`; required. |
| `work_order_id` | Optional related work order id. Phase one can leave this null. |
| `category` | Rule provider category, such as `WATER`, `ELECTRICAL`, `ELEVATOR`, `PUBLIC_FACILITY`, `BUILDING`, or `OTHER`. |
| `priority` | AI recommended priority from 1 to 4. Generated work orders may copy this value. |
| `urgency_level` | AI urgency level: `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`. This explains the numeric priority for AI and staff-facing workflows. |
| `risk_level` | Risk level: `LOW`, `MEDIUM`, or `HIGH`. |
| `recommended_team` | Suggested maintenance team. This is a team hint, not a worker assignment. |
| `suggested_action` | Suggested handling action for staff. |
| `summary` | Short explanation of the repair report. |
| `extracted_location` | Location supplement extracted from the repair text, such as `3栋2单元1801厨房水槽下方` or `地下车库B区电梯口`. This does not replace the canonical `house_id`. |
| `suggested_response_minutes` | AI suggested response time limit in minutes. This is not estimated repair duration. |
| `safety_tips` | JSON array text of staff or resident safety reminders. |
| `matched_keywords` | Comma-separated keywords that explain why the rule provider matched. |
| `confidence` | Integer confidence from 0 to 100. Lower values require more staff review. |
| `manual_review_needed` | Whether staff should review before acting on the recommendation. |
| `provider` | Provider source. Phase one uses `RULE`. |
| `provider_version` | Provider version, such as `rule-v1.1`. |
| `latest` | Whether this snapshot is the latest snapshot for the repair report. |
| `create_time` | Snapshot creation time. |

Indexes:

- `idx_repair_latest` on `repair_id`, `latest` supports normal latest-snapshot reads.
- `idx_work_order_id` supports future joins from work orders to analysis snapshots.

## Latest Snapshot Rule

A repair report can have multiple snapshots. Manual re-analysis creates a new snapshot instead of overwriting history.

Normal reads and AI-assisted work order creation use the latest snapshot:

- `POST /api/workorder/ai/repair/{repairId}/analyze` creates a new latest snapshot.
- Before saving the new snapshot, previous latest snapshots for the same `repair_id` are marked `latest = false`.
- The new snapshot is saved with `latest = true`.
- `GET /api/workorder/ai/repair/{repairId}/latest` returns the current latest snapshot.
- `GET /api/workorder/ai/repair/{repairId}/history` returns all snapshots for audit use, newest first.

If a repair report exists but has no snapshot, the latest API returns a failure response with `AI analysis snapshot not found`. History returns an empty list.

## API Usage

All phase-one APIs use the existing `Result` response envelope.

### Create Manual Analysis

`POST /api/workorder/ai/repair/{repairId}/analyze`

Use this when staff explicitly requests AI analysis for an existing repair report.

Behavior:

- Validates that the repair report exists.
- Runs the rule-first provider.
- Persists a new analysis snapshot.
- Marks older snapshots for the same repair report as not latest.
- Returns the created snapshot.

Failure behavior:

- Missing repair report returns a failure response with `Repair report not found`.

### Read Latest Analysis

`GET /api/workorder/ai/repair/{repairId}/latest`

Use this on repair detail screens or backend flows that need the current recommendation without creating a new analysis.

Behavior:

- Validates that the repair report exists.
- Returns the current latest snapshot.
- Does not create or modify snapshots.

Failure behavior:

- Missing repair report returns `Repair report not found`.
- Existing repair report with no snapshot returns `AI analysis snapshot not found`.

### Read Analysis History

`GET /api/workorder/ai/repair/{repairId}/history`

Use this for audit screens and troubleshooting repeated analyses.

Behavior:

- Validates that the repair report exists.
- Returns all snapshots for the repair report, newest first.
- Includes each snapshot's `latest` flag.
- Does not create or modify snapshots.

Failure behavior:

- Missing repair report returns `Repair report not found`.
- Existing repair report with no snapshots returns an empty list.

## Work Order Creation Boundary

When a work order is generated from a repair report, `WorkOrderServiceImpl#createFromRepair` reuses AI analysis in this order:

1. Try to read the latest snapshot.
2. If no latest snapshot exists, create one automatically.
3. copy only `priority` from the analysis into the generated work order.
4. If AI analysis fails, use default priority `1` and continue creating the work order.

Phase one deliberately preserves human-controlled dispatch:

- AI does not assign a specific worker.
- AI does not set `worker_id`, `worker_name`, or `worker_phone`.
- `recommended_team` stays in the AI snapshot as a staff-facing suggestion.
- `extracted_location` is an AI text extraction hint only. Canonical address and community ownership still come from `repair.house_id`, `repair.community_id`, and `house-service`.
- Existing duplicate work order behavior remains unchanged: if a work order already exists for the repair report, that work order is returned.

## Provider Boundary

Phase one uses a deterministic rule-first provider. The provider is exposed through `AiWorkOrderAnalysisProvider`, and the service layer consumes that interface instead of depending on rule implementation details.

`rule-v1.1` keeps machine fields stable and staff-facing fields readable:

- Machine fields stay enum-like: `category`, `priority`, `urgency_level`, `risk_level`, `provider`, and `provider_version`.
- Staff-facing fields use Chinese copy: `recommended_team`, `suggested_action`, `summary`, and `safety_tips`.
- If multiple repair categories are detected, the provider selects one primary category by business risk priority and sets `manual_review_needed` with a collaboration note in `suggested_action`.

## Provider Selection

`workorder-service` routes AI analysis through `RoutingAiWorkOrderAnalysisProvider`. The active provider is selected by configuration:

```yaml
smart-community:
  ai:
    workorder:
      provider: rule
```

The same value can be supplied with the `AI_WORKORDER_PROVIDER` environment variable.

Supported values:

| Value | Behavior |
| --- | --- |
| `rule` | Uses `RuleAiWorkOrderAnalysisProvider`. This is the default and current production-safe mode. |
| `spring-ai` | Uses `SpringAiWorkOrderAnalysisProvider` to call the standalone `ai-service`. If the remote call fails or returns no result, the router logs a warning and falls back to `rule`. |

When `spring-ai` is selected, `workorder-service` calls the standalone AI service:

```yaml
smart-community:
  ai:
    workorder:
      provider: spring-ai
      ai-service-url: http://localhost:8090
```

The same settings can be supplied with environment variables:

```text
AI_WORKORDER_PROVIDER=spring-ai
AI_SERVICE_URL=http://localhost:8090
```

The remote endpoint is:

```http
POST /api/ai/workorder/analyze
```

This keeps phase one runnable without API keys or network calls while reserving a provider boundary for a future model provider. Future model integration should preserve the persisted snapshot contract and continue setting `provider` and `provider_version` so reports can distinguish rule output from model output.

## Source Of Truth

This document follows:

- `.scratch/ai-work-order-assistant/PRD.md`
- `docs/adr/0001-persist-ai-work-order-analysis.md` (ADR-0001)
- `docs/adr/0002-rule-first-ai-work-order-provider.md` (ADR-0002)

If implementation changes, update this document and the documentation coverage test together.

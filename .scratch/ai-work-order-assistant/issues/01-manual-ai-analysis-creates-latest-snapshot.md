Status: ready-for-agent

# Manual AI Analysis Creates Latest Snapshot

## Parent

.scratch/ai-work-order-assistant/PRD.md

## What to build

Build the first complete vertical slice of the AI work order assistant: staff can trigger AI analysis for an existing repair report, the system produces an AI analysis result through the rule-first provider, persists it as an AI analysis snapshot, and marks it as the latest snapshot for that repair report.

This slice should establish the core domain model, persistence behavior, provider boundary, and manual analysis API needed by the rest of phase one.

## Acceptance criteria

- [x] `POST /api/workorder/ai/repair/{repairId}/analyze` analyzes an existing repair report and returns the created AI analysis snapshot in the existing `Result` response envelope.
- [x] The AI analysis result includes category, priority, risk level, recommended team, suggested action, summary, matched keywords, confidence, manual review needed, and provider/source metadata.
- [x] The created snapshot is persisted in a dedicated AI analysis snapshot table.
- [x] Re-analyzing the same repair report creates a new snapshot instead of overwriting the old one.
- [x] When a new snapshot is created, previous snapshots for the same repair report are no longer latest and the new snapshot becomes latest.
- [x] A missing repair report returns a clear failure response and does not create a snapshot.
- [x] The rule-first provider is behind a replaceable service boundary so a future model provider can be added without changing controller or persistence contracts.
- [x] Service-level tests cover successful analysis, missing repair report, latest snapshot replacement, and representative rule provider output.

## Blocked by

None - can start immediately

## Comments

- Implemented with service-level TDD around `AiWorkOrderAnalysisService`, a rule-first provider, MyBatis-backed snapshot store, the manual analyze API, and `sql/workorder_ai_analysis_schema.sql`.

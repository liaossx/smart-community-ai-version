Status: ready-for-agent

# Document Database And API Usage For AI Work Order Assistant

## Parent

.scratch/ai-work-order-assistant/PRD.md

## What to build

Document the AI work order assistant's database and API usage so future backend, frontend, and operations work can integrate with phase one consistently.

This slice should capture the durable behavior created by the previous implementation slices.

## Acceptance criteria

- [x] SQL/schema documentation describes the AI analysis snapshot table and its important fields.
- [x] Documentation describes the latest snapshot rule: a repair report can have multiple snapshots, but normal reads and work order creation use the latest snapshot.
- [x] Documentation describes the three phase-one APIs and their intended usage.
- [x] Documentation states that generated work orders copy only priority from AI analysis.
- [x] Documentation states that AI does not assign a specific worker in phase one.
- [x] Documentation states that phase one uses the rule-first provider and reserves a provider boundary for future model integration.
- [x] Documentation is consistent with the PRD and ADR-0001/ADR-0002.

## Blocked by

- .scratch/ai-work-order-assistant/issues/01-manual-ai-analysis-creates-latest-snapshot.md
- .scratch/ai-work-order-assistant/issues/02-read-latest-ai-analysis-for-repair-report.md
- .scratch/ai-work-order-assistant/issues/03-read-ai-analysis-history-for-repair-report.md
- .scratch/ai-work-order-assistant/issues/04-use-ai-priority-during-work-order-creation.md

## Comments

- Added `docs/ai-work-order-assistant.md` covering the AI analysis snapshot table, important fields, latest snapshot rule, three phase-one APIs, work order creation boundary, human-controlled dispatch boundary, rule-first provider boundary, and PRD/ADR source-of-truth references.
- Added `AiWorkOrderAssistantDocumentationTest` so future changes keep the database/API usage documentation present and aligned with phase-one contracts.
- Verified with `mvn -pl workorder-service test` (`Tests run: 30, Failures: 0, Errors: 0, Skipped: 0`).

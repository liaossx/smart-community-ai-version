Status: ready-for-agent

# Read AI Analysis History For Repair Report

## Parent

.scratch/ai-work-order-assistant/PRD.md

## What to build

Expose all AI analysis snapshots for a repair report so staff can audit repeated analyses and see how recommendations changed over time.

This slice completes the audit/history read path.

## Acceptance criteria

- [x] `GET /api/workorder/ai/repair/{repairId}/history` returns all AI analysis snapshots for an existing repair report in the existing `Result` response envelope.
- [x] History results are ordered predictably, with the newest snapshot first unless the existing project conventions strongly prefer another order.
- [x] Each history item shows whether it is the latest snapshot.
- [x] If the repair report exists but has no AI analysis snapshots, the API returns an empty list.
- [x] If the repair report does not exist, the API returns a clear failure response.
- [x] The history read path does not create or modify snapshots.
- [x] Tests cover multiple snapshots, ordering, latest marker visibility, empty history, and missing repair reports.

## Blocked by

- .scratch/ai-work-order-assistant/issues/01-manual-ai-analysis-creates-latest-snapshot.md

## Comments

- Implemented `AiWorkOrderAnalysisService#getAnalysisHistory` and `GET /api/workorder/ai/repair/{repairId}/history`. The history path validates that the repair report exists, returns newest-first snapshots, returns an empty list when no snapshots exist, and does not mutate snapshot state.

Status: ready-for-agent

# Read Latest AI Analysis For Repair Report

## Parent

.scratch/ai-work-order-assistant/PRD.md

## What to build

Expose the latest AI analysis snapshot for a repair report so staff and future repair detail screens can retrieve the current recommendation without triggering a new analysis.

This slice completes the read path for the default AI analysis result.

## Acceptance criteria

- [x] `GET /api/workorder/ai/repair/{repairId}/latest` returns the latest AI analysis snapshot for an existing repair report in the existing `Result` response envelope.
- [x] The response includes the full AI analysis result: category, priority, risk level, recommended team, suggested action, summary, matched keywords, confidence, manual review needed, and provider/source metadata.
- [x] If the repair report exists but has no AI analysis snapshot, the API returns a clear empty or failure response according to existing project conventions.
- [x] If the repair report does not exist, the API returns a clear failure response.
- [x] The read path does not create a new AI analysis snapshot.
- [x] Tests cover returning the latest snapshot, ignoring older snapshots, handling no snapshot, and handling missing repair reports.

## Blocked by

- .scratch/ai-work-order-assistant/issues/01-manual-ai-analysis-creates-latest-snapshot.md

## Comments

- Implemented `AiWorkOrderAnalysisService#getLatestAnalysis` and `GET /api/workorder/ai/repair/{repairId}/latest`. Missing repair reports fail with `Repair report not found`; existing repair reports without snapshots fail with `AI analysis snapshot not found`.

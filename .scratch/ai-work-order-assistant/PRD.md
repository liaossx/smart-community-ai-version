Status: ready-for-agent

# PRD: AI Work Order Assistant Phase One

## Problem Statement

Property staff currently handle repair reports and work orders mostly through manual judgment. When a resident submits a repair report, staff need to read the fault type and description, estimate urgency, decide which maintenance group should handle it, and decide whether the report carries safety risk. This creates inconsistent priority decisions, slower handling for urgent reports, and weak traceability around why a work order was treated as urgent or normal.

The project needs an AI work order assistant that improves staff decision quality without making the first release dependent on external model providers or fully automated dispatch.

## Solution

Build phase one of the AI work order assistant inside `workorder-service`.

The assistant analyzes existing repair reports and produces an AI analysis result containing category, priority, risk level, recommended team, suggested action, summary, matched keywords, confidence, and whether manual review is needed. Staff can trigger the analysis manually from a repair report, retrieve the latest analysis result, and view historical AI analysis snapshots for auditability.

When a work order is generated from a repair report, the system reuses the latest AI analysis snapshot if one exists. If none exists, it creates one automatically. The generated work order copies only the recommended priority from the AI analysis result. It does not assign a specific worker.

Phase one uses a deterministic rule-first provider so the workflow is stable, testable, and runnable without an API key. The service boundary should allow a future model provider to replace or augment the rule provider.

## User Stories

1. As a property administrator, I want to trigger AI analysis for a repair report, so that I can quickly understand the fault category and urgency.
2. As a property administrator, I want the AI analysis result to include a concise summary, so that I can understand the resident's issue without rereading the full description every time.
3. As a property administrator, I want the AI analysis result to recommend a priority, so that urgent work orders are easier to identify.
4. As a property administrator, I want the AI analysis result to include a risk level, so that I can notice reports that may require immediate attention.
5. As a property administrator, I want the AI analysis result to recommend a maintenance team, so that I can decide who should handle the issue without the AI assigning a specific worker.
6. As a property administrator, I want the AI analysis result to include suggested handling actions, so that I have a starting point for communicating with maintenance staff.
7. As a property administrator, I want the AI analysis result to show matched keywords, so that I can understand why the assistant made its recommendation.
8. As a property administrator, I want the AI analysis result to include confidence, so that I can judge whether to trust the recommendation.
9. As a property administrator, I want the AI analysis result to flag manual review when needed, so that uncertain or high-risk reports stay under human control.
10. As a property administrator, I want to retrieve the latest AI analysis result for a repair report, so that the repair detail page can show the current recommendation.
11. As a property administrator, I want to view AI analysis history for a repair report, so that I can audit how recommendations changed over time.
12. As a property administrator, I want repeated analysis to create snapshots instead of overwriting history, so that past recommendations remain traceable.
13. As a property administrator, I want only one AI analysis snapshot marked as latest per repair report, so that the UI and work order flow have a clear default recommendation.
14. As a property administrator, I want generated work orders to reuse the latest AI analysis result, so that manual analysis is not wasted.
15. As a property administrator, I want generated work orders to create AI analysis automatically when no latest snapshot exists, so that work orders still receive priority assistance.
16. As a property administrator, I want AI-assisted work order creation to copy only priority into the work order, so that the existing dispatch workflow remains under staff control.
17. As a maintenance worker, I want AI suggestions to avoid directly assigning me work without staff confirmation, so that assignment remains accountable.
18. As a system operator, I want AI analysis results persisted independently from work orders, so that future reports can measure AI usage and adoption.
19. As a developer, I want the rule-first provider to expose a simple provider interface, so that a future model provider can be added without rewriting controller or persistence logic.
20. As a developer, I want the rule engine tested through behavior-focused cases, so that category, priority, risk, and manual review decisions remain stable.
21. As a developer, I want the AI analysis snapshot service tested independently from controllers, so that latest snapshot behavior can be verified without full HTTP tests.
22. As a developer, I want work order creation tested for AI priority reuse, so that automatic work order generation keeps the agreed business boundary.
23. As a product owner, I want phase one to avoid real model API dependency, so that the feature can be developed and demonstrated locally.
24. As a product owner, I want the first phase limited to `workorder-service`, so that AI value is proven before creating a shared AI capability layer.
25. As a future maintainer, I want the AI analysis table to store provider/source metadata, so that rule-based and future model-based recommendations can be distinguished.

## Implementation Decisions

- The phase-one feature is called the AI work order assistant.
- The phase-one boundary is `workorder-service`; it does not create a shared AI capability layer yet.
- The assistant operates on existing repair reports, not arbitrary text previews.
- The assistant exposes three phase-one APIs:
  - `POST /api/workorder/ai/repair/{repairId}/analyze`
  - `GET /api/workorder/ai/repair/{repairId}/latest`
  - `GET /api/workorder/ai/repair/{repairId}/history`
- `POST /api/workorder/ai/analyze-preview` is out of scope for phase one.
- The AI analysis result includes category, priority, risk level, recommended team, suggested action, summary, matched keywords, confidence, and manual review needed.
- The AI analysis result does not include a specific worker assignment.
- AI analysis results are persisted as AI analysis snapshots in a dedicated table.
- A repair report can have multiple AI analysis snapshots.
- Exactly one snapshot should be treated as the latest snapshot for normal reads and work order reuse.
- New manual analysis creates a new snapshot and marks it as latest for that repair report.
- The persistence model should allow snapshots to reference the repair report and optionally the generated work order.
- Generated work orders reuse the latest AI analysis snapshot when one exists.
- Generated work orders create an AI analysis snapshot automatically when no latest snapshot exists.
- Generated work orders copy only AI-recommended priority into the work order.
- Generated work orders do not copy recommended team into worker fields and do not set worker id, worker name, or worker phone.
- The rule-first provider is the first implementation of AI analysis.
- The service boundary should make the provider replaceable by a future model provider.
- Rule provider output should include source/provider metadata so later statistics can distinguish rule and model output.
- Existing repair report submission should not fail because AI analysis is unavailable.
- Existing work order assignment and completion flows should keep their current behavior except for priority initialization during work order creation.
- Any existing early AI code should be reconciled with this PRD rather than treated as the final design.

## Testing Decisions

- Good tests should verify observable behavior: returned analysis fields, persisted latest snapshot behavior, and work order priority reuse. They should not assert private helper methods or keyword implementation details unless those are exposed as matched keywords.
- The rule provider should have focused unit tests covering water, electrical, elevator, public facility, building/general, high-risk, uncertain, and default cases.
- The AI analysis snapshot service should be tested for creating snapshots, marking previous snapshots as not latest, returning latest, and returning history in a predictable order.
- The work order creation flow should be tested for both paths: reusing an existing latest snapshot and creating a new snapshot when none exists.
- Controller tests or lightweight integration tests should verify the three API contracts return the expected `Result` envelope and handle missing repair reports cleanly.
- Tests should cover invalid repair ids, missing repair reports, provider failure fallback, and default priority behavior.
- Because this repository currently has limited visible test coverage, start with service-level tests for the deep modules and add controller tests after the service contracts are stable.

## Out of Scope

- A shared AI capability layer for all services.
- A real model provider or external LLM API integration.
- Fully automatic dispatch to a specific worker.
- Recommending `workerId`, `workerName`, or `workerPhone`.
- Blocking resident repair submission on AI analysis.
- AI preview for arbitrary text not tied to a repair report.
- AI complaint summary, notice generation, community content review, and operational analytics.
- Frontend UI implementation unless a later issue explicitly adds it.
- Training or fine-tuning a model.

## Further Notes

- This PRD follows ADR-0001, which requires persisting AI work order analysis snapshots.
- This PRD follows ADR-0002, which chooses a rule-first provider for phase one.
- The local issue tracker for follow-up tasks is `.scratch/ai-work-order-assistant/`.
- The next step is to run `/to-issues` against this PRD and split the work into independently implementable vertical slices.

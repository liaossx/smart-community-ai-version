Status: ready-for-agent

# Use AI Priority During Work Order Creation

## Parent

.scratch/ai-work-order-assistant/PRD.md

## What to build

Integrate the AI work order assistant into work order creation. When a work order is generated from a repair report, the system should reuse the latest AI analysis snapshot if one exists, or create one automatically if none exists. The generated work order copies only the AI-recommended priority.

This slice must preserve human-controlled dispatch: AI must not assign a specific worker.

## Acceptance criteria

- [x] Creating a work order for a repair report with an existing latest AI analysis snapshot copies that snapshot's priority into the work order.
- [x] Creating a work order for a repair report without a latest AI analysis snapshot automatically creates one and copies its priority into the work order.
- [x] Work order creation does not copy recommended team into worker fields.
- [x] Work order creation does not set worker id, worker name, or worker phone from AI output.
- [x] Existing duplicate work order behavior remains intact: if a work order already exists for the repair report, the existing work order is returned.
- [x] If AI analysis fails, work order creation still follows a safe default priority behavior and does not break resident or staff workflows.
- [x] Tests cover reuse of existing latest snapshot, automatic snapshot creation, worker fields remaining untouched, duplicate work order behavior, and AI failure fallback.

## Blocked by

- .scratch/ai-work-order-assistant/issues/01-manual-ai-analysis-creates-latest-snapshot.md

## Comments

- Implemented `WorkOrderServiceImpl#createFromRepair` integration with the persisted AI analysis service. Work order creation reuses the latest snapshot when present, creates one when absent, copies only `priority`, leaves worker assignment fields untouched, preserves duplicate work order short-circuit behavior, and falls back to priority `1` if AI analysis fails.
- Verified with `mvn -pl workorder-service test` (`Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`).

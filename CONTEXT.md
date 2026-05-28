# Smart Community Context

This file is the shared domain language for agent-assisted work in this repo. Keep it concise and update it when `/grill-with-docs` resolves new terms or decisions.

## Project Shape

This is a Java Spring Cloud smart community backend split into service modules:

- `gateway-service`: API gateway and cross-origin configuration.
- `common-module`: shared result types, security, logging, Redis, MQ, and utility code.
- `user-service`: user login, registration, profile, and related external lookups.
- `house-service`: community, house, and house binding workflows.
- `property-service`: property operations such as fees, notices, complaints, visitors, and express delivery.
- `parking-service`: parking spaces, vehicles, reservations, orders, account, gate, and lease workflows.
- `workorder-service`: repair reports, work orders, assignment, processing, and statistics.
- `community-service`: community topics, activities, and group workflows.
- `system-service`: system config, operation logs, admin statistics, and message listeners.

## Current Focus

The current modernization focus is the AI work order module in `workorder-service`.

## Glossary

- Repair report: A resident-submitted maintenance request stored in `biz_repair`.
- Work order: An actionable maintenance task generated from a repair report and stored in `biz_work_order`.
- AI work order assistant: The first-phase AI capability in `workorder-service`. It helps staff analyze repair reports by classifying repair content, recommending priority, identifying risk, suggesting handling actions, and optionally supporting dispatch decisions. It does not fully automate dispatch in phase one.
- AI analysis result: The AI work order assistant's output for a repair report. In phase one it includes category, priority, risk level, recommended team, suggested action, summary, matched keywords, confidence, and whether manual review is needed.
- AI analysis snapshot: A persisted AI analysis result for a repair report. A repair report may have multiple snapshots; the latest snapshot is the default one staff see and generated work orders reuse.
- Recommended team: The maintenance group suggested by the AI work order assistant, such as plumbing, electrical, elevator maintenance, public facility, or general maintenance. It is not a specific worker assignment.
- AI-assisted work order creation: When a work order is generated, the system reuses or creates the latest AI analysis snapshot and copies only the recommended priority into the work order. It does not assign a specific worker.
- Manual review needed: A flag on an AI analysis result indicating that staff should verify the recommendation before acting on it.
- AI capability layer: A possible future shared AI layer for other modules such as complaints, notices, and community content. This is not the phase-one boundary.
- Local markdown issue tracker: This repo's lightweight issue tracker under `.scratch/<feature-slug>/`.

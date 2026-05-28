Status: ready-for-agent

# Harden Rule Provider Behavior And Fallbacks

## Parent

.scratch/ai-work-order-assistant/PRD.md

## What to build

Improve the rule-first provider so the AI work order assistant produces stable, explainable, and safe recommendations across the expected repair report categories and risk levels.

This slice is focused on behavior quality and fallback semantics, not new API endpoints.

## Acceptance criteria

- [x] Rule provider behavior covers water, electrical, elevator, public facility, building/general, high-risk, uncertain, and default repair report cases.
- [x] Rule provider output includes matched keywords when a rule matched the repair report.
- [x] Rule provider output includes confidence values that reflect strong matches, weak matches, and default cases.
- [x] Rule provider sets manual review needed for high-risk, uncertain, or low-confidence cases.
- [x] Rule provider includes provider/source metadata that distinguishes rule output from future model output.
- [x] Provider failure paths return a safe fallback analysis or allow the calling service to apply a safe fallback without breaking the workflow.
- [x] Tests cover category selection, priority selection, risk level selection, confidence, manual review needed, matched keywords, provider/source metadata, and fallback behavior.

## Blocked by

- .scratch/ai-work-order-assistant/issues/01-manual-ai-analysis-creates-latest-snapshot.md

## Comments

- Hardened `RuleAiWorkOrderAnalysisProvider` with deterministic rule branches for water, electrical, elevator, public facility, building, high-risk, uncertain, weak-match, default, and null-input fallback cases. Rules now produce matched keywords, priority, risk level, confidence, manual review flags, suggested actions, and provider metadata consistently.
- Added Chinese repair keyword coverage through Unicode escapes so common local inputs such as elevator trapped reports use the same classification and high-risk path without source encoding churn.
- Verified with `mvn -pl workorder-service test` (`Tests run: 29, Failures: 0, Errors: 0, Skipped: 0`).

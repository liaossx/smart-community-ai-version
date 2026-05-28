# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- `CONTEXT.md` at the repo root.
- `docs/adr/` for architectural decisions that touch the area you're about to work in.

If any of these files do not exist yet, proceed silently. The producer skill (`/grill-with-docs`) can expand them when terms or decisions get resolved.

## File structure

This repo uses a single-context layout:

```text
/
|-- CONTEXT.md
|-- docs/
|   |-- adr/
|   `-- agents/
|-- common-module/
|-- gateway-service/
|-- user-service/
|-- house-service/
|-- parking-service/
|-- property-service/
|-- workorder-service/
|-- community-service/
`-- system-service/
```

## Use the glossary's vocabulary

When output names a domain concept in an issue title, refactor proposal, hypothesis, or test name, use the term as defined in `CONTEXT.md`.

If the concept is not in the glossary yet, either reconsider whether the term belongs to this project or note it for `/grill-with-docs`.

## Flag ADR conflicts

If output contradicts an existing ADR, surface it explicitly rather than silently overriding it.

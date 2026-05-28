# Use a rule-first AI work order provider

Accepted. Phase one of the AI work order assistant uses a deterministic rule engine to complete the workflow, while keeping the service boundary open for a future model provider. This avoids blocking the first implementation on API keys, network availability, prompt stability, or model JSON parsing, but still lets the project replace or augment the rule provider with a real model later.

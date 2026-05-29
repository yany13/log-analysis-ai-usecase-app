---
name: feedback-token-preservation
description: Never output full boilerplate files unless explicitly commanded — snippets and architecture configs first
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 4993df77-2ed9-4cfb-bcca-5daac1735259
---

Never output full boilerplate code files unless explicitly commanded. Default to structural snippets, targeted patch blocks, or high-level logical architecture configurations.

**Why:** Token preservation is a stated project guardrail across multiple blueprint documents (system_context.md, project_memory.md). The user wants architecture-first thinking, not walls of generated code.

**How to apply:** When asked to implement something, lead with the structural design and integration plan. Only produce full files when the user explicitly says "generate the file" or equivalent.

## What

What does this PR change? Summarize in a sentence or two.

## Why

Why is this change needed? Link the issue it closes, if any (`Closes #123`).

## How

How did you implement it? Call out any non-obvious design decisions. Link a
new or updated ADR under `Aurora/docs/adr/` if this changes an architectural
decision.

## Test Plan

How did you verify this works? Check what applies and describe manual steps
for anything not covered by automated tests.

- [ ] `./mvnw clean verify` passes locally (from the `Aurora/` directory)
- [ ] Added or updated tests covering this change
- [ ] Manually tested the affected endpoint(s) — describe steps/requests below

```
<manual test steps, curl commands, or request/response examples>
```

## Checklist

- [ ] Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (`type(scope): description`)
- [ ] No secrets, credentials, or generated files committed
- [ ] Docs/ADRs updated if this changes public behavior or architecture

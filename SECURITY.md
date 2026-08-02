# Security Policy

## Reporting a Vulnerability

Don't open a public issue for security vulnerabilities. Instead, email
**basheermazari4@gmail.com** with:

- A description of the vulnerability and its potential impact
- Steps to reproduce it (a request/response example or a small repro is
  ideal)
- Any suggested fix, if you have one

You should get an acknowledgment within 5 business days. This is a
solo-maintained project, so response and fix timelines aren't guaranteed,
but every report gets triaged. If you don't hear back within two weeks,
follow up on the same thread.

Please give the maintainer a reasonable window to fix the issue before any
public disclosure. Credit is given in the fix's commit message or release
notes if you'd like it, unless you ask to stay anonymous.

## Supported Versions

Aurora is pre-1.0 (`0.0.1-SNAPSHOT`) and under active development. There
are no maintained release branches — only the latest commit on `main`
receives security fixes. If you're running an older commit, update to
`main` before reporting an issue that might already be fixed.

## Known Dev-Only Defaults — Don't Use These in Production

Aurora ships with conveniences meant for local development. None of these
are safe outside a local/dev environment, and reports about them "as
configured out of the box" are expected, not novel findings — but please
still verify your deployment overrides them:

- **Default admin account.** `DataInitializer` seeds an `admin` account
  with a hardcoded default password on first startup. Override it with the
  `ADMIN_USERNAME`, `ADMIN_PASSWORD`, and `ADMIN_EMAIL` environment
  variables in any environment beyond local dev.
- **Committed JWT signing secret.** `application.properties` ships a
  default `spring.app.jwtSecret` for local use. Override it with the
  `JWT_SECRET` environment variable everywhere else — anyone with the
  default secret can forge valid tokens.
- **H2 console enabled.** `/h2-console` is reachable without
  authentication (`spring.h2.console.enabled=true`, permitted in
  `WebSecurityConfig`) against the in-memory H2 database. This should
  never be enabled against a real database or exposed publicly.

If you find a way these defaults (or anything else) create risk beyond
"an operator forgot to override a documented dev default," that's a valid
report — please send it in.

## Scope

This policy covers the Aurora codebase in this repository. It doesn't
cover third-party dependencies (report those upstream) or infrastructure
you deploy Aurora on.

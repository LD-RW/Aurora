
## Metadata

- **Status:** Accepted

- **Date:** 2026-07-13
## Context

Aurora's REST API needs to authenticate requests from clients (an SPA and/or mobile app) without relying on server-rendered login pages or cookies-by-default form login. The API is stateless by design elsewhere in the stack (no server-side session state for `Product`/`Category`/`User` operations), and the client population is expected to be decoupled front-end apps calling the API directly, not browser form posts.

Four approaches were considered:

1. **Traditional server-side session** (`HttpSession` + Spring Security's default form-login/session model): simplest to wire up and well understood, but requires a shared session store (e.g., Redis, or sticky sessions at the load balancer) to scale horizontally across more than one instance, and doesn't map naturally onto a REST API meant to be consumed by decoupled clients.
2. **Delegate to an external Identity Provider via OAuth2/OIDC** (Keycloak, Auth0, Okta, etc.): offloads password storage, token issuance, MFA, and social login to a purpose-built system, at the cost of standing up and operating additional infrastructure and a recurring cost/vendor dependency. This is disproportionate to Aurora's current MVP scope (`Product`/`Category`/`User` only, small team, per [[ADR-0001]]).
3. **Opaque/reference tokens validated against a server-side store**: a random token is issued and looked up (DB or cache) on every request. Trivially revocable (delete the row to force logout) and doesn't expose any claims to the client, but requires a stateful lookup on every authenticated request and an additional data store (e.g., Redis) that isn't otherwise part of the stack yet.
4. **Self-contained JWT** (chosen): a signed, self-describing token that a request can be authenticated from using signature verification alone, with no per-request server-side state.

Option 4 keeps the API stateless and avoids adding new infrastructure (no session store, no external IdP, no token-lookup cache), matching the project's current MVP scale.

## Decision

Aurora authenticates API requests using a custom, stateless JWT scheme built directly on the `jjwt` library (`io.jsonwebtoken`, `0.13.0`), rather than delegating to Spring Security's OAuth2 Resource Server support or an external IdP:

- **Signing:** HMAC-SHA (`Keys.hmacShaKeyFor`) with a symmetric secret read from `spring.app.jwtSecret`. The same secret is used to sign and verify, since Aurora is currently a single deployable monolith.
- **Claims:** the token carries only the subject (`username`) plus standard `iat`/`exp` claims — no roles or authorities are embedded in the payload (`JwtUtils.generateTokenFromUsername`).
- **Expiration:** short-lived, configured via `spring.app.jwtExpirationMS` (currently 5 minutes). There is no refresh-token flow yet; a token simply expires and the client must re-authenticate.
- **Extraction and validation:** a custom `OncePerRequestFilter` (`AuthTokenFilter`) reads the token from the `Authorization: Bearer` header (`JwtUtils.getJwtFromHeader`), validates its signature and expiration (`JwtUtils.validateToken`), and — on success — re-resolves the user's authorities on every request via `UserDetailsService.loadUserByUsername` rather than trusting authorities embedded in the token, before populating `SecurityContextHolder`.
- **Failure handling:** a dedicated `AuthenticationEntryPoint` (`AuthEntryPointJwt`) returns a uniform JSON 401 response for any unauthenticated access to a protected endpoint, consistent with the centralized error-handling approach in [[ADR-0002 - Centralized Exception Handling with a Uniform API Response Envelope]].
- **Delivery mechanism:** the token is currently sent via the `Authorization: Bearer` header. Moving delivery to a `Secure`/`HttpOnly`/`SameSite` cookie is an already-planned follow-up (tracked separately) once the signin endpoint exists — this ADR only covers the token scheme itself, not its transport.
- **No revocation store:** there is no blacklist, allowlist, or server-side session record for issued tokens. A token is valid for any request that presents a correct signature and hasn't expired, full stop.

## Consequences

### Positive (Benefits)

- **Fully stateless authentication:** no server-side session store is required, so the API can scale horizontally behind a load balancer with no sticky sessions and no shared session cache.
- **No infrastructure added:** unlike the OAuth2/IdP or opaque-token options, this requires no new service (no Redis, no Keycloak instance) — consistent with the project's current MVP scale and small-team maintenance burden (per [[ADR-0001- Layered Architecture with Interface-Driven Services and DTO Isolation]]).
- **Cheap per-request verification:** validating a token is a local signature check (CPU-bound), not a network or DB round trip, aside from the deliberate `loadUserByUsername` call described below.
- **Authorities are always current:** because roles aren't embedded in the token, a role change or account deactivation takes effect on the very next request rather than waiting for the old token to expire — a partial mitigation for not having a revocation mechanism.
- **Library-backed cryptography:** signing, parsing, and signature verification are delegated to `jjwt` rather than hand-rolled, reducing the chance of introducing a crypto or encoding bug.
- **Consistent failure contract:** unauthenticated requests get the same structured JSON envelope as other API errors, via `AuthEntryPointJwt`, rather than a default Spring Security login redirect or an unstructured 403.

### Negative (Trade-offs & Technical Debt)

- **No revocation:** because validity is purely signature-plus-expiry, there is no way to force-invalidate a specific token before it naturally expires — for example on logout, password change, or a detected compromise. The short (5-minute) expiration bounds this exposure window but doesn't eliminate it.
- **No refresh-token flow:** with only a 5-minute expiration and no refresh mechanism, clients must re-authenticate with credentials every 5 minutes. This is acceptable for early development/testing but is not viable as-is for a real client experience and will need a refresh-token (or sliding-session) design before this ships to real users.
- **Secret is a weak, committed plaintext value:** `spring.app.jwtSecret` is currently a short, human-typed string committed directly to `application.properties`, not a securely generated random key and not sourced from an environment variable or secrets manager. This both weakens the HMAC signature (HS256 wants a sufficiently long, high-entropy key) and means the signing secret is sitting in source control — a real problem for anything beyond local development.
- **Not zero-I/O per request:** because authorities are deliberately not trusted from the token and are re-resolved via `UserDetailsService.loadUserByUsername` on every request, the "stateless" scheme still costs one DB lookup per authenticated request. This is a conscious trade favoring correctness (immediate role/deactivation effect) over pure statelessness, but it's worth naming explicitly since it's the main thing a fully stateless JWT setup is usually chosen to avoid.
- **Header-based delivery is an interim state:** delivering the token via `Authorization: Bearer` means whatever the client does with it (e.g., storing it in `localStorage`) determines its exposure to XSS-based token theft. Migrating delivery to a `Secure`/`HttpOnly` cookie is already planned as follow-up work; until then, client-side token storage choices are a live risk this ADR does not resolve.
- **Symmetric signing ties issuing and verifying together:** using a shared HMAC secret for both signing and verification is fine for a single monolith, but would block splitting Aurora into multiple services later, since any service that needs to *verify* tokens would also gain the ability to *mint* them. Moving to asymmetric signing (RS256/ES256) would be a prerequisite for that kind of decomposition.

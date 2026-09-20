# Authentication and Authorization Service

## Interview brief

Design an identity service that authenticates users and authorizes access without treating transport security as its only defense. Preserve two distinct source variations:

1. a login API whose credentials remain protected even when a TLS certificate or termination point is compromised; and
2. an authentication/authorization service supporting OTP and OAuth.

The first is a threat-model exercise, not merely another OAuth flow. Clarify whether the attacker can observe traffic, terminate TLS, alter a web client, control DNS, or compromise the application server. No protocol can protect a password typed into attacker-controlled JavaScript; a defensible answer uses phishing-resistant, origin-bound credentials such as passkeys or a trusted native client, plus replay-resistant challenges.

## Scope and variations

- Register, verify, suspend, and recover an identity.
- Authenticate with passkey/password policy, one-time password, or an external OAuth/OIDC provider.
- Issue, rotate, revoke, and introspect sessions or tokens.
- Evaluate roles and resource-level permissions with default deny.
- Record security events without recording secrets.
- Keep **transport-compromise resilience** separate from **factor/provider extensibility** in the design and test plan.

## Core model

`User`, `Credential`, `Authenticator`, `LoginChallenge`, `OtpChallenge`, `OAuthTransaction`, `IdentityProvider`, `Session`, `RefreshTokenFamily`, `Role`, `Permission`, `Policy`, `ResourceScope`, and `SecurityEvent`.

An `Authenticator` owns type-specific verification. `AuthorizationService` evaluates an authenticated principal, action, resource, tenant, and context; it does not reimplement authentication.

## Invariants

- Passwords, OTP values, private keys, authorization codes, and refresh tokens are never logged or returned after creation.
- A challenge is random, short-lived, bound to its intended flow, and consumed at most once.
- OTP verification increments the attempt counter atomically; expiry, success, or the attempt limit makes it terminal.
- OAuth callbacks must match the initiating `state`, redirect URI, provider, and PKCE verifier.
- Refresh-token rotation invalidates the predecessor; reuse revokes the token family.
- A suspended user or revoked session cannot gain access through a stale authorization cache.
- Authorization is tenant-aware and default-deny; a role grant cannot escape its resource scope.

## Conceptual API

| Operation | Purpose |
| --- | --- |
| `startLogin(identity, method, clientContext)` | Create a short-lived challenge without revealing account existence. |
| `verifyLogin(challengeId, proof, idempotencyKey)` | Consume a passkey, password proof, or OTP proof once. |
| `startOAuth(provider, redirectUri, pkceChallenge)` | Persist state and return the provider authorization target. |
| `completeOAuth(state, code, pkceVerifier)` | Validate the callback, link/resolve identity, and create a session. |
| `refresh(refreshToken, idempotencyKey)` | Rotate a refresh-token family and issue a new access token. |
| `revokeSession(sessionId, actor)` | Revoke one session and publish cache invalidation. |
| `authorize(principal, action, resource, context)` | Return an allow/deny decision with policy evidence. |

## Key flows, concurrency, and failure

For an origin-bound login, the server creates a nonce and the trusted authenticator signs the challenge plus origin/audience context. Verification atomically marks the nonce used before issuing a session. A replay sees the terminal challenge and returns the original safe outcome or a rejection.

OTP delivery is asynchronous: persist the hashed challenge first, then dispatch through an outbox. Delivery failure does not create a second valid code silently. Resends invalidate or explicitly supersede the previous challenge.

OAuth provider timeouts leave the transaction pending; callbacks are idempotent by provider, state, and authorization-code fingerprint. Concurrent refreshes use compare-and-set on the active token generation so only one wins.

Authorization caches require short TTLs plus revocation/version events. On policy-store failure, security-sensitive actions fail closed. Apply rate limits per account, device, network, and challenge without exposing whether an identity exists.

## Design decisions

- Prefer passkeys/WebAuthn for the compromised-transport variation; application-layer password encryption alone does not stop a modified web client.
- Keep credential verification behind strategy adapters and external providers behind ports.
- Use short-lived access tokens and server-tracked refresh-token families for revocation and reuse detection.
- Separate authentication assurance from authorization policy so methods can carry different assurance levels.

## Follow-up questions

- Is the client a trusted native app, a browser page, or both?
- Must authorization be RBAC, ABAC, relationship-based, or a composition?
- What account-recovery proof is acceptable, and can support staff override it?
- What are the session, audit-retention, data-residency, and multi-tenant requirements?
- How are signing keys rotated, stored, and recovered?

## Provenance and implementation status

- Pinned prompts: [transport-compromise login](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L157) and [OTP/OAuth authentication and authorization](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/questions.md#L217).
- The pinned primer has no solution-index row or local implementation for this topic.
- The primer declares its license as [`TODO`](https://github.com/prasadgujar/low-level-design-primer/blob/49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd/README.md#L49-L50) and contains no `LICENSE` file.
- This is an original interview specification. No runnable code is included.

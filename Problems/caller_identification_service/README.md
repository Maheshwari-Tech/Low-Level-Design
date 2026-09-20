# Caller Identification Service

## Prompt and scope

Design a caller directory that registers an account by phone number and stores the identity information needed to identify an incoming number: contact details, personal profile, address, business tag, and social profiles. Add lookup and profile-update boundaries around the source's model.

Call routing, carrier integration, call recording, payments, and a crowd-sourced spam-scoring algorithm are follow-ups rather than assumed source features.

## Core model

- `Account`: ID, phone number, credentials, last-access time, tag, contact, personal information, and social information.
- `Contact`: country code, email, and phone number.
- `PersonalInfo`: name, date of birth, gender, address, and company.
- `Address`: address lines, street, city, country, and postal code.
- `SocialInfo`: map from `SocialProfileType` to profile identifier or URL.
- `Tag`: source business categories such as education, services, legal, and personal care.

## Invariants

- Store phone numbers in one normalized E.164 form and enforce one active account per number.
- Account ID and phone identity are stable; credentials are hashes, never plain text.
- Required identity fields and allowed tags are validated at creation.
- Profile visibility and caller-identification consent are explicit; lookup never leaks private fields.
- Social profile types are unique within an account and profile collections are defensively copied.

## API

The source contains model classes only. A minimal interview boundary is:

```text
register(accountDraft) -> AccountId
identify(normalizedPhone, viewerContext) -> CallerCard | NotFound
updateProfile(accountId, patch, expectedVersion)
recordAccess(accountId, timestamp)
searchByNameOrTag(query, pageToken) -> Page<CallerCard>
```

Authentication and profile administration should not be coupled to the read-optimized caller lookup.

## Main flow

1. Normalize and validate a registration phone number.
2. Atomically reserve the number and create the account aggregate.
3. Project public fields into a phone-indexed `CallerCard`.
4. On an incoming call, normalize the number, read the projection, apply viewer/privacy policy, and return the card.
5. Update the projection and audit trail after profile changes.

## Concurrency and failure handling

- Registration needs a unique phone-number constraint; a read-then-create sequence races.
- Use optimistic versions for profile updates and make retryable commands idempotent.
- Distinguish not found, hidden profile, invalid number, duplicate account, and stale version.
- Cache lookups only with an invalidation/version rule. Encrypt sensitive data and rate-limit enumeration.

## Design solution

Treat `Account` as the write aggregate, place normalization and uniqueness in an account service/repository, and maintain a separate immutable lookup projection keyed by normalized phone. Use policy objects for privacy and tag validation. The source's Lombok value types are a reasonable starting model, but a service, indexes, persistence, and visibility rules are still required.

## Source variations and provenance

| Classification | Exact local clone path | Pinned upstream | Notes |
| --- | --- | --- | --- |
| Model-only fragment | [`References/kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/TrueCaller1`](../../References/kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/TrueCaller1/) | [tree at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/LLD-Practice/TrueCaller1) | Maven/Lombok account and profile model; no application service or driver. |

No second implementation or exact duplicate is present in the clone for this question.

## Follow-ups

- Add crowd-sourced names, spam reports, confidence, moderation, and abuse controls.
- Design prefix/name/tag search and ranking separately from exact phone lookup.
- Support contact-book contribution with consent, provenance, deletion, and conflict resolution.
- Add block lists, business verification, account recovery, and regional retention rules.

## Implementation status

**Model-only reference; no code copied here.** The source defines immutable Lombok-backed account/profile types and a Maven dependency, but it has no main, tests, repository, lookup API, authentication, or caller-identification workflow.

The code remains unchanged in the `References/kumaransg-LLD` clone. That clone has no repository-level license, so this page summarizes the design and links to the pinned source rather than redistributing it.

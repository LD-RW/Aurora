
## Metadata

- **Status:** Accepted

- **Date:** 2026-07-11


## Context

Aurora needs to let a `User` store one or more shipping/billing addresses. The initial implementation modeled this as a `@ManyToMany` relationship between `User` and `Address`, backed by a `user_address` join table, on the reasoning that multiple people can plausibly share the same physical address (for example, coworkers at the same company building).

That model was reconsidered before any repository, service, or DTO layer got built on top of it, so changing course now only touches the entity mapping itself.

The core problem with sharing a single `Address` row across unrelated `User` records is that `Address` is mutable. If two users are linked to the same row and one of them edits it (fixing a typo, changing an apartment number), the change applies to every other user pointing at that row too, including on past orders that reference it. This is a correctness risk for anything tied to order fulfillment, not just a modeling nicety.

Three alternatives were considered:

1. **Shared rows via many-to-many** (the original implementation): removes duplicate address text across users, at the cost of the mutability hazard described above, plus added complexity for insert-time duplicate-checking and safe deletion (an address can't simply be orphan-removed if another user still references it).
2. **Value object** (JPA `@Embeddable`, no own identity or table): avoids the sharing hazard entirely by copying the address into each owner, but doesn't support a "saved addresses" list per user with its own lifecycle (add, edit, or delete a specific saved address by ID).
3. **One-to-many, owned by `User`**: each `Address` row belongs to exactly one `User`; duplicate address text across different users is allowed and not merged.

Option 3 is also the pattern already used for `Product`'s `seller` relationship (`Product.user`, an owned `@ManyToOne`/`@OneToMany` pair), so it's consistent with an existing convention in the codebase rather than introducing a second way of expressing ownership.

## Decision

`Address` is owned exclusively by `User` via a standard bidirectional one-to-many relationship:

- `User.addresses` is the inverse side: `@OneToMany(mappedBy = "user", cascade = {PERSIST, MERGE}, orphanRemoval = true, fetch = LAZY)`.
- `Address.user` is the owning side: `@ManyToOne(fetch = LAZY)` with a `user_id` foreign key column.
- The `user_address` join table is removed; `addresses` no longer has independent identity shared across users.
- Duplicate address text across different users is expected and permitted; the system doesn't attempt to detect or merge "same building, different tenant" cases at the data-model level. If that kind of duplicate detection is ever needed (for example, for shipping analytics), it belongs in a query or report, not in the foreign-key structure.
- This ADR doesn't cover how `Order`/`Cart` will reference addresses. Per standard practice, an order should copy address fields at checkout time rather than hold a live reference to a `User`'s saved address, so editing or deleting a saved address never changes the shipping address on a past order. That follow-up decision belongs with the Order feature.

## Consequences

### Positive (Benefits)

- **No cross-user mutation hazard:** editing or deleting one user's address can never affect another user's data, because no two users ever reference the same row.
- **Correct cascade and orphan semantics:** `orphanRemoval = true` works cleanly on a true one-to-many, removing an `Address` from `User.addresses` deletes the row outright, with no ambiguity about whether another owner still needs it (which wasn't expressible under the many-to-many model without extra checks).
- **Consistent with existing conventions:** mirrors the already-established `User`/`Product` (seller) ownership pattern, so the codebase has one way of expressing "owned by a user," not two.
- **Simpler queries and joins:** a plain foreign key on `addresses.user_id` replaces the `user_address` join table, removing a join for the common case of "get this user's addresses."

### Negative (Trade-offs & Technical Debt)

- **Storage duplication:** identical address text (same building, different employees) gets stored once per user rather than once total. Given `Address` rows are small and per-user address books are short, this isn't expected to matter in practice.
- **No shared "organization address" concept:** if Aurora later wants a true shared address owned by an `Account`/`Organization` entity (for example, a B2B company address referenced by many contacts), that will need its own model; this decision only settles the individual-`User` case.
- **Order snapshot behavior isn't implemented yet:** until the Order feature copies address data at checkout, there's no runtime enforcement of the principle that a saved-address edit shouldn't change a past order's shipping address; it currently exists only as an intended follow-up.

## Metadata

- **Status:** Accepted

- **Date:** 2026-09-12

- **Context Tags:** `#orders` `#concurrency` `#data-integrity` `#jpa`


## Context

Stock was checked in exactly one place: `CartServiceImpl.addProductToCart`, when an item entered the cart. `OrderServiceImpl.placeOrder` then subtracted the cart line's quantity from `Product.quantity` unconditionally, with no second look.

A cart persists in the database and has no expiry, so the gap between those two moments is unbounded. Anything that changed the stock figure in between was simply ignored at checkout. Two independent failures followed from that, both reproduced against MySQL before this change:

1. **Sequential oversell.** With five units in stock, two users could each add five to their cart (legal at the time), and both could then check out successfully. `products.quantity` ended at `-5`, with two orders promising the same five units.
2. **Lost update under concurrency.** Even with a re-check added, two checkouts running at the same time could both read "5 in stock," both pass the check, and both write `0` -- the second overwriting the first rather than stacking on it. Stock finished at `0` while ten units had been sold.

The second failure is the harder one, and it interacts with how JPA caches entities. `placeOrder` reaches each `Product` through `cart.getItems()`, so by the time stock is reserved the entity is already managed by the persistence context. **A JPQL query that returns an already-managed entity hands back the cached instance, discarding the column values it just read.** A locking finder (`@Lock(PESSIMISTIC_WRITE)` on a `@Query`) therefore acquires the row lock correctly but still reports the pre-lock quantity -- the lock serializes the two transactions without either of them seeing the other's result, which is exactly the bug it was meant to prevent.

Four approaches were considered:

1. **Re-check stock with a plain read before decrementing.** Fixes the sequential case and nothing else. Two concurrent checkouts still read the same figure and still lose one of the two updates. Rejected as a half-measure that makes the remaining bug harder to spot, since the obvious symptom disappears.
2. **Optimistic locking via an `@Version` column on `Product`.** Correct, and cheap when contention is rare. Rejected for this path because the loser of the race gets an `OptimisticLockException` after the order rows have already been built, which has to be translated into a meaningful message or retried; the contended case here (the last few units of a popular product) is precisely when it fires most.
3. **An atomic conditional `UPDATE`** -- `UPDATE products SET quantity = quantity - :n WHERE product_id = :id AND quantity >= :n`, treating an affected-row count of zero as "insufficient stock." Genuinely atomic and immune to the staleness problem, since the database evaluates the comparison. Rejected on balance: as a bulk operation it bypasses the persistence context, leaving the managed `Product` stale for the rest of the transaction (including the response payload), and clearing the context to compensate would detach the cart and order entities the same method is still working with.
4. **`EntityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE)` before the check.** Chosen. It issues `SELECT ... FOR UPDATE` *and* overwrites the cached entity state with the row it read, so the transaction that waits for the lock observes what the winner committed.

## Decision

- `OrderServiceImpl.placeOrder` calls a private `reserveStock(...)` step after the address is validated and **before** any payment, order, or order-item row is written, so a rejected checkout never leaves partial records behind.
- For each cart line, `reserveStock` calls `entityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE)`, compares the refreshed quantity against the line quantity, and throws an `APIException` naming the remaining stock if it falls short. Only then does it decrement.
- **Cart lines are locked in `productId` order.** A transaction holds every lock it takes until it commits, so two carts containing the same two products in opposite orders could otherwise each take one lock and wait forever on the other. Sorting by a stable key means whichever transaction wins the lowest id also wins the rest.
- The decrement moved out of the loop that empties the cart. That loop now only removes cart lines, keeping "reserve stock" and "clear the cart" as separate concerns.
- Quantity is independently constrained at the cart boundary (`@Min(1)` on the path variable, re-asserted in `CartServiceImpl`). A negative quantity previously passed the stock guard outright -- `product.getQuantity() < quantity` reads `5 < -5`, which is false -- and then *raised* stock at checkout, since the decrement subtracts a negative number.

## Consequences

### Positive (Benefits)

- Stock can't go negative through the checkout path, sequentially or concurrently. Verified by running two simultaneous checkouts against the last five units three times: exactly one order succeeded on each run, the other was rejected with a message naming the remaining quantity, and stock settled at zero.
- The loser of a race gets a `400` explaining what happened and how much is left, rather than a lost update it can't see or an opaque `500`.
- Nothing is written before stock is secured, so a failed checkout leaves no orphaned payment or order rows.

### Negative (Trade-offs & Technical Debt)

- **Checkouts for the same product now serialize.** That's the point, but it means a burst of orders for one product queues on a row lock, and a long-running transaction holding that lock blocks the rest. Acceptable at Aurora's scale; a high-traffic deployment would want the transaction kept short or a reservation model that doesn't hold locks across the whole checkout.
- **No lock timeout is configured**, so a blocked checkout waits for the database default (`innodb_lock_wait_timeout`, 50 seconds) before failing. Long enough to look like a hang to a caller. Setting an explicit timeout and translating it into a response the caller can retry is deliberately left for when contention is real rather than hypothetical.
- **Stock is reserved only at checkout, not while the item sits in the cart.** A user can still fill a cart with something that sells out before they finish, and only find out at the last step. Reserving at add-to-cart time would need expiry handling and a background release for abandoned carts -- a much larger feature, and out of scope here.
- **`refresh()` discards in-memory changes to the `Product` that haven't been flushed.** Harmless today, since nothing modifies a product earlier in this transaction, but it's a real constraint on any future code that wants to.

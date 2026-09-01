
## Metadata

- **Status:** Accepted

- **Date:** 2026-09-02

- **Context Tags:** `#search` `#database-portability` `#spring-boot`


## Context

Aurora's product search originally matched only `productName` via a leading-wildcard `LIKE` query (`findByProductNameLikeIgnoreCase`). A leading `%` forces a full table scan (no index can be used), the query never considered `description` or `category`, there was no relevance ranking, and there was no multi-term matching -- a search like "red running shoes" was treated as one literal substring rather than three independent terms. User input also flowed unescaped into the query. These issues are tracked in issue #3.

Aurora runs on two databases depending on environment: H2 in-memory by default (used for local development, the full automated test suite, and CI, since it needs no external services), with a `mysql` Spring profile switching to a real MySQL 8 instance via Docker Compose for anyone who wants a closer-to-production database.

Real full-text search -- tokenization, relevance ranking, multi-term matching -- depends on support the underlying database provides; there's no portable JPQL for it. MySQL has native `MATCH()/AGAINST()` full-text search built into InnoDB. H2 has its own, unrelated full-text extension, a different API entirely rather than a drop-in substitute.

Three approaches were considered:

1. **Replace the `LIKE` query outright with a MySQL-only `MATCH()/AGAINST()` native query.** Solves every problem in issue #3 when running on MySQL, but breaks search completely for anyone on H2 -- still the default profile for a plain `./mvnw spring-boot:run` and for every automated test. Rejected: a feature that silently stops working depending on which database happens to be active is worse than the substring search it would replace.
2. **Hibernate Search + Lucene**, indexing entities inside the JVM independent of the underlying database. Would behave identically on H2 and MySQL, avoiding the split entirely. Rejected for now: a new dependency and a Lucene index to configure and maintain, disproportionate to Aurora's current scale and the zero-new-infrastructure approach the MySQL profile itself was added under.
3. **A `ProductSearchStrategy` interface with two implementations, selected by Spring profile:** the existing substring query for the default/H2 case, and a native MySQL full-text query for the `mysql` profile. Chosen: search keeps working everywhere, every blocking issue from #3 is fixed specifically for MySQL-backed environments, and no new dependency is needed.

## Decision

- `ProductSearchStrategy` is a plain interface (`Page<Product> search(String keyword, Pageable pageable)`), with two `@Component` implementations gated by `@Profile("!mysql")` and `@Profile("mysql")` respectively. `ProductServiceImpl` depends only on the interface, not on either database-specific query directly.
- `SubstringProductSearchStrategy` wraps the pre-existing `findByProductNameLikeIgnoreCase` query unchanged, so default/H2 behavior doesn't regress.
- `FullTextProductSearchStrategy` builds a MySQL boolean-mode query server-side: the raw keyword is sanitized (MySQL's operator characters -- `+ - * " ( ) ~ < > @` -- stripped from each token) and rebuilt into required, prefix-matched terms (`+word*` per token), giving real multi-term matching without ever accepting user-controlled query syntax.
- The `FULLTEXT` index (`ft_products_name_description`, on `product_name` and `description`) is created via `schema-mysql.sql`, which only runs under the `mysql` profile (`spring.sql.init.platform=mysql`), since Hibernate's `ddl-auto` has no `FULLTEXT` index type. `spring.sql.init.continue-on-error=true` tolerates the "duplicate key" error that same script produces on every startup after the first, since MySQL's `CREATE FULLTEXT INDEX` has no `IF NOT EXISTS` support.
- The search endpoint moved from a path variable (`/api/public/products/keyword/{keyword}`) to a query parameter (`/api/public/products/search?keyword=...`), avoiding the awkwardness of spaces and special characters in a path segment.
- Automated coverage is split the same way the strategy is: the substring path is tested under the default H2 profile in the existing multi-OS CI job, and the full-text path is tested in a separate, MySQL-only CI job (a JUnit `mysql` tag, filtered out of the main job), since `MATCH()/AGAINST()` has no H2 equivalent to test against.

## Consequences

### Positive (Benefits)

- Search never breaks for anyone on the default H2 profile -- the substring path is untouched.
- Every blocking issue from #3 (single-field match, full-scan `LIKE`, no multi-term matching, unescaped input) is fixed for MySQL-backed environments, with no new runtime dependency.
- The sanitize-then-tokenize approach means user input can never inject its own boolean-mode operators into the query.
- Test coverage genuinely exercises real full-text behavior (prefix matching, multi-term matching) against a real MySQL instance, rather than being skipped or faked.

### Negative (Trade-offs & Technical Debt)

- **Search behavior now depends on which profile is active.** H2 users get substring matching; MySQL users get full-text matching. This is a real, visible difference in result quality between environments, not just an implementation detail -- acceptable here since H2 is dev/test-only and MySQL is the intended target for anyone running Aurora for real, but worth remembering if that assumption ever changes.
- **`schema-mysql.sql`'s re-run isn't truly idempotent.** It depends on `continue-on-error` swallowing an expected error on every startup after the first, rather than a migration tool tracking what's already applied. Acceptable without Flyway or Liquibase adopted yet, but would need revisiting if that changes.
- **`innodb_ft_min_token_size` (default 3)** means words under three characters are never indexed or matched, even with prefix matching. Not addressed here; would need a server-config change and an index rebuild if it ever matters.
- **The MySQL-tagged tests are destructive against whatever database they run against**, since a `FULLTEXT` search can't see an uncommitted row inserted earlier in the same transaction, ruling out this project's usual transactional-rollback test pattern; each test commits its own setup and clears the `products`/`categories` tables afterward instead. Safe in CI, since it runs against a fresh container every time; running these tests locally against a real MySQL instance with development data in it will delete that data.

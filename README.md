# Aurora

[![Aurora CI](https://github.com/LD-RW/Aurora/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/LD-RW/Aurora/actions/workflows/ci.yml)

An e-commerce REST API built with Spring Boot 4 and Java 25 -- catalog, cart, addresses, and order placement, backed by custom JWT authentication and a layered, test-driven architecture.

## Features

- **Catalog** -- categories and products, paginated and sortable listings, image upload
- **Authentication** -- custom stateless JWT auth delivered via an HTTP-only cookie, role-based authorization (`ADMIN` / `SELLER` / `USER`)
- **Cart** -- add/update/remove products, automatically kept in sync when a product's price or availability changes
- **Addresses** -- full CRUD with ownership-based authorization (an address is only visible/editable by its owner or an admin)
- **Orders** -- converts a user's cart into a persisted order, payment record, and stock adjustment in a single atomic transaction

## Architecture

Aurora follows a strict layered architecture -- `Controller → Service → Repository → Entity` -- with DTOs isolated from entities via MapStruct mappers. Key design decisions are written up as ADRs rather than left implicit:

- [ADR-0001 -- Layered Architecture with Interface-Driven Services and DTO Isolation](Aurora/docs/adr/ADR-0001-%20Layered%20Architecture%20with%20Interface-Driven%20Services%20and%20DTO%20Isolation.md)
- [ADR-0002 -- Centralized Exception Handling with a Uniform API Response Envelope](Aurora/docs/adr/ADR-0002%20-%20Centralized%20Exception%20Handling%20with%20a%20Uniform%20API%20Response%20Envelope.md)
- [ADR-0003 -- Owned One-to-Many User-Address Relationship](Aurora/docs/adr/ADR-0003%20-%20Owned%20One-to-Many%20User-Address%20Relationship.md)
- [ADR-0004 -- Custom Stateless JWT Authentication](Aurora/docs/adr/ADR-0004%20-%20Custom%20Stateless%20JWT%20Authentication.md)

Every feature is tracked as a GitHub issue and shipped through a dedicated, reviewed pull request -- the issue tracker and PR history are the project's changelog.

## Quality & security

This isn't just CRUD scaffolding -- a recurring part of the workflow is auditing already-shipped code for real bugs, proving them with a failing test, then fixing and re-verifying:

- Found and fixed multiple mass-assignment/IDOR vulnerabilities (for example, a client could overwrite another user's data by supplying an existing ID on a create request; an authenticated user could ship an order to a different user's saved address) -- each closed with a regression test that fails against the vulnerable code and passes after the fix
- Audited and fixed N+1 query problems across the cart and product modules
- 60+ automated tests spanning repository, service, controller, and entity layers, run in CI on every pull request across Ubuntu, Windows, and macOS
- Prose linting (Vale) enforced in CI alongside the test suite

## Tech stack

Spring Boot 4 · Spring Data JPA · Spring Security · MapStruct · Lombok · Jakarta Bean Validation · H2 (development) / MySQL 8 via Docker Compose · GitHub Actions

## Running locally

By default, Aurora runs against an in-memory H2 database with zero setup:

```bash
cd Aurora
./mvnw spring-boot:run
```

To run against MySQL instead (via Docker Compose) and browse the schema through Adminer, see [`Aurora/docs/running-with-mysql-or-h2.md`](Aurora/docs/running-with-mysql-or-h2.md).

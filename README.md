# Aurora

[![Aurora CI](https://github.com/LD-RW/Aurora/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/LD-RW/Aurora/actions/workflows/ci.yml)
![Status](https://img.shields.io/badge/status-in%20development-yellow)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Aurora is an e-commerce REST API built with Spring Boot 4 and Java 25. It covers product catalog, shopping cart, address management, and order placement, with custom JWT authentication and role-based access control.

> **Aurora is a personal learning project and is still under active development.** Modules are built incrementally, one GitHub issue at a time -- see the [open issues](https://github.com/LD-RW/Aurora/issues) for what's planned next. It isn't a production-ready storefront, so treat it as a work in progress rather than a finished product.

## Features

- **Catalog** -- categories and products, paginated and sortable listings, image upload and retrieval
- **Search** -- keyword search over the product catalog; substring matching by default, real full-text search (multi-term, relevance-ranked) when running against MySQL
- **Authentication** -- custom stateless JWT auth delivered via an HTTP-only cookie, role-based authorization (`ADMIN` / `SELLER` / `USER`)
- **Cart** -- add/update/remove products, automatically kept in sync when a product's price or availability changes
- **Addresses** -- full CRUD with ownership-based authorization (an address is only visible/editable by its owner or an admin)
- **Orders** -- converts a user's cart into a persisted order, payment record, and stock adjustment in a single atomic transaction; paginated admin listing, lookup by ID, and a user's own order history

## Getting started

### Prerequisites

- Java 25 (JDK)
- Docker, only if you want to run against MySQL instead of the default in-memory database

### Run it

```bash
git clone https://github.com/LD-RW/Aurora.git
cd Aurora/Aurora
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080` against an in-memory H2 database -- nothing else to install or configure. A default admin account is seeded automatically on first startup (`admin` / `admin123`, both overridable via the `ADMIN_USERNAME` / `ADMIN_PASSWORD` environment variables).

Try it once it's up:

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'
```

### Run the tests

```bash
cd Aurora
./mvnw test
```

### Using MySQL instead of H2

H2 resets on every restart. To run against a persistent MySQL database via Docker Compose (with a web UI to browse it), see [`Aurora/docs/running-with-mysql-or-h2.md`](Aurora/docs/running-with-mysql-or-h2.md).

## Tech stack

Spring Boot 4 · Spring Data JPA · Spring Security · MapStruct · Lombok · Jakarta Bean Validation · H2 (development) / MySQL 8 via Docker Compose · GitHub Actions

## Architecture

Aurora follows a strict layered architecture -- `Controller → Service → Repository → Entity` -- with DTOs isolated from entities via MapStruct mappers. Key design decisions are written up as ADRs rather than left implicit:

- [ADR-0001 -- Layered Architecture with Interface-Driven Services and DTO Isolation](Aurora/docs/adr/ADR-0001-%20Layered%20Architecture%20with%20Interface-Driven%20Services%20and%20DTO%20Isolation.md)
- [ADR-0002 -- Centralized Exception Handling with a Uniform API Response Envelope](Aurora/docs/adr/ADR-0002%20-%20Centralized%20Exception%20Handling%20with%20a%20Uniform%20API%20Response%20Envelope.md)
- [ADR-0003 -- Owned One-to-Many User-Address Relationship](Aurora/docs/adr/ADR-0003%20-%20Owned%20One-to-Many%20User-Address%20Relationship.md)
- [ADR-0004 -- Custom Stateless JWT Authentication](Aurora/docs/adr/ADR-0004%20-%20Custom%20Stateless%20JWT%20Authentication.md)
- [ADR-0005 -- Product Search Strategy: MySQL Full-Text Search vs the H2 Fallback](Aurora/docs/adr/ADR-0005%20-%20Product%20Search%20Strategy%20-%20Substring%20Fallback%20vs%20MySQL%20Full-Text.md)

Every feature is tracked as a GitHub issue and shipped through a dedicated, reviewed pull request -- the issue tracker and PR history are the project's changelog.

## Quality & security

A recurring part of the workflow is auditing already-shipped code for real bugs, proving them with a failing test, then fixing and re-verifying:

- Found and fixed multiple mass-assignment/IDOR vulnerabilities (for example, a client could overwrite another user's data by supplying an existing ID on a create request; an authenticated user could ship an order to a different user's saved address) -- each closed with a regression test that fails against the vulnerable code and passes after the fix
- Audited and fixed N+1 query problems across the cart and product modules
- 80+ automated tests spanning repository, service, controller, and entity layers, run in CI on every pull request across Ubuntu, Windows, and macOS, plus a dedicated job that runs the MySQL-specific full-text search tests against a real MySQL service container
- Prose linting (Vale) enforced in CI alongside the test suite

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md).

## License

[MIT](LICENSE)

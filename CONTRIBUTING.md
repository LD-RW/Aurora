# Contributing to Aurora

Thanks for considering a contribution to Aurora, a Spring Boot 4 e-commerce
REST API. This guide walks you through setting up your environment, running
the test suite, and submitting a pull request (PR).

By participating in this project, you agree to follow the
[Code of Conduct](CODE_OF_CONDUCT.md).

## Ways to Contribute

- **Report a bug** using the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md).
- **Propose a feature** using the [feature request template](.github/ISSUE_TEMPLATE/feature_request.md).
- **Open a PR** for a fix, feature, or documentation improvement. For
  anything nontrivial, open an issue first so you don't spend time on an
  approach that won't land.

## Prerequisites

- **JDK 25** (Temurin recommended; matches CI)
- **Git**
- No local database setup needed — Aurora runs on an in-memory H2 database
  by default

You don't need Maven installed globally; the project ships the Maven
Wrapper (`mvnw`).

## Setting Up Your Dev Environment

```bash
git clone git@github.com:LD-RW/Aurora.git
cd Aurora/Aurora   # the Maven project lives in this nested directory
./mvnw clean install
```

If you use an IDE (IntelliJ IDEA, Eclipse, VS Code), enable **annotation
processing**. The project relies on Lombok and MapStruct to generate code
at compile time, and the build fails silently in the IDE without it.

### Running the App Locally

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. A default admin account is
seeded on startup (`admin` / `admin123` by default — override with the
`ADMIN_USERNAME`, `ADMIN_PASSWORD`, and `ADMIN_EMAIL` environment
variables). The H2 console is available at `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:test`) for inspecting the in-memory database while
the app is running.

### Running Tests

```bash
./mvnw test          # unit and integration tests
./mvnw clean verify   # full build: compile, test, package — what CI runs
```

Run `clean verify` before opening a PR. CI runs this same command across
Linux, Windows, and macOS, so a pass locally on your platform doesn't
guarantee a pass everywhere, but it catches most issues.

### Prose Linting

Aurora runs [Vale](https://vale.sh/) with the Microsoft style package
against `README.md` and `Aurora/docs/` (this covers the [architecture
decision records](Aurora/docs/adr/)). If you're editing either, install
Vale and run it locally to catch style issues before CI does:

```bash
vale sync
vale README.md Aurora/docs/
```

## Code Style and Architecture

Aurora follows a strict layered architecture:
`Controller → Service (interface + impl) → Repository → Entity`, with DTOs
isolating the API surface from JPA entities. Before making a structural
change, skim the existing [ADRs](Aurora/docs/adr/) — they document *why*
the codebase is shaped the way it is, not just what it does.

- Never return `@Entity` objects directly from a controller; map to a DTO.
- Constructor-inject dependencies (Lombok's `@RequiredArgsConstructor`),
  don't use field injection.
- If your change introduces or reverses an architectural decision, add or
  update an ADR under `Aurora/docs/adr/` in the same PR.

## Commit Messages

Aurora uses [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): short, imperative description
```

Common types in this repo: `feat`, `fix`, `refactor`, `test`, `docs`,
`ci`. The scope is usually the affected module or domain (`security`,
`auth`, `user`, `adr`). Examples from the project history:

```
feat(security): add custom UserDetails, UserDetailsService and repositories
fix(security): restrict product image updates to admins
docs(adr): note JWT secret hardening in ADR-0004
```

## Branch Naming

Match the prefix to the change type: `feature/`, `fix/`, `refactor/`,
`test/`, `docs/`. For example, `feature/seller-role` or
`docs/retroactive-adrs`.

## Submitting a Pull Request

1. Fork the repo (or branch directly if you have write access) and create a
   branch following the naming convention above.
2. Make your change, following the code style and commit conventions
   described here.
3. Run `./mvnw clean verify` and, if you touched `README.md` or
   `Aurora/docs/`, `vale README.md Aurora/docs/`.
4. Open a PR against `main`. The [PR template](.github/pull_request_template.md)
   walks you through describing what changed, why, and how you tested it —
   fill it in rather than deleting it.
5. A reviewer is assigned automatically based on [CODEOWNERS](.github/CODEOWNERS).
   CI must pass (build-and-test matrix plus prose lint) before merge.

## License

By contributing, you agree that your contributions are licensed under the
project's [MIT License](LICENSE).

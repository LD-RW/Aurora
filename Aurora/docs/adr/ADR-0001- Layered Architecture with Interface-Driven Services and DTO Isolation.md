
## Metadata

- **Status:** Accepted (Retrospective)
    
- **Date:** 2026-07-04
    
- **Context Tags:** `#structure` `#maintainability` `#spring-boot` `#architecture`
## Context

Aurora is an e-commerce REST API built on **Spring Boot 4.0.4** and **Java 25**. From its inception, the system required an architectural structure capable of scaling seamlessly as the domain grows (initially covering `Category` and `Product`, with planned expansion into `Cart`, `Orders`, and `Users`).

The architecture must satisfy three primary criteria:

1. Isolate HTTP web concerns from core business logic and persistence.
    
2. Ensure JPA entities never leak across the API boundary.
    
3. Remain easy to reason about and navigate for a small development team.
    

### Mitigation of Core Spring CRUD Risks

This architectural decision explicitly addresses two recurring vulnerabilities in standard Spring applications:

- **Entity Leakage:** Returning `@Entity` objects directly couples the external wire contract to the internal database schema. This frequently invites runtime serialization bugs, such as `LazyInitializationException` when dealing with lazy-loaded relationships (e.g., `Product.category` defined as a `ManyToOne(fetch = FetchType.LAZY)` association).
    
- **Fat Controllers:** Allowing business rules to drift into the web layer makes controllers difficult to unit test and leads to widespread code duplication.
    

## Decision

I have adopted a strict, unidirectional **four-layer data flow**:

$$\text{Controller} \longrightarrow \text{Service (Interface + Impl)} \longrightarrow \text{Repository} \longrightarrow \text{Entity}$$

To enforce this structure, I will adhere to the following implementation conventions:

- **Interface-Driven Services:** Every service is decoupled into an interface (e.g., `ProductService`) and an implementation class (e.g., `ProductServiceImpl`). Dependency injection is handled exclusively via constructor injection, streamlined by Lombok's `@RequiredArgsConstructor`.
    
- **Strict Boundary Isolation:** Controllers communicate exclusively using Data Transfer Objects (e.g., `ProductDTO`, `CategoryDTO`) and standardized response wrappers (`ProductResponse`, `CategoryResponse`, `APIResponse`). **Entities are strictly forbidden from crossing the controller boundary.**
    
- **Automated Mapping:** Entity-to-DTO translation is entirely delegated to MapStruct mappers configured with `componentModel = "spring"`. This includes nested flattening (e.g., mapping `category.categoryId` to a top-level `categoryId`). Hand-written mapping code is prohibited.
    
- **Centralized Configuration:** Cross-cutting, global constants (such as default page sizes, sorting whitelists, and pagination defaults) are unified inside a single `AppConstants` class.
    

## Consequences

### Positive (Benefits)

- **Centralized Business Logic:** Core domain rules (e.g., `calculateSpecialPrice`, duplicate-name validations) are fully isolated within the service layer, making them independently and reliably testable.
    
- **Decoupled API Contract:** The API contract is immune to database schema migrations. Because DTOs are explicitly shaped, lazy-loaded categories never trigger unexpected serialization behavior.
    
- **Compile-Time Safety:** MapStruct eliminates tedious boilerplate code and catches data type or naming mismatches at compile time rather than failing at runtime.
    

### Negative (Trade-offs & Technical Debt)

- **Increased Ceremony:** The strict interface-per-service rule introduces overhead for simple services that currently only require a single implementation. This could be argued as premature abstraction at our current MVP scale.
    
- **Object Multiplication:** Managing three parallel object shapes per aggregate (`Entity`, `DTO`, and `Response`) increases the surface area that developers must keep in sync when fields change.
    
- **Assembly Duplication:** Response assembly logic for `ProductResponse` is currently duplicated across `getAllProducts`, `searchByCategory`, and `searchByKeyword`. As the codebase scales, this pattern will require refactoring into a shared helper or assembler component.
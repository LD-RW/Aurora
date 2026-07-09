
## Metadata

- **Status:** Accepted (Retrospective)
    
- **Date:** 2026-07-04
    
- **Context Tags:** `#error-handling` `#api-contract` `#consistency` `#spring-boot`
## Context

REST clients require predictable, structured, and machine-readable error responses to handle failures gracefully. Without a strict, centralized convention, individual controllers tend to invent disparate error shapes and arbitrary HTTP status mappings.

Furthermore, Aurora relies on **Jakarta Bean Validation** to validate inbound DTOs at the web layer. If unhandled, these validation failures surface as generic HTTP 500 internal server errors or raw framework stack traces, compromising API security and developer experience. The application needs a mechanism to intercept these failures and translate them into meaningful, actionable HTTP 400 responses.

## Decision

I have centralized all error handling declaratively using a single Spring `@RestControllerAdvice` component named `MyGlobalExceptionHandler`. This component intercepts exceptions application-wide, mapping them to a uniform response envelope or a dedicated validation structure:

- **`ResourceNotFoundException`:** Thrown exclusively by the service layer when an entity lookup fails (for example, `.findById(...).orElseThrow(...)`). This exception is mapped directly to a **404 Not Found** status.
    
- **`APIException`:** Used for core business-rule violations (for example, duplicate product names, invalid sorting fields, or empty result constraints). This exception is mapped to a **400 Bad Request** status.
    
- **`MethodArgumentNotValidException`:** Intercepts Jakarta Bean Validation failures and maps them to a **400 Bad Request** status. The response body is structured as a clear `field -> message` map, allowing frontend clients to easily bind error messages to corresponding form inputs.
    
- **`APIResponse { message, status }`:** Every non-validation domain error is serialized through this consistent wrapper, guaranteeing a predictable wire format for downstream consumers.
    

> **Architectural Alignment:** Business invariants are strictly enforced within the **service layer**, never inside the controllers. This ensures that core validation and domain rules apply uniformly across the application, regardless of which endpoint or controller triggers the operation.

## Consequences

### Positive (Benefits)

- **Clean, Happy-Path Controllers:** Controllers remain entirely free of repetitive `try/catch` boilerplate and explicit HTTP status plumbing, keeping the web layer lean and readable.
    
- **Stable Client Contracts:** Downstream consumers interact with a highly predictable error contract—receiving an `APIResponse` object for standard domain errors and a clean field-to-message map for input validation.
    
- **Plug-and-Play Error Handling:** Any newly created endpoints automatically inherit this global error-handling infrastructure without requiring additional configuration.
    

### Negative (Trade-offs & Technical Debt)

- **Semantic Conflation:** Routing an empty result set through `APIException -> 400 Bad Request` conflates _empty data_ with _malformed or invalid input_. An empty product list should ideally return an HTTP `200 OK` with an empty array `[]`. This choice favors loud, explicit feedback over strict REST purity and will likely require future revision.
    
- **Divergent Error Formats:** The error envelope isn't perfectly uniform. Validation errors yield a bare key-value map, whereas standard domain errors return an `APIResponse` object. This forces frontend clients to implement logic to handle two distinct JSON failure shapes.
    
- **Lack of a Safety Net:** The current handler lacks a fallback `@ExceptionHandler(Exception.class)` method. Consequently, unhandled runtime exceptions will still slip through, leaking default Spring framework error pages to consumers.
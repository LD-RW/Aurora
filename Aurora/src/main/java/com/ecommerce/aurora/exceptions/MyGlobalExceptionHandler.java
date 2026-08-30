package com.ecommerce.aurora.exceptions;

import com.ecommerce.aurora.payload.APIResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class MyGlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MyGlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse> resourceNotFoundExceptionHandler(@NonNull ResourceNotFoundException e){
        APIResponse apiResponse = new APIResponse(e.getMessage(), false);
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(APIException.class)
    public ResponseEntity<APIResponse> resourceNotFoundExceptionHandler(APIException e){
        APIResponse apiResponse = new APIResponse(e.getMessage(), false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<APIResponse> authenticationExceptionHandler(AuthenticationException e){
        APIResponse apiResponse = new APIResponse("Bad credentials", false);
        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Bean Validation on an entity (e.g. Payment.paymentMethod) fires at persist/flush time,
     * not at the controller boundary -- with no handler, it fell through to the catch-all
     * below as a 500, even though it's really a 400-shaped client error.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> constraintViolationExceptionHandler(ConstraintViolationException e) {
        Map<String, String> response = new HashMap<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String fieldName = propertyPath.contains(".")
                    ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1)
                    : propertyPath;
            response.put(fieldName, violation.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Last-resort handler for exceptions Spring's own MVC infrastructure doesn't already
     * recognize (see {@link #handleExceptionInternal} for those) -- genuine bugs, not
     * malformed requests. {@code ResponseEntityExceptionHandler} already maps things like a
     * bad path variable or an unsupported HTTP method to the correct 4xx status, so this is
     * only reached for exceptions outside that known list.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse> globalExceptionHandler(Exception e){
        LOG.error("Unhandled exception", e);
        APIResponse apiResponse = new APIResponse("An unexpected error occurred", false);
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException e,
                                                                    @NonNull HttpHeaders headers,
                                                                    @NonNull HttpStatusCode status,
                                                                    @NonNull WebRequest request) {
        Map<String, String> response = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(err -> {
            String fieldName = ((FieldError) err).getField();
            String message = err.getDefaultMessage();
            response.put(fieldName, message);
        });
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Final funnel for every framework-recognized exception ResponseEntityExceptionHandler
     * already maps to the correct status (bad path variable, unsupported method, 404, etc.).
     * Wraps them in the same APIResponse envelope as the rest of the API instead of Spring's
     * default body, so a malformed request never leaks a raw framework error shape.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception e, @Nullable Object body,
                                                               @NonNull HttpHeaders headers, @NonNull HttpStatusCode statusCode,
                                                               @NonNull WebRequest request) {
        LOG.warn("Framework-handled exception: {} -> {}", e.getClass().getSimpleName(), statusCode);
        APIResponse apiResponse = new APIResponse(e.getMessage(), false);
        return new ResponseEntity<>(apiResponse, headers, statusCode);
    }
}

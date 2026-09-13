package com.ecommerce.aurora.exceptions;

/**
 * Raised when a resource can't be removed because other records still point at it -- deleting an
 * address an order was placed to, for example.
 *
 * Distinct from {@link APIException} (a 400) because nothing about the request itself is
 * malformed: the same call would have succeeded before the reference existed, and may succeed
 * again once it's gone. That maps to 409 Conflict rather than Bad Request.
 */
public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String message) {
        super(message);
    }
}

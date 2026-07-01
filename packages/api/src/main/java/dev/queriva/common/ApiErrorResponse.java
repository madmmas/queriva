package dev.queriva.common;

/**
 * Standard error body returned by {@link GlobalExceptionHandler}.
 */
public record ApiErrorResponse(String error) {
}

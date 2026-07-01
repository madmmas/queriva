package dev.queriva.common;

import java.util.stream.Collectors;

import dev.queriva.ingest.DocumentConflictException;
import dev.queriva.ingest.EmbedSidecarException;
import dev.queriva.ingest.EmbeddingModelMismatchException;
import dev.queriva.ingest.InvalidDistanceException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Central exception handler for all API controllers (code-quality.mdc B8).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maps Bean Validation failures on request bodies to HTTP 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ApiErrorResponse(message));
    }

    /**
     * Maps constraint violations on path and query parameters to HTTP 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ApiErrorResponse(message));
    }

    /**
     * Maps unknown distance metric strings to HTTP 400.
     */
    @ExceptionHandler(InvalidDistanceException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidDistance(InvalidDistanceException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(exception.getMessage()));
    }

    /**
     * Maps duplicate collection creation to HTTP 409.
     */
    @ExceptionHandler(CollectionAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCollectionAlreadyExists(CollectionAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(exception.getMessage()));
    }

    /**
     * Maps missing collections to HTTP 404.
     */
    @ExceptionHandler(CollectionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCollectionNotFound(CollectionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(exception.getMessage()));
    }

    /**
     * Maps embedding model mismatches to HTTP 400.
     */
    @ExceptionHandler(EmbeddingModelMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleEmbeddingModelMismatch(EmbeddingModelMismatchException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(exception.getMessage()));
    }

    /**
     * Maps duplicate document conflicts to HTTP 409.
     */
    @ExceptionHandler(DocumentConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleDocumentConflict(DocumentConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(exception.getMessage()));
    }

    /**
     * Maps embed sidecar failures to HTTP 503.
     */
    @ExceptionHandler(EmbedSidecarException.class)
    public ResponseEntity<ApiErrorResponse> handleEmbedSidecarFailure(EmbedSidecarException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse(exception.getMessage()));
    }

    /**
     * Maps invalid enum values in request bodies to HTTP 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(exception.getMessage()));
    }

    /**
     * Maps Qdrant client failures to HTTP 503.
     */
    @ExceptionHandler(QdrantOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleQdrantOperation(QdrantOperationException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse(exception.getMessage()));
    }
}

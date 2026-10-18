package com.buildat.http;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    public record FieldError(String field, String code, String message) { }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var servletRequest = ((ServletWebRequest) request).getRequest();
        var code = Problems.code(status.value());
        var detail = Problems.detail(status.value());
        if (ex instanceof MethodArgumentNotValidException) {
            code = "VALIDATION_FAILED";
            detail = "One or more fields are invalid.";
        }
        var problem = Problems.create(status.value(), code, detail, servletRequest);
        if (ex instanceof MethodArgumentNotValidException invalid) {
            List<FieldError> errors = invalid.getBindingResult().getFieldErrors().stream()
                    .map(error -> new FieldError(error.getField(), "INVALID_VALUE", "The value is invalid."))
                    .toList();
            problem.setProperty("errors", errors);
        }
        // Keep framework protocol headers (for example Allow and Accept).
        return new ResponseEntity<>(problem, headers, status);
    }

    @ExceptionHandler(ApiFailure.class)
    ResponseEntity<Object> applicationFailure(ApiFailure failure, HttpServletRequest request) {
        var headers = new HttpHeaders();
        headers.setCacheControl("no-store");
        if (failure.status() == 401) headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
        return new ResponseEntity<>(Problems.create(failure.status(), failure.code(),
                Problems.detail(failure.status()), request), headers, HttpStatusCode.valueOf(failure.status()));
    }

    @ExceptionHandler({org.springframework.dao.PessimisticLockingFailureException.class,
            org.springframework.transaction.TransactionTimedOutException.class,
            org.springframework.dao.QueryTimeoutException.class})
    ResponseEntity<Object> transactionConflict(Exception exception, HttpServletRequest request) {
        return applicationFailure(new ApiFailure(409, "ACCOUNT_STATE_CONFLICT"), request);
    }

    @ExceptionHandler({org.springframework.dao.DataAccessResourceFailureException.class,
            org.springframework.transaction.CannotCreateTransactionException.class})
    ResponseEntity<Object> databaseUnavailable(Exception exception, HttpServletRequest request) {
        return applicationFailure(new ApiFailure(503, "DATABASE_UNAVAILABLE"), request);
    }

    @ExceptionHandler({org.springframework.transaction.TransactionSystemException.class,
            org.springframework.orm.jpa.JpaSystemException.class})
    ResponseEntity<Object> persistenceFailure(Exception exception, HttpServletRequest request) {
        Throwable original = exception instanceof org.springframework.transaction.TransactionSystemException transaction
                ? transaction.getOriginalException() : exception;
        for (Throwable cause = original; cause != null; cause = cause.getCause()) {
            // A dead pooled connection can fail rollback with a pool-generated SQLException
            // that has no SQLSTATE. Hibernate identifies the JDBC transaction boundary failure.
            if (cause instanceof org.hibernate.TransactionException && cause.getCause() instanceof java.sql.SQLException
                    || cause instanceof org.hibernate.exception.JDBCConnectionException
                    || cause instanceof java.sql.SQLException sql && sql.getSQLState() != null
                        && (sql.getSQLState().startsWith("08")
                            || java.util.Set.of("57P01", "57P02", "57P03").contains(sql.getSQLState()))) {
                return databaseUnavailable(exception, request);
            }
        }
        return unexpected(exception, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception ex, HttpServletRequest request) {
        // Do not log exception text: it may contain SQL, credentials or submitted content.
        logger.error("Unexpected request failure; requestId=" + RequestIdFilter.requestId(request)
                + "; exceptionType=" + ex.getClass().getName());
        return ResponseEntity.internalServerError()
                .body(Problems.create(500, "INTERNAL_ERROR", Problems.detail(500), request));
    }
}

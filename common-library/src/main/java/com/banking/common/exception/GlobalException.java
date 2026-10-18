package com.banking.common.exception;

import com.banking.common.dto.commonRes.ErrorResponse;
import com.banking.common.dto.commonRes.ValidationResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalException {

    /*
     * ============================================================
     * 400 - Bean Validation
     * ============================================================
     *
     * Handles:
     *
     * @Valid
     * @NotNull
     * @NotEmpty
     * @Positive
     * @Future
     * @Size
     * etc.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        Map<String, String> fieldErrors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        fieldErrors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ValidationResponse(
                        false,
                        "Validation failed",
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI(),
                        fieldErrors
                ));
    }


    /*
     * ============================================================
     * 400 - Invalid JSON / Invalid Enum / Unknown Fields
     * ============================================================
     *
     * Handles:
     *
     * Invalid JSON
     * Invalid enum value
     * Invalid number/string/date format
     * Unknown JSON property
     * Malformed request body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ValidationResponse> handleMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {

        Map<String, String> fieldErrors = new HashMap<>();

        Throwable cause = exception;

        while (cause != null) {

            /*
             * Unknown JSON field
             *
             * Example:
             *
             * {
             *     "paymentMethod": "RAZORPAY",
             *     "randomField": "abc"
             * }
             */
            if (cause instanceof UnrecognizedPropertyException ex) {

                fieldErrors.put(
                        ex.getPropertyName(),
                        "Unknown field"
                );

                break;
            }

            /*
             * Invalid enum / invalid data type
             *
             * Example:
             *
             * "paymentMethod": "INVALID"
             *
             * or
             *
             * "amount": "abc"
             */
            if (cause instanceof InvalidFormatException ex) {

                String fieldName = ex.getPath()
                        .stream()
                        .findFirst()
                        .map(JsonMappingException.Reference::getFieldName)
                        .orElse("unknown");

                String message;

                if (ex.getTargetType() != null
                        && ex.getTargetType().isEnum()) {

                    message = "Invalid value. Accepted values: "
                            + Arrays.toString(
                            ex.getTargetType().getEnumConstants()
                    );

                } else {

                    message = "Invalid value. Expected type: "
                            + ex.getTargetType().getSimpleName();
                }

                fieldErrors.put(fieldName, message);

                break;
            }

            cause = cause.getCause();
        }

        /*
         * If no specific field error was identified
         */
        if (fieldErrors.isEmpty()) {

            fieldErrors.put(
                    "request",
                    "Malformed or invalid request body"
            );
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ValidationResponse(
                        false,
                        "Invalid request body",
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI(),
                        fieldErrors
                ));
    }


    /*
     * ============================================================
     * 400 - Missing Request Parameter
     * ============================================================
     *
     * Example:
     *
     * @RequestParam PaymentMethod paymentMethod
     *
     * Request:
     *
     * POST /api/payments
     *
     * without paymentMethod
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ValidationResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {

        Map<String, String> fieldErrors = new HashMap<>();

        fieldErrors.put(
                exception.getParameterName(),
                "Request parameter is required"
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ValidationResponse(
                        false,
                        "Invalid request parameter",
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI(),
                        fieldErrors
                ));
    }


    /*
     * ============================================================
     * 400 - Wrong Request Parameter / Path Variable Type
     * ============================================================
     *
     * Example:
     *
     * @RequestParam Long salonId
     *
     * Request:
     *
     * ?salonId=abc
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ValidationResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {

        Map<String, String> fieldErrors = new HashMap<>();

        String fieldName = exception.getName();

        String expectedType = exception.getRequiredType() != null
                ? exception.getRequiredType().getSimpleName()
                : "valid type";

        fieldErrors.put(
                fieldName,
                "Invalid value. Expected type: " + expectedType
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ValidationResponse(
                        false,
                        "Invalid request parameter",
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI(),
                        fieldErrors
                ));
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {

        Map<String, String> fieldErrors = new HashMap<>();

        exception.getConstraintViolations()
                .forEach(violation -> {

                    String fieldName = violation.getPropertyPath()
                            .toString();

                    fieldErrors.put(
                            fieldName,
                            violation.getMessage()
                    );
                });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ValidationResponse(
                        false,
                        "Validation failed",
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

    /*
     * ============================================================
     * 400 - Missing Path Variable
     * ============================================================
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ValidationResponse> handleMissingPathVariable(
            MissingPathVariableException exception,
            HttpServletRequest request
    ) {

        Map<String, String> fieldErrors = new HashMap<>();

        fieldErrors.put(
                exception.getVariableName(),
                "Path variable is required"
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ValidationResponse(
                        false,
                        "Invalid request path",
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI(),
                        fieldErrors
                ));
    }


    /*
     * ============================================================
     * 400 - Custom Bad Request
     * ============================================================
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException exception,
            HttpServletRequest request
    ) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        false,
                        exception.getMessage(),
                        LocalDateTime.now(),
                        status.value(),
                        request.getRequestURI()
                ));
    }


    /*
     * ============================================================
     * 404 - Resource Not Found
     * ============================================================
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(
            NotFoundException exception,
            HttpServletRequest request
    ) {

        HttpStatus status = HttpStatus.NOT_FOUND;

        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        false,
                        exception.getMessage(),
                        LocalDateTime.now(),
                        status.value(),
                        request.getRequestURI()
                ));
    }


    /*
     * ============================================================
     * 404 - Endpoint Not Found
     * ============================================================
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {

        HttpStatus status = HttpStatus.NOT_FOUND;

        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        false,
                        "Endpoint not found",
                        LocalDateTime.now(),
                        status.value(),
                        request.getRequestURI()
                ));
    }


    /*
     * ============================================================
     * 403 - Forbidden
     * ============================================================
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException exception,
            HttpServletRequest request
    ) {

        HttpStatus status = HttpStatus.FORBIDDEN;

        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        false,
                        exception.getMessage(),
                        LocalDateTime.now(),
                        status.value(),
                        request.getRequestURI()
                ));
    }


    /*
     * ============================================================
     * 409 - Duplicate / Conflict
     * ============================================================
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateException(
            DuplicateKeyException exception,
            HttpServletRequest request
    ) {

        HttpStatus status = HttpStatus.CONFLICT;

        log.warn(
                "Duplicate resource | method={} | path={} | message={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        false,
                        "Resource already exists",
                        LocalDateTime.now(),
                        status.value(),
                        request.getRequestURI()
                ));
    }


    /*
     * ============================================================
     * 405 - HTTP Method Not Supported
     * ============================================================
     *
     * Example:
     *
     * GET /api/bookings
     *
     * when only POST is supported.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {

        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;

        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        false,
                        "HTTP method not supported for this endpoint",
                        LocalDateTime.now(),
                        status.value(),
                        request.getRequestURI()
                ));
    }


    /*
     * ============================================================
     * 415 - Unsupported Content Type
     * ============================================================
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {

        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;

        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        false,
                        "Unsupported content type",
                        LocalDateTime.now(),
                        status.value(),
                        request.getRequestURI()
                ));
    }


    /*
     * ============================================================
     * 500 - Unexpected Exception
     * ============================================================
     *
     * IMPORTANT:
     *
     * Full exception is logged on server.
     * Internal exception details are NOT exposed to client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request
    ) {

        log.error(
                "Unhandled exception | method={} | path={}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        false,
                        "Something went wrong, Please try after some time",
                        LocalDateTime.now(),
                        status.value(),
                        request.getRequestURI()
                ));
    }
}
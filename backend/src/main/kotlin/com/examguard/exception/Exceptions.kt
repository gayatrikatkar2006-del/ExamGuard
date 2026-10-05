package com.examguard.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

/** Custom exception hierarchy: every business-rule failure maps to an HTTP status. */
open class ApiException(message: String, val status: HttpStatus) : RuntimeException(message)

class BadRequestException(message: String) : ApiException(message, HttpStatus.BAD_REQUEST)
class UnauthorizedException(message: String) : ApiException(message, HttpStatus.UNAUTHORIZED)
class NotFoundException(message: String) : ApiException(message, HttpStatus.NOT_FOUND)
class ConflictException(message: String) : ApiException(message, HttpStatus.CONFLICT)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApi(e: ApiException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(e.status).body(mapOf("error" to (e.message ?: "Request failed")))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val fields: Map<String, String> =
            e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return ResponseEntity.badRequest().body(
            mapOf("error" to (fields.values.firstOrNull() ?: "Validation failed"), "fields" to fields)
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(e: HttpMessageNotReadableException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.badRequest().body(mapOf("error" to "Malformed request body"))

    @ExceptionHandler(AccessDeniedException::class)
    fun handleDenied(e: AccessDeniedException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "You do not have permission for this action"))

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNotFound(e: NoResourceFoundException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Not found"))

    @ExceptionHandler(Exception::class)
    fun handleOther(e: Exception): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to "Unexpected server error: ${e.javaClass.simpleName}"))
}

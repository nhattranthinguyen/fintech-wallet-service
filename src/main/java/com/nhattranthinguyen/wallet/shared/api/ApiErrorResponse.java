package com.nhattranthinguyen.wallet.shared.api;

import java.time.OffsetDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiError", description = "Standard error response")
public record ApiErrorResponse(
    @Schema(example = "2026-07-25T03:34:10Z")
    OffsetDateTime timestamp,

    @Schema(example = "400")
    int status,

    @Schema(example = "Bad Request")
    String error,

    @Schema(example = "Validation failed")
    String message,

    @Schema(description = "Validation messages keyed by field name")
    Map<String, String> errors
) {
}

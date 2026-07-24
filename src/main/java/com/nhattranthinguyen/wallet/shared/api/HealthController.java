package com.nhattranthinguyen.wallet.shared.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Health", description = "Service availability checks")
public class HealthController {
    @GetMapping("/ping")
    @Operation(
        summary = "Ping the service",
        description = "Returns a simple response when the application is running."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Service is available",
        content = @Content(
            mediaType = "text/plain",
            schema = @Schema(type = "string", example = "pong")
        )
    )
    public String ping() {
        return "pong";
    }
}

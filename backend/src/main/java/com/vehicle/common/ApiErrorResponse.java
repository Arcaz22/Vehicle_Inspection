package com.vehicle.common;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ApiErrorResponse", description = "Standard API error response")
public record ApiErrorResponse(
        @Schema(name = "status_code", description = "HTTP status code", example = "400") int statusCode,
        String message,
        List<ApiError> errors) {}

package com.vehicle.common;

import com.vehicle.inspection.InspectionSummary;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "InspectionListResponse", description = "Successful paginated inspection response")
public record InspectionListResponseSchema(
        @Schema(name = "status_code", example = "200") int statusCode,
        String message,
        List<InspectionSummary> data,
        ApiResponse.PageMeta meta) {}

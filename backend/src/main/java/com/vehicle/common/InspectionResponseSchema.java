package com.vehicle.common;

import com.vehicle.inspection.InspectionView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "InspectionResponse", description = "Standard successful response containing one inspection")
public record InspectionResponseSchema(
        @Schema(name = "status_code", example = "200") int statusCode,
        String message,
        InspectionView data) {}

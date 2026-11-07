package com.vehicle.common;

import com.vehicle.vehicle.VehicleView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "VehicleListResponse", description = "Standard successful response containing vehicles")
public record VehicleListResponseSchema(
        @Schema(name = "status_code", example = "200") int statusCode,
        String message,
        List<VehicleView> data) {}

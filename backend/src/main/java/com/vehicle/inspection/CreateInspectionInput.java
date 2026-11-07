package com.vehicle.inspection;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "CreateInspectionRequest")
public record CreateInspectionInput(
        @Schema(name = "vehicle_id", description = "ID of an existing vehicle", required = true, example = "12") Long vehicleId,
        @Schema(name = "inspector_name", description = "Name of the inspector", required = true, example = "Rina") String inspectorName,
        @Schema(name = "has_key", description = "Whether the vehicle key is available", required = true, example = "true") Boolean hasKey,
        @Schema(description = "Inspection items; optional when creating a draft") List<InspectionItemInput> items) {}

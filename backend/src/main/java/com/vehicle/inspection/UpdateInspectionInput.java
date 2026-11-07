package com.vehicle.inspection;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "UpdateInspectionRequest", description = "Replaces all items on an editable inspection")
public record UpdateInspectionInput(
        @Schema(description = "Current inspection version used for optimistic locking", required = true, example = "1") Long version,
        @Schema(name = "inspector_name", required = true, example = "Rina") String inspectorName,
        @Schema(name = "has_key", required = true, example = "true") Boolean hasKey,
        @Schema(description = "Replacement list of all inspection items", required = true) List<InspectionItemInput> items) {}

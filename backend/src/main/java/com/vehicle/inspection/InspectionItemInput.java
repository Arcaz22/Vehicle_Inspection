package com.vehicle.inspection;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "InspectionItemRequest")
public record InspectionItemInput(
        @Schema(name = "point_code", description = "Point code from checklist.json", required = true, example = "EXT_FRONT") String pointCode,
        @Schema(description = "GOOD, MINOR_DAMAGE, MAJOR_DAMAGE, or NOT_INSPECTABLE", required = true, example = "GOOD") String condition,
        @Schema(description = "Required when condition is NOT_INSPECTABLE", example = "Key unavailable") String note,
        @Schema(name = "photo_refs", description = "File references; at least one is required for MAJOR_DAMAGE") List<String> photoRefs) {}

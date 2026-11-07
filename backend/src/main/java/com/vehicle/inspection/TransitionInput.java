package com.vehicle.inspection;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "TransitionInspectionRequest")
public record TransitionInput(
        @Schema(description = "Target status", required = true, example = "SUBMITTED") String to,
        @Schema(description = "Name or identifier of the actor", required = true, example = "supervisor-01") String actor,
        @Schema(description = "Optional transition note", example = "Checklist complete") String note) {}

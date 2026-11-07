package com.vehicle.audit;

import java.time.Instant;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AuditEvent")
public record AuditEventView(
        @Schema(name = "inspection_id") Long inspectionId,
        String action,
        @Schema(name = "from_status") String fromStatus,
        @Schema(name = "to_status") String toStatus,
        String actor,
        Instant at,
        Map<String, Object> meta) {}

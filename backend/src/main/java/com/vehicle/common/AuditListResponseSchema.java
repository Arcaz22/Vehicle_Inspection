package com.vehicle.common;

import com.vehicle.audit.AuditEventView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "AuditListResponse", description = "Successful response containing inspection audit events")
public record AuditListResponseSchema(
        @Schema(name = "status_code", example = "200") int statusCode,
        String message,
        List<AuditEventView> data) {}

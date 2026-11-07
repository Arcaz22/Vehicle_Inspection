package com.vehicle.common;

import com.vehicle.inspection.SyncBatchView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "SyncResponse", description = "Successful batch sync response")
public record SyncResponseSchema(
        @Schema(name = "status_code", example = "200") int statusCode,
        String message,
        SyncBatchView data) {}

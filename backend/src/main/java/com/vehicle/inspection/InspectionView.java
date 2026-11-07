package com.vehicle.inspection;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "Inspection")
public record InspectionView(
        Long id,
        @Schema(name = "vehicle_id") Long vehicleId,
        @Schema(name = "client_id") UUID clientId,
        @Schema(name = "inspector_name") String inspectorName,
        @Schema(name = "has_key") boolean hasKey,
        InspectionStatus status,
        Long version,
        @Schema(name = "created_at") Instant createdAt,
        @Schema(name = "updated_at") Instant updatedAt,
        List<InspectionItemView> items) {}

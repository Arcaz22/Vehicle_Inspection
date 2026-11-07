package com.vehicle.inspection;

import java.util.List;
import java.util.UUID;

public record SyncInspectionInput(
        UUID clientId,
        Long baseVersion,
        Long vehicleId,
        String inspectorName,
        Boolean hasKey,
        String status,
        List<InspectionItemInput> items) {}

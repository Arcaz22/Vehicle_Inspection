package com.vehicle.inspection;

import com.vehicle.common.ApiError;

import java.util.List;
import java.util.UUID;

public record SyncResult(
        UUID clientId,
        String result,
        Long inspectionId,
        Long serverVersion,
        InspectionStatus serverStatus,
        List<ApiError> errors) {}

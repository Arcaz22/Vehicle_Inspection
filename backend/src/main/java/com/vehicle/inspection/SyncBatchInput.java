package com.vehicle.inspection;

import java.util.List;

public record SyncBatchInput(String deviceId, List<SyncInspectionInput> inspections) {}

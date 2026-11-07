package com.vehicle.inspection;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "InspectionItem")
public record InspectionItemView(
        @Schema(name = "point_code") String pointCode,
        InspectionCondition condition,
        String note,
        @Schema(name = "photo_refs") List<String> photoRefs) {}

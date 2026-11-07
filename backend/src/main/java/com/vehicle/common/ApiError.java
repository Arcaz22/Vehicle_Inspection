package com.vehicle.common;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ApiError")
public record ApiError(String field, String reason) {}

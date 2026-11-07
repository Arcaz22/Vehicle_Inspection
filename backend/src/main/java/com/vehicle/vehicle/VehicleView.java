package com.vehicle.vehicle;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record VehicleView(
        @Schema(description = "Vehicle identifier", example = "12") Long id,
        @Schema(name = "plate_number", description = "Vehicle registration plate", example = "B 1001 ABC") String plateNumber,
        @Schema(description = "Vehicle type", example = "CAR") String type,
        @Schema(description = "Vehicle brand", example = "Toyota") String brand,
        @Schema(description = "Vehicle model", example = "Avanza") String model,
        @Schema(description = "Manufacturing year", example = "2020") Integer year) {}

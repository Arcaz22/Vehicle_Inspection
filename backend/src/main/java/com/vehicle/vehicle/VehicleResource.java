package com.vehicle.vehicle;

import com.vehicle.common.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/vehicles")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Vehicles", description = "Read-only vehicle catalog")
public class VehicleResource {
    @Inject
    VehicleRepository vehicles;

    @GET
    @Operation(operationId = "listVehicles", summary = "List vehicles", description = "Returns the 20 seeded CAR vehicles. This endpoint is not paginated.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Vehicles retrieved", content = @Content(schema = @Schema(implementation = com.vehicle.common.VehicleListResponseSchema.class))),
            @APIResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class)))
    })
    public ApiResponse<List<VehicleView>> list() {
        List<VehicleView> result = vehicles.listOrdered().stream()
                .map(vehicle -> new VehicleView(vehicle.id, vehicle.plateNumber, vehicle.type,
                        vehicle.brand, vehicle.model, vehicle.year))
                .toList();
        return ApiResponse.of(200, "Vehicles retrieved successfully", result);
    }
}

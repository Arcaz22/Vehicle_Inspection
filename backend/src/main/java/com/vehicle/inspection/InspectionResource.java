package com.vehicle.inspection;

import com.vehicle.common.ApiException;
import com.vehicle.common.ApiError;
import com.vehicle.common.ApiResponse;
import com.vehicle.audit.AuditEventView;
import com.vehicle.audit.AuditService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Locale;

@Path("/api/v1/inspections")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Inspections", description = "Create, update, retrieve, list, transition, and synchronize inspections")
public class InspectionResource {
    @Inject
    InspectionService service;

    @Inject
    AuditService audit;

    @Inject
    SyncItemProcessor syncProcessor;

    @POST
    @Operation(operationId = "createInspection", summary = "Create a draft inspection")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Inspection created", content = @Content(schema = @Schema(implementation = com.vehicle.common.InspectionResponseSchema.class))),
            @APIResponse(responseCode = "400", description = "Request validation failed", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class))),
            @APIResponse(responseCode = "404", description = "Vehicle not found", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class)))
    })
    public Response create(CreateInspectionInput input) {
        InspectionView created = service.create(input);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.of(Response.Status.CREATED.getStatusCode(), "Inspection created successfully", created))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(operationId = "updateInspection", summary = "Replace a draft or recheck inspection")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Inspection updated", content = @Content(schema = @Schema(implementation = com.vehicle.common.InspectionResponseSchema.class))),
            @APIResponse(responseCode = "400", description = "Request validation failed", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class))),
            @APIResponse(responseCode = "404", description = "Inspection not found", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Inspection is not editable or version is stale", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class)))
    })
    public ApiResponse<InspectionView> update(
            @Parameter(description = "Inspection ID", example = "101") @PathParam("id") long id,
            UpdateInspectionInput input) {
        return ApiResponse.of(200, "Inspection updated successfully", service.update(id, input));
    }

    @GET
    @Path("/{id}")
    @Operation(operationId = "getInspection", summary = "Get inspection details", description = "Returns an inspection and its inspection items.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Inspection retrieved", content = @Content(schema = @Schema(implementation = com.vehicle.common.InspectionResponseSchema.class))),
            @APIResponse(responseCode = "404", description = "Inspection not found", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class)))
    })
    public ApiResponse<InspectionView> get(
            @Parameter(description = "Inspection ID", example = "101") @PathParam("id") long id) {
        return ApiResponse.of(200, "Inspection retrieved successfully", service.get(id));
    }

    @POST
    @Path("/{id}/transition")
    @Operation(operationId = "transitionInspection", summary = "Transition inspection status")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Status transitioned", content = @Content(schema = @Schema(implementation = com.vehicle.common.InspectionResponseSchema.class))),
            @APIResponse(responseCode = "400", description = "Unknown status or incomplete inspection", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class))),
            @APIResponse(responseCode = "404", description = "Inspection not found", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Illegal status transition", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class)))
    })
    public ApiResponse<InspectionView> transition(
            @Parameter(description = "Inspection ID", example = "101") @PathParam("id") long id,
            TransitionInput input) {
        return ApiResponse.of(200, "Inspection status updated successfully", service.transition(id, input));
    }

    @GET
    @Path("/{id}/audit")
    @Operation(operationId = "getInspectionAudit", summary = "Get inspection audit history", description = "Returns audit events ordered by event time ascending.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Audit events retrieved", content = @Content(schema = @Schema(implementation = com.vehicle.common.AuditListResponseSchema.class))),
            @APIResponse(responseCode = "404", description = "Inspection not found", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class)))
    })
    public ApiResponse<List<AuditEventView>> audit(
            @Parameter(description = "Inspection ID", example = "101") @PathParam("id") long id) {
        if (!service.exists(id)) {
            throw ApiException.notFound("Inspection not found");
        }
        return ApiResponse.of(200, "Inspection audit retrieved successfully", audit.findByInspectionId(id));
    }

    @GET
    @Operation(operationId = "listInspections", summary = "List inspections", description = "Filters inspections and sorts by updated_at descending. Sort direction is fixed by the API contract.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Inspections retrieved", content = @Content(schema = @Schema(implementation = com.vehicle.common.InspectionListResponseSchema.class))),
            @APIResponse(responseCode = "400", description = "Invalid filter or pagination parameter", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class)))
    })
    public ApiResponse<List<InspectionSummary>> list(
            @Parameter(description = "Filter by inspection status", schema = @Schema(enumeration = {"DRAFT", "SUBMITTED", "APPROVED", "RECHECK"}))
            @QueryParam("status") String statusValue,
            @Parameter(description = "Filter by vehicle ID", example = "12")
            @QueryParam("vehicle_id") Long vehicleId,
            @Parameter(description = "Page number, starting at 1", example = "1")
            @DefaultValue("1") @QueryParam("page") int page,
            @Parameter(description = "Number of records per page, maximum 100", example = "10")
            @DefaultValue("10") @QueryParam("limit") int limit) {
        InspectionStatus status = parseStatus(statusValue);
        List<InspectionSummary> data = service.list(status, vehicleId, page, limit);
        long total = service.count(status, vehicleId);
        return ApiResponse.page(200, "Inspections retrieved successfully", data,
                new ApiResponse.PageMeta(page, limit, total));
    }

    @POST
    @Path("/sync")
    @Operation(operationId = "syncInspections", summary = "Synchronize offline inspections", description = "Processes each inspection independently and returns results in request order.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Batch processed", content = @Content(schema = @Schema(implementation = com.vehicle.common.SyncResponseSchema.class))),
            @APIResponse(responseCode = "400", description = "Invalid batch body", content = @Content(schema = @Schema(implementation = com.vehicle.common.ApiErrorResponse.class)))
    })
    public ApiResponse<SyncBatchView> sync(SyncBatchInput input) {
        if (input == null || input.inspections() == null) {
            throw ApiException.validation(List.of(new ApiError("inspections", "array is required")));
        }
        if (input.deviceId() == null || input.deviceId().isBlank()) {
            throw ApiException.validation(List.of(new ApiError("device_id", "is required")));
        }
        List<SyncResult> results = input.inspections().stream()
                .map(item -> syncProcessor.process(input.deviceId(), item))
                .toList();
        return ApiResponse.of(200, "Inspection batch synchronized", new SyncBatchView(results));
    }

    private InspectionStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return InspectionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw ApiException.validation(List.of(new ApiError("status", "is not a valid inspection status")));
        }
    }
}

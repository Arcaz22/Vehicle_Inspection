package com.vehicle.common;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class OptimisticLockExceptionMapper implements ExceptionMapper<OptimisticLockException> {
    @Override
    public Response toResponse(OptimisticLockException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ApiErrorResponse(Response.Status.CONFLICT.getStatusCode(), "Inspection version is stale",
                        List.of(new ApiError("version", "was changed by another request"))))
                .build();
    }
}

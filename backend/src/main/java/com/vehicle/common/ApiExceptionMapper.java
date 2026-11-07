package com.vehicle.common;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {
    @Override
    public Response toResponse(ApiException exception) {
        return Response.status(exception.status())
                .entity(new ApiErrorResponse(exception.status().getStatusCode(), exception.getMessage(), exception.errors()))
                .build();
    }
}

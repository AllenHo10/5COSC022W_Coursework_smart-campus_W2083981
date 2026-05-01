package com.example.exception;

import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * Part 5.3 – Maps SensorUnavailableException to HTTP 403 Forbidden.
 *
 * A sensor in MAINTENANCE status is physically disconnected and must not accept new readings.
 * 403 Forbidden is appropriate here: the server understands the request but refuses to
 * fulfil it due to the current state/policy of the resource.
 */
@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException e) {
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status", 403,
                        "error", "Forbidden",
                        "message", e.getMessage(),
                        "sensorId", e.getSensorId(),
                        "hint", "Change sensor status to ACTIVE before posting readings."
                ))
                .build();
    }
}

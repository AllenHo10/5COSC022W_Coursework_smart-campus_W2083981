package com.example.exception;

import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * Part 5.1 – Maps RoomNotEmptyException to HTTP 409 Conflict.
 *
 * Returned when a client attempts to DELETE a room that still has sensors assigned.
 * The JSON body clearly communicates why the operation was blocked so the client
 * can take corrective action (deregister sensors first).
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException e) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status", 409,
                        "error", "Conflict",
                        "message", e.getMessage(),
                        "roomId", e.getRoomId(),
                        "activeSensors", e.getSensorCount(),
                        "hint", "Please deregister all sensors from this room before deleting it."
                ))
                .build();
    }
}

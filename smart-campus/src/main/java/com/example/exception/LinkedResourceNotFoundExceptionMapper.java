package com.example.exception;

import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * Part 5.2 – Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.
 *
 * Why 422 and not 404?
 * HTTP 404 "Not Found" means the *requested resource URL* does not exist — the endpoint itself
 * cannot be located. HTTP 422 means the request was syntactically valid and the endpoint was
 * found, but the *payload content* is semantically invalid because it references an entity that
 * does not exist in the system. Here the POST /sensors URL is perfectly valid; it is the
 * roomId *value inside the JSON body* that is unresolvable. Using 404 would be misleading —
 * it would imply the /sensors endpoint is missing. 422 precisely communicates: "I understood
 * your request but cannot process it due to a semantic error in your data."
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException e) {
        return Response.status(422) // 422 Unprocessable Entity
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status", 422,
                        "error", "Unprocessable Entity",
                        "message", e.getMessage(),
                        "missingResourceType", e.getResourceType(),
                        "missingResourceId", e.getResourceId(),
                        "hint", "Ensure the referenced " + e.getResourceType() + " exists before creating this resource."
                ))
                .build();
    }
}

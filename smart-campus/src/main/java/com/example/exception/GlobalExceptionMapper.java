package com.example.exception;

import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.4 – Global safety net: catches any Throwable not handled by a more specific mapper.
 *
 * Cybersecurity risk of exposing stack traces:
 * A raw Java stack trace leaks precise information an attacker can exploit:
 *  - Full class/package names reveal the internal framework and library versions,
 *    enabling targeted CVE searches (e.g., "this app uses Jersey 2.35 which has CVE-XXXX").
 *  - File paths and line numbers disclose the source structure, aiding code-injection targeting.
 *  - Exception messages often contain SQL snippets, internal hostnames, or database schema names.
 *  - The technology stack itself (Quarkus, Hibernate, etc.) becomes known, narrowing attack surface.
 * By returning only a generic 500 message, we practice security-by-obscurity as a baseline
 * defence, while logging the full trace server-side for legitimate debugging.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        // Log the full details server-side so developers can investigate
        LOG.log(Level.SEVERE, "Unhandled exception intercepted by GlobalExceptionMapper", e);

        // Return a generic, non-revealing error to the external consumer
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status", 500,
                        "error", "Internal Server Error",
                        "message", "An unexpected error occurred. Please contact the system administrator."
                ))
                .build();
    }
}

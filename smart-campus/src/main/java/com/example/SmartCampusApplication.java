package com.example;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application entry point.
 * All resource paths are versioned under /api/v1.
 *
 * By default, JAX-RS creates a new instance of each Resource class per request (request-scoped).
 * This means data stored as instance fields would be lost between requests.
 * To share state across requests we use a CDI @ApplicationScoped DataStore singleton,
 * injected via @Inject, which is thread-safe via ConcurrentHashMap.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
}

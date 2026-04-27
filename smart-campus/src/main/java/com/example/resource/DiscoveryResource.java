package com.example.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1.2 – Discovery endpoint.
 * Returns API metadata including version, contact, and HATEOAS-style resource links.
 *
 * HATEOAS (Hypermedia as the Engine of Application State) allows clients to navigate
 * the API entirely through links embedded in responses, rather than relying on
 * out-of-band static documentation. This means clients are decoupled from hardcoded URLs
 * and can discover available operations dynamically, reducing brittle coupling and
 * making the API more evolvable.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("api", "Smart Campus Sensor & Room Management API");
        meta.put("version", "1.0.0");
        meta.put("description", "RESTful API for managing campus rooms and IoT sensors.");
        meta.put("contact", Map.of(
            "name", "Campus IT Admin",
            "email", "admin@university.ac.uk"
        ));

        Map<String, String> links = new LinkedHashMap<>();
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        meta.put("resources", links);

        Map<String, String> hateoas = new LinkedHashMap<>();
        hateoas.put("self", "/api/v1");
        hateoas.put("rooms", "/api/v1/rooms");
        hateoas.put("sensors", "/api/v1/sensors");
        meta.put("_links", hateoas);

        return Response.ok(meta).build();
    }
}

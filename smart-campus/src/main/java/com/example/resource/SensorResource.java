package com.example.resource;

import com.example.exception.LinkedResourceNotFoundException;
import com.example.model.Room;
import com.example.model.Sensor;
import com.example.store.DataStore;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Part 3 – Sensor Operations & Part 4 – Sub-Resource Locator.
 *
 * @Consumes(APPLICATION_JSON) technical consequence:
 * If a client sends a request with Content-Type: text/plain or application/xml,
 * JAX-RS will reject it before the method is even invoked, returning HTTP 415
 * Unsupported Media Type. The runtime checks the Content-Type header against the
 * declared @Consumes types and short-circuits with 415 on a mismatch.
 *
 * @QueryParam vs @PathParam for filtering:
 * Query parameters (?type=CO2) are semantically correct for filtering a collection
 * because filtering does not change the identity of the resource being addressed –
 * /sensors is still the sensors collection. A path segment (/sensors/type/CO2)
 * implies a distinct hierarchical resource, which is misleading. Query params also
 * allow multiple independent filters to be combined freely (?type=CO2&status=ACTIVE)
 * without requiring exponential path combinations.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @Inject
    DataStore store;

    // GET /api/v1/sensors  (optional ?type=xxx filter)
    @GET
    public Collection<Sensor> getSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = store.getSensors().values();
        if (type == null || type.isBlank()) {
            return all;
        }
        return all.stream()
                .filter(s -> type.equalsIgnoreCase(s.getType()))
                .collect(Collectors.toList());
    }

    // POST /api/v1/sensors
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Sensor id is required."))
                    .build();
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A sensor with id '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // Part 3.1 – Validate that the referenced roomId actually exists
        String roomId = sensor.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "roomId is required."))
                    .build();
        }
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            // Throw custom exception → 422 mapper
            throw new LinkedResourceNotFoundException("Room", roomId);
        }

        // Default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.getSensors().put(sensor.getId(), sensor);
        // Register sensor in room's sensorIds list
        room.getSensorIds().add(sensor.getId());

        return Response.created(URI.create("/api/v1/sensors/" + sensor.getId()))
                .entity(sensor)
                .build();
    }

    // GET /api/v1/sensors/{sensorId}
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // DELETE /api/v1/sensors/{sensorId}
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                    .build();
        }
        // Remove from parent room's list
        Room room = store.getRooms().get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().remove(sensorId);
        }
        store.getSensors().remove(sensorId);
        return Response.noContent().build();
    }

    /**
     * Part 4.1 – Sub-Resource Locator.
     * Returns a SensorReadingResource instance to handle all /sensors/{sensorId}/readings paths.
     * No HTTP method annotation here — JAX-RS uses this method as a locator.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId, store);
    }
}

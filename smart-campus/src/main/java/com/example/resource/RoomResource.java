package com.example.resource;

import com.example.exception.RoomNotEmptyException;
import com.example.model.Room;
import com.example.model.Sensor;
import com.example.store.DataStore;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Part 2 – Room Management.
 *
 * Returning full objects vs IDs:
 * Returning only IDs keeps the list response small and fast (low bandwidth), but forces
 * clients to make N additional GET requests to retrieve each room's details (the "N+1 problem").
 * Returning full objects in the list is higher bandwidth but reduces round-trips and client-side
 * complexity, which is preferable when the client typically needs all the data.
 * A pragmatic middle ground is a "summary" projection (id + name only) with links to full detail.
 *
 * Idempotency of DELETE:
 * DELETE is idempotent by the HTTP spec: multiple identical requests must produce the same server
 * state. In this implementation, the first DELETE removes the room and returns 204. Subsequent
 * identical requests find no room and return 404. While the response code differs, the server
 * state (room absent) is identical after both calls, satisfying the idempotency contract.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @Inject
    DataStore store;

    // GET /api/v1/rooms
    @GET
    public Collection<Room> getAllRooms() {
        return store.getRooms().values();
    }

    // POST /api/v1/rooms
    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Room id is required."))
                    .build();
        }
        if (store.getRooms().containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A room with id '" + room.getId() + "' already exists."))
                    .build();
        }
        store.getRooms().put(room.getId(), room);
        return Response.created(URI.create("/api/v1/rooms/" + room.getId()))
                .entity(room)
                .build();
    }

    // GET /api/v1/rooms/{roomId}
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room '" + roomId + "' not found."))
                    .build();
        }
        return Response.ok(room).build();
    }

    // DELETE /api/v1/rooms/{roomId}
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room '" + roomId + "' not found."))
                    .build();
        }
        // Business logic: block deletion if sensors are still assigned
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }
        store.getRooms().remove(roomId);
        return Response.noContent().build();
    }
}

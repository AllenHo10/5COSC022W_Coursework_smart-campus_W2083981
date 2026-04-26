package com.example.store;

import com.example.model.Room;
import com.example.model.Sensor;
import com.example.model.SensorReading;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton application-scoped store for all in-memory data.
 * Using ConcurrentHashMap ensures thread-safe access without explicit synchronization
 * on individual read/write operations.
 */
@ApplicationScoped
public class DataStore {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    // readings keyed by sensorId -> list of readings
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    public DataStore() {
        // Seed some sample data
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Science Lab", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, "LIB-301");
        r1.getSensorIds().add(s1.getId());
        sensors.put(s1.getId(), s1);

        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 420.0, "LAB-101");
        r2.getSensorIds().add(s2.getId());
        sensors.put(s2.getId(), s2);

        readings.put("TEMP-001", new ArrayList<>());
        readings.put("CO2-001", new ArrayList<>());
    }

    public Map<String, Room> getRooms() { return rooms; }
    public Map<String, Sensor> getSensors() { return sensors; }

    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.computeIfAbsent(sensorId, k -> new ArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        getReadingsForSensor(sensorId).add(reading);
    }
}

# Smart Campus – Sensor & Room Management API

A RESTful JAX-RS API built with **Quarkus** and **RESTEasy** for managing campus rooms and IoT sensors.  
All data is held in-memory using `ConcurrentHashMap` and `ArrayList` — no database required.

---

## API Overview

| Base Path | Description |
|---|---|
| `GET /api/v1` | Discovery – API metadata and resource links |
| `/api/v1/rooms` | Room management (CRUD) |
| `/api/v1/sensors` | Sensor registration and retrieval |
| `/api/v1/sensors/{id}/readings` | Historical sensor readings (sub-resource) |

---

## Build & Run

### Prerequisites

- Java 17+
- Apache Maven 3.8+

### macOS / Linux

```bash
# Install (if needed)
brew install openjdk@17 maven

# Build and start dev server
cd smart-campus
mvn quarkus:dev
```

### Windows (PowerShell)

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
winget install Apache.Maven
cd smart-campus
mvn quarkus:dev
```

The first run downloads dependencies (~2 min). Once you see `Quarkus started`, the API is live at:

```
http://localhost:8080/api/v1
```

---

## Sample curl Commands

### 1. Discovery endpoint
```bash
curl -s http://localhost:8080/api/v1 | jq
```

### 2. Create a room
```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"ENG-201","name":"Engineering Lab","capacity":40}' | jq
```

### 3. Register a sensor (valid roomId)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":22.0,"roomId":"ENG-201"}' | jq
```

### 4. Filter sensors by type
```bash
curl -s "http://localhost:8080/api/v1/sensors?type=Temperature" | jq
```

### 5. Post a sensor reading (updates currentValue)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-002/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.7}' | jq
```

### 6. Get reading history for a sensor
```bash
curl -s http://localhost:8080/api/v1/sensors/TEMP-002/readings | jq
```

### 7. Attempt to delete a room with sensors (→ 409 Conflict)
```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 | jq
```

### 8. Register sensor with non-existent roomId (→ 422 Unprocessable Entity)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","roomId":"FAKE-999"}' | jq
```

### 9. Post reading to a MAINTENANCE sensor (→ 403 Forbidden)
```bash
# First set sensor to maintenance (delete & re-create with MAINTENANCE status)
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"MNT-001","type":"CO2","status":"MAINTENANCE","roomId":"LIB-301"}' | jq

curl -s -X POST http://localhost:8080/api/v1/sensors/MNT-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":500.0}' | jq
```

---

## Report – Answers to Coursework Questions

### Part 1.1 – JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (request-scoped). This means any instance field on a resource class is discarded after the request completes. If data were stored as instance fields (e.g., `private Map<String, Room> rooms = new HashMap<>()`), every request would start with an empty map, making persistence impossible.

To manage shared in-memory data, this project uses a **CDI `@ApplicationScoped` `DataStore` singleton**, injected via `@Inject`. The singleton is created once for the application lifetime and shared across all requests. Because multiple requests can arrive concurrently, the data structures are `ConcurrentHashMap` instances, which provide thread-safe atomic operations for reads and writes without requiring explicit `synchronized` blocks. This prevents race conditions such as two simultaneous `POST /rooms` requests creating duplicate entries or a `DELETE` and a `GET` conflicting mid-operation.

---

### Part 1.2 – HATEOAS

HATEOAS (Hypermedia as the Engine of Application State) means embedding navigational links inside API responses so clients can discover available actions without consulting out-of-band documentation. For example, a response to `GET /api/v1/rooms/LIB-301` might include `"_links": {"sensors": "/api/v1/rooms/LIB-301/sensors", "delete": "/api/v1/rooms/LIB-301"}`.

This benefits client developers in several key ways. Static documentation becomes stale and must be manually synchronised with the API implementation. Hypermedia keeps the client decoupled — if a URL structure changes, the server updates the links in responses and clients that follow links rather than hardcode URLs require no changes. It also self-documents the available transitions from each resource state, reducing the risk of clients constructing invalid URLs. It is considered a hallmark of mature RESTful design because it achieves true client-server decoupling, one of REST's core architectural constraints.

---

### Part 2.1 – ID-only vs Full Object in List Responses

Returning only IDs (e.g., `["LIB-301", "LAB-101"]`) keeps the list payload very small, which is efficient for bandwidth, especially with thousands of rooms. However, the client must then make one additional `GET /rooms/{id}` request per room to obtain usable data — the classic **N+1 request problem** — increasing total latency and server load significantly.

Returning full objects in the list eliminates the N+1 problem and reduces client-side complexity since all data is immediately usable. The trade-off is a larger initial response payload. The pragmatic choice depends on use case: if clients typically need all room details (e.g., to render a dashboard), full objects are superior. If clients only ever need a subset of rooms, a summary projection with a link to the detail endpoint (partial HATEOAS) offers the best of both.

---

### Part 2.2 – Idempotency of DELETE

DELETE is **idempotent** by the HTTP specification. Idempotency means applying the same operation multiple times produces the same server state as applying it once — it does **not** require the same response code each time.

In this implementation: the first `DELETE /rooms/ENG-201` removes the room and returns `204 No Content`. A second identical request finds no room and returns `404 Not Found`. The response code differs, but the server state is identical after both calls (the room is absent). This satisfies the idempotency contract. Clients that retry a DELETE due to a network timeout will not corrupt the system, which is the practical guarantee idempotency provides.

---

### Part 3.1 – `@Consumes` and Content-Type Mismatches

The `@Consumes(MediaType.APPLICATION_JSON)` annotation declares that the endpoint only accepts requests with `Content-Type: application/json`. If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, JAX-RS intercepts the request **before the resource method is invoked** and automatically returns **HTTP 415 Unsupported Media Type**. The framework inspects the `Content-Type` header, finds no registered `MessageBodyReader` that can deserialise the incoming format to the method's parameter type, and short-circuits with 415. This means no malformed or incorrectly typed data can ever reach the business logic.

---

### Part 3.2 – `@QueryParam` vs Path Segment for Filtering

`@QueryParam` (`/sensors?type=CO2`) is semantically correct for filtering because:

1. **Resource identity**: The path `/sensors` identifies the sensors *collection* as a resource. A filter is a view into that collection, not a different resource. Encoding the filter as a path segment (`/sensors/type/CO2`) falsely implies a separate hierarchical resource exists at that URI.
2. **Composability**: Query parameters combine freely: `?type=CO2&status=ACTIVE`. Achieving the same with path segments requires exponential route permutations.
3. **Optionality**: Query params are naturally optional. If omitted, the full collection is returned. Path segments are structural and cannot be easily omitted.
4. **Cacheability and convention**: Search/filter semantics are universally understood as a query string concern. Intermediate proxies and clients expect this pattern.

---

### Part 4.1 – Sub-Resource Locator Pattern

The Sub-Resource Locator pattern delegates a path subtree to a dedicated class. In this project, `SensorResource` handles `/sensors/**`, and its locator method returns a `SensorReadingResource` instance to handle `/sensors/{id}/readings/**`.

Benefits in large APIs:
- **Separation of concerns**: `SensorResource` owns sensor CRUD logic; `SensorReadingResource` owns time-series history logic. Each class has a single, clear responsibility.
- **Reduced complexity**: A "god controller" handling every nested route becomes thousands of lines long and impossible to maintain. Sub-resources keep each class small and focused.
- **Independent testability**: `SensorReadingResource` can be unit-tested in isolation by instantiating it directly with a mock `DataStore`.
- **Composability**: The same sub-resource class could theoretically be reused under different parent paths without duplication.

---

### Part 5.2 – Why 422 is More Accurate Than 404 for Missing References

HTTP 404 "Not Found" signals that the **URL itself** cannot be resolved — the requested endpoint does not exist on the server. When a client `POST`s to `/api/v1/sensors` with a `roomId` that doesn't exist, the endpoint `/api/v1/sensors` is perfectly valid and found. The problem is that the **value inside the JSON payload** references a non-existent entity — a semantic error in the request data, not a routing error.

HTTP 422 "Unprocessable Entity" precisely means: "the server understood the request syntax and located the endpoint, but the contained instructions are semantically erroneous." Using 404 here would mislead clients into thinking the `/sensors` endpoint itself is missing, causing unnecessary confusion. 422 accurately tells the client: "your URL is fine, but fix your payload."

---

### Part 5.4 – Cybersecurity Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers carries several concrete security risks:

1. **Technology fingerprinting**: The trace reveals the exact framework (`io.quarkus`, `org.jboss.resteasy`), library versions, and Java version. Attackers can cross-reference these against public CVE databases to identify known vulnerabilities.
2. **Internal path disclosure**: Stack traces include full class package names and source file paths (e.g., `com.example.store.DataStore.getReadingsForSensor(DataStore.java:42)`), revealing the application's internal architecture and making targeted code-injection easier.
3. **Database and schema leakage**: Exception messages often include SQL queries, table names, or column names that help an attacker map the data model for SQL injection attacks.
4. **Business logic exposure**: Unexpected NullPointerExceptions or logic errors reveal code paths and conditional branches, assisting an attacker in crafting inputs that exploit edge cases.

The mitigation is to log the full trace server-side (visible only to authorised operators) while returning only a generic, opaque 500 message to external consumers — exactly what the `GlobalExceptionMapper` implements.

---

### Part 5.5 – JAX-RS Filters vs Manual Logging

Inserting `Logger.info()` calls inside every resource method violates the **Single-Responsibility Principle**: resource methods should contain business logic, not infrastructure concerns. Concretely:

- **DRY (Don't Repeat Yourself)**: Logging must be duplicated in every method. Forgetting a single one creates blind spots in observability.
- **Consistency**: A filter enforces a uniform log format across all endpoints automatically. Manual statements drift in format over time.
- **Maintainability**: If the log format needs to change (e.g., adding a correlation ID), there is exactly one place to update in the filter vs. dozens of resource methods.
- **Separation of concerns**: Filters are an AOP (Aspect-Oriented Programming) mechanism. Cross-cutting concerns like logging, authentication, and CORS belong in filters, keeping business code clean and readable.

# Space Management Tool #

COMSW4156 Advanced Software Engineering 

**Group Members**: Alyssa King, Annie Wang, Janice Wong, Karin Novelia, Shobini Iyer, Shriya Mani

### Overview 
This general-purpose space booking/reservation service will allow client programs to find, reserve, modify and cancel reservations for different spaces. The service is responsible for preventing overlapping reservations, checking that a space can accommodate the requested amount of people and ranking spaces based on a booker’s preferences, and optimally calculating reservation prices based on the space, duration, capacity, or pricing rules. It can also provide usage reports showing metrics like space utilization, space assignments, average reservation duration, peak booking time, and revenue for paid places. The service will maintain its own datastore of spaces, clients, and reservations.

### Skeleton Code ###

This repository was bootstrapped using the website https://start.spring.io/.
Initializr is a tool that generates a SpringBoot project using the user provided
customizations. Users configure the build automation tool they want to use, the
project language, SpringBoot version, project metadata and any dependencies.

Customizations:
- **Project**: Maven
- **Language**: Java
- **SpringBoot**: 4.1.2
- **Java**: 17
- **Dependencies**: SpringWeb, Spring JPA, Validation, PostgreSQL Driver, H2 Database

### Database Setup ###
This project uses PostgreSQL when testing locally, and an in-memory H2 database when running tests.

#### Local Testing ####
When testing locally, a Docker Postgre SQL database is spun up and ensures each person has their own copy of the DB to test and modify, but the structure will be the same for each person.

| Situation | Database | Config file |
|---|---|---|
| Running the service (`./mvnw spring-boot:run`) | PostgreSQL 17 in Docker | `src/main/resources/application.properties` |
| Running tests (`./mvnw test`) | H2 (in memory) | `src/test/resources/application.properties` |

**Prerequisite:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

**Start the database:**
```bash
docker compose up -d
```

**Run the service** (Hibernate creates or updates the tables from the entity classes on startup):
```bash
./mvnw spring-boot:run
```

**Other commands:**

| Command | What it does |
|---|---|
| `docker compose ps` | Check that the `jasaks-postgres` container is running |
| `docker compose stop` | Stop the database (data is kept) |
| `docker compose down -v` | Delete the database **and all its data**, for a clean reset |
| `docker exec -it jasaks-postgres psql -U jasaks -d jasaks` | Open a SQL prompt inside the database |

#### How `docker-compose.yaml` is configured

| Setting | Purpose |
|---|---|
| `services: postgres` | Defines a single container running the database. |
| `image: postgres:17` | Pins the Postgres version so everyone runs the same one. |
| `container_name: jasaks-postgres` | Gives the container a fixed name, so the same `docker` commands work on every machine. |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Create the `jasaks` database and login the first time the container starts. These must match `spring.datasource.*` in `src/main/resources/application.properties`. |
| `ports: "5433:5432"` | Maps port 5433 on local machine to Postgres's default port 5432 inside the container.|
| `volumes` (both blocks) | Keep the data when the container restarts. The top-level `volumes:` block declares a named storage area (`jasaks-data`); the one under the service attaches it to Postgres's data folder. |

The database connection settings in `application.properties` can be overridden with the `DB_URL`, `DB_USER` and `DB_PASSWORD` environment variables, e.g. to point at a cloud database instead.

### Tests (H2 in-memory database)

`./mvnw test` runs against **H2**, an in-memory database that starts with the tests and disappears when the tests finish. 
Each test run starts with an empty database, creates the tables from the entity classes, and inserts whatever data that test needs.

## Data Model

Entities live in `src/main/java/com/jasaks/model/`. Each one maps to a database table, which Hibernate creates automatically from the class.

| Entity | Table | Key fields | Notes |
|---|---|---|---|
| `Place` | `places` | `placeId`, `placeName`, `timeZone`, `openTime`, `closeTime`, `refundHours`, `refundPercent` | A building or venue. Holds the operating hours and cancellation policy shared by all its spaces. |
| `Space` | `spaces` | `spaceId`, `place`, `spaceName`, `capacity`, `hourlyRate`, `amenities` | A bookable room inside a Place. Amenities are stored in a separate `space_amenities` table. |
| `Booker` | `bookers` | `bookerId`, `name` | Who a reservation is for. The `bookerId` is supplied by the client program, not generated. |
| `Reservation` | `reservations` | `reservationId`, `booker`, `space`, `startTime`, `endTime`, `groupSize`, `price`, `status`, `createdAt` | Links a Booker to a Space for a time window. |

**Relationships:** a Place has many Spaces; a Space has many Reservations; a Booker has many Reservations. These are `@ManyToOne` links, stored as the foreign keys `place_id`, `space_id` and `booker_id`.

**Reservation status** (`ReservationStatus`): `CONFIRMED`, `COMPLETED` or `CANCELLED`. Cancelled reservations are kept rather than deleted, so reports keep the history.

### Time conventions
- `startTime`, `endTime`, `openTime` and `closeTime` are in the **place's local time zone** (`Place.timeZone`, e.g. `America/New_York`). Clients in other time zones must convert before calling the API.
- Any check against the current time uses the place's zone: `LocalDateTime.now(ZoneId.of(place.getTimeZone()))`, never the server's clock.
- `createdAt` is an `Instant`, an exact moment independent of time zone, and is set automatically when the reservation is first saved.
- Money (`hourlyRate`, `price`) is stored as `BigDecimal` with 2 decimal places.

## Repositories

Repositories live in `src/main/java/com/jasaks/repository/`. Each one extends Spring Data's `JpaRepository`, which provides `save`, `findById`, `findAll`, `existsById` and `deleteById` with no code. Spring generates the implementations at startup.

| Repository | Custom methods |
|---|---|
| `PlaceRepository` | none |
| `BookerRepository` | none |
| `SpaceRepository` | `findByPlace_PlaceId(placeId)`: all spaces in a place |
| `ReservationRepository` | `hasOverlap(spaceId, start, end)`: whether any non-cancelled reservation on the space overlaps the window. Used by Create and Search. |
| | `hasOverlapExcluding(spaceId, start, end, excludeId)`: the same check, ignoring one reservation. Used by Modify, so a reservation doesn't conflict with itself. |
| | `findByBookerBookerId(bookerId)`: all of a booker's reservations |
| | `findActiveReservationsInWindow(spaceId, start, end)`: non-cancelled reservations overlapping a window. Used by the space report. |
| | `existsBySpaceSpaceId(spaceId)`: whether a space has any reservations. Blocks deleting it. |

**Overlap rule:** two time ranges overlap when each one starts before the other ends (`existing.start < new.end AND existing.end > new.start`). Back-to-back bookings, such as 10:00–12:00 followed by 12:00–13:00, do **not** overlap.

## Endpoints
### 1. List Spaces

**Endpoint:** `GET /api/spaces`

**Description:** Retrieves all spaces or filters spaces by a specific place.

**Input:**
- **Query Parameter (optional):** `placeId` (Long)
    - If omitted, returns all spaces.
    - If provided, returns only spaces belonging to the specified place.
- **Side effect** : None. This operation is read-only
- **Output**: A JSON array of SpaceResponse objects, containing fields such as spaceId, placeId, placeName, spaceName, capacity, hourlyRate, and amenities.
- **Status Codes** :
  -   200 OK: Successfully retrieves the spaces.
  -   An invalid placeId format may result in 400 Bad Request.
**Example Requests:**
```http
GET /api/spaces
GET /api/spaces?placeId=1
```

## Build and Run

**Clone the repo:**

```
git clone https://github.com/shriyamani/team-jasaks-COMSW4156.git
```

**Run maven:**
```
mvn spring-boot:run
```

## Testing

**Testing Frameworks used**:
- JUnit 5 (JUnit Jupiter)
- Spring Boot Test — @SpringBootTest for full-context integration tests
- Spring MockMvc — controller/HTTP layer testing
    - Hamcrest — assertions such as hasSize (pulled in transitively by Spring Test)      

**Mocking  Frameworks Used:**
- Mockito (JUnit 5 integration)                                                        
                                                                                                                                                                       
**Supporting tools:**                                                                                                                                           
- JaCoCo 0.8.14 — code coverage       

**To run tests, from root directory run:**
```
mvn clean test 
```
## Checkstyle
**From root directory run:**
```
mvn checkstyle:check
```

## AI Documentation
#### 1. Free CLI AI Tool OpenCode: https://opencode.ai/

**Usage:** Used to fill in javadoc comments so that the checkstyle run would come back clean 
- **Prompt:** Modify all files added on the feature/spaces-crud branch so that it aligns with the rules defined in the checkstyle.xml ruleset. 
   - OpenCode: 45,433 tokens   (Big Pickle model)
- **Prompt:** Implement the CRUD endpoints for managing the space objects.
   - OpenCode: 60,743 tokens (Big Pickle model)

#### 2. Gemini Pro 3.8 Flash (Student Subscription) within VS Code

**Usage:** Designed and implemented list user reservations endpoint and space report tool. Designed DTO records, and added repository queries. Generated unit tests. Wrote JavaDoc comments.

- **Prompt:** Implement the endpoint at GET /api/reservations. It should take a required userID and optional query parameters startDate, endDate, placeId, and spaceId. Return a list of ReservationResponse objects. Include JavaDoc comments.
   - ~8,500 tokens
- **Prompt:** Implement GET /api/reports/spaces. Inputs are required placeId, startDate, and endDate, with an optional spaceId. Calculate the following metrics for each space in the place: total reservations, total booked hours, average duration, space utilization percentage, peak booking hour, peak booking date, average group size, average capacity used percentage, and total revenue. Throw appropriate errors. In the chat window, output a sample response payload. Include JavaDoc comments.
  - ~13,800 tokens

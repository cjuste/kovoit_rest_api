# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
mvn clean package        # Build the project
mvn spring-boot:run      # Run the application (default port 8080)
mvn test                 # Run all tests
mvn test -Dtest=TravelerServiceTest  # Run a single test class
```

## Architecture

Spring Boot REST API written in Java using Jersey (JAX-RS) for routing.

**Request flow:** `TravelerApi` (JAX-RS resource) → `TravelerService` (business logic) → response

**Key structural decisions:**
- Jersey is registered via `JerseyConfig.java` — new API resources must be registered there with `register(MyApi.class)`
- API resources live in `src/main/java/com/kovoit/restapi/api/`
- Data models (beans) live in `src/main/java/com/kovoit/restapi/bean/` — implemented as Java Records
- Services live in `src/main/java/com/kovoit/restapi/service/`

**Current endpoints:**
- `GET /traveler/` — returns a list of `Traveler` objects as JSON

## Tech Stack

- Java 25 (Temurin 25.0.3)
- Spring Boot 4.0.5
- Jersey/JAX-RS for REST routing
- Jackson for JSON serialization (Records supported natively)
- Mockito + AssertJ for testing

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

Spring Boot REST API written in Kotlin using Jersey (JAX-RS) for routing.

**Request flow:** `TravelerApi` (JAX-RS resource) → `TravelerService` (business logic) → response

**Key structural decisions:**
- Jersey is registered via `JerseyConfig.kt` — new API resources must be registered there with `register(MyApi::class.java)`
- API resources live in `src/main/kotlin/com/kovoit/restapi/api/`
- Data models (beans) live in `src/main/kotlin/com/kovoit/restapi/bean/`
- Services live in `src/main/kotlin/com/kovoit/restapi/service/`

**Current endpoints:**
- `GET /traveler/` — returns a list of `Traveler` objects as JSON

## Tech Stack

- Kotlin 1.2.71 / Java 8
- Spring Boot 2.1.1
- Jersey/JAX-RS for REST routing
- Jackson Kotlin module for JSON serialization
- Mockito-Kotlin + AssertJ for testing
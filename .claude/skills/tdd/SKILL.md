---
name: TDD
description: Each time you're implementing a new feature or fixing a bug, write tests first.
---

### Mandatory sequence — never skip a step

1. **Write unit tests** (they must fail — no implementation yet)
2. **Implement** the minimal code to make unit tests pass
3. **Write integration tests** (they must also fail before wiring is complete)
4. **Verify** integration tests pass
5. **Refactor** if needed — all tests must stay green

A feature is NOT done until step 4 is complete.

### Unit tests
- Use Mockito + AssertJ (`@ExtendWith(MockitoExtension.class)`)
- AAA pattern: Arrange / Act / Assert
- One assertion per test when possible
- Test names describe behavior: `methodName_expectedBehavior_givenCondition`

### Integration tests (mandatory for every new API endpoint or service)
- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `RestTemplate`
- Use **Testcontainers** for a real Elasticsearch instance (see `TravelerIntegrationTest` for the pattern)
- Use **WireMock** to stub external HTTP services (Nominatim geocoding)
- Cover at minimum: happy path HTTP status, response body shape, data persisted in Elasticsearch
- Place in `src/test/java/com/kovoit/restapi/` (same package as `TravelerIntegrationTest`)
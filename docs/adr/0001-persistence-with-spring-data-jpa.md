# ADR 0001: Persistence with Spring Data JPA

- Status: Proposed
- Date: 2026-10-24
- Change: `add-user-accounts`

## Context

The walking skeleton only talks to the database through Flyway and plain JDBC. The first business tables (user accounts and refresh tokens) need a persistence layer, and the choice will be reused by every later feature. MatchMap is geographic at its core: pins, countries and cities will need PostGIS geometry types and spatial queries.

## Decision

Use Spring Data JPA with Hibernate as the persistence layer.

- Flyway owns the schema. Hibernate only validates it at startup (`spring.jpa.hibernate.ddl-auto=validate`) and never creates or alters tables.
- `spring.jpa.open-in-view` is disabled, so no lazy loading happens outside service transactions.
- Code is written against the Jakarta Persistence (JPA) API: annotations, `EntityManager`, `@Lock`. Hibernate is the provider underneath. Its native `Session` API is used only where a Hibernate extension requires it.
- Spatial columns will be mapped with Hibernate Spatial when the first geographic feature arrives. Hibernate Spatial is a Hibernate extension, not part of JPA, so that mapping is tied to Hibernate.
- Pessimistic row locks (the `FOR UPDATE` of refresh token rotation) are declared with `@Lock(LockModeType.PESSIMISTIC_WRITE)`.

## Alternatives considered

- **Spring Data JDBC.** Simpler model with no persistence context, but no mapping for PostGIS geometry types; every spatial column would need custom converters.
- **jOOQ.** Type-safe SQL with excellent control, but it adds a code-generation step to the build, and its open-source licence only covers open-source databases.
- **Plain `JdbcTemplate`.** No extra abstraction, but every query, mapping and transaction boundary is hand-written.

## Consequences

- Entities and repositories follow a widely known model, which lowers the entry cost for contributors.
- Hibernate Spatial gives direct access to PostGIS types. PostGIS functions that JPQL cannot express are written as native queries; Hibernate does not check native queries at startup, so they are covered by integration tests.
- Replacing the JPA provider would keep the entities and repositories, but the spatial mapping would have to be rewritten.
- Schema drift between Flyway and the entities stops the application at startup instead of failing at runtime.
- JPA's persistence context must be handled with care (transaction boundaries, pessimistic locks where concurrent updates matter).

# ADR 0001: Persistence with Spring Data JPA

- Status: Accepted
- Date: 2026-10-24
- Change: `add-user-accounts`

## Context

The walking skeleton only talks to the database through Flyway and plain JDBC. The first business tables (user accounts and refresh tokens) need a persistence layer, and the choice will be reused by every later feature. MatchMap is geographic at its core: pins, countries and cities will need PostGIS geometry types and spatial queries.

## Decision

Use Spring Data JPA with Hibernate as the persistence layer.

- Flyway owns the schema. Hibernate only validates it at startup (`spring.jpa.hibernate.ddl-auto=validate`) and never creates or alters tables.
- `spring.jpa.open-in-view` is disabled, so no lazy loading happens outside service transactions.
- Spatial columns will be mapped with Hibernate Spatial when the first geographic feature arrives.

## Alternatives considered

- **Spring Data JDBC.** Simpler model with no persistence context, but no mapping for PostGIS geometry types; every spatial column would need custom converters.
- **jOOQ.** Type-safe SQL with excellent control, but it adds a code-generation step to the build, and its open-source licence only covers open-source databases.
- **Plain `JdbcTemplate`.** No extra abstraction, but every query, mapping and transaction boundary is hand-written.

## Consequences

- Entities and repositories follow a widely known model, which lowers the entry cost for contributors.
- Hibernate Spatial gives direct access to PostGIS types and functions.
- Schema drift between Flyway and the entities stops the application at startup instead of failing at runtime.
- JPA's persistence context must be handled with care (transaction boundaries, `FOR UPDATE` locks where concurrent updates matter).

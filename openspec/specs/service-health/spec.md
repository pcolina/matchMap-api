# service-health Specification

## Purpose
Lets developers, Docker Compose and future orchestrators check whether the API process is running and whether it can reach its database.

## Requirements

### Requirement: Health endpoint reports application and database status
The system SHALL expose `GET /actuator/health` without authentication. The JSON response SHALL contain an overall `status` and a `components` object with `application.status` and `database.status`, each being `UP` or `DOWN`.

#### Scenario: Application and database available
- **WHEN** a client sends `GET /actuator/health` and the database accepts connections
- **THEN** the response is HTTP 200
- **AND** `status` is `UP`
- **AND** `components.application.status` is `UP`
- **AND** `components.database.status` is `UP`

#### Scenario: Database unavailable
- **WHEN** a client sends `GET /actuator/health` and the database stopped accepting connections after the application started
- **THEN** the response is HTTP 503
- **AND** `status` is `DOWN`
- **AND** `components.application.status` is `UP`
- **AND** `components.database.status` is `DOWN`

#### Scenario: Application not running
- **WHEN** a client sends `GET /actuator/health` and the API process is not running
- **THEN** the connection is refused and no response body is returned, which the client treats as the application being down

### Requirement: Health endpoint does not expose internal details
The health response SHALL NOT include connection strings, hostnames, credentials, database product versions or exception messages.

#### Scenario: Response contains only statuses
- **WHEN** a client sends `GET /actuator/health` in any database state
- **THEN** the response body contains only `status` fields and the `components` names `application` and `database`

# Backend Local Quality Gate

## Scope

The backend quality gate is a local, reproducible command sequence. It requires
Java 21, Maven 3.6.3 or newer, and a reachable Docker daemon because the
integration tests start PostgreSQL 18 through Testcontainers.

Run from the repository root:

```bash
mvn -B -ntp -f server/pom.xml test
mvn -B -ntp -f server/pom.xml jacoco:report
```

The JaCoCo report is written to `server/target/site/jacoco/`. There is no
coverage threshold; failure to generate the report still fails the gate.

## Contract

- The complete Maven test suite must pass without skipping the Testcontainers tests.
- The JaCoCo report must be generated successfully.
- A machine without Docker cannot claim this gate passed; rerun it on a Docker-capable machine.
- The commands use only isolated test data and must never target a production database.

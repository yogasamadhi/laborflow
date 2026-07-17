# Backend CI Gate

## Scope

`.workflow/PRPipeline.yml` is the Gitee Go definition for pull requests targeting
`master`. It runs the same backend commands used locally:

```bash
mvn -B -ntp -f server/pom.xml test
mvn -B -ntp -f server/pom.xml jacoco:report
```

The job requires Java 21, Maven 3.6.3 or newer, and Docker access. Docker is
required because the backend integration tests start PostgreSQL through
Testcontainers; no application deployment, production database, credential, or
external service is involved.

The `jacoco:report` step (powered by the `jacoco-maven-plugin` declared in
`server/pom.xml`) emits a coverage report at `server/target/site/jacoco/`.
There is no enforced coverage threshold yet — the report is for visibility
only. A failing `jacoco:report` (e.g. plugin misconfiguration) is still a failed
gate.

## Enablement

The repository administrator must complete these Gitee project settings once:

1. Enable Gitee Go for the repository and create/import the pipeline from
   `.workflow/PRPipeline.yml`.
2. Confirm the pipeline uses a runner with Java 21 and access to the Docker
   daemon required by Testcontainers.
3. Open a pull request targeting `master` and confirm the `PR Backend Test`
   pipeline runs `mvn -B -ntp -f server/pom.xml test` and `jacoco:report`
   successfully.
4. Protect `master` in Gitee and require the successful `PR Backend Test`
   status before merging pull requests.

If the runner does not provide Docker, the integration tests must not be
silently skipped. Move the pipeline to a Docker-capable Gitee Go runner or
record an approved exception in the launch readiness review.

## Contract

- Every pull request into `master` runs the complete Maven test suite and
  produces a JaCoCo coverage report.
- A failed Maven command is a failed quality gate and must block merge.
- Changes to the Java version, Maven command, Testcontainers setup, or branch
  trigger require an update to this file and a successful pipeline dry run.

# Fulfilment Warehouse Management — Case Study

This repository contains the Java/Quarkus assignment implementation and written case-study responses.

## Contents

- `java-assignment/` — Quarkus REST application (Java 17+, Maven, PostgreSQL).
- `case-study/BRIEFING.md` — domain overview and warehouse replacement rules.
- `case-study/CASE_STUDY.md` — analysis and recommendations for all five cost-control scenarios.
- `java-assignment/CODE_ASSIGNMENT.md` — implementation requirements.
- `java-assignment/QUESTIONS.md` — design, API-contract, and testing answers.

## Implemented

- Location identifier resolution (case-insensitive, whitespace-tolerant).
- Store create/update/patch synchronization is scheduled after transaction completion and only runs after a successful commit.
- Warehouse list/create/retrieve/archive/replace endpoints.
- Warehouse validation for required fields, business-unit code, location, location warehouse-count and capacity limits, stock/capacity consistency, and replacement stock/capacity rules.
- Warehouse history is retained in the database through archival timestamps; the normal listing returns active warehouses.
- Tests for location resolution and fulfilment-association API constraints.
- Bonus fulfilment association feature linking products, stores, and active warehouses with all three assignment limits enforced.

## Requirements

- JDK 17+
- Docker (recommended for PostgreSQL in development/test) or a compatible PostgreSQL instance
- Network access on first Maven Wrapper run to download Maven dependencies

## Run locally

From `java-assignment/`:

```bash
./mvnw test
./mvnw quarkus:dev
```

The API is served at `http://localhost:8080`. The OpenAPI-generated Warehouse API contract is in `src/main/resources/openapi/warehouse-openapi.yaml`.

For production-profile PostgreSQL, the sample settings in `src/main/resources/application.properties` use database `quarkus_test` on `localhost:15432`. Start a local database with:

```bash
docker run --rm --name quarkus_test \
  -e POSTGRES_USER=quarkus_test \
  -e POSTGRES_PASSWORD=quarkus_test \
  -e POSTGRES_DB=quarkus_test \
  -p 15432:5432 postgres:13.3
```

## Important implementation notes

- Warehouse archival is a soft archive (`archivedAt`), preserving history rather than deleting records.
- A replacement reuses the business-unit code, archives the previous active instance, and creates a new instance in the same transaction.
- For a production-grade integration, consider a transactional outbox for reliable legacy-system synchronization. The current after-commit callback meets the assignment's ordering requirement, but a process crash after commit could still prevent delivery.
- The Maven build could not be verified in the preparation environment because Maven Central DNS/network access was unavailable. A GitHub Actions workflow runs `./mvnw --batch-mode test` on pushes and pull requests; run the same command locally before submission.

## Suggested GitHub workflow

1. Create a new repository on GitHub.
2. Unzip this project and open the `fcs-interview-code-assignment-main` folder.
3. Review the implementation and run `cd java-assignment && ./mvnw test` locally.
4. Commit and push:

```bash
git init
git add .
git commit -m "Complete fulfilment warehouse case study assignment"
git branch -M main
git remote add origin https://github.com/<your-user>/<your-repository>.git
git push -u origin main
```

Do not commit credentials, access tokens, or production data.

# Questions

## 1. Database access and manipulation strategies

The code currently mixes Panache Active Record (`Store` inherits `PanacheEntity` and calls `persist()`/`findById()` directly), a Panache repository for `Product`, and a domain port/repository adapter for `Warehouse`. I would gradually standardize around repository interfaces and application/use-case services for business operations. This keeps REST resources focused on HTTP concerns, makes business rules easier to unit-test without a database, and avoids persistence details leaking into domain logic. I would not refactor everything in one large change: preserve behavior with tests, introduce a consistent pattern for new work, and migrate existing endpoints incrementally.

For transaction boundaries, a single application service should own a business transaction. Database writes and external side effects need different reliability treatment: external calls should happen after commit, ideally through a transactional outbox with retries rather than an in-process callback when delivery is business-critical. Add database constraints for invariants such as unique active business-unit codes where possible, and use migrations rather than relying on production import scripts.

## 2. OpenAPI-generated Warehouse API versus hand-written Product and Store endpoints

OpenAPI-first development provides an explicit contract, consistent schemas and status codes, generated interfaces/clients, and a useful basis for contract testing. It helps teams coordinate independently and makes API changes reviewable. Its costs are build/code-generation complexity, generated-code constraints, and the need to keep the specification aligned with actual behavior.

Hand-written endpoints are quick to start and flexible, but can drift in naming, validation, error formats, and documentation; clients may discover incompatibilities only at runtime. For a multi-team or externally consumed API, I would prefer an OpenAPI contract as the source of truth for all three resources, with generated interfaces/clients and hand-written implementations. For a small internal prototype, hand-written endpoints can be reasonable, but I would still publish and test a contract. CI should regenerate code and run contract/integration tests so spec and implementation do not diverge.

## 3. Testing strategy under time and resource constraints

I would prioritize tests by business risk and likelihood of regression:

1. **Unit tests** for location resolution and warehouse rules: duplicate code, invalid location, warehouse-count and aggregate-capacity limits, stock exceeding capacity, archive behavior, replacement stock matching, and replacement capacity.
2. **Repository/integration tests** using the same database engine as production where practical, verifying persistence, queries, transaction rollback, and the ability to retain archived history.
3. **REST/API tests** for successful CRUD/replacement flows, status codes, malformed or incomplete requests, not-found cases, and error payloads.
4. **Store transaction tests** proving the legacy gateway is called only after commit and is not called after rollback.
5. **Contract tests** against the OpenAPI specification, plus a small end-to-end smoke test for the most important business journey.

Tests should be deterministic, isolated, and use known fixtures; avoid dependence on test execution order. Run fast unit tests on every commit and integration/contract tests in CI before merge. Track coverage as a diagnostic, but prioritize meaningful assertions on critical rules over a raw coverage percentage. Add a regression test for every production defect and review test gaps when requirements or API contracts change.

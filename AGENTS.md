# AGENTS.md — FlexMotion Cycles

Instructions for AI assistants and agents working in this repository (practice AI2).
They must stay consistent with the QA Manual (`docs/TQS26- Quality Manual report.pdf`).
When a convention changes, the team updates this file in the same pull request.

## 1. Project context

FlexMotion Cycles is the TQS (Teste e Qualidade de Software, UA) group project: an electric
bike-share platform run as **fleet-as-a-service** for the fictional city of **Vale Claro**.
A specialised operator runs the fleet under an SLA; the municipality (client) sets the rules
and follows KPIs. Two services share one platform, one account per user, one telemetry feed:

- **Conventional**: e-bikes and cargo e-bikes at docked stations. Unlock now or book for
  15 min (fixed fee); pay-as-you-go (unlock fee + per-minute rate, deposit, overtime).
- **Accessible**: wheelchair-bikes, handbikes and adapted tricycles at hubs with a technician.
  Booked for a day/time, naming the passenger and at least one support need; fixed price.

Roles: end user, operator / field technician, client manager, platform admin.
Team: David Monteiro (Team Coordinator), Lucas Reis (Product Owner),
Tomás Lopes (QA Engineer), Murilo Francisco (DevOps Master). Everyone develops.

Sources of truth (read them before guessing):
- Business rules BR1–BR24, personas, scenarios, user stories US-01..US-24:
  `docs/TQS26- Product Specs report.pdf`
- Practices, policies, AI rules: `docs/TQS26- Quality Manual report.pdf`
- Course guidelines: `docs/guidelines/` · Diagrams: `docs/architecture.jpeg`,
  `docs/umlflexmotion.drawio.png`, `docs/bicycle_lifecycle.jpeg`
- Personas, user stories and acceptance criteria: `docs/user_stories.md`
- Backlog, epics, story keys, Xray tests: Jira project **FM**
  (https://flexmotion-cycles.atlassian.net/jira/software/projects/FM/boards/1/backlog).
  Agents have **no Jira access**: get Jira data from the human (Step 0, section 6).
  Never invent keys or acceptance criteria.

## 2. Architecture

**Layered monolith organised with ports and adapters (hexagonal).**

```
Web UI (Thymeleaf, responsive) ──HTTP──► REST API (controllers, JWT + role check)
                                              │ invoke
                                              ▼
                                   Services (business rules)
                                    │ ports (interfaces)   │ repositories
                                    ▼                      ▼
                     Adapters / Fakes              PostgreSQL (JPA)
        MQTT telemetry · Stripe sandbox · OpenStreetMap · Notifications · BicycleRack
```

Rules every change must respect:
- **Controllers** (`boundary`) handle HTTP only: validation, mapping DTOs, status codes.
  No business logic; they call services.
- **Services** (`services`) hold all business rules (rental/booking, vehicle lifecycle,
  stations, users, maintenance tickets, pricing). They depend only on **ports** and
  repositories, never on a concrete external system.
- **Repositories** (`data`) are Spring Data JPA interfaces over PostgreSQL.
- **Domain** (`domain`) holds JPA entities. Entities are **never** exposed in the REST API:
  requests and responses use DTOs.
- **Ports**: Telemetry (+ vehicle commands), Payment (hold, charge, release, refund),
  Geolocation, Notification, BicycleRack. Each port has a real adapter **and** a fake for tests.
- Telemetry is event-driven: the fleet simulator (own container) publishes to RabbitMQ with
  the MQTT plugin; one `MqttTelemetryAdapter` subscribes. The simulator must be replaceable
  by changing only the adapter.
- Every vehicle state change (from a user or from telemetry) runs in one transaction with
  **optimistic locking** on the vehicle and follows the lifecycle below.
- Time-dependent rules read time from an **injected `java.time.Clock`** (never `now()`
  directly), so boundaries such as 14:59 vs 15:00 are testable.
- Money uses `BigDecimal`. No card data is stored, only sandbox tokens. Support needs are
  health data: visible only to the booking's technicians. All data is synthetic.

Vehicle lifecycle (`docs/bicycle_lifecycle.jpeg`): `AVAILABLE → RESERVED | IN_USE |
MAINTENANCE | REMOVED`; `RESERVED → IN_USE | AVAILABLE`; `IN_USE → AVAILABLE`
(maintenance after the trip if needed); `MAINTENANCE → AVAILABLE | REMOVED`; `REMOVED` is final.
Invalid transitions are rejected.

Code layout:
- Backend: `projFlexMotion/` — Spring Boot 4, Java 21, Maven wrapper.
  Base package `pt.ua.tqs.flexmotion_cycles`, organised **by technical layer**
  (`boundary`, `services`, `domain`, `data`; ports and adapters get their own packages).
- Fleet simulator: `fleet-simulator/` — separate Spring Boot 4 service (US-07), Java 21,
  Maven. Base package `pt.ua.tqs.fleet_simulator`, same layering (`domain`, `services`,
  `ports`, `config`; adapters in their own package). It reaches the platform only through
  the MQTT broker.
- Infrastructure: `docker-compose.yml` (PostgreSQL 18, RabbitMQ 4.1 + MQTT, backend,
  fleet simulator), variables in `.env.example`. CI: `.github/workflows/ci.yml`.

## 3. Build, run and test commands

```bash
cp .env.example .env                 # once, from the repository root
docker compose up -d --build         # full stack; health: http://localhost:8000/actuator/health
cd projFlexMotion && ./mvnw test     # unit tests (*Test, Surefire) + JaCoCo report
cd projFlexMotion && ./mvnw verify   # unit + integration tests (*IT, Failsafe, Testcontainers)
cd fleet-simulator && mvn verify     # fleet simulator: unit + integration tests + JaCoCo
```

Run the relevant tests before declaring any step finished and report the real result.

## 4. Language

Everything written in the repository is in **English**: code, identifiers, comments, Javadoc,
documentation, test names, Gherkin scenarios, commit messages and pull request text.
Conversation with the team may be in Portuguese; repository content may not.

## 5. Version control — forbidden for agents

- **Never** run git commands that change state: no `commit`, `push`, `pull`, `merge`,
  `rebase`, `checkout`/`switch`, `branch`, `stash`, `reset`, `tag`, `add`, etc.
- Never create pull requests, never merge, never deploy. Humans do all of it.
- The agent only **suggests** branch names and commit messages; the human runs them.

## 6. Feature workflow (every implementation request starts here)

**Step 0 — Traceability (Jira + Xray).** Before writing any code, **ask the human** and wait
for the answers. Do not continue while any of these is missing:
- the Jira story key (`FM-<n>`) and the user story it maps to (`US-xx`);
- the epic the story belongs to;
- the Xray Tests already linked to the story (keys), if any;
- authorisation for the scope: which layers/files may be changed, whether new dependencies
  may be added, and whether commands such as `docker compose` or test runs may be executed.

Then:
1. Confirm the story against `docs/user_stories.md`; flag any difference to the human.
2. Restate the acceptance criteria and the business rules (BR*) it touches.
3. Review the Xray Tests the human listed (Gherkin scenarios / generic tests). If none
   exist, propose them (Gherkin from the acceptance criteria) for the human to create in Xray.
4. Suggest the branch name for the human to create (from Jira or the CLI):
   `<type>/FM-<n>-<short-kebab-description>`, e.g. `feat/FM-12-book-ebike`.
5. Propose a plan of small steps and wait for the human's OK.

**Small steps.** Implement one small, reviewable step at a time (one layer, one rule, or one
test set). After each step stop, explain it, and let the human understand and validate it
before the next. Prefer test-first (TDD) for business rules.

**Explain every code change with Before and After.** For each file touched show:

````markdown
**File:** `projFlexMotion/src/main/java/.../BicycleService.java`
**Before:**
```java
// existing code (or "new file")
```
**After:**
```java
// new code
```
**Why:** what changed, which rule/acceptance criterion it implements, and how it is tested.
````

**Suggest a commit message after every step** (Conventional Commits), with the Jira key so
the commit appears in the story's development panel:

```
<type>(<scope>): <imperative description> [FM-<n>]
```

Types: `feat`, `fix`, `test`, `refactor`, `docs`, `style`, `perf`, `build`, `ci`, `chore`.
Examples: `test(booking): add boundary tests for 15-minute expiry [FM-12]`,
`feat(booking): expire bookings after 15 minutes [FM-12]`.
The pull request title must also contain `FM-<n>`.

## 7. Testing rules

- **Unit tests** (`*Test`, JUnit 5 + Mockito, no Spring context): mandatory for every new or
  changed business rule in the service layer. Mock repositories and ports. Cover the exact
  boundaries from the BR tables (e.g. battery 19% refused / 20% accepted).
- **Integration tests** (`*IT`, Failsafe): repositories with Testcontainers (PostgreSQL),
  REST API with `@SpringBootTest` + REST Assured, telemetry adapter against the agreed
  message format, payment adapter against Stripe test mode. Never call public services
  (e.g. OpenStreetMap) from tests: use a mocked HTTP server.
- **Acceptance (BDD)**: core stories have Gherkin scenarios (from the Jira acceptance
  criteria) automated with Cucumber. Feature files live under `src/test/resources`.
- **End-to-end**: Playwright for core UI flows; not run on every build.
- **Non-functional**: k6 load tests against the SLOs in the Product Specs report.
- **Xray traceability**: every test must be linked to its story. Put `@Requirement("FM-<n>")`
  on **each test method** (the listener ignores it on the class); link Cucumber scenarios to
  the story manually in Xray. Do not rename test packages/methods casually:
  Xray auto-provisions tests by package + method name.
- Never weaken, skip or delete a failing test to make the build pass; report it.

## 8. Code style

- Java follows the **AOSP Java code style**. Constructor injection; no field `@Autowired`.
- Validate input with Bean Validation on DTOs; return proper HTTP status codes.
- **No hardcoded values.** Business constants (fees, limits, durations) are named constants
  or configuration, never magic numbers. Environment values (URLs, hosts, ports, topics,
  credentials) come from `application.properties` / environment variables, never from code.
- Keep comments short and useful; do not comment the obvious.
- Do not add dependencies without explaining why and asking the human first.

## 9. Definition of done (per story)

Acceptance criteria met · unit tests for every rule · integration tests where persistence,
API or adapters change · Gherkin scenarios for core stories · tests linked in Xray ·
CI green and SonarQube quality gate passing · reviewed and approved by a human in a PR
(Scrum Master or DevOps Master) · documentation/API (OpenAPI) updated.

## 10. AI rules (course + team policy)

- A human is accountable for every change and must be able to explain it at a checkpoint.
  Never produce code the human cannot follow; explain instead of rushing.
- Agents never merge, release or deploy. AI review comments are suggestions only.
- Never send secrets, credentials or personal data to AI services; use synthetic data.
  Do not read or print `.env` values.
- Do not generate reports, evidence or test results without running/checking them.
- Planning and design decisions are made by the team: when in doubt, **ask**.
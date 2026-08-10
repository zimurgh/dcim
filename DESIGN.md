= DCIM

== Goals

* Re-design change / inventory workflow so staging is not a hassle
** Saving independent of validation
** Validation issues are traceable and diagnosable
* Bulk updates
* Correct inaccurate info / typos as late as possible (until apply)
* Append-only asset history with stable identities
* Change Specs for owner-firm connectivity / billing work, with CHREC traceability

== Project Layout

----
client/     Angular app
server/     Spring Boot Modulith
spec/       TLA+ models
----

== Motivation

=== Change Spec inflexibility

1) Migrations don't really work — A switch migration was originally intended to allow updating aspects of a cross connect that has billing impact or the cross connect name (latency, speed, patch panel, mdf patch number). In practice, switch migrations involve changing way more than a couple billing/naming fields — practically a new cross connect.

2) Can't amend change specs — Corrections/mistakes often surface after the window where a change spec can still be modified; once committed it was too late.

3) Firm transfers don't really work — When a firm is updated, the change does not propagate to cross connects and services owned/billed by that firm. Firms need a stable id (and transfers need an explicit change process).

=== Inability to efficiently work with data

1) No way to perform bulk updates properly — e.g. 10 cross connects needing the same extranet switch change requires editing each record.

2) Copy/paste doesn't work in a majority of places in the UI.

== Decided design

=== Asset / ledger model

Every durable asset has:

* `T_*_IDENTITY` — stable id; target of FKs across revisions
* `T_*_HISTORY` — append-only revisions
* Shared audit columns (`AuditHistory`): `VALID_FROM` / `VALID_TO`, `APPLIED_AT` / `APPLIED_BY`, `ACTION`, `STATUS`
* Current row: `VALID_TO IS NULL`

History is never rewritten. Corrections after apply are new changes.

Site spatial tree (Modulith module `site`, internal packages):

----
DataCenter → Cage → Rack → RackDevice → RackDevicePort
----

Each child FKs its parent's **identity** id.

Organization holds parties (e.g. Firm) with the same identity + history pattern.

=== Modulith modules

[cols="1,2"]
|===
| Module | Role

| `asset`
| Shared history base types and validate/apply ports (`AssetChangeValidator`, `AssetChangeApplier`)

| `organization`
| Firms and similar commercial parties

| `site`
| Whole spatial inventory tree (not one Modulith module per level)

| `workflow`
| Changes, Change Specs, CHRECs, promotion, validation orchestration, apply

| `connectivity`
| Cross-connects, market data feeds, and cables between firms and ports
|===

Workflow orchestrates; `organization` / `site` / `connectivity` implement type-specific validate/apply.
Intra-site dependency checks (e.g. terminate device ⇒ ports) use site queries plus change-spec/batch context — not async events for the guard itself.

=== Change validation

* **Save independent of validation**: create/amend Untracked or Staged always succeeds (including invalid payloads).
* **Diagnosable**: `GET /api/changes/{id}/validate` and `GET /api/change-specs/{id}/validate` return structured `ValidationIssue`s (`code`, optional `field`, `message`, related identity ids).
* **Apply is a hard gate**: staged apply (lone change or Change Spec) refuses with conflict when any issues remain; ledger is not mutated.
* **Batch-aware**: validators see sibling staged intents on the same Change Spec so terminate-parent + terminate-children in one batch can pass.
* **Apply order** on a Change Spec: dependents first, then parents.
* **Deferred**: field-level editability / RBAC (“can this user edit this field?”).
* Common issue codes include `UNKNOWN_FIELD`, `MISSING_FIELD`, `NAME_CLASH`, `VALUE_CLASH`, `ACTIVE_CHILDREN`, `ACTIVE_REFERENCES`, `STALE_BASE`, `REFERENCE_NOT_FOUND`, `REFERENCE_NOT_ACTIVE`.

Connectivity shape:

* Latency — Low Latency (LL) or Ultra Low Latency (ULL) catalog asset
* Speed — 1G or 10G catalog asset
* Charge Type — named catalog asset; optional on Cross Connect Type and Market Data Feed Type
* Cross Connect Type — named catalog asset required on every Cross Connect; optional Charge Type
* Cross Connect — required circuit id, Cross Connect Type, Latency, and Speed; optional Market Segment; owner firm + billing firm required; provider firm optional
* Market Data Feed — child of a Cross Connect (0..*); required Market Data Feed Type; same firm roles as Cross Connect
* Market Data Feed Type — named catalog asset required on every Market Data Feed; optional Charge Type
* Document — child of a Cross Connect (0..*)
* Cable — two rack-device ports; optional Cross Connect association (a Cross Connect is expected to have 1..* cables when fully provisioned)

=== When a Change Spec is required

* **Required** if work impacts the **owner** firm's **billing** or **connectivity**
* **Not required** if transparent to both
* Change Spec is scoped to the **owner** firm

=== Change lifecycle (three stages, three tables)

Universal stages: *Untracked → Staged → Committed* (promoted between tables).

Stable id: `T_CHANGE_IDENTITY` (`CHANGE_ID` never changes across promotions).

[cols="1,2"]
|===
| Table | Meaning

| `T_CHANGE_UNTRACKED`
| Early capture; JSON shape unknown / unconstrained; no known asset type

| `T_CHANGE_STAGED`
| Known `ASSET_TYPE` + schema-shaped payload; validatable; may belong to a Change Spec

| `T_CHANGE_COMMITTED`
| Applied; typed; ledger links via link table
|===

Payload bytes live separately in `T_CHANGE_PAYLOAD` (stage tables reference `CHANGE_PAYLOAD_ID`). Prefer a **new payload row on amend**.

Committed ↔ history association uses a **link table** (many asset types coming):

----
T_CHANGE_COMMITTED_HISTORY
  CHANGE_ID
  ASSET_TYPE
  HISTORY_ID
  ROLE            -- e.g. CREATED | CLOSED_PRIOR
----

Invariant: at most one open row in Untracked *or* Staged per `CHANGE_ID`; after apply, Committed (+ links) exists and open stage rows are removed.

=== Stage → asset status (per action)

Statuses are derived from action × stage (data-driven workflow lookup), e.g. Add:

[cols="1,1"]
|===
| Stage | Status

| Untracked | Draft
| Staged | Pending Add
| Committed | Active
|===

Same pattern for Update / Terminate (e.g. Pending Update, Pending Terminate → Active / Terminated).

Before commit, status describes the **change** (UI overlay). At commit, the committed status is written on the **history** row.

=== Change Specs and CHRECs

* `T_CHANGE_SPEC` — owner firm, process status, metadata
* Membership — spec ↔ `CHANGE_ID` (Staged changes for the billing path)
* `T_CHREC` — Jira issue records
* `T_CHANGE_SPEC_CHREC` — many-to-many

Rules:

* Changes may be **added to / removed from** a Change Spec until the spec is **Applied**
* **0** CHRECs allowed in Draft
* **≥1** CHREC required to enter **Pending Billing** and any later process step
* Post-apply amend is always a **new** Change (and a new/updated Change Spec when billing/connectivity rules require one)

=== Process vs official truth

Spec statuses (Draft → Pending Billing → … → **Applied**) are **process gates**, not a second inventory.

* No long-lived “frozen but uncommitted” inventory that looks official in the UI
* Amend allowed until **apply** (including under Pending Billing)
* Optional commercial checkpoint at Pending Billing = snapshot/export of **spec contents**; amend may require re-approval / re-export
* **Applied to append-only history = official** for the system

UI: ledger = current truth; open changes shown as an explicit pending overlay.

=== Apply and multi-asset work

* One Change = one primary asset intent (action + payload)
* Apply typically closes the prior history row and inserts a new one → multiple `T_CHANGE_COMMITTED_HISTORY` rows
* Multi-asset work (e.g. terminate RackDevice + its ports) = **multiple Changes** in one spec/batch
* Validation: inventory answers “what is still live?”; workflow supplies “what is in this batch?”; validators intersect them (structured, per-item errors)
* Apply order: dependents first, then parent

=== Formal methods

Focus TLA+ / property tests on: linear history per identity, apply-only ledger advances, optimistic concurrency (`baseHistoryId`), promotion invariants, Change Spec / CHREC gates, dependency coverage — not full column schemas.

=== Explicitly avoided

* Exclusive per-asset-type FK columns on `T_CHANGE_COMMITTED` (use link table)
* JSON stored inside stage tables (use `T_CHANGE_PAYLOAD`)
* Global DAG of all edits (linear history per identity; optional local depends-on only if needed)
* Dual “visible working set that is almost official” before apply

== Design Q&A

**Q: Should it be possible to modify history without going through Changes?**
A: No. The only way to advance the ledger is apply (commit) of Changes. That keeps a single apply path. Not every Change must sit on a Change Spec — only those that affect owner-firm billing or connectivity.

**Q: Should asset-specific validation live in workflow?**
A: No. Common editing/validation mechanics (`ValidationIssue`, payload helpers, apply gate, batch context) live in `asset` / `workflow`; asset-specific rules live in domain modules via `AssetChangeValidator`. Workflow is data-driven (lookup table in schema) for stage→status and process gates. Field-level edit permissions are deferred.

**Q: When are changes validated?**
A: Anytime a change is Staged (on-demand validate API). Apply always re-validates and blocks on issues. Untracked save/amend never requires validation.

**Q: When an asset record is sent to the client, what should be sent?**
A: A custom shallow DTO via a view — encode both names and ids for references.

**Q: When an asset change is sent to the server, what should be sent?**
A: Enough to identify the target and intent: stable asset id (if any), base history id (optimistic concurrency), and a JSON payload / diff. Untracked may omit typed asset shape until promotion to Staged.

== Domain catalog (assets / products)

* Firm
* Exchange (OPTIONS | EQUITIES | FUTURES)
* Market Segment (Equities Index | Agricultural Futures)
* Data Center, Cage, Rack, Rack Device, Rack Device Port
* Rack Device Type (Patch Panel | Extranet Switch | Matrix Switch | Tap)
* Rack Device Port Type
* Cross Connect, Cross Connect Type
* Latency (LL | ULL)
* Speed (1G | 10G)
* Charge Type
* Market Data Feed, Market Data Feed Type
* Document
* Cable

== Tech Stack

=== Client

* Angular 21
* ag-grid 36 Enterprise
* Spartan NG
* Tailwind CSS v4
* fast-check

=== Server

* MariaDB
* Spring Boot 4
* Java 25
* Liquibase
* Spring Modulith
* jqwik

# DCIM

Gradle multi-project:

- `client/` — Angular 21, AG Grid Enterprise 36, Spartan NG (`@spartan-ng/brain`), Tailwind CSS v4, fast-check
- `server/` — Spring Boot 4.1 Modulith, Java 25, Liquibase, MariaDB, jqwik
- `spec/` — TLA+ models

## Prerequisites

Use the project flake (or your NixOS `java` / `node` shells):

```bash
nix develop
```

## Server

```bash
./gradlew :server:test
./gradlew :server:bootRun
```

Configure MariaDB via env vars (`DCIM_DB_HOST`, `DCIM_DB_PORT`, `DCIM_DB_NAME`, `DCIM_DB_USER`, `DCIM_DB_PASSWORD`) or edit `server/src/main/resources/application.properties`.

Tests use an in-memory H2 database (`application-test.properties`).

## Client

```bash
cd client && pnpm install && pnpm start
# or
./gradlew :client:build
```

Add UI primitives with Spartan:

```bash
cd client
./node_modules/.bin/ng g @spartan-ng/cli:ui
```

Set an AG Grid Enterprise licence in `client/src/main.ts` when you have one (local runs work without it, with watermark/warnings).

## Formal methods

```bash
nix develop -c bash -lc 'pcal spec/ChangeSpec.tla && tlc -config spec/ChangeSpec.cfg spec/ChangeSpec.tla'
```

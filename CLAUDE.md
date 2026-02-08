# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

VanillaBP Business Cockpit - a BPMS-agnostic microservice providing a unified UI for business workflows and user tasks. Workflow-specific content (user task forms, status sites) is loaded dynamically from business services via Module Federation and proxied through Spring Cloud Gateway.

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.0.4 (reactive/WebFlux), Spring Cloud Gateway, Spring Data MongoDB Reactive
- **Frontend:** React 18, TypeScript 4.8, Grommet UI, styled-components
- **Database:** MongoDB with ReplicaSet (required for change streams / live UI updates)
- **Build:** Maven 3 (multimodule), NPM via frontend-maven-plugin, Node v22.22.0
- **Code generation:** OpenAPI (REST APIs), Protocol Buffers, MapStruct (Java bean mapping)

## Build Commands

### Full build (requires local Verdaccio NPM registry running)
```sh
mvn -Dnpm.registry=http://localhost:4873 package -P unpublish-npm
```

### Backend-only rebuild (skips NPM)
```sh
mvn install -Plocal-install
```

### Java-only install (skips all NPM operations)
```sh
mvn install -Pjava-install
```

### UI library continuous build (in any UI source directory)
```sh
npm start
```

### Run tests
```sh
mvn test
```

## Local Development Setup

### Prerequisites
1. Add to `/etc/hosts`: `127.0.0.1 business-cockpit-mongo`
2. Create `~/.npmrc` with:
   ```
   @vanillabp:registry=http://localhost:4873
   //localhost:4873/:_authToken="fake"
   ```
3. Start infrastructure:
   ```sh
   cd development
   docker-compose up -d
   ```
   This starts MongoDB ReplicaSet and Verdaccio (local NPM registry at http://localhost:4873).

### Running the application
```sh
# Start simulator (mimics a workflow module)
cd development/simulator
java --add-opens=java.base/java.lang=ALL-UNNAMED -jar target/simulator-*-runnable.jar

# Start business cockpit
cd container
java -Dspring.profiles.active=local -jar target/business-cockpit-*-runnable.jar
```
- Business cockpit UI: http://localhost:8080/ (user: `test`, password: `test`)
- Test data generator: http://localhost:8079/testdata/usertask/form
- MongoDB: `mongodb://business-cockpit-mongo:27017` (user: `business-cockpit`, password: `business-cockpit`, db: `business-cockpit`, replica set: `rs-business-cockpit`)

## Module Structure

```
apis/                        REST API definitions (OpenAPI + Protobuf)
  bpms-api/                  Workflow/usertask lifecycle event reporting (REST & Kafka)
  official-gui-api/          REST API consumed by the UI
  workflow-provider-api/     APIs workflow modules implement
ui/                          NPM libraries (@vanillabp/bc-*)
  bc-types/                  TypeScript type definitions
  bc-shared/                 Shared components used by both cockpit UI and workflow modules
  bc-ui/                     Components for building custom cockpit UIs
container/                   Spring Boot application + React webapp (src/main/webapp)
adapters/                    Hexagonal architecture adapters for BPMS integration
  commons/                   Shared adapter code
  camunda7/                  Camunda 7 adapter
  camunda8/                  Camunda 8 adapter
spi-for-java/                Service Provider Interface for workflow modules
commons/                     Shared Spring Boot utilities
development/                 Local dev tooling
  dev-shell-react/           React dev shell (HMR for user task forms without running cockpit)
  dev-shell-angular/         Angular dev shell
  simulator/                 Mock workflow module microservice
  docker-compose.yaml        MongoDB + Verdaccio
```

## Architecture Notes

- **Reactive style:** Backend uses Spring WebFlux (non-blocking). All Spring Data repositories are reactive.
- **Module Federation:** Workflow modules expose `UserTaskForm`, `UserTaskList`, `WorkflowPage`, `WorkflowList` as federated module parts. These are loaded dynamically by the cockpit UI from business services through the proxy.
- **Proxy pattern:** The cockpit backend proxies all requests to workflow module business services, providing a unified security umbrella (JWT-based, passed via session cookie).
- **NPM linking:** The Maven build establishes `npm link` between local packages. Changes in one UI package are immediately available to others during development.
- **MapStruct config:** Uses `unmappedTargetPolicy=ERROR` and `defaultComponentModel=spring`. All mapper issues are compile-time errors.
- **NoSQL rationale:** MongoDB is used because workflow modules report arbitrary business data for list columns, requiring flexible schema and dynamic indexing.

## Maven Profiles

| Profile | Purpose |
|---|---|
| `unpublish-npm` | Remove previously published NPM packages from Verdaccio before republishing |
| `local-install` | Rebuild and republish to local registry (skips npm update) |
| `java-install` | Skip all NPM operations entirely |
| `build-runnable-jars` | Package as executable JARs |

## Coding Conventions

- **Language:** All code comments, JavaDoc, and documentation must be written in **English**.
- **Test method names:** Use descriptive names in the format `methodName_scenario_expectedResult` (e.g., `createTask_withNullId_throwsIllegalArgumentException`).
- **Test comments:** Add a brief introductory comment before each code section within test methods to improve readability and maintainability.

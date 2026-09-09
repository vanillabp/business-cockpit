[![Apache License V.2](https://img.shields.io/badge/license-Apache%20V.2-blue.svg)](./LICENSE)

![VanillaBP](./readme/vanillabp-headline.png)

# VanillaBP Business Cockpit

The *VanillaBP Business Cockpit* is the application business people work in: it lists the user tasks
they may work on and the business cases those belong to, whichever system runs them, and it renders
each system's own forms inside its own pages. Like [VanillaBP](https://www.vanillabp.io) itself it is
[BPMS](https://en.wikipedia.org/wiki/Business_process_management#Definitions)-agnostic, so workflows
from different systems end up in one unifying user interface.

**This file is for somebody working on this repository.** What the cockpit does, how it is run, how
an application is derived from it and how a workflow module is connected to it is in the
[wiki](https://github.com/vanillabp/business-cockpit/wiki).

**Contents:**

1. [Building it](#building-it)
1. [Test coverage](#test-coverage)
1. [The modules](#the-modules)
1. [Noteworthy & Contributors](#noteworthy--contributors)
1. [License](#license)

## Building it

Java 21, Maven, and a local NPM registry, because the build publishes the user interface packages
before it consumes them. [development/README.md](./development/README.md) sets that up and is the
place to start; it also holds the MongoDB, the Kafka broker and the mail catcher the tests and a
local run want.

```sh
mvn -Dnpm.registry=http://localhost:4873 package -P unpublish-npm
```

The application runs on Spring Boot 4.1 and Java 21, on Spring MVC with virtual threads. There is no
dual build: the Spring Boot 3 code paths are gone, and applications still on Spring Boot 3.5 stay on
the 0.3.x line. The consequences for somebody deriving an application from the cockpit are in the
wiki, under [Releases](https://github.com/vanillabp/business-cockpit/wiki/Releases).

## Test coverage

Coverage is measured once per deployable and per platform, because none of them executes another's
code and a single number spanning them would hide whichever is weakest. The cockpit library, the
container and what they carry with them run in a business cockpit application. The version 1 BPMS
adapters run inside a workflow module, next to the business code. The version 2 extension runs
there too, on Spring Boot and on Quarkus, and each platform's tests reach only that platform's
glue - the difference between its two numbers is what names the features one of them never runs.

|                            | Spring Boot | Quarkus |
|----------------------------|-------------|---------|
| Business cockpit container | [![Coverage](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fvanillabp.github.io%2Fbusiness-cockpit%2Fapplication-spring-boot-report%2Findex.html&search=Total.*%3F.([0-9]%2B)[^0-9]*%3F%25&replace=%241%25&flags=m&label=Coverage&color=green&cacheSeconds=60)](https://vanillabp.github.io/business-cockpit/application-spring-boot-report) | |
| BPMS adapters (version 1)  | [![Coverage](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fvanillabp.github.io%2Fbusiness-cockpit%2Fadapters-spring-boot-report%2Findex.html&search=Total.*%3F.([0-9]%2B)[^0-9]*%3F%25&replace=%241%25&flags=m&label=Coverage&color=green&cacheSeconds=60)](https://vanillabp.github.io/business-cockpit/adapters-spring-boot-report) | |
| Extension commons          | [![Coverage](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fvanillabp.github.io%2Fbusiness-cockpit%2Fextensions-commons-spring-boot-report%2Findex.html&search=Total.*%3F.([0-9]%2B)[^0-9]*%3F%25&replace=%241%25&flags=m&label=Coverage&color=green&cacheSeconds=60)](https://vanillabp.github.io/business-cockpit/extensions-commons-spring-boot-report) | [![Coverage](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fvanillabp.github.io%2Fbusiness-cockpit%2Fextensions-commons-quarkus-report%2Findex.html&search=Total.*%3F.([0-9]%2B)[^0-9]*%3F%25&replace=%241%25&flags=m&label=Coverage&color=green&cacheSeconds=60)](https://vanillabp.github.io/business-cockpit/extensions-commons-quarkus-report) |

Each badge reads the report it links to, so the number shown here and the number in the report
cannot drift apart. Both reports are published on every build of the default branch. How they are
produced and what breaks a build is described with the
[test-coverage-report module](#the-modules).
## The modules

In the order they build on each other:

1. **[commons](./commons)**:<br>Spring Boot functionality used by the cockpit, and usable next to it:
   the JWT handling, the Kafka settings and the small utilities several modules share.
1. **[openapi-generator-fixes](./openapi-generator-fixes)**:<br>Patches applied while the API clients
   and servers below `apis` are generated.
1. **[apis](./apis)**:<br>The generated clients and servers of the three interfaces the cockpit has:
   `bpms-api` for what workflow modules report, `official-gui-api` for what the user interface reads,
   `workflow-provider-api` for what a workflow module may implement to change the cockpit's behaviour.
1. **[ui](./ui)**:<br>The NPM packages: `bc-types` for the TypeScript types, `bc-shared` for what a
   workflow module's user interface and the cockpit both use, `bc-ui` for what a cockpit user
   interface is built from.
1. **[spi-for-java](./spi-for-java)**:<br>The annotations and interfaces a workflow module's business
   code is written with to report business data about its user tasks and workflows.
1. **[extensions-commons](./extensions-commons)**:<br>The platform-neutral half of the integration
   into VanillaBP Version 2, on Spring Boot and on Quarkus. The three halves which know a BPMS live in
   their own repositories and consume this one as a published artifact:
   [businesscockpit-camunda7-adapter](https://github.com/vanillabp/businesscockpit-camunda7-adapter),
   [businesscockpit-camunda8-adapter](https://github.com/vanillabp/businesscockpit-camunda8-adapter)
   and
   [businesscockpit-process-engine-api-adapter](https://github.com/vanillabp/businesscockpit-process-engine-api-adapter).
1. **[adapters](./adapters)**:<br>The Version 1 integration, built on `io.vanillabp:spring-boot-support`.
   It is maintained for the 0.8.x line and ends with 0.9.0, and its README files are where the
   version 1 documentation stays.
   1. **[commons](./adapters/commons)**:<br>What more than one version 1 adapter needs.
   1. **[camunda7](./adapters/camunda7)**:<br>For workflow modules on [Camunda 7](http://www.camunda.org).
   1. **[camunda8](./adapters/camunda8)**:<br>For workflow modules on [Camunda 8](http://www.camunda.io).
1. **[business-cockpit](./business-cockpit)**:<br>The cockpit as a library: services, persistence, GUI
   API, security extension points, ingestion of what workflow modules report, and the React
   application. It builds no runnable jar.
1. **[container](./container)**:<br>The runnable microservice, built from the library plus a main
   class, the concrete GUI API controllers and the defaults of a standalone deployment.
1. **[development](./development)**:<br>The local development environment, and the tools which support
   developing a workflow module: the dev shells for React and Angular, the dev shell simulator and the
   simulator.
1. **[test-coverage-report](./test-coverage-report)**:<br>The aggregated coverage reports and the
   gate which judges them. A build writes each report to `report` below its own directory here, and
   the default branch publishes them as the pages the [coverage badges](#test-coverage) read. The
   thresholds are properties of the root `pom.xml`, in percent of covered instructions, which is the
   number the badges show. A build breaks when a report falls below its threshold. It also breaks
   when a module produces coverage data no aggregated report reads, because everything covered by
   that module alone would otherwise count as missed with nobody able to fix it by writing a test.
   Between the threshold and the rule of 90 the build passes, and the gate prints on every run how
   far each report still is from that rule.

## Noteworthy & Contributors

VanillaBP was developed by [Phactum](https://www.phactum.at) with the intention of giving back to the community as it has benefited the community in the past.

![Phactum](./readme/phactum.png)

## License

Copyright 2024 Phactum Softwareentwicklung GmbH

Licensed under the Apache License, Version 2.0

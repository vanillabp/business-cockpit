![VanillaBP](./readme/vanillabp-headline.png)

# VanillaBP Business Cockpit

[![Apache License V.2](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)

The *VanillaBP Business Cockpit* is the application business people work in. It lists the user tasks
they may work on and the business cases those tasks belong to, whichever system runs them, and it
renders each system's own forms inside its own pages. Like [VanillaBP](https://www.vanillabp.io)
itself it is [BPMS](https://en.wikipedia.org/wiki/Business_process_management#Definitions)-agnostic,
so workflows from different systems end up in one user interface.

**This file is for somebody working on this repository.** What the cockpit does, how it is run, how
an application is derived from it and how a workflow module is connected to it is in the
[wiki](https://github.com/vanillabp/business-cockpit/wiki).

What an application has to change when it moves to a new version of the cockpit is in
[UPGRADE.md](./UPGRADE.md).

**Contents:**

1. [Building it](#building-it)
2. [Test coverage](#test-coverage)
3. [The modules](#the-modules)
4. [Noteworthy & Contributors](#noteworthy--contributors)
5. [License](#license)

## Building it

You need Java 21 and Maven. The build is a Java build only, and it needs neither node nor npm.
[development/README.md](./development/README.md) holds the MongoDB, the Kafka broker and the mail
catcher which the tests and a local run need.

This repository holds no user interface. The jar of `business-cockpit` carries none, and neither
does the runnable jar of `container`. The React application, the npm packages and the dev shells
which used to be here are in the Git history. Decision 63 in [DECISIONS.md](./DECISIONS.md) says
why.

The build reads the snapshots of the VanillaBP platform and of the MongoDB changeset library
`com.phactum.mongodb:mongodb-changesets`. Both come from the snapshot repository of Maven Central,
`https://central.sonatype.com/repository/maven-snapshots/`. The parent
`io.vanillabp:release-parent` names that repository, and reading it needs no login.

Every push to `main` publishes the snapshot of this repository to the same snapshot repository of
Maven Central. A snapshot carries no sources jar. Only the modules which an application or an
adapter builds against carry a javadoc jar: they set the property `skip.javadoc.jar` to false. A
release carries both jars for every module it publishes. Fewer files make Maven Central answer
`429 Too Many Requests` less often. When it does, `.mvn/maven.config` makes Maven wait and repeat
that one request, and the file says how long. Decision 85 in
[`DECISIONS.md`](./DECISIONS.md) holds the decision and the list of modules.

```sh
mvn spotless:apply
mvn install
```

Every build also checks the format with Spotless, and a violation fails it. So run
`spotless:apply` before you commit.

A pull request is checked by one job. It builds and tests the whole reactor with `install`.

Every build runs javadoc over the code written by hand, and a single warning fails the build. A
public class, method or field without a comment is such a warning, and so is a class without an
explicit constructor. The code which the OpenAPI generator, protoc, MapStruct and the Quarkus
extension processor write below `target` is not checked, because nobody can add a comment there.
The javadoc jar of a release still documents it. Javadoc does not run Lombok, so it does not see a
constructor which Lombok writes. Write such a constructor out instead.

Once a night, the workflow `Chain check` runs `bin/check-the-chain.sh`. It looks at the main
branches of this repository and the three adapter repositories, and at the MongoDB changeset
library. It asks whether the four name the same Quarkus, Spring Boot and other shared versions,
whether a version stayed behind without a pull request or a reason, whether the newest run on main
was green, and whether the published snapshot belongs to the head of main. It builds nothing. A
finding becomes one issue with the label `chain-check`, and a finding which is still there the
next night is a comment on that issue. Run the script locally to get the same report. It needs
the GitHub CLI, logged in. It reads the snapshots without a login.

The chain check builds nothing. So when a new platform snapshot breaks main and nobody pushes
here, the chain check does not notice. The workflow `Nightly build` covers this. Once a night it
runs the pull-request build on main, with `--update-snapshots`, so it builds against the snapshots
published since the last run. It publishes nothing. If a publish of main is running or waiting, the
nightly build waits for it, so the two never run at the same time. A red night becomes one issue with
the label `nightly-build`, and a night which is still red is a comment on that issue. The two
adapter repositories for Camunda 7 and the Process Engine API have the same workflow.

The application runs on Spring Boot 4.1 and Java 21, on Spring MVC with virtual threads. There is no
dual build. The Spring Boot 3 code paths are gone, and applications still on Spring Boot 3.5 stay on
the 0.3.x line. The consequences for somebody deriving an application from the cockpit are in the
wiki, under [Releases](https://github.com/vanillabp/business-cockpit/wiki/Releases).

## Test coverage

Coverage is measured once per deployable and per platform. None of them runs another's code, and a
single number across them would hide the weakest one. The cockpit library, the container and what
they bring along run in a business cockpit application. The extension runs inside a workflow module,
next to the business code, on Spring Boot and on Quarkus. The tests of each platform reach only the
glue code of that platform. So the gap between the two numbers shows the features which one of the
two platforms never runs.

|                            |                                                                                                                                                                               Spring Boot                                                                                                                                                                                |                                                                                                                                                                             Quarkus                                                                                                                                                                              |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Business cockpit container | [![Coverage](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fvanillabp.github.io%2Fbusiness-cockpit%2Fapplication-spring-boot-report%2Findex.html&search=Total.*%3F.([0-9]%2B)[^0-9]*%3F%25&replace=%241%25&flags=m&label=Coverage&color=green&cacheSeconds=60)](https://vanillabp.github.io/business-cockpit/application-spring-boot-report)               |                                                                                                                                                                                                                                                                                                                                                                  |
| Extension commons          | [![Coverage](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fvanillabp.github.io%2Fbusiness-cockpit%2Fextensions-commons-spring-boot-report%2Findex.html&search=Total.*%3F.([0-9]%2B)[^0-9]*%3F%25&replace=%241%25&flags=m&label=Coverage&color=green&cacheSeconds=60)](https://vanillabp.github.io/business-cockpit/extensions-commons-spring-boot-report) | [![Coverage](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fvanillabp.github.io%2Fbusiness-cockpit%2Fextensions-commons-quarkus-report%2Findex.html&search=Total.*%3F.([0-9]%2B)[^0-9]*%3F%25&replace=%241%25&flags=m&label=Coverage&color=green&cacheSeconds=60)](https://vanillabp.github.io/business-cockpit/extensions-commons-quarkus-report) |

Each badge reads the report it links to, so the number shown here and the number in the report
cannot drift apart. All three reports are published on every build of the default branch. How they
are produced, what the gate prints on every run and what a run stopping at `package` does instead is
in [test-coverage-report](./test-coverage-report).

## The modules

In the order they build on each other:

1. **[spi-for-java](./spi-for-java)**:<br>The annotations and interfaces a workflow module's business
   code is written with to report business data about its user tasks and workflows.
2. **[support](./support)**:<br>What the cockpit and a workflow module behind it share: the SPI
   above, the check of the cockpit's login token, the user and the groups, the exceptions for a user
   task which is unknown or no longer open, and a REST client for the APIs of a BPMS or of the
   cockpit. It brings no MongoDB and no Kafka. One sub-module per platform, today only
   `spring-boot`, published as `business-cockpit-spring-boot-support`. With it as its only
   dependency, a workflow module compiles.
3. **[apis](./apis)**:<br>The generated clients and servers of the three interfaces the cockpit has:
   `bpms-api` for what workflow modules report, `official-gui-api` for what a user interface reads,
   `workflow-provider-api` for what a workflow module may implement to change the cockpit's behaviour.
4. **[extensions-commons](./extensions-commons)**:<br>The platform-neutral half of the integration
   into VanillaBP Version 2, on Spring Boot and on Quarkus. The five halves which know a BPMS live in
   their own repositories and consume this one as a published artifact:
   [business-cockpit-camunda7-adapter](https://github.com/vanillabp/business-cockpit-camunda7-adapter),
   [business-cockpit-camunda8-adapter](https://github.com/vanillabp/business-cockpit-camunda8-adapter),
   [business-cockpit-cibseven-adapter](https://github.com/vanillabp/business-cockpit-cibseven-adapter),
   [business-cockpit-operaton-adapter](https://github.com/vanillabp/business-cockpit-operaton-adapter)
   and
   [business-cockpit-process-engine-api-adapter](https://github.com/vanillabp/business-cockpit-process-engine-api-adapter).
5. **[business-cockpit](./business-cockpit)**:<br>The cockpit as a library: services, persistence, GUI
   API, security extension points and ingestion of what workflow modules report, with the MongoDB,
   Kafka and error handling helpers only the cockpit needs. It builds no runnable jar.
6. **[container](./container)**:<br>The runnable microservice, built from the library plus a main
   class, the concrete GUI API controllers and the defaults of a standalone deployment.
7. **[bom](./bom)**:<br>The BOM of a derived cockpit application. It names the Spring Boot and
   Spring Cloud BOMs the cockpit is built against, and the cockpit artifacts of the same release. An
   application imports it and names no other version of the cockpit.
8. **[development](./development)**:<br>The local development environment, and the tools which support
   developing a workflow module: the dev shell simulator and the simulator. Next to them,
   `workflow-module-on-support-only` proves in every build that a workflow module compiles with
   `support` as its only dependency.
9. **[test-coverage-report](./test-coverage-report)**:<br>The aggregated coverage reports and the
   gate which judges them. A build writes each report to `report` below its own directory here, and
   the default branch publishes them as the pages the [coverage badges](#test-coverage) read. The
   thresholds are properties of the root `pom.xml`, in percent of covered instructions, which is the
   number the badges show. A build breaks when a report falls below its threshold. It also breaks
   when a module produces coverage data no aggregated report reads. Everything covered by that
   module alone would otherwise count as missed, and nobody could fix that by writing a test.
   Between the threshold and the rule of 90 the build passes, and the gate prints on every run how
   far each report still is from that rule. The same module checks that every test class of this
   repository keeps quiet while it passes, so the log of a red build holds the output of the test
   which failed and almost nothing else.

## Noteworthy & Contributors

VanillaBP was developed by [Phactum](https://www.phactum.at) with the intention of giving back to the community as it has benefited the community in the past.

![Phactum](./readme/phactum.png)

## License

Copyright 2024 Phactum Softwareentwicklung GmbH

Licensed under the Apache License, Version 2.0

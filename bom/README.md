# BOM

The BOM (bill of materials) of the cockpit. It names the version of every artifact of a cockpit
release, and it imports the VanillaBP BOM those artifacts are built against. It names no Spring
Boot, Spring Cloud or Quarkus version. So a derived cockpit on Spring Boot imports it, and so does
a workflow module on Quarkus:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.vanillabp.businesscockpit</groupId>
      <artifactId>business-cockpit-bom</artifactId>
      <version>${business-cockpit.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

The application chooses the versions of its framework itself: `spring-boot-starter-parent` as its
parent or an import of `spring-boot-dependencies`, or `quarkus-bom`. A derived cockpit takes the
Spring Boot line the cockpit is built with, which is the version of `spring-boot-dependencies` in
the root `pom.xml`. Then it names `io.vanillabp.businesscockpit:business-cockpit`, the Spring Boot
starters and, for Kafka, `bpms-protobuf-api`, all without a version.

## What is in it

- The VanillaBP BOM.
- `mongodb-changesets`, in the version the cockpit is built with.
- The artifacts of a derived cockpit: `business-cockpit` and `bpms-protobuf-api`, and the modules
  the library brings with it, `business-cockpit-spring-boot-support`, `business-cockpit-support-core`,
  `spi-for-java`, `bpms-api-server` and `official-gui-api-server`.
- What a workflow module uses to talk to the cockpit: `business-cockpit-quarkus-support` and
  `business-cockpit-quarkus-support-test`, the two clients of the official GUI API, and the API
  of the dev shell simulator.

The cockpit's adapters are not in it. A workflow module gets them for its BPMS, and each is released
in a repository of its own.

Spring Cloud is not in it either. The library uses the Spring Cloud gateway for its proxy under
`/wm/`, and the library's POM brings the gateway in the version the cockpit is built with. An
application which uses Spring Cloud for something of its own imports `spring-cloud-dependencies`
itself, in the release train which fits its Spring Boot.

Protobuf is not pinned. Protobuf 4 generated code calls
`RuntimeVersion.validateProtobufGencodeVersion()` in every static initializer, and the class fails
to load when the runtime is older than the gencode. A newer runtime is fine. `bpms-protobuf-api`
is older than the runtime Spring Boot names, so it needs no pin. The Camunda 8 client brings
gencode which can be newer than that runtime, so the pin belongs to the Camunda 8 cockpit adapter.
Its README says per release line what an application pins.

JUnit, Lombok and MapStruct are not in it either. They are what this repository builds with, and an
application chooses its own.

## Changing it

The parent of this module is `io.vanillabp:release-parent` and not the root `pom.xml`. An import
hands on the whole effective `dependencyManagement` of a BOM, the inherited part too. So this module
cannot inherit the versions of the root `pom.xml` and writes them a second time. Change both places
in the same commit. Renovate does that on its own.

Because of the other parent, the release workflow runs `versions:set` with
`-DprocessAllModules=true`. Without it, this module would keep its SNAPSHOT version and the release
would stop.

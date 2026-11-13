# BOM

The BOM (bill of materials) of a derived cockpit application. A derived cockpit is a Spring Boot
application of its own which depends on the library `business-cockpit`. The application imports
this BOM and gets every version it needs from it:

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

Then it names `io.vanillabp.businesscockpit:business-cockpit`, the Spring Boot starters and, for
Kafka, `bpms-protobuf-api`, all without a version. Only `spring-boot-maven-plugin` still needs one,
because a plugin is not managed by a BOM. It is the version of `spring-boot-dependencies` in this
`pom.xml`.

## What is in it

- The BOMs of OpenFeign and OkHttp. The module `business-cockpit-spring-boot-support` needs newer versions than Spring Cloud
  names. Without them an application gets OpenFeign 13.6.1, which compiles and then fails at
  runtime. They come first, because the first import of a coordinate wins.
- The BOMs of Spring Boot and Spring Cloud, and the VanillaBP BOM.
- `mongodb-changesets`, in the version the cockpit is built with.
- The artifacts of a derived cockpit: `business-cockpit` and `bpms-protobuf-api`, and the modules
  the library brings with it, `business-cockpit-spring-boot-support`, `spi-for-java`, `bpms-api-server` and
  `official-gui-api-server`.

What a workflow module uses is not in it. A workflow module gets the cockpit through the cockpit's
adapter for its BPMS, which is released in a repository of its own. A workflow module may also run
on Quarkus, and the Spring BOMs would be wrong there.

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

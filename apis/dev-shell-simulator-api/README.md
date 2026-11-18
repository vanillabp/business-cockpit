# Dev shell simulator API

The paths only the [dev shell simulator](../../development/dev-shell-simulator) answers, and no
cockpit does. The OpenAPI file is
[`openapi/dev-shell-simulator-api.yaml`](./openapi/dev-shell-simulator-api.yaml). It holds two
groups of paths:

- `/simulator-api/usertask/{userTaskId}/report`, the last report of a workflow module about a user
  task, exactly as the module sent it. A test of a workflow module reads from there what the
  official API leaves out, see decision 94 in the repository's [DECISIONS.md](../../DECISIONS.md).
- `/dev-shell/user/**`, with which a dev shell lists the made-up users and switches between them.

Every path in the file is written in full, so a client takes the address of the simulator as its
base address, for example `http://localhost:8079`. The file also says which parts of the two other
APIs the simulator answers: the BPMS API at `/bpms/api/v1_1` and the official GUI API at
`/official-api/v1`.

`server` holds the Spring interfaces the simulator implements. `client` holds Spring HTTP
interfaces for a caller, published as `io.vanillabp.businesscockpit:dev-shell-simulator-api-client`.
It has no auto-configuration. A test turns an interface into a client with a `RestClient` of its
own:

```java
final var simulatorApi = HttpServiceProxyFactory
    .builderFor(RestClientAdapter.create(RestClient.create("http://localhost:8079")))
    .build()
    .createClient(SimulatorApi.class);
final Map<String, Object> report = simulatorApi.getLastReportOfUserTask(userTaskId).getBody();
```

A user task the simulator does not hold answers 404, which the `RestClient` throws as
`HttpClientErrorException.NotFound`.

Both jars carry the OpenAPI file at their root, as `dev-shell-simulator-api.yaml`. Whoever needs a
client of another kind, for example a Quarkus test or a TypeScript dev shell, generates it from
there. The `openapi-generator-maven-plugin` reads the file from the class path of the plugin:

```xml
<plugin>
  <groupId>org.openapitools</groupId>
  <artifactId>openapi-generator-maven-plugin</artifactId>
  <dependencies>
    <dependency>
      <groupId>io.vanillabp.businesscockpit</groupId>
      <artifactId>dev-shell-simulator-api-client</artifactId>
      <version>${businesscockpit.version}</version>
    </dependency>
  </dependencies>
  <executions>
    <execution>
      <goals>
        <goal>generate</goal>
      </goals>
      <configuration>
        <inputSpec>dev-shell-simulator-api.yaml</inputSpec>
        <generatorName>jaxrs-spec</generatorName>
        <!-- ... -->
      </configuration>
    </execution>
  </executions>
</plugin>
```

Built as part of the reactor build described in the [root README](../../README.md#building-it).

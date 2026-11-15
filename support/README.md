# Support

What the Business Cockpit and a workflow module behind it share. A workflow module depends on one
of the modules below, and so does the cockpit. With it as its only dependency, a workflow module
compiles.

There is one module per platform, because what a workflow module needs to check a login or to answer
a request depends on the platform it runs on:

- [spring-boot](./spring-boot), published as
  `io.vanillabp.businesscockpit:business-cockpit-spring-boot-support`. Its README says what it holds.
- `quarkus` does not exist yet. It will be added next to `spring-boot`.

This directory itself is only the parent POM, `support-parent`, which lists the modules. Nothing
builds against it.

The sections below describe a part of `support` for both platforms. Where a part exists only on one
platform so far, the section says so.

## REST clients

`support` builds REST clients out of a few properties, so an application does not write the same
client configuration again for every API it calls. The cockpit's development simulator uses it to
call the cockpit, and an application uses it to call any other REST API. The classes are in the
package `io.vanillabp.cockpit.support.rest.adapter`.

Only the Spring Boot module has them today. All classes except `ClientsConfigurationBase` know no
Spring, so the Quarkus module can reuse them later. `ClientsConfigurationBase` is the part which
needs a Quarkus counterpart.

### What `ClientsConfigurationBase` does

`ClientsConfigurationBase` is the base class of a Spring configuration which builds
[OpenFeign](https://github.com/OpenFeign/feign) clients. OpenFeign turns a Java interface into a
REST client. The requests are sent by [OkHttp](https://square.github.io/okhttp/). One call,
`configureFeignBuilder(clientClass, builder, properties)`, applies all settings of one `Client` to a
`Feign.Builder`:

- **Timeouts.** `connect-timeout` and `read-timeout` in milliseconds. Redirects are followed.
- **TLS.** The server's certificate and host name are checked by default. A truststore file adds
  the certificates the client trusts and holds the client's own certificate, if the server asks for
  one. `verify-ssl: false` turns the checks off, for tests against a server with a self-signed
  certificate.
- **Proxy.** An HTTP proxy, with or without user name and password.
- **Logging.** `log: true` logs every request and every response, body included. The lines are
  written at level `INFO`, by the logger named after the class given as `clientClass`.
- **Retry.** Off by default, so a request is sent once. Switched on, a failed request is sent again
  a few times, with a growing pause in between. OpenFeign sends a request again only after an error
  it marks as retryable, for example when no answer arrives. A `401` is retryable too if OAuth is
  configured, see below.
- **JSON.** If the application has a Jackson 3 `JsonMapper` bean, request and response bodies are
  written and read with it. Spring Boot creates one when Jackson is on the classpath. A form body
  is written as a form. Without the bean, OpenFeign keeps its own encoder and decoder, which know no
  JSON.
- **Authentication.** HTTP basic authentication with user name and password, or an OAuth bearer
  token. The token is fetched with the client credentials grant. If both are configured, basic
  authentication wins.

`configureFeignBuilder` has a second form with a fourth parameter, a `Consumer<OkHttpClient.Builder>`.
It changes the OkHttp client after the TLS settings and before the proxy and logging settings. Use
it for what the properties do not cover, for example an interceptor of your own.

Each part has a `protected` method of its own: `configureTls`, `configureProxyAndLogger`,
`configureRetry`, `configureJsonEncoding`, `configureBasicAuthentication` and
`configureOauthAuthentication`. A subclass overrides one of them to change that part only.

`getProperties(injectionPoint, propertiesInterface, properties)` is for an application with several
workflow modules, where each module has its own properties bean of the same type. It returns the
bean whose package is closest to the package of the class which asks for the client.

The package holds a few more helpers:

- `bearer.BearerTokenBasedAuthInterceptor` sends a bearer token with each request. A subclass says
  how a token is fetched. The OAuth support is such a subclass. Write your own for another kind of
  token issuer.
- `feign.OffsetDateTimeAwareExpander` writes an `OffsetDateTime` parameter in UTC with milliseconds,
  for example `2024-01-31T12:00:00.000Z`. Name it as `expander` of a Feign `@Param`.
- `versioning.ApiVersionAware` is implemented by something which knows the version of the API it
  talks to.
- `tls.TlsTruststoreUtil` builds the trust managers and key managers out of a PKCS12 file.

### The properties of a client

`Client` holds the settings of one client. A properties class of the application extends it, or
holds a field of its type, and Spring Boot binds the keys below to it. The keys are relative to the
prefix of that class, for example `inventory.base-url` for the prefix `inventory`.

|             Key             |     Default     |                                                                     Meaning                                                                     |
|-----------------------------|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `base-url`                  | `to-be-defined` | The URL all requests start with. The default is a placeholder, and a client with it counts as not configured: `isInitialized()` is `false`.     |
| `connect-timeout`           | `1500`          | How long the client waits for a connection, in milliseconds.                                                                                    |
| `read-timeout`              | `10000`         | How long the client waits for the answer once it is connected, in milliseconds.                                                                 |
| `log`                       | `false`         | Logs each request and response, body included, at level `INFO`.                                                                                 |
| `verify-ssl`                | `true`          | Checks the server's TLS certificate and host name.                                                                                              |
| `ssl-truststore-filename`   | none            | A PKCS12 file with the certificates the client trusts and the client's own certificate. Without it, the JVM's defaults apply.                   |
| `ssl-truststore-password`   | none            | The password of that file. It opens the certificates and the key of the client certificate.                                                     |
| `proxy.host`                | none            | The host of the HTTP proxy. Without it, no proxy is used.                                                                                       |
| `proxy.port`                | `0`             | The port of the proxy. Set it together with `proxy.host`.                                                                                       |
| `proxy.username`            | none            | The user name for the proxy. Without it, the client does not log in at the proxy.                                                               |
| `proxy.password`            | none            | The password for the proxy.                                                                                                                     |
| `authentication.basic`      | `false`         | Uses HTTP basic authentication. User name and password alone do not switch it on.                                                               |
| `authentication.username`   | none            | The user name for HTTP basic authentication.                                                                                                    |
| `authentication.password`   | none            | The password for HTTP basic authentication.                                                                                                     |
| `authentication.oauth.*`    | none            | Fetches an OAuth token and sends it as bearer token. See the next table.                                                                        |
| `retry.enabled`             | `false`         | Sends a failed request again. Without the `retry` keys, or with `false`, a request is sent once.                                                |
| `retry.max-attempts`        | `5`             | How often a request is sent at most, the first attempt included.                                                                                |
| `retry.period`              | `100ms`         | The pause before the first retry. A duration, for example `100ms` or `2s`.                                                                      |
| `retry.max-period`          | `1s`            | The longest pause between two attempts.                                                                                                         |
| `additional-get-parameters` | none            | Extra query parameters by name. `ClientsConfigurationBase` does not read them. They are there for an application which builds its own requests. |

`authentication.oauth` is a `Client` of its own, `OauthAuthentication`. Its `base-url` is the token
endpoint of the OAuth server. Its TLS, proxy and logging keys apply to the requests for a token.
Its timeouts and retry keys are not used: a request for a token has OkHttp's own timeouts. It adds
three keys:

|                 Key                  | Default |                                                   Meaning                                                   |
|--------------------------------------|---------|-------------------------------------------------------------------------------------------------------------|
| `authentication.oauth.base-url`      | none    | The token endpoint. OAuth is used only if it is set.                                                        |
| `authentication.oauth.client-id`     | none    | The id of the OAuth client. Building the client fails with a message if it is missing.                      |
| `authentication.oauth.client-secret` | none    | The secret of the OAuth client. Building the client fails with a message if it is missing.                  |
| `authentication.oauth.basic`         | `false` | Sends client id and secret in an HTTP basic authentication header instead of the form. Servers differ here. |

A token is fetched once and shared by all requests. It is replaced shortly before it expires: a
tenth of its lifetime before, but at most 30 seconds before. A token whose answer has no
`expires_in` is used until the server answers `401`. After a `401` the next request fetches a new
token. The request which got the `401` is sent again only if `retry.enabled` is `true`. Otherwise
the caller gets a `feign.RetryableException`. If the token endpoint refuses, the message names the
key which is most likely wrong.

### A client module brings its AutoConfiguration

An API with a Java client gets a Maven module of its own for the client, generated from the
OpenAPI file of the API. If that module also brings the Spring configuration which builds the
client, an application only adds the dependency and sets the properties. Then it injects the
interface of the API. Nobody writes the configuration a second time.

The example calls an inventory service. Its OpenAPI file is `inventory.yaml`, and the module is
`inventory-api-client`.

**1. Generate an OpenFeign interface.** The `java` generator of
[openapi-generator](https://openapi-generator.tech) with the library `feign` writes interfaces
which OpenFeign can implement. The cockpit's own [`bpms-api-client`](../apis/bpms-api/client) is
built the same way.

```xml
<plugin>
  <groupId>org.openapitools</groupId>
  <artifactId>openapi-generator-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>inventory-client</id>
      <goals>
        <goal>generate</goal>
      </goals>
      <configuration>
        <inputSpec>${project.basedir}/../inventory.yaml</inputSpec>
        <generatorName>java</generatorName>
        <library>feign</library>
        <apiPackage>com.example.inventory.api.client</apiPackage>
        <modelPackage>com.example.inventory.api.client.model</modelPackage>
        <invokerPackage>com.example.inventory.api.client</invokerPackage>
        <generateSupportingFiles>true</generateSupportingFiles>
        <!-- only the files the generated interfaces and models need to compile -->
        <supportingFilesToGenerate>ApiClient.java,ApiResponse.java,ApiResponseDecoder.java,EncodingUtils.java,StringUtil.java,RFC3339DateFormat.java,RFC3339JavaTimeModule.java,RFC3339InstantDeserializer.java,HttpBasicAuth.java,HttpBearerAuth.java,ApiKeyAuth.java</supportingFilesToGenerate>
        <configOptions>
          <useJakartaEe>true</useJakartaEe>
          <openApiNullable>false</openApiNullable>
          <useTags>true</useTags>
        </configOptions>
        <generateApiTests>false</generateApiTests>
        <generateModelTests>false</generateModelTests>
      </configuration>
    </execution>
  </executions>
</plugin>
```

The module depends on `business-cockpit-spring-boot-support`, which brings OpenFeign and OkHttp,
on `spring-boot-autoconfigure`, and on what the generated supporting files import:
`feign-jackson`, `jackson-databind` and `jackson-datatype-jsr310` of Jackson 2, and
`jakarta.annotation-api`. The generated `ApiClient` is not used, but the interfaces extend its
marker interface `ApiClient.Api`, so it has to compile. The JSON bodies are read and written with
the application's Jackson 3 `JsonMapper` anyway.

**2. Say which properties the client reads.** The class extends `Client`, so all keys of the table
above work below its prefix. A default which suits this API goes into the constructor.

```java
@ConfigurationProperties(prefix = "inventory")
public class InventoryClientProperties extends Client {

  public InventoryClientProperties() {
    // the user waits for the answer, so wait less than the default 10 seconds
    setReadTimeout(2000);
  }

}
```

**3. Build the client.** The AutoConfiguration extends `ClientsConfigurationBase`. It checks the
base URL while the application starts, so a missing value stops the start with a message which
names the key. It does not wait for the first request to fail.

```java
@AutoConfiguration
@EnableConfigurationProperties(InventoryClientProperties.class)
public class InventoryClientAutoConfiguration extends ClientsConfigurationBase {

  @Bean
  public InventoryApi inventoryApi(
      final InventoryClientProperties properties) {

    if (!properties.isInitialized()) {
      throw new IllegalStateException("""
          The inventory client has no address. Set 'inventory.base-url' to where the \
          inventory service answers.""");
    }

    final var builder = Feign.builder();
    configureFeignBuilder(InventoryApi.class, builder, properties);
    return builder.target(InventoryApi.class, properties.getBaseUrl());

  }

}
```

**4. Register it.** The file
`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
of the module names the class:

```text
com.example.inventory.api.client.InventoryClientAutoConfiguration
```

**5. Use it in the application.** The application adds the dependency, sets the properties and
injects the interface:

```xml
<dependency>
  <groupId>com.example</groupId>
  <artifactId>inventory-api-client</artifactId>
</dependency>
```

```yaml
inventory:
  base-url: https://inventory.example.com/api
  authentication:
    oauth:
      base-url: https://login.example.com/oauth2/token
      client-id: stock-service
      client-secret: ${INVENTORY_CLIENT_SECRET}
  retry:
    enabled: true
```

```java
@Service
public class StockService {

  private final InventoryApi inventory;

  public StockService(
      final InventoryApi inventory) {
    this.inventory = inventory;
  }

}
```

An answer with an error status comes as a `feign.FeignException`, for example
`FeignException.NotFound` for `404`. No answer at all comes as a `feign.RetryableException`.

The [blueprints](https://github.com/vanillabp-blueprints/blueprints) show this with complete,
tested modules, for example the clients of `module-simulator`.

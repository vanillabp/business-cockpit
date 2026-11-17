# Support

What the Business Cockpit and a workflow module behind it share. A workflow module depends on the
module for its platform, and so does the cockpit. With it as its only dependency, a workflow module
compiles.

What a workflow module needs to check a login or to answer a request depends on the platform it
runs on. Everything else is the same on both. So there are three modules:

- [core](./core), published as `io.vanillabp.businesscockpit:business-cockpit-support-core`. The
  classes which know no platform: `LoginTokenVerifier`, `DirectoryUserLookup` and
  `TokenUserDetails`, which check the cockpit's login token and find its user and groups,
  `UserDetails`, `UserDetailsImpl`, `UserDirectoryService` and `GroupHierarchyResolver` for the user
  and the groups, `BcUnauthorizedException`, `BcUnknownTaskException`, `BcTaskNotOpenException`
  and `BcTaskClaimedByAnotherUserException`, and `TlsTruststoreUtil` and `ApiVersionAware` for REST clients. Its only dependencies are Nimbus,
  for the signature of the token, and the SLF4J API. Nobody names it directly, because both
  platform modules bring it.
- [spring-boot](./spring-boot), published as
  `io.vanillabp.businesscockpit:business-cockpit-spring-boot-support`. The cockpit and a Spring Boot
  workflow module depend on it. Its README says what it holds.
- [quarkus](./quarkus), published as `io.vanillabp.businesscockpit:business-cockpit-quarkus-support`.
  A Quarkus workflow module depends on it. It checks the login token in the cockpit's cookie,
  tells the user of a request, and answers the exceptions for REST requests. The REST client
  follows. Its README says more.
- [quarkus-it](./quarkus-it), a Quarkus application with the Quarkus module as a plain dependency.
  It is not published. Its tests show that Quarkus finds everything in the jar on its own.
- [quarkus-it-without-security](./quarkus-it-without-security), the same without
  `quarkus-security`. It is not published either. Its tests show that a workflow module which has
  the support only for its exceptions builds and starts.

The classes of `core` kept their packages, `io.vanillabp.cockpit.support...`, when they moved out
of the Spring Boot module. So a package can have classes in `core` and in a platform module. Code
which used them changes no import.

This directory itself is only the parent POM, `support-parent`, which lists the modules. Nothing
builds against it.

Both platform modules check the login token with the same classes of `core`, so a token the
cockpit writes is accepted by a workflow module on either platform, and the user has the same
groups there.

The sections below describe a part of `support` for both platforms. Where a part exists only on one
platform so far, the section says so.

## REST clients

`support` builds REST clients out of a few keys, so an application does not write the same client
configuration again for every API it calls. There are two ways, and only the Spring Boot module has
them today:

- **HTTP service clients**, in the package `io.vanillabp.cockpit.support.rest.httpservice`. This is
  the way forward. A client is an interface generated from the OpenAPI file of the API, and Spring
  Framework turns it into a client. Spring Boot configures it from keys, and `support` adds what
  Spring Boot has no key for. The client of the cockpit's
  [official GUI API](../apis/official-gui-api/client) is built this way.
- **`ClientsConfigurationBase`**, in the package `io.vanillabp.cockpit.support.rest.adapter`. The
  older way, with OpenFeign and OkHttp. It stays until the clients built with it have moved, see
  [The older way](#the-older-way-clientsconfigurationbase).

### HTTP service clients

Spring Framework 7 turns an interface with `@HttpExchange` annotations into a client. The generator
`spring` of [openapi-generator](https://openapi-generator.tech) writes such an interface with the
library `spring-http-interface`. `@ImportHttpServices(group = ..., types = ...)` puts interfaces
into an HTTP service group, and each group gets a `RestClient` of its own. Spring Boot configures a
group from keys: its address, its timeouts, its TLS bundle and a few more.

`HttpServiceClientConventions` configures **every** HTTP service group of the application. Its
auto-configuration comes with `support`, so nothing has to be switched on. It adds:

- **Keys at a prefix of your choice.** All keys of a group live below one prefix, which the client
  module or the application chooses. See [Where the keys of a group live](#where-the-keys-of-a-group-live).
- **A check of `base-url` at the start.** Spring Boot would build the client without an address
  and fail at the first request.
- **A proxy**, with or without login.
- **`verify-ssl: false`**, which trusts every server, for test systems with a self-signed
  certificate.
- **Logging** of each request and its answer, bodies included, at level `INFO`.
- **Retries** of a request which got no answer, with a growing pause in between.
- **OAuth**: a bearer token of the client credentials grant, fetched by Spring Security, shared by
  all requests of the group and replaced in time.
- **A check of every key at the start**: a key below the prefix which no setting reads stops the
  start, so a typo like `read-timout` does not go unnoticed.

What Spring Boot and Spring Framework do anyway stays as it is: JSON bodies are written and read
with the application's Jackson `JsonMapper`, a form body is sent as a form, redirects are followed,
and `default-header.*` and `apiversion.*` work.

The requests are sent by the JDK's own `java.net.http.HttpClient`. No other HTTP library comes
with `support`.

#### Where the keys of a group live

The keys of a group live below its prefix. A client module, or an application for a group of its
own, names the prefix with an `HttpServiceGroupPrefix` bean:

```java
@Bean
HttpServiceGroupPrefix inventoryKeys() {
  return HttpServiceGroupPrefix.of("inventory", "inventory.client");
}
```

Then `inventory.client.base-url`, `inventory.client.read-timeout`, `inventory.client.proxy.host`
and all other keys below belong to the group `inventory`. A group without such a bean has its keys
where Spring Boot looks for them, `spring.http.serviceclient.<group>`. The keys of the table below
work in both places.

The prefix is not fixed by `support`, and it starts with no `vanillabp`. It plays the part of the
properties class a client of `ClientsConfigurationBase` binds: the module or the application says
where its client's keys are.

Timeouts for all clients at once are Spring Boot's keys `spring.http.clients.connect-timeout` and
`spring.http.clients.read-timeout`. The key of a group wins over them.

#### The keys of a group

The keys are relative to the prefix of the group, for example `inventory.client.base-url` for the
prefix `inventory.client`. The first part of the table are Spring Boot's own keys, the second part
the ones `support` adds.

|                 Key                  |                       Default                        |                                                                        Meaning                                                                         |
|--------------------------------------|------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `base-url`                           | none                                                 | The address all requests start with, for example `https://inventory.example.com/api`. Required: without it the start stops.                            |
| `connect-timeout`                    | `spring.http.clients.connect-timeout`, else `1500ms` | How long the client waits for a connection. A duration, for example `1500ms` or `2s`.                                                                  |
| `read-timeout`                       | `spring.http.clients.read-timeout`, else `10s`       | How long the client waits for the answer.                                                                                                              |
| `redirects`                          | `follow-when-possible`                               | Whether redirects are followed: `follow-when-possible`, `follow` or `dont-follow`.                                                                     |
| `ssl.bundle`                         | none                                                 | The name of a Spring Boot SSL bundle with the certificates the client trusts and its own certificate. Without it, the JVM's defaults apply.            |
| `default-header.<name>`              | none                                                 | A header sent with every request, for example `default-header.Authorization: Basic c2NvdHQ6dGlnZXI=` for HTTP basic authentication.                    |
| `apiversion.*`                       | none                                                 | Spring Boot's keys for API versions, see its reference documentation.                                                                                  |
| `cookie-handling`                    | Spring Boot's default                                | Whether the client keeps cookies between requests: `enable-when-possible`, `enable` or `disable`.                                                      |
| `verify-ssl`                         | `true`                                               | Checks the server's certificate and host name. `false` trusts every server and keeps the client certificate of `ssl.bundle`. A warning says so.        |
| `proxy.host`                         | none                                                 | The host of the HTTP proxy. Without it, no proxy is used.                                                                                              |
| `proxy.port`                         | none                                                 | The port of the proxy. Required with `proxy.host`.                                                                                                     |
| `proxy.username`                     | none                                                 | The user name for the proxy. Without it, the client does not log in at the proxy.                                                                      |
| `proxy.password`                     | empty                                                | The password for the proxy.                                                                                                                            |
| `log`                                | `false`                                              | Logs each request and its answer with headers and bodies at level `INFO`. The values of `Authorization`, `Proxy-Authorization` and cookies are hidden. |
| `retry.enabled`                      | `false`                                              | Sends a request again which got no answer. Without it, a request is sent once.                                                                         |
| `retry.max-attempts`                 | `5`                                                  | How often a request is sent at most, the first attempt included.                                                                                       |
| `retry.period`                       | `100ms`                                              | The pause before the first retry. Each further pause is one and a half times longer.                                                                   |
| `retry.max-period`                   | `1s`                                                 | The longest pause between two attempts.                                                                                                                |
| `oauth.token-uri`                    | none                                                 | The token endpoint of the OAuth server. OAuth is used only if it is set.                                                                               |
| `oauth.client-id`                    | none                                                 | The id of the OAuth client. Required with `oauth.token-uri`.                                                                                           |
| `oauth.client-secret`                | none                                                 | The secret of the OAuth client. Required with `oauth.token-uri`.                                                                                       |
| `oauth.client-authentication-method` | `client_secret_post`                                 | Where client id and secret go: `client_secret_post` in the form, `client_secret_basic` in an HTTP basic authentication header. Servers differ here.    |
| `oauth.scope`                        | none                                                 | The scopes asked for, a list. Without it, the server gives its default scope.                                                                          |
| `oauth.renew-before`                 | `30s`                                                | A token is replaced when a tenth of its lifetime is left, but at most this long before it expires.                                                     |
| `oauth.lifetime-without-expires-in`  | `1h`                                                 | How long a token is used if the token endpoint does not say (no `expires_in`). A `401` drops it earlier.                                               |

The SSL bundle is Spring Boot's: `spring.ssl.bundle.jks.<name>.truststore.*` for the certificates
to trust, `...keystore.*` for the client's own certificate. One PKCS12 file can be both, like the
truststore file of `ClientsConfigurationBase`.

An error status of an answer is not retried, because the server saw the request. The one exception
is a `401` of a group with OAuth: the token is dropped, and the next attempt fetches a new one.

The login for the proxy is sent with every request, before the proxy asks. Only Basic is
supported. The JDK's own way, an `Authenticator`, is not used, because a client with an
`Authenticator` drops every `Authorization` header the application sets, the bearer token
included.

#### OAuth

With `oauth.token-uri` a group sends a bearer token of the client credentials grant. It needs
Spring Security's OAuth client, `org.springframework.security:spring-security-oauth2-client`, which
`support` names as an optional dependency. The start says so if it is missing. The keys are the ones
of the table above. Spring Security's own registration keys,
`spring.security.oauth2.client.registration.*`, are not read, so all keys of a group stay below one
prefix.

- A token is fetched once and shared by all requests of the group, whichever user is logged in.
- A token is replaced when a tenth of its lifetime is left, but at most `oauth.renew-before` before
  it expires. So a token of an hour is replaced 30 seconds before its end, and a token of a minute 6
  seconds before.
- A token whose answer has no `expires_in` is used for `oauth.lifetime-without-expires-in`.
- After a `401` the token is dropped, and the next request fetches a new one. The request which got
  the `401` is sent again only if `retry.enabled` is `true`. Otherwise the caller gets a
  `HttpClientErrorException.Unauthorized`.
- Token requests go through the group's proxy and use its TLS settings, `ssl.bundle` and
  `verify-ssl` included.
- If the token endpoint refuses, the message names the key which is most likely wrong, for example
  `inventory.client.oauth.client-id` and `inventory.client.oauth.client-secret` for
  `invalid_client`. If it cannot be reached, the message names `oauth.token-uri` and the proxy keys.
  The caller gets an `OAuth2AuthorizationException` with that message.

#### What stops the start

The start stops with one list of every problem of every group, so one restart shows all of them.
`HttpServiceClientsMisconfiguredFailureAnalyzer` prints the list without a stack trace. Each entry
names the key to set or to change:

- a group without `base-url`, an address which is no complete `http` or `https` address, and an
  address with a placeholder nothing resolves, like `${INVENTORY_URL}`;
- a key below the prefix which no setting reads. Environment variables are not checked, because
  their names cannot be told apart from those of other programs;
- keys below `spring.http.serviceclient.<group>` for a group whose `HttpServiceGroupPrefix` names
  another prefix;
- an `HttpServiceGroupPrefix` for a group which does not exist, and two prefixes for one group;
- `proxy.host` without `proxy.port`, or proxy keys without `proxy.host`;
- `retry.max-attempts` below 1;
- an `ssl.bundle` which names no bundle;
- `oauth.token-uri` without `oauth.client-id` or `oauth.client-secret`, an unknown
  `oauth.client-authentication-method`, and a missing Spring Security OAuth client.

#### What a caller gets

An answer with an error status comes as Spring's `HttpClientErrorException` or
`HttpServerErrorException`, with a subclass per status, for example
`HttpClientErrorException.NotFound` for `404`. No answer at all comes as `ResourceAccessException`.
These are the types of every `RestClient`. A caller which moves from `ClientsConfigurationBase`
catches them instead of Feign's `FeignException` and `RetryableException`.

#### Changing more

Three kinds of Spring beans change what the keys do not cover, and all three run after the
conventions:

- a `RestClientHttpServiceGroupConfigurer` changes chosen groups: interceptors, headers, the
  handling of an error status, the request factory;
- a `RestClientCustomizer` changes every `RestClient` of the application;
- a `ClientHttpRequestFactoryBuilderCustomizer` changes the JDK's `HttpClient.Builder` of every
  client.

A proxy needs the JDK's `HttpClient`. An application which sends with another library, for
example Apache HttpClient through `spring.http.clients.imperative.factory`, gets a message at the
start for a group with a proxy.

### A client module brings its AutoConfiguration

An API with a Java client gets a Maven module of its own for the client, generated from the
OpenAPI file of the API. If that module also brings the auto-configuration which imports the
client, an application only adds the dependency and sets the keys. Then it injects the interface.
Nobody writes the configuration a second time.

The example calls an inventory service. Its OpenAPI file is `inventory.yaml`, and the module is
`inventory-api-client`. The tests of the package `rest.httpservice` in `support` use the same
example.

**1. Generate the interface.** The generator `spring` with the library `spring-http-interface`
writes an interface with `@HttpExchange` annotations and the models, nothing else:

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
        <generatorName>spring</generatorName>
        <library>spring-http-interface</library>
        <apiPackage>com.example.inventory.api.client</apiPackage>
        <modelPackage>com.example.inventory.api.client.model</modelPackage>
        <configOptions>
          <useSpringBoot4>true</useSpringBoot4>
          <useJakartaEe>true</useJakartaEe>
          <useTags>true</useTags>
          <interfaceOnly>true</interfaceOnly>
          <useBeanValidation>false</useBeanValidation>
          <openApiNullable>false</openApiNullable>
          <useJspecify>true</useJspecify>
        </configOptions>
        <generateApiTests>false</generateApiTests>
        <generateModelTests>false</generateModelTests>
      </configuration>
    </execution>
  </executions>
</plugin>
```

The module depends on `business-cockpit-spring-boot-support`, which brings Spring's HTTP service
clients and the conventions, on `spring-boot-starter-jackson` for the JSON bodies, and on what
the generated sources import: `jakarta.annotation-api` and `jspecify`. The cockpit's
[official GUI API client](../apis/official-gui-api/client/spring-boot/pom.xml) is a complete
example.

**2. Give the client a type of its own.** An empty sub-interface of the generated one:

```java
public interface InventoryClient extends InventoryApi {
}
```

Spring could import `InventoryApi` itself. The sub-interface gives the module a bean type of its
own, so a workflow module which imports `InventoryApi` into a group of its own gets no second bean
of its type.

**3. Import it into a group, with its prefix.**

```java
@AutoConfiguration(before = HttpServiceClientAutoConfiguration.class)
@ConditionalOnMissingBean(InventoryApi.class)
@ImportHttpServices(group = InventoryClientAutoConfiguration.GROUP, types = InventoryClient.class)
public class InventoryClientAutoConfiguration {

  public static final String GROUP = "inventory";

  @Bean
  HttpServiceGroupPrefix inventoryClientKeys() {
    return HttpServiceGroupPrefix.of(GROUP, "inventory.client");
  }

}
```

`before = HttpServiceClientAutoConfiguration.class` lets Spring Boot see the group: its HTTP
service client configuration only starts when a group exists. `@ConditionalOnMissingBean` lets the
module step back when the application imports the API into groups of its own, see step 6. The
`base-url` is checked at the start, so the module writes no check of its own.

**4. Register it.** The file
`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
of the module names the class:

```text
com.example.inventory.api.client.InventoryClientAutoConfiguration
```

**5. Use it in the application.** The application adds the dependency, sets the keys and injects
the interface:

```xml
<dependency>
  <groupId>com.example</groupId>
  <artifactId>inventory-api-client</artifactId>
</dependency>
```

```yaml
inventory:
  client:
    base-url: https://inventory.example.com/api
    read-timeout: 2s
    retry:
      enabled: true
    oauth:
      token-uri: https://login.example.com/oauth2/token
      client-id: stock-service
      client-secret: ${INVENTORY_CLIENT_SECRET}
```

```java
@Service
public class StockService {

  private final InventoryClient inventory;

  public StockService(
      final InventoryClient inventory) {
    this.inventory = inventory;
  }

}
```

**6. Two workflow modules with different keys.** Two workflow modules of one application may call
the same service for different parts of it: one module needs an answer within 300 milliseconds, the
other waits three seconds. Each module imports the generated interface into a group of its own,
with a prefix of its own:

```java
// in the orders module
interface OrdersInventory extends InventoryApi {
}

@Configuration
@ImportHttpServices(group = "orders-inventory", types = OrdersInventory.class)
class OrdersInventoryClient {

  @Bean
  HttpServiceGroupPrefix ordersInventoryKeys() {
    return HttpServiceGroupPrefix.of("orders-inventory", "orders.inventory");
  }

}

// in the billing module
interface BillingInventory extends InventoryApi {
}

@Configuration
@ImportHttpServices(group = "billing-inventory", types = BillingInventory.class)
class BillingInventoryClient {

  @Bean
  HttpServiceGroupPrefix billingInventoryKeys() {
    return HttpServiceGroupPrefix.of("billing-inventory", "billing.inventory");
  }

}
```

```yaml
orders:
  inventory:
    base-url: https://inventory.example.com/api
    read-timeout: 300ms
billing:
  inventory:
    base-url: https://inventory.example.com/api
    read-timeout: 3s
```

The orders module injects `OrdersInventory`, the billing module `BillingInventory`. The group of
the client module steps back, because the application has beans of `InventoryApi` now. Each module
keeps its keys below its own prefix. Values the two share are the application's business: it can
write them twice, or bring them in a library of its own. `TwoModulesTest` in `support` shows this
with two timeouts.

### The older way: `ClientsConfigurationBase`

The classes of the package `io.vanillabp.cockpit.support.rest.adapter` build OpenFeign clients.
They stay until the clients built with them have moved to HTTP service clients. New clients use the
way above.

`TlsTruststoreUtil` and `ApiVersionAware` know no platform and are in `core`. The rest needs Spring:
`ClientsConfigurationBase` uses Spring's bean factory, and `BearerTokenBasedAuthInterceptor` and
`OauthBearerTokenHandler` build on it, the first as its subclass and the second through
`configureOkHttpClient`. The properties classes `Client`, `Authentication`, `OauthAuthentication`,
`Proxy` and `Retry` and the Feign helper `OffsetDateTimeAwareExpander` know no Spring. They stay in
the Spring Boot module anyway, because they describe what `ClientsConfigurationBase` reads.

To move a client, see the section about HTTP service clients in the repository's
[UPGRADE.md](../UPGRADE.md). The keys keep their meaning, and most keep their names.

#### What `ClientsConfigurationBase` does

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

#### The properties of a client

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

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
  and `BcTaskClaimedByAnotherUserException`. Its only dependencies are Nimbus,
  for the signature of the token, and the SLF4J API. Nobody names it directly, because both
  platform modules bring it.
- [spring-boot](./spring-boot), published as
  `io.vanillabp.businesscockpit:business-cockpit-spring-boot-support`. The cockpit and a Spring Boot
  workflow module depend on it. Its README says what it holds.
- [quarkus](./quarkus), published as `io.vanillabp.businesscockpit:business-cockpit-quarkus-support`.
  A Quarkus workflow module depends on it. It checks the login token in the cockpit's cookie,
  tells the user of a request, answers the exceptions for REST requests, and builds REST clients
  from keys. Its README says more.
- [quarkus-test](./quarkus-test), published as
  `io.vanillabp.businesscockpit:business-cockpit-quarkus-support-test`. `TestLogin`, which logs a
  user in for the tests of a Quarkus workflow module, so its permission checkers apply.
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

## The user interface from an nginx

The cockpit loads the user interface of a workflow module from `/wm/<id>/bc-manifest.json` and
forwards that to `/<id>/bc-manifest.json` of the application, and the same for every file the
manifest names. Usually those files are in the UI jar, a dependency of the application, and Spring
Boot and Quarkus serve them from `META-INF/resources/`. If the user interface runs in an nginx of its
own instead, with a release cycle of its own, the application passes those requests on. `support`
brings the route for it on both platforms. The workflow module needs no class and no key for it.
The application sets the address of the nginx:

```properties
loan-approval.user-interface.address=http://nginx:8080
```

The key is below the id of the workflow module, the content of its file `META-INF/workflow-module`,
so each module of an application gets an address of its own. As an environment variable it is
`LOAN_APPROVAL_USER_INTERFACE_ADDRESS`. Only the scheme, the host and the port count. The path of a
request stays as it is, so the nginx serves the files below `/<id>/` as well.

The route takes every GET below `/<id>/` and passes it on with its path and its query. The nginx
gets the `Host` of the address, and the browser gets its answer as it is, `404` included, but
without the headers which hold for the connection to the nginx only: `Connection`, `Keep-Alive`,
`Proxy-Connection`, `Proxy-Authenticate`, `TE`, `Trailer`, `Transfer-Encoding`, `Upgrade` and every
header `Connection` names. Such a header makes an answer over HTTP/2 malformed. Spring Cloud Gateway
leaves them out on its own, on Quarkus the route does it. The route leaves out three kinds of paths,
which the application answers itself:

- the API of the module below `/<id>/api/`;
- a path with a dot segment, `.` or `..`, or with an encoded dot, slash or backslash, which could
  leave `/<id>/` on the nginx;
- every other method than GET.

The route runs behind the security of the application: a path which needs a login needs it for the
files of the nginx as well. With the address, the nginx answers even if the UI jar is there too.
Without it, there is no route and no library is needed.

The application adds the library which passes a request on:

|  Platform   |                                                                 Dependency                                                                 |
|-------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| Spring Boot | `org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc`, with the BOM `org.springframework.cloud:spring-cloud-dependencies` |
| Quarkus     | `io.vertx:vertx-http-proxy`, whose version the Quarkus BOM names                                                                           |

On Spring Boot the route is a bean of Spring Cloud Gateway named `<id>-user-interface`. A workflow
module which still brings a bean of that name, its own route from before, keeps it, and the log
says that the route of `support` is left out. On Quarkus the route is a route of Vert.x. It comes
after the security of Quarkus, so `quarkus.http.auth.permission.*` covers it, and before Quarkus
REST and the files of the jars.

Spring Cloud Gateway changes one default of every HTTP client of the application: unless the
application sets `spring.http.clients.redirects` itself, the gateway sets it to `dont-follow`, and
it hands a request factory built with that value to every `RestClient`. The HTTP service groups of
`support` are not affected: they follow redirects unless their own `redirects` key says otherwise,
and they keep the request factory their keys build, see [Changing more](#changing-more). The
application's other clients, the ones it builds itself, get the default of the gateway. The routes
of the gateway pass a redirect on to the browser as it is.

The client which sends the requests of the gateway comes from `support` as well, the bean
`gatewayClientWhichChangesNothing` of `GatewayClientAutoConfiguration`. It is the client of every
route of the gateway in the application, also of a route the application writes itself, and of the
proxy of the workflow modules in a cockpit. It passes a compressed answer on as it is, with its
`Content-Encoding`. Spring's own clients unpack such an answer and drop the header. It speaks
HTTP/1.1, because the client of the JDK otherwise asks for an upgrade to HTTP/2 (`Upgrade: h2c`),
and the development server of Vite does not answer that. The keys `spring.http.clients.*` hold for
it as well. An application which needs another client writes a bean of the type `ProxyExchange`,
and the one of `support` steps back.

The start ends, with one message for all of them, if

- an address is no address, has no `http` or `https`, holds a user, or has a path, a query or a
  fragment;
- a key `<id>.user-interface.address` names a workflow module the application does not have,
  with the ids it has;
- an address is set, but the library is missing.

`UserInterfaceRoutes` of [core](./core) does the checks and `UserInterfaceRoute` decides which
paths the route takes, so both platforms take the same requests and say the same words.

## REST clients

`support` builds REST clients out of a few keys, so an application does not write the same client
configuration again for every API it calls. On Spring Boot these are the
[HTTP service clients](#http-service-clients) of the package
`io.vanillabp.cockpit.support.rest.httpservice`. A client is an interface generated from the
OpenAPI file of the API, and Spring Framework turns it into a client. Spring Boot configures it from
keys, and `support` adds what Spring Boot has no key for. The client of the cockpit's
[official GUI API](../apis/official-gui-api/client) is built this way. On Quarkus it is
[`RestClients`](#rest-clients-on-quarkus), with the same keys.

Neither brings an HTTP library of its own. OpenFeign and OkHttp, which the REST client of version 1
was built on, are gone. A workflow module which still wants them names them in its own POM.

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
- **The path of an API.** A client module may declare the path of its API, for example
  `/gui/api/v1`, as an `HttpServiceApiPath` bean with the API's interfaces. Every group with one of
  them gets the path added to its `base-url`, so `base-url` is the address of the server only, and
  a client of a new version of the API brings its new path along. The clients of the cockpit's
  official GUI API do so, with the server path of the OpenAPI file. A `base-url` which ends with the
  path stops the start, because each call would hold it twice.
- **A proxy**, with or without login.
- **`verify-ssl: false`**, which trusts every server, for test systems with a self-signed
  certificate.
- **Logging** of each request and its answer, bodies included, at level `INFO`.
- **Retries** of a request which got no answer, with a growing pause in between.
- **The size of the connection pool**, for an application which sends with Apache HttpClient 5.
- **OAuth**: a bearer token of the client credentials grant, fetched by Spring Security, shared by
  all requests of the group and replaced in time.
- **A check of every key at the start**: a key below the prefix which no setting reads stops the
  start, so a typo like `read-timout` does not go unnoticed.

What Spring Boot and Spring Framework do anyway stays as it is: JSON bodies are written and read
with the application's Jackson `JsonMapper`, an answer of an operation which returns a `String`,
for example one with `text/plain`, is read as text, a form body is sent as a form, redirects are
followed, and `default-header.*` and `apiversion.*` work. A client module writes no code of its own
for any of them.

The requests are sent by the JDK's own `java.net.http.HttpClient`. No other HTTP library comes
with `support`. If the application has Apache HttpClient 5 on its class path, for example through
the client of Camunda 8, Spring Boot sends with that library instead. It opens five connections to
a server at the same time and lets every further request wait. A group which sends many requests
at once sets `connection-pool-size`.

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
properties class a client of version 1 bound: the module or the application says where its
client's keys are.

Timeouts for all clients at once are Spring Boot's keys `spring.http.clients.connect-timeout` and
`spring.http.clients.read-timeout`. The key of a group wins over them.

#### A group which only some applications use

Every group which is there needs its `base-url`. A module which calls a service only if the
application wants it imports its group only if the application sets the address. It puts
`@ConditionalOnHttpServiceGroupAddress` on the class which imports the group, with the prefix of
the group:

```java
@Configuration
@ConditionalOnHttpServiceGroupAddress(prefix = "orders.inventory")
@ImportHttpServices(group = "orders-inventory", types = OrdersInventory.class)
class OrdersInventoryClient {

  @Bean
  HttpServiceGroupPrefix ordersInventoryKeys() {
    return HttpServiceGroupPrefix.of("orders-inventory", "orders.inventory");
  }

}
```

Without the address there is no group and no client, and the application starts. The class which
calls the service takes an `ObjectProvider<OrdersInventory>` and skips the call if
`getIfAvailable()` answers `null`. With the address the group is checked like every other group,
so a broken address or a key nobody reads still stops the start. The condition only asks whether
`<prefix>.base-url` is set. Spring Boot's `@ConditionalOnProperty` would read the value and stop
the start at a placeholder nothing resolves, like `${INVENTORY_URL}`, with a message which does not
name the group. The condition names the prefix itself, because it is decided before any bean
exists, and so before the `HttpServiceGroupPrefix` bean. A client module does the same for its own
group, see [A client module brings its AutoConfiguration](#a-client-module-brings-its-autoconfiguration).
`AnOptionalGroupTest` shows both.

#### The keys of a group

The keys are relative to the prefix of the group, for example `inventory.client.base-url` for the
prefix `inventory.client`. The first part of the table are Spring Boot's own keys, the second part
the ones `support` adds.

|                 Key                  |                       Default                        |                                                                                            Meaning                                                                                             |
|--------------------------------------|------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `base-url`                           | none                                                 | The address all requests start with, for example `https://inventory.example.com/api`. Required: without it the start stops.                                                                    |
| `connect-timeout`                    | `spring.http.clients.connect-timeout`, else `1500ms` | How long the client waits for a connection. A duration, for example `1500ms` or `2s`.                                                                                                          |
| `read-timeout`                       | `spring.http.clients.read-timeout`, else `10s`       | How long the client waits for the answer.                                                                                                                                                      |
| `redirects`                          | `follow`                                             | Whether redirects are followed: `follow-when-possible`, `follow` or `dont-follow`. `spring.http.clients.redirects` does not apply.                                                             |
| `ssl.bundle`                         | none                                                 | The name of a Spring Boot SSL bundle with the certificates the client trusts and its own certificate. Without it, the JVM's defaults apply.                                                    |
| `default-header.<name>`              | none                                                 | A header sent with every request, for example `default-header.Authorization: Basic c2NvdHQ6dGlnZXI=` for HTTP basic authentication.                                                            |
| `apiversion.*`                       | none                                                 | Spring Boot's keys for API versions, see its reference documentation.                                                                                                                          |
| `cookie-handling`                    | Spring Boot's default                                | Whether the client keeps cookies between requests: `enable-when-possible`, `enable` or `disable`.                                                                                              |
| `verify-ssl`                         | `true`                                               | Checks the server's certificate and host name. `false` trusts every server and keeps the client certificate of `ssl.bundle`. A warning says so.                                                |
| `proxy.host`                         | none                                                 | The host of the HTTP proxy. Without it, no proxy is used.                                                                                                                                      |
| `proxy.port`                         | none                                                 | The port of the proxy. Required with `proxy.host`.                                                                                                                                             |
| `proxy.username`                     | none                                                 | The user name for the proxy. Without it, the client does not log in at the proxy.                                                                                                              |
| `proxy.password`                     | empty                                                | The password for the proxy.                                                                                                                                                                    |
| `log`                                | `false`                                              | Logs each request and its answer with headers and bodies at level `INFO`. The values of `Authorization`, `Proxy-Authorization` and cookies are hidden.                                         |
| `connection-pool-size`               | the HTTP library's                                   | How many connections to the server the group opens at most at the same time. Apache HttpClient 5 opens five without it. The JDK's `HttpClient` has no limit, so the key changes nothing there. |
| `retry.enabled`                      | `false`                                              | Sends a request again which got no answer. Without it, a request is sent once.                                                                                                                 |
| `retry.max-attempts`                 | `5`                                                  | How often a request is sent at most, the first attempt included.                                                                                                                               |
| `retry.period`                       | `100ms`                                              | The pause before the first retry. Each further pause is one and a half times longer.                                                                                                           |
| `retry.max-period`                   | `1s`                                                 | The longest pause between two attempts.                                                                                                                                                        |
| `oauth.token-uri`                    | none                                                 | The token endpoint of the OAuth server. OAuth is used only if it is set.                                                                                                                       |
| `oauth.client-id`                    | none                                                 | The id of the OAuth client. Required with `oauth.token-uri`.                                                                                                                                   |
| `oauth.client-secret`                | none                                                 | The secret of the OAuth client. Required with `oauth.token-uri`.                                                                                                                               |
| `oauth.client-authentication-method` | `client_secret_post`                                 | Where client id and secret go: `client_secret_post` in the form, `client_secret_basic` in an HTTP basic authentication header. Servers differ here.                                            |
| `oauth.scope`                        | none                                                 | The scopes asked for, a list. Without it, the server gives its default scope.                                                                                                                  |
| `oauth.renew-before`                 | `30s`                                                | A token is replaced when a tenth of its lifetime is left, but at most this long before it expires.                                                                                             |
| `oauth.lifetime-without-expires-in`  | `1h`                                                 | How long a token is used if the token endpoint does not say (no `expires_in`). A `401` drops it earlier.                                                                                       |

The SSL bundle is Spring Boot's: `spring.ssl.bundle.jks.<name>.truststore.*` for the certificates
to trust, `...keystore.*` for the client's own certificate. One PKCS12 file can be both, like the
truststore file of version 1.

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

- a group without `base-url`, an address which is no complete `http` or `https` address, an
  address with a placeholder nothing resolves, like `${INVENTORY_URL}`, and an address which ends
  with the path of the API of the group, see `HttpServiceApiPath` above, and interfaces of two
  APIs with different paths in one group. A group which only some applications use is not there without its address, see
  [A group which only some applications use](#a-group-which-only-some-applications-use);
- a key below the prefix which no setting reads. Environment variables are not checked, because
  their names cannot be told apart from those of other programs;
- keys below `spring.http.serviceclient.<group>` for a group whose `HttpServiceGroupPrefix` names
  another prefix;
- an `HttpServiceGroupPrefix` for a group which does not exist, and two prefixes for one group;
- `proxy.host` without `proxy.port`, or proxy keys without `proxy.host`;
- `retry.max-attempts` below 1;
- `connection-pool-size` below 1, and `connection-pool-size` while the application sends with an
  HTTP library other than the JDK's `HttpClient` and Apache HttpClient 5;
- an `ssl.bundle` which names no bundle;
- `oauth.token-uri` without `oauth.client-id` or `oauth.client-secret`, an unknown
  `oauth.client-authentication-method`, and a missing Spring Security OAuth client.

#### What a caller gets

An answer with an error status comes as Spring's `HttpClientErrorException` or
`HttpServerErrorException`, with a subclass per status, for example
`HttpClientErrorException.NotFound` for `404`. No answer at all comes as `ResourceAccessException`.
These are the types of every `RestClient`. A caller which moves from a client of version 1
catches them instead of Feign's `FeignException` and `RetryableException`.

#### Changing more

Three kinds of Spring beans change what the keys do not cover, and all three run after the
conventions:

- a `RestClientHttpServiceGroupConfigurer` changes chosen groups: interceptors, headers, the
  handling of an error status, the request factory;
- a `RestClientCustomizer` changes every `RestClient` of the application, but not the request
  factory of a group: `RequestFactoryAfterTheCustomizers` puts the factory the keys built back
  right after the customizers, because Spring Cloud Gateway sets a factory of its own on every
  client. A `RestClientHttpServiceGroupConfigurer` still changes it;
- a `ClientHttpRequestFactoryBuilderCustomizer` changes the JDK's `HttpClient.Builder` of every
  client.

A proxy needs the JDK's `HttpClient`. An application which sends with another library, for
example Apache HttpClient through `spring.http.clients.imperative.factory`, gets a message at the
start for a group with a proxy.

### REST clients on Quarkus

The Quarkus module builds clients of Quarkus' REST client with
`io.vanillabp.cockpit.support.rest.restclient.RestClients`. A client is an interface the generator
`jaxrs-spec` writes from the OpenAPI file of the API, as the Quarkus blueprints generate theirs.
The keys are the ones of the HTTP service clients above, with the same names and defaults, so a
workflow module moves between the platforms without renaming a key. They live at a prefix the
workflow module chooses, never below `vanillabp`, `quarkus` or `business-cockpit`.

A workflow module produces each client it needs while the application starts, so a missing key
stops the start and not the first call:

```java
@ApplicationScoped
public class LoanApprovalClients {

  @Produces
  @Singleton
  @Startup
  @LoanApproval
  InventoryApi inventory(
      final RestClients restClients) {
    return restClients.build(InventoryApi.class, "loan-approval.inventory");
  }

}
```

```properties
loan-approval.inventory.base-url=https://inventory.example.com/api
loan-approval.inventory.read-timeout=2s
loan-approval.inventory.oauth.token-uri=https://login.example.com/oauth/token
loan-approval.inventory.oauth.client-id=loan-approval
loan-approval.inventory.oauth.client-secret=${INVENTORY_CLIENT_SECRET}
```

Two workflow modules of one application may call the same service through the same interface with
different keys: each builds its client at a prefix of its own and tells its bean apart by a
qualifier, like `@LoanApproval` above. All clients built at one prefix share its settings and its
OAuth token.

The application adds the extension `quarkus-rest-client-jackson`. `support` names Quarkus' REST
client as an optional dependency only, see the README of the Quarkus module.

What differs from Spring Boot, because the platforms differ:

- `tls-configuration-name` names a configuration of the Quarkus TLS registry,
  `quarkus.tls.<name>.*`, which holds the certificates the client trusts and its own. It takes the
  place of `ssl.bundle`. `verify-ssl: false` trusts every server here as well.
- `cookie-handling` and `apiversion.*` do not exist. A key nobody reads stops the start, and for
  these and `ssl.bundle` the message says what to do instead.
- The timeouts have no default for all clients, like `spring.http.clients.*`. Without the keys
  they are `1500ms` and `10s`.
- Without `connection-pool-size` the clients at a prefix open up to 50 connections at the same
  time, the default of Quarkus. Further requests wait for a free connection.
- Only a method which waits for its answer is retried. A method which returns a `Uni` or a
  `CompletionStage` is called once.
- An answer with an error status comes as the REST client's `WebApplicationException`, no answer at
  all as `ProcessingException`. A token request which fails comes as `TokenRequestFailed`, a
  `ProcessingException` whose message names the key to check, and it is never retried.
- `RestClients.build(api, prefix, builder -> ...)` hands the builder to the caller after the keys
  are applied, for example to register a filter of its own.
- The path of an API is a `RestClientApiPath` bean, which a client module produces with
  `@Unremovable`, because only `RestClients` looks it up. The Quarkus client of the cockpit's
  official GUI API does so. A `base-url` which ends with the path stops the start, as on Spring
  Boot.
- A class which is no bean, a test for example, creates the builder with `new RestClients()`. It
  reads the keys of the running application all the same.

The keys are read one by one and not by a `@ConfigMapping`, because a mapping would refuse an
unknown key before any guiding message. Every problem of a prefix ends in one
`RestClientsMisconfigured` which lists them all: the problems of the list in
[What stops the start](#what-stops-the-start) which apply here, a `tls-configuration-name` which
names no configuration, and a key nobody reads with the key that was probably meant.

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
@ConditionalOnHttpServiceGroupAddress(prefix = "inventory.client")
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
service client configuration only starts when a group exists.
`@ConditionalOnHttpServiceGroupAddress` imports the group only if the application sets its
address, so the dependency alone asks for no key, in a test for example.
`@ConditionalOnMissingBean` lets the module step back when the application imports the API into
groups of its own, see step 6. An address which is set is checked at the start, so the module
writes no check of its own.

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

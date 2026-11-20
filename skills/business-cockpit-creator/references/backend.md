# The backend repository

*Part of the description in [`../SKILL.md`](../SKILL.md).*

The cockpit backend is a Spring Boot application of the organisation. It depends on the library
`io.vanillabp.businesscockpit:business-cockpit`, which brings the services, the persistence, the
GUI API, the proxy to the workflow modules, the BPMS API the workflow modules report to, and the
notifications. The library has no main class, no login and no list controllers. Only the
application knows who may log in and who sees what, so the application writes those parts.

When the user wants it (`Q-BACKEND`), the skill creates this repository from the interview. It
writes what the library leaves to the application, and nothing more. When the organisation keeps a
backend of its own, the skill does not touch it. It tells the user which dependency to add, see
[project.md](project.md#the-backend-repository).

Read the `container` module of the `business-cockpit` repository at the cockpit version of the
interview (`Q-COCKPIT-VERSION`) before you generate. It is the runnable application of the cockpit,
and it is the worked example of everything below. The wiki says the same for a reader:
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit)
and [Security](https://github.com/vanillabp/business-cockpit/wiki/Security). The test application
`container/src/test/java/io/vanillabp/derived/cockpit/DerivedCockpitApplication.java` is the
shortest complete application there is: it lives outside the packages of the library, so nothing
of the library reaches it by the component scan, exactly like the application you generate.

1. [Spring Boot only](#spring-boot-only)
2. [The layout](#the-layout)
3. [The parent pom](#the-parent-pom)
4. [With Gradle](#with-gradle)
5. [The application](#the-application)
6. [The controllers of the lists](#the-controllers-of-the-lists)
7. [The login](#the-login)
8. [The BPMS API](#the-bpms-api)
9. [The user directory](#the-user-directory)
10. [The pictures of persons](#the-pictures-of-persons)
11. [The configuration](#the-configuration)
12. [The docker-compose.yaml](#the-docker-composeyaml)
13. [The test that starts the application](#the-test-that-starts-the-application)
14. [The integration tests of the backend](#the-integration-tests-of-the-backend)
15. [The README](#the-readme)

## Spring Boot only

The cockpit backend exists for Spring Boot only. The library is a Spring MVC application on virtual
threads. The Quarkus code in the `business-cockpit` repository belongs to the extension of a
workflow module, not to the cockpit. So the skill creates a Spring Boot backend and asks no question
about the framework. `Q-BACKEND-FRAMEWORK` is Spring Boot whenever the skill creates the backend.

## The layout

```
pom.xml                         the parent: versions, the cockpit's BOM, the two modules
types/
  pom.xml                       downloads <scope>/bc-types, writes and compiles UiUriTypes.java
application/
  pom.xml                       business-cockpit, the UI jar, the optional starters
  src/main/java/<package>/
    CockpitApplication.java     the main class
    lists/                      one controller per list of Q-LIST-CONTROLLERS
    security/                   the two security chains and userDetailsProvider
    users/                      the two mappers, and the user directory for configuration
  src/main/resources/config/
    application.yaml            what every environment shares
    application-local.yaml      the values for docker-compose.yaml
  src/test/java/<package>/      the test that starts the application, the integration tests
                                and the simulator they use
docker-compose.yaml             MongoDB, Mailpit, Keycloak for Q-LOGIN-KIND oidc, Kafka for Q-KAFKA
keycloak/
  realm.json                    the realm Keycloak imports at the start, for Q-LOGIN-KIND oidc
README.md                       how to start it, and the order of the builds
```

This is the layout with Maven. With Gradle (`Q-BUILD-TOOL`) the build files differ and everything
else stays the same, see [With Gradle](#with-gradle).

The module `types` builds the types jar for the workflow modules, see
[project.md](project.md#the-types-jar). The module `application` is the counterpart of `container`.
`<package>` is the Java package of `Q-BACKEND-COORDINATES`.

## The parent pom

The library's own parent is `io.vanillabp:release-parent`, which carries no Spring. So the
library pins its versions through BOMs, and the application has to use the same ones. The cockpit
publishes them as one BOM, `io.vanillabp.businesscockpit:business-cockpit-bom`, in the version of
the cockpit. The parent imports it in its `dependencyManagement`, with the version of
`Q-COCKPIT-VERSION` in the property `business-cockpit.version`:

```xml
<dependency>
  <groupId>io.vanillabp.businesscockpit</groupId>
  <artifactId>business-cockpit-bom</artifactId>
  <version>${business-cockpit.version}</version>
  <type>pom</type>
  <scope>import</scope>
</dependency>
```

The BOM brings `spring-boot-dependencies`, `spring-cloud-dependencies` (the library's proxy under
`/wm/` is `spring-cloud-starter-gateway-server-webmvc`), `io.vanillabp:vanillabp-bom`,
`mongodb-changesets`, and the cockpit's own artifacts, among them `business-cockpit` and
`bpms-protobuf-api`. So the modules name all of them without a version. Neither the BOM nor the
library brings OpenFeign or OkHttp. An application that uses them for clients of its own names them,
with their versions, in its own POM. Do not import the Spring BOMs a second time, and do not import
one before this BOM. The first import of a coordinate wins.

Pin `spring-boot-maven-plugin` to the version of `spring-boot-dependencies` that the BOM imports,
because a BOM gives no plugin a version and no `spring-boot-starter-parent` does either. Read it
from `bom/pom.xml` of the `business-cockpit` repository at the tag of `Q-COCKPIT-VERSION`. The
parent also sets the Java version of `Q-JAVA-VERSION` and the property `ui.version`, the version
of the user interface. Both modules read `ui.version`: `types` downloads `<scope>/bc-types` of
that version, and `application` takes the UI jar of that version.

The library depends on `com.phactum.mongodb:mongodb-changesets`, and the BOM pins the version the
cockpit is built with. Its releases and its snapshots both come from Maven Central, so neither asks
for credentials. A release of the cockpit names a release of the changesets. A snapshot of the
cockpit may name a snapshot of the changesets, and the BOM itself is then a snapshot too. Both come
from the snapshot repository of Maven Central
(`https://central.sonatype.com/repository/maven-snapshots/`), and the parent names that
repository only for as long as `Q-COCKPIT-VERSION` is a snapshot. Propose a release version in
`Q-COCKPIT-VERSION`.

## With Gradle

When `Q-BUILD-TOOL` is Gradle, the repository has the same two modules and follows the same rules.
Only the build files differ. The skill writes them in the Kotlin DSL and commits the Gradle wrapper,
so a build agent needs a JDK and nothing else:

```
settings.gradle.kts             the two modules: include("types", "application")
build.gradle.kts                what both modules share: the group, the version, the Java version
gradle.properties               business-cockpit.version and ui.version
types/build.gradle.kts          downloads <scope>/bc-types, writes and compiles UiUriTypes.java
application/build.gradle.kts    business-cockpit, the UI jar, the optional starters
gradlew, gradle/wrapper/        the Gradle wrapper
```

What [The parent pom](#the-parent-pom) says for Maven looks like this with Gradle:

- The cockpit's BOM comes in as a platform, and the dependencies then name no version:

  ```kotlin
  val cockpitVersion = property("business-cockpit.version")
  implementation(platform("io.vanillabp.businesscockpit:business-cockpit-bom:$cockpitVersion"))
  ```
- Do not apply the plugin `io.spring.dependency-management`. Next to the Spring Boot plugin it
  imports the Spring Boot BOM in the version of the plugin, which is a second import of the Spring
  BOMs.
- The plugin `org.springframework.boot` goes into `application` only. It gets the version of
  `spring-boot-dependencies` that the cockpit's BOM imports, read from `bom/pom.xml` of the
  `business-cockpit` repository at the tag of `Q-COCKPIT-VERSION`.
- The Java version of `Q-JAVA-VERSION` is a toolchain:
  `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }`.
- The snapshot repository of Maven Central is in `repositories` only for as long as
  `Q-COCKPIT-VERSION` is a snapshot.

Where this page names the `pom.xml` of a module, take its `build.gradle.kts`. `./gradlew build`
builds both modules, and `./gradlew :application:bootRun` starts the application.

## The application

The module `application` depends on:

- `io.vanillabp.businesscockpit:business-cockpit`, without a version, from the BOM,
- the UI jar of `Q-UI-JAR` in the version `ui.version`, with scope `runtime`, when `Q-HOSTING` is
  `embedded`,
- `spring-boot-starter-security-oauth2-client` when `Q-LOGIN-KIND` is `oidc`. The older name
  `spring-boot-starter-oauth2-client` is deprecated,
- `spring-kafka` and `io.vanillabp.businesscockpit:bpms-protobuf-api`, without a version, when
  `Q-KAFKA` is yes. The library marks both optional.
- `spring-boot-starter-mail` when `Q-MAIL` is yes. The library marks it optional.
- for the test, `spring-boot-starter-test` and the MongoDB module of Testcontainers.

The main class says nothing about the cockpit. The library's auto-configurations do the rest,
because the dependency is there:

```java
package com.example.cockpit;

@SpringBootApplication
public class CockpitApplication {

    public static void main(final String... args) {
        SpringApplication.run(CockpitApplication.class, args);
    }

}
```

Its package must not lie inside `io.vanillabp.cockpit`. There the component scan would pick up
classes of the library on top of the auto-configurations. The deprecated base class
`io.vanillabp.cockpit.BusinessCockpitApplication` is empty, and the generated application never
extends it.

The library switches on `@EnableWebSecurity`, `@EnableMethodSecurity(securedEnabled = true)`,
`@EnableAsync` and `@EnableScheduling` for the whole application, and says so in the log on every
start. The generated application keeps them. The BPMS API needs method security, and the
notifications and the live updates need scheduling.

## The controllers of the lists

The library shapes the answers of the GUI API, and it leaves open which user tasks, cases and
modules go into them. Three abstract classes hold the shape:
`AbstractUserTaskListGuiApiController`, `AbstractWorkflowListGuiApiController` and
`AbstractWorkflowModulesGuiApiController`. A subclass answers one question, who may see what, with a
visibility. That visibility then holds for the list, the search, the page of a single row and every
action on it. Without a controller a list stays empty, because nothing answers it.

The skill writes one controller per list of `Q-LIST-CONTROLLERS`. The default follows the screens
of the interview:

|      Controller      |            Path            |              Abstract class               |                          Visibility                          |           Generated when            |
|----------------------|----------------------------|-------------------------------------------|--------------------------------------------------------------|-------------------------------------|
| task list            | `/gui/api/v1`              | `AbstractUserTaskListGuiApiController`    | `UserTaskVisibility.everythingTheUserMayWorkOn(currentUser)` | always, it serves `/tasks`          |
| the user's own tasks | `/gui/api/v1/current-user` | `AbstractUserTaskListGuiApiController`    | `UserTaskVisibility.onlyWhatIsTheUsersOwn(currentUser)`      | always, it serves `/my-tasks`       |
| tasks of the roles   | `/gui/api/v1/user-roles`   | `AbstractUserTaskListGuiApiController`    | `UserTaskVisibility.whatTheUsersGroupsMayTake(currentUser)`  | `Q-SUB-LISTS` has sub-lists by role |
| case list            | `/gui/api/v1`              | `AbstractWorkflowListGuiApiController`    | `WorkflowVisibility.workflowsAddressedTo(currentUser)`       | always                              |
| workflow modules     | `/gui/api/v1`              | `AbstractWorkflowModulesGuiApiController` | `WorkflowModuleVisibility.modulesAddressedTo(currentUser)`   | always, the loader needs it         |

```java
@RestController
@RequestMapping(path = "/gui/api/v1")
public class TaskListController extends AbstractUserTaskListGuiApiController {

    @Override
    protected UserTaskVisibility userTasksVisibleTo(final UserDetails currentUser) {
        return UserTaskVisibility.everythingTheUserMayWorkOn(currentUser);
    }

}
```

The user interface asks the path of the list it shows, see
[gui-api.md](gui-api.md#the-endpoints). A path that no controller answers is a list the
user interface must not offer. So the screens of the interview and the controllers agree, and the
checklist proves it (`BE-004`).

The visibilities are the ready-made views the cockpit ships. Each one can also be built by hand,
for example for a sub-list narrowed to some task types. Write that into the controller of that
list, and write the rule into its Javadoc. The rule the shipped views apply is described in the
wiki under [Who sees which user task](https://github.com/vanillabp/business-cockpit/wiki/Security#who-sees-which-user-task).

## The login

The library brings no login. The application writes the security filter chain of the GUI API as a
bean named `guiHttpSecurity`, with `@Order(99)`. The class
`container/src/main/java/io/vanillabp/cockpit/security/GuiSecurityConfiguration.java` is the
template. Every chain keeps four things, whatever the login is:

1. The five matchers of `io.vanillabp.cockpit.config.web.WebSecurityConfiguration` stay
   `permitAll`: `appInfoRequestMatcher`, `currentUserRequestMatcher`, `assetsRequestMatcher`,
   `staticRequestMatcher` and `workflowModulesProxyRequestMatcher`. They are asked for before
   anybody is logged in. Keep `currentUserRequestMatcher` as it is, and do not open
   `/gui/api/v1/app/current-user/**`: the user settings below it need a login.
2. `PassiveJwtSecurityFilter` reads the token from the cookie on every later request.
3. `JwtRenewalFilter` comes right behind it, with `updatesRequestMatcher`, so a login does not end
   after `business-cockpit.jwt.cookie.expires-duration` while somebody works.
4. `JwtSecurityContextRepository` is the place the login is written to. So every login ends in the
   same cookie, `bc` by default.

`anyRequest()` is `authenticated()`. CSRF and CORS stay off, as in `container`, because the browser
sees one origin and the cookie is the only credential.

The chain differs only in how the user logs in (`Q-LOGIN-KIND`).

### OIDC, the default

The user logs in at an OpenID Connect provider, by default the Keycloak of `docker-compose.yaml`.
Spring Security's `oauth2Login` handles the redirect. A post processor of its filter sets the
`JwtSecurityContextRepository`, so the login ends in the cockpit's cookie. The chain sends PKCE,
although the client has a secret:

```java
final var authorizationRequests = new DefaultOAuth2AuthorizationRequestResolver(
    clientRegistrationRepository,
    OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
authorizationRequests.setAuthorizationRequestCustomizer(
    OAuth2AuthorizationRequestCustomizers.withPkce());

http
    .csrf(AbstractHttpConfigurer::disable)
    .cors(AbstractHttpConfigurer::disable)
    .anonymous(AbstractHttpConfigurer::disable)
    .authorizeHttpRequests(requests -> requests
        .requestMatchers(appInfoRequestMatcher, currentUserRequestMatcher, assetsRequestMatcher,
            staticRequestMatcher, workflowModulesProxyRequestMatcher)
        .permitAll()
        .anyRequest()
        .authenticated())
    // the GUI API answers 401 instead of a redirect to the provider, so the user interface
    // notices a lost login and sends the whole browser to the login. It comes before
    // oauth2Login, so it is asked first
    .exceptionHandling(exceptions -> exceptions
        .defaultAuthenticationEntryPointFor(
            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
            PathPatternRequestMatcher.withDefaults().matcher("/gui/api/**")))
    .oauth2Login(login -> login
        .authorizationEndpoint(endpoint -> endpoint
            .authorizationRequestResolver(authorizationRequests))
        .withObjectPostProcessor(new ObjectPostProcessor<OAuth2LoginAuthenticationFilter>() {
          @Override
          public <O extends OAuth2LoginAuthenticationFilter> O postProcess(final O filter) {
            filter.setSecurityContextRepository(jwtSecurityContextRepository);
            return filter;
          }
        }))
    .logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessHandler(jwtLogoutSuccessHandler))
    .addFilterAfter(new PassiveJwtSecurityFilter(properties.getJwt(), jwtMapper),
        BasicAuthenticationFilter.class)
    .addFilterAfter(new JwtRenewalFilter(properties.getJwt(), jwtMapper, updatesRequestMatcher),
        PassiveJwtSecurityFilter.class);
```

The cockpit's token names the user but carries no groups. The groups come from the user directory
on every request, see [The user directory](#the-user-directory). So do not write a
`GrantedAuthoritiesMapper` for the groups of the provider's token: they never reach the cockpit's
token. The id of the user is the name of the login, the claim set as `user-name-attribute` of the
provider, `preferred_username` by default (`Q-OIDC-PROVIDER`). It becomes the subject of the
cockpit's token, and the directory must know the user by it. The provider is configured with the usual Spring Boot keys
below `spring.security.oauth2.client.registration.<id>` and `.provider.<id>`, the client secret
from an environment variable. The login paths are Spring Security's: `/oauth2/authorization/<id>` and
`/login/oauth2/code/<id>`. They go into `Q-LOGIN`.

Do not write `oauth2Login(login -> login.securityContextRepository(..))`. It has no effect: while
Spring Security builds the chain, it replaces that repository with the one of the whole chain,
which keeps the login in the HTTP session. The login then seems to work in the browser, but no
cookie is written, and a workflow module behind the proxy gets no user. The post processor runs
after that replacement. PKCE is needed because Spring Authorization Server asks every client for
it, and Spring Security adds it on its own only for a client without a secret.

A request of the GUI API without a login answers 401, not a redirect. A `fetch` would follow the
redirect to the provider and fail there. With a 401 the user interface sends the whole browser to
the login, see [gui-api.md](gui-api.md#staying-logged-in).

The module `container` of the `business-cockpit` repository has this chain, in
`GuiSecurityConfiguration`, and a test which logs in at a provider played by a test server,
`ALoginAtAnOpenIdConnectProviderTest`. Write the integration test of the login of the generated
backend first anyway (`BE-007`), before anything builds on it. It is one of the
[integration tests of the backend](#the-integration-tests-of-the-backend).

### A login form

The user logs in with a form of the backend. Spring Security's `formLogin` takes the place of
`oauth2Login`. The `JwtSecurityContextRepository` comes the same way, with a post processor of
`UsernamePasswordAuthenticationFilter`, because `formLogin`'s own `securityContextRepository` is
replaced like the one of `oauth2Login`. The users and their passwords come from
the user directory, see [The user directory](#the-user-directory). The login paths are `/login` and
`/logout`. This way is not proven by a test of the cockpit either.

### The token travels in a cookie

The cockpit reads the token from its cookie only. `PassiveJwtSecurityFilter` knows no
`Authorization` header. So the generated backend always uses the cookie, and `Q-TOKEN` is `cookie`.
If the user asks for the header, tell them that the cockpit cannot read it today. Do not generate
a filter of your own for it. Without the header, `Q-HOSTING` `in-front` loses its main reason, a
user interface that loads before the login. It stays an option: an nginx in front of the user
interface and the backend still gives the browser one origin for both, see
[SKILL.md](../SKILL.md#where-the-user-interface-runs).

The same configuration class writes the bean `userDetailsProvider`, a `JwtUserDetailsProvider`.
The library needs it and brings none. Without it the application does not start. It needs no
directory of its own: the library's `jwtMapper` asks the `UserDirectoryService` for the user of
every request, and the details take the groups, the name and the e-mail address from that answer.
Do not write a `jwtMapper` of your own. If you have to, pass the `UserDirectoryService` to
`new JwtAuthenticationTokenMapper(properties, userDirectoryService, groupHierarchyService::resolveGroups)`.

## The BPMS API

The workflow modules report to the BPMS API below `/bpms/api/v1/**` and `/bpms/api/v1_1/**`. The
application writes its chain as a bean named `bpmsApiHttpSecurity`, with `@Order(1)`. The class
`container/src/main/java/io/vanillabp/cockpit/security/BpmsApiSecurityConfiguration.java` is the
template: HTTP basic with one user from `bpms-api.username` and `bpms-api.password`, the realm
`bpms-api.realm-name`, the role `BpmsApiAuthority.BPMS_API_ROLE`, and a
`NullSecurityContextRepository`, because a workflow module authenticates on every request. Copy it
as it is. The password comes from an environment variable (`Q-BPMS-API-USER`), never from a file in
the repository.

When neither chain is there, the library names the missing one in the log on every start. The
checklist asks that the log names none (`BE-006`).

## The user directory

Only the application knows where its users live. The library asks three beans for it:

- `io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService`, from the artifact
  `business-cockpit-spring-boot-support`, which the library brings: finds users for a search, lists
  all users and gets one by id. The user interface needs it to propose a task to a
  person and to show a person. The cockpit also calls `getUser(id)` on every request of a
  logged-in user, because the token carries no groups. The groups are `getAuthorities()` of the
  answer: the groups the user is assigned to, without the group hierarchy, which the cockpit
  resolves itself.
- `io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper`: turns a user or a group into the
  `Person` and `Group` of the GUI API.
- `io.vanillabp.cockpit.users.model.PersonAndGroupMapper`: turns them into the person and group
  the cockpit stores.

`DerivedCockpitApplication` in the tests of `container` shows all three on a fixed list of users.
Where the list comes from is `Q-USER-DIRECTORY`:

- `keycloak`, the default: the users and groups of the Keycloak realm. The library brings this
  directory, so the skill writes no `UserDirectoryService` for it, only the configuration, see
  below. The login and the directory then come from one place, so the ids always match.
- `configuration`: a list of users with their groups in `application.yaml`, below a prefix of the
  application, read with `@ConfigurationProperties`. With `Q-LOGIN-KIND` `form` it also holds each
  password as a `bcrypt` hash. It is the choice when there is no Keycloak, and the default with
  `Q-LOGIN-KIND` `form`. The skill writes this directory in `users/`.
- `ldap`: the users and groups of an LDAP directory. The library brings this directory as well. The
  skill writes the configuration and adds `org.springframework.ldap:spring-ldap-core` to the
  `pom.xml` of `application`, without a version. The library does not bring Spring LDAP along.

With `keycloak` or `ldap` the two mappers in `users/` take the library's directory as their
`UserDirectoryService`. Do not write a `UserDirectoryService` next to it: the application does not
start with two.

The users of the directory and the users of the login must be the same people with the same ids.
With OIDC the id is the claim of `Q-OIDC-PROVIDER`, by default `preferred_username`.

`getUser` runs on every request. The directories of the library for Keycloak and LDAP keep each
user with its groups for 14 hours, `business-cockpit.user-directory.cache-duration`. Every night at
2 a.m. in the time zone of the server they empty the whole cache,
`business-cockpit.user-directory.cache-clear-cron`, `-` switches this off. So a group given or
taken away counts the next day at the latest. A locked account ends earlier, because its login
token is not refreshed any more. Operations can empty the cache at once with the actuator endpoint
`userdirectorycache`: a `DELETE` on `/actuator/userdirectorycache`. Like every actuator endpoint it
is reachable only where operations exposes it. The directory with `configuration` reads memory and
needs no cache. What the cockpit does with the answer:

- A user the directory does not know keeps the login, but has no groups. The log warns once a
  minute at most for each such user.
- If `getUser` throws, the request fails and the log shows the error. The directories of the
  library do that when Keycloak or LDAP does not answer, and the message says what to check.

The library's directories give the groups only from `getUser`. A search and the list of all users
give users without groups, because the cockpit reads only their ids.

### The configuration of the library's directories

With `keycloak`, `application.yaml` names the realm and the client of the directory, the secret
from an environment variable:

```yaml
business-cockpit:
  user-directory:
    type: keycloak
    keycloak:
      server-url: ${KEYCLOAK_URL}
      realm: business-cockpit
      client-id: business-cockpit-directory
      client-secret: ${KEYCLOAK_DIRECTORY_SECRET}
      user-id: username
```

`user-id` is `username` when the login names the user by `preferred_username`, the default of
`Q-OIDC-PROVIDER`. It is `id` when the login names the user by `sub`.

With `ldap`:

```yaml
business-cockpit:
  user-directory:
    type: ldap
    ldap:
      url: ${LDAP_URL}
      base: dc=example,dc=com
      bind-dn: cn=cockpit,ou=services,dc=example,dc=com
      bind-password: ${LDAP_BIND_PASSWORD}
      user-search-base: ou=people
      group-search-base: ou=groups
```

The other keys have defaults for OpenLDAP: users of the object class `inetOrgPerson` with the id in
`uid`, and groups which name their members by DN in `member`. Ask the user for the object class and
the attributes when the directory is an Active Directory. The wiki page
[Security](https://github.com/vanillabp/business-cockpit/wiki/Security) lists every key. The
application tells the rest while it starts: a missing key ends the start, with a line to copy.

## The pictures of persons

Write this part only when the interview says there are pictures of persons (`Q-AVATARS`). Without
it the user interface shows the initials of a person, and the backend has nothing to do.

The GUI API of the cockpit has no picture of a person, and the library brings no endpoint for one.
So the application writes a controller of its own, in `users/`, that serves the picture of a person
by user id:

```
GET /person-pictures/{userId}
```

The source is the directory of the organisation, as `Q-AVATAR-SOURCE` says. For example, with
Microsoft Entra ID the controller asks Microsoft Graph for the photo of the user, with a client of
its own that may read the photos of all users. The secret of that client comes from an environment
variable. With an LDAP directory the picture is often an attribute of the user, for example
`jpegPhoto` or `thumbnailPhoto`. How a user id of the cockpit becomes the key of the source is part
of the answer, for example the user principal name in Entra ID.

The controller answers:

- 200 with the picture, its own content type, for example `image/jpeg`, and
  `X-Content-Type-Options: nosniff`. A content type that is not an image is not passed on; the
  answer is 404 instead.
- 404 when the person has no picture, and also when the source does not know the user. The answer
  does not tell the two apart.
- 404 as well when the source does not answer, with a warning in the log. The user interface keeps
  the initials either way.

Security:

- Only a user who is logged in gets a picture. The path is not one of the open matchers of
  [The login](#the-login), and no other matcher opens it. `anyRequest()` is `authenticated()`, so the
  chain `guiHttpSecurity` protects it like the GUI API.
- Without a login the path answers 401, not a redirect to the provider. Add its pattern
  `/person-pictures/**` to the `defaultAuthenticationEntryPointFor` of the chain, next to
  `/gui/api/**`. A redirect would only make the browser load the login page as a picture.
- Every user who is logged in may see the picture of every person, as they may see the name.
- The user id is checked before it reaches the source: it must not be empty, and it is passed to
  the source as one encoded value, never pasted into a path, a filter or a query of the source
  unescaped.

Caching, as `Q-AVATAR-CACHE` says:

- The controller keeps each picture in the memory of the application for that time, and also the
  fact that a person has none. So it asks the source once per person in that time, not on every
  request. The cache holds a limited number of pictures and drops the oldest first.
- Every answer, 200 and 404, carries `Cache-Control: private, max-age=<seconds>` with the same time.
  `private`, because only a user who is logged in may see it, so no shared cache must keep it. The
  browser then asks again only when the time has run out.

The application names the address for the user interface in `business-cockpit.additional-properties`,
under the key `person-picture-uri`, see [The configuration](#the-configuration). The user interface
asks for no picture without that key. With `Q-HOSTING` `in-front` the nginx image forwards
`/person-pictures/**` to the backend, like the GUI API.

With a backend of the organisation's own, the skill writes none of this. Tell the user that their
backend has to serve the pictures with the same answers, protection and headers, and name the
address in `person-picture-uri`. The path may then be a different one.

## The configuration

`application.yaml` holds what every environment shares. `container`'s
`src/main/resources/config/application.yaml` is the template, because its values are not on the
application's class path and nobody inherits them:

```yaml
mongodb:
  write-concern: majority
  write-concern-journal: true

spring:
  threads:
    virtual:
      enabled: true
  mvc:
    static-path-pattern: "/**"
  web:
    resources:
      static-locations: "classpath:/static/"

bpms-api:
  realm-name: BPMS-API
  username: ${BPMS_API_USERNAME}
  password: ${BPMS_API_PASSWORD}

application:
  spa-default-file: classpath:/static/index.html

business-cockpit:
  title-short: ...
  title-long: ...
  application-uri: ${COCKPIT_URI}
  default-locale: en
  additional-properties:
    languages: en,de
  jwt:
    hmacSHA256-base64: ${COCKPIT_JWT_KEY}
```

- `spring.mongodb.uri` names MongoDB. Spring Boot 4 no longer reads `spring.data.mongodb`.
- `business-cockpit.jwt.hmacSHA256-base64` is the key the cookie's token is signed with, 32 bytes
  in base64. It comes from the environment.
- `business-cockpit.additional-properties` carries the languages of `Q-LANGUAGES` as one string,
  see [gui-api.md](gui-api.md#the-endpoints).
  With `Q-AVATARS` it also carries `person-picture-uri: /person-pictures/{userId}`, see
  [The pictures of persons](#the-pictures-of-persons).
  Put no YAML list below `additional-properties`. The library binds it into a
  `Map<String, Object>`, and Spring Boot turns a list there into an object with the keys `"0"`,
  `"1"` and so on, in no fixed order, so `GET /app/info` hands the user interface an object where it
  expects an array. Write a list as a map instead, keyed by the id, for example
  `task-types: { confirmRide: Confirm ride, payRide: Pay ride }`, or as one string separated by
  commas, as `languages` is. The user interface puts the entries in the order it needs, for example
  by their label. The keys stay as they are written, `task-types` is not turned into `taskTypes`.
- `business-cockpit.default-locale` is `Q-LANGUAGE-DEFAULT`, the language of a notification e-mail
  for a user who has none of their own.
- `application.spa-default-file` and the static locations only matter for `Q-HOSTING` `embedded`.
  With `behind` the application has the gateway route to the nginx instead, see
  [project.md](project.md#behind-the-backend-behind).
- With `Q-KAFKA`, `spring.kafka.bootstrap-servers`, `bpms-api.kafka.group-id-suffix` and
  `bpms-api.kafka.topics.workflow`, `.user-task` and `.workflow-module` switch on the reading of
  Kafka. The library also needs the system property `workerId`, so the `README.md` says how to set
  it. `container/src/main/resources/config/application-kafka.yaml` is the template.
- With `Q-MAIL`, `spring.mail.host`, `spring.mail.port`,
  `business-cockpit.notification.smtp.enabled`, `.smtp.from` and
  `business-cockpit.notification.interval` switch on the e-mails.
  `container/src/main/resources/config/application-mailpit.yaml` is the template.

The application tells the rest while it starts. A value it cannot run without ends the start, and
the message lists every missing key with an example. A feature that is not configured yet gives a
warning. So start it once and follow what it prints.

`application-local.yaml` holds the values for `docker-compose.yaml`: the address of MongoDB, of
Kafka, of Mailpit and of Keycloak, the client secret of the local realm, a JWT key for local use
only and the BPMS API user. The `README.md` starts the application with the profile `local`.

## The docker-compose.yaml

One `docker compose up` starts everything the application needs on a developer's machine.
`development/docker-compose.yaml` of the `business-cockpit` repository is the template for the
first three:

| Service  |                Image                 |                                      Why                                       |                    When                     |
|----------|--------------------------------------|--------------------------------------------------------------------------------|---------------------------------------------|
| MongoDB  | `mongo`, the version of the template | the storage; a replica set, because change streams and the changesets need one | always                                      |
| Kafka    | `apache/kafka`, in KRaft mode        | the reports of the workflow modules, when they come through Kafka              | only with `Q-KAFKA`                         |
| Mailpit  | `axllent/mailpit`                    | catches the notification e-mails, web interface on port 8025                   | always; the application sends with `Q-MAIL` |
| Keycloak | `quay.io/keycloak/keycloak`          | the OIDC provider, with the realm of `keycloak/` imported at the start         | `Q-LOGIN-KIND` is `oidc`                    |

Pin every image to a version. MongoDB runs as a single-node replica set. Start it with
`--replSet` and initiate the set in a health check or a setup container, so nobody has to type
`rs.initiate()`. The same setup creates the database `business-cockpit` and a user
`business-cockpit` with the password `business-cockpit` and the role `dbOwner` on that database,
as `development/mongo/` of the template does. `application-local.yaml` connects as that user. The application connects with `directConnection=true`, because the member's name
inside the compose network does not resolve outside it.

Keycloak starts with `start-dev --import-realm`. The compose file mounts `keycloak/realm.json`
into `/opt/keycloak/data/import/`, so Keycloak creates the realm at its first start. The skill
writes that file for local development:

- a realm, by default `business-cockpit`,
- a confidential client, by default `business-cockpit`, with the redirect address
  `http://localhost:8080/login/oauth2/code/<id>` and the secret that `application-local.yaml`
  names,
- with `Q-USER-DIRECTORY` `keycloak`: a second confidential client, by default
  `business-cockpit-directory`, with service accounts on, only the role `view-users` of
  `realm-management`, the client scope `kc-admin-api` among its `defaultClientScopes`, and its
  secret named in `application-local.yaml`. Without that client scope a newer Keycloak refuses the
  token of the client at its admin REST API,
- exactly one group and one user with a password, and the user is a member of the group. They are
  a template, not a set of test users.

There is no mapper for a claim `groups`. The cockpit takes the groups of a user from the user
directory, not from the token, see [The user directory](#the-user-directory).

The groups decide which user tasks and cases a user sees. So the template is not enough to try the
visibilities out. The `README.md` tells the developer to add test users with different groups to
`realm.json`, for example one per group of `Q-VISIBLE-GROUPS` and `Q-SUB-LISTS`, and to start
Keycloak again with an empty volume, so the realm is imported again. These users and the secret are
for a developer's machine only, and the `README.md` says that too.

## The test that starts the application

One test starts the whole application against MongoDB of Testcontainers, as a replica set, the
way `container/src/test/java/io/vanillabp/derived/cockpit/ApplicationWithoutTheBaseClassTest.java`
does. It sets the few keys the start needs, then asks `GET /gui/api/v1/app/info` and checks the
title. A second request, without a login, to a list answers 401. With OIDC the test needs no
Keycloak: it also checks that a page without a login is sent on to the provider.

This test proves that the generated application starts and that nothing of the library is missing.
The end-to-end tests of the UI repository do not start this backend. They run against the
TypeScript simulator of the UI repository, see [project.md](project.md#the-simulators). The login
and the lists are proven by the integration tests of this repository, see below.

## The integration tests of the backend

The backend repository has integration tests of its own. They prove what only the backend does,
and they use no mocks and change no data directly. The data comes in the way it comes in
production: the test tells the simulator of the backend to call an API of the cockpit, for example
the BPMS API to report a case or a user task. The simulator is Java code in the tests of the
repository, simulator 2 of [the list of simulators](project.md#the-simulators). The test then
checks the result from the outside, through the GUI API. Most of the time that is enough. Where it
helps, it also reads the state inside, for example records loaded through a repository of the
Spring test context, when the effect outside comes only with a delay.

They prove:

- the login of `Q-LOGIN-KIND`. With `oidc`, a request without a login is sent on to Keycloak. The
  test follows the redirects with an HTTP client that keeps cookies, sends the login form of
  Keycloak and comes back with the cookie of the cockpit. Then `GET /gui/api/v1/app/current-user`
  shows the user with the groups from the user directory (`BE-007`). With `form`, the login form of the
  backend does the same (`BE-009`),
- every list of `Q-LIST-CONTROLLERS`: it answers, and it shows each test user only the user tasks
  and cases that user may see (`BE-004`). The simulator reports the data through the BPMS API
  first, then the test reads the lists as different users,
- with `Q-AVATARS`, the pictures of persons: a picture for a person who has one, 404 for a person
  who has none, 401 without a login, the `Cache-Control` header, and that the source is asked once
  per person within the time of `Q-AVATAR-CACHE` (`BE-021`, `BE-022`, `BE-023`). The simulator
  stands in for the source of `Q-AVATAR-SOURCE`, for example as an HTTP server that answers like
  Microsoft Graph, and counts the requests.

They start the application the way the test above does, with MongoDB of Testcontainers, and with
`oidc` also a Keycloak of Testcontainers that imports `keycloak/realm.json`. They are plain Java
tests and need no browser, so the repository stays without a `package.json` (`PKG-014`).
`Q-E2E-TOOL` holds for the UI repository only.

## The README

The `README.md` of the backend repository says:

- how to start it: `docker compose up`, then the application with the profile `local`, and the
  system property `workerId` when Kafka is read,
- which users exist locally and with which password, and that they are for local use only,
- with `Q-LOGIN-KIND` `oidc`: that `keycloak/realm.json` has one group and one user as a template,
  that the developer adds test users with different groups there, because the groups decide which
  user tasks and cases a user sees, and how to import the realm again,
- which environment variables a real deployment sets: the MongoDB address, the JWT key, the BPMS
  API user, the client secret of the provider, the mail server,
- the order of the builds, see [project.md](project.md#where-the-types-jar-is-built): the cockpit
  release, then the user interface, then this repository, the types jar and the application,
- how a new version of the user interface gets in: a new value of `ui.version`.


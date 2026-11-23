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
8. [When a browser goes away](#when-a-browser-goes-away)
9. [The BPMS API](#the-bpms-api)
10. [The user directory](#the-user-directory)
11. [The pictures of persons](#the-pictures-of-persons)
12. [The configuration](#the-configuration)
13. [The docker-compose.yaml](#the-docker-composeyaml)
14. [The test that starts the application](#the-test-that-starts-the-application)
15. [The integration tests of the backend](#the-integration-tests-of-the-backend)
16. [The README](#the-readme)

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
    security/                   the two security chains, userDetailsProvider and, for
                                Q-LOGIN-KIND oidc, the logout at the provider
    web/                        the advice for a client that went away
    users/                      the two mappers, and the user directory for configuration
    userinterface/              the route to the dev server or the nginx of the user interface
  src/main/resources/config/
    application.yaml            what every environment shares
    application-local.yaml      the values for docker-compose.yaml
    application-ui-dev-server.yaml  the address of the dev server of the user interface
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
template. Every chain keeps five things, whatever the login is:

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
5. Every request of the dispatcher types `ASYNC` and `ERROR` is let through, with
   `dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()` as the first
   rule. The template does not have this rule yet. The reason is in
   [When a browser goes away](#when-a-browser-goes-away) (`BE-033`).

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
        // a later dispatch of a request that was checked already, see "When a browser goes away"
        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR)
        .permitAll()
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
        // keeps the ID token for the logout at the provider
        .successHandler(new KeepTheIdTokenForTheLogout())
        .withObjectPostProcessor(new ObjectPostProcessor<OAuth2LoginAuthenticationFilter>() {
          @Override
          public <O extends OAuth2LoginAuthenticationFilter> O postProcess(final O filter) {
            filter.setSecurityContextRepository(jwtSecurityContextRepository);
            return filter;
          }
        }))
    .logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessHandler(new LogoutAtTheProvider(properties.getJwt(), clientRegistrationRepository)))
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
`ALoginAtAnOpenIdConnectProviderTest`. Its logout ends the login in the cockpit only, so take the
logout from [Logging out at the provider](#logging-out-at-the-provider) instead. Write the
integration test of the login of the generated backend first anyway (`BE-007`), before anything
builds on it. It is one of the
[integration tests of the backend](#the-integration-tests-of-the-backend).

### Logging out at the provider

`/logout` has to end the login at the provider as well, not only the cookie of the cockpit. The
provider keeps a session of its own. Without its end, the next request after the logout goes to the
provider, the provider sees its session and sends the user back logged in, without a question. To
the user the logout then does nothing that a reload does not undo (`AUTH-007`).

So the generated backend uses the RP-initiated logout of OpenID Connect: after the logout the
browser goes to the `end_session_endpoint` of the provider, with three parameters:

- `id_token_hint`, the ID token the provider gave at the login. It tells the provider whose session
  ends, so it asks no question.
- `client_id`, the client of `Q-OIDC-PROVIDER`.
- `post_logout_redirect_uri`, the start page of the cockpit. The provider sends the browser back
  there, and because the session there has ended, the start page asks for a new login.

The cockpit keeps nothing of the provider after the login, so the application keeps the ID token
itself, in a cookie of its own (`BE-032`). The cookie is `HttpOnly` and has the path `/logout`, so
the browser sends it with the logout only. Spring Security's
`OidcClientInitiatedLogoutSuccessHandler` does not help here: it takes the ID token from the login
of the request, and the login of the cockpit is its own token from the cookie, not the login of the
provider.

```java
/** Keeps the ID token of the provider in a cookie of its own, for the logout at the provider. */
class KeepTheIdTokenForTheLogout extends SavedRequestAwareAuthenticationSuccessHandler {

  static final String ID_TOKEN_COOKIE = "bc-id-token";

  @Override
  public void onAuthenticationSuccess(final HttpServletRequest request,
      final HttpServletResponse response, final Authentication authentication)
      throws ServletException, IOException {
    if (authentication.getPrincipal() instanceof OidcUser user) {
      final var cookie = ResponseCookie.from(ID_TOKEN_COOKIE, user.getIdToken().getTokenValue())
          .httpOnly(true)
          .secure(request.isSecure())
          .sameSite("Lax")
          .path("/logout")
          .build();
      response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    super.onAuthenticationSuccess(request, response, authentication);
  }

}

/** Ends the login in the cockpit, and at the provider when it offers an end of session. */
class LogoutAtTheProvider implements LogoutSuccessHandler {

  private final JwtProperties jwtProperties;

  private final ClientRegistrationRepository clientRegistrations;

  LogoutAtTheProvider(final JwtProperties jwtProperties,
      final ClientRegistrationRepository clientRegistrations) {
    this.jwtProperties = jwtProperties;
    this.clientRegistrations = clientRegistrations;
  }

  @Override
  public void onLogoutSuccess(final HttpServletRequest request,
      final HttpServletResponse response, final Authentication authentication) throws IOException {
    JwtSecurityContextRepository.clearCookie(jwtProperties, response);
    final var idToken = WebUtils.getCookie(request, KeepTheIdTokenForTheLogout.ID_TOKEN_COOKIE);
    response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie
        .from(KeepTheIdTokenForTheLogout.ID_TOKEN_COOKIE, "").path("/logout").maxAge(0).build()
        .toString());

    final var startPage = ServletUriComponentsBuilder.fromContextPath(request).path("/")
        .toUriString();
    final var provider = clientRegistrations.findByRegistrationId("keycloak");
    final var endOfSession = provider.getProviderDetails().getConfigurationMetadata()
        .get("end_session_endpoint");
    if (endOfSession == null) {
      response.sendRedirect(startPage);
      return;
    }
    response.sendRedirect(UriComponentsBuilder.fromUriString(endOfSession.toString())
        .queryParamIfPresent("id_token_hint", Optional.ofNullable(idToken).map(Cookie::getValue))
        .queryParam("client_id", provider.getClientId())
        .queryParam("post_logout_redirect_uri", startPage)
        .encode()
        .toUriString());
  }

}
```

Use the registration id of `Q-OIDC-PROVIDER` in place of `keycloak`. Spring reads
`end_session_endpoint` from the discovery document of the provider, so configure the provider with
`issuer-uri`. Without it the metadata is empty, and the logout ends the login in the cockpit only.
The ID token has often expired when the user logs out. That is fine: the specification of the
RP-initiated logout asks the provider to accept an expired ID token as the hint. Without the cookie,
for example after a login from before this logout handler, the provider asks the user whether to log
out, and that is fine as well.

The provider sends the browser back only to an address it knows. So `keycloak/realm.json` names the
start page of the cockpit as a post-logout redirect URI of the client, see
[The docker-compose.yaml](#the-docker-composeyaml). A real provider needs the address of the real
cockpit there.

### A login form

The user logs in with a form of the backend. Spring Security's `formLogin` takes the place of
`oauth2Login`. The `JwtSecurityContextRepository` comes the same way, with a post processor of
`UsernamePasswordAuthenticationFilter`, because `formLogin`'s own `securityContextRepository` is
replaced like the one of `oauth2Login`. The users and their passwords come from
the user directory, see [The user directory](#the-user-directory). The login paths are `/login` and
`/logout`. The logout handler of the template, `JwtLogoutSuccessHandler`, is enough here, because
the login lives in the cookie of the cockpit only. This way is not proven by a test of the cockpit
either.

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

## When a browser goes away

A browser goes away all the time: the user closes a tab, reloads the page, or the laptop sleeps.
Each time the update stream ends from the browser's side. That is normal and no error, so it must
not fill the log with errors and warnings. Two things in the application see to that.

The update stream runs asynchronously. The request that opened it has ended long ago on its thread,
and the stream answers from another one. When the browser closes the tab, Tomcat dispatches the same
request a second time, as an `ASYNC` or an `ERROR` dispatch. Spring Security checks every dispatch.
But `PassiveJwtSecurityFilter` reads the cookie only once per request and does not run again on
this dispatch. So Spring Security finds no login and writes an
`AuthenticationCredentialsNotFoundException` into the log. The first dispatch was checked already,
so the chain lets these later dispatches through without a check:

```java
.authorizeHttpRequests(requests -> requests
    .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR)
    .permitAll()
    // then the open matchers and anyRequest(), as above
```

This is the first rule of `guiHttpSecurity`, whatever the login is (`BE-033`). It lets nothing
through that was not checked before: a request that reaches an `ASYNC` or `ERROR` dispatch has
passed the chain on its first dispatch.

The second thing is the write to a connection that is gone. The next event or ping of the stream
fails with `java.io.IOException: Broken pipe`, and the log shows "Servlet container error
notification for disconnected client". Spring turns such an error into an
`AsyncRequestNotUsableException`. The application handles it in an advice of its own and logs it at
`DEBUG` (`BE-034`). Then a developer who looks for it still finds it, and nobody else sees it:

```java
/** A browser that went away, for example a closed tab with an open update stream, is no error. */
@RestControllerAdvice
class ClientWentAway {

  private static final Logger log = LoggerFactory.getLogger(ClientWentAway.class);

  @ExceptionHandler(AsyncRequestNotUsableException.class)
  void ignore(final AsyncRequestNotUsableException clientIsGone) {
    log.debug("The client went away: {}", clientIsGone.getMessage());
  }

}
```

The handler returns nothing, because there is nobody left to answer. Do not lower the level of a
logger of Spring or Tomcat instead: that would hide every other error of the same logger as well.

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

## The route to the user interface

The backend passes every path it does not know to the user interface with one proxy route of
Spring Cloud Gateway. With `Q-HOSTING` `behind` the route goes to the nginx image and is always there, see
[project.md](project.md#behind-the-backend-behind). With `embedded` and `behind` it also goes to the
dev server of the user interface, in the profile `ui-dev-server`, so a developer works against the
real backend, see [project.md](project.md#developing-against-the-backend). It is the same route,
only the address differs, so the application writes it once, in `userinterface/`:

```java
@Configuration
@ConditionalOnProperty("user-interface.uri")
public class UserInterfaceRouteConfiguration {

    // after the controllers (order 0), before the static files of the UI jar
    // (Ordered.LOWEST_PRECEDENCE - 1)
    private static final int AFTER_THE_CONTROLLERS = Ordered.LOWEST_PRECEDENCE - 10;

    @Bean
    public RouterFunctionMapping userInterfaceRoute(
            @Value("${user-interface.uri}") final String uri) {
        final var route = GatewayRouterFunctions
            .route("user-interface")
            .route(RequestPredicates.method(HttpMethod.GET).and(RequestPredicates.path("/**")),
                HandlerFunctions.http())
            .before(BeforeFilterFunctions.uri(uri))
            .build();
        final var mapping = new RouterFunctionMapping(route);
        mapping.setOrder(AFTER_THE_CONTROLLERS);
        return mapping;
    }

}
```

Do not make the route a bean of the type `RouterFunction`, as the routes under `/wm/` are. Spring MVC
puts every such bean into its one `RouterFunctionMapping`, and that mapping has the order `-1`: it
is asked before the controllers. A route `/**` there would take every `GET` of the GUI API, whatever
`@Order` it has. Its own mapping with a late order comes after the controllers and after the routes
under `/wm/`. So the GUI API, the login and the proxy of the workflow modules answer as before. The
static files of the UI jar and the shell for an unknown path (`application.spa-default-file`) come
after this mapping, so with the route the dev server or the nginx answers them. The route sits behind the security chain like every other path: the paths
below `/assets/` stay open, everything else needs the login, see [The login](#the-login).

`application-ui-dev-server.yaml` holds one value, the address of the dev server on its fixed port:

```yaml
user-interface:
  uri: http://127.0.0.1:5173
```

Two things keep the route from reaching a real Vite dev server, and the small HTTP server of the
tests below shows neither of them:

- The address is `127.0.0.1`, not `localhost`. Vite listens on `localhost`, and Node.js may resolve
  that to the IPv6 address `::1` only, while Java tries `127.0.0.1` first. So the dev server sets
  `server.host: '127.0.0.1'`, see [project.md](project.md#developing-against-the-backend), and the
  backend names the same address.
- The client of the route speaks HTTP/1.1. The JDK `HttpClient` asks a server for HTTP/2 over a
  plain connection (`Upgrade: h2c`), and Vite does not answer that the way the client expects. The
  library brings the client of the gateway, and that client speaks HTTP/1.1 and passes a compressed
  answer on as it is, with its `Content-Encoding`. It holds for this route and for the proxy of
  the workflow modules below `/wm/` (`EXT-059`). So the application writes no client, no
  `ClientHttpRequestFactory` and no `spring.http.clients.imperative.factory` for the gateway. See
  [rough-edges.md](rough-edges.md#the-jdk-client-and-the-vite-dev-server).

With `behind`, `application.yaml` sets `user-interface.uri` to the address of the nginx from the
environment, and the profile `ui-dev-server` replaces it with the dev server. With `embedded`
only the profile sets it, so a deployment never has the route. With `in-front` there is no route
and no such profile.

The integration tests of the backend prove the route with a small HTTP server in place of the dev
server: with the profile, an unknown path and a path below `/assets/` reach that server, and the
GUI API does not; without the profile, the UI jar answers.

## The dashboard

Write this part only when the interview asks for a dashboard with charts by type (`Q-DASHBOARD`).
The GUI API cannot list the distinct values of a field. The types are not configured either,
because then every new workflow module would need a change of the configuration. So the
application writes a controller of its own, in `dashboard/`, which reads the types from the data
the user may see:

```
GET /gui/api/v1/dashboard/user-task-types
GET /gui/api/v1/dashboard/workflow-kinds
```

The first answers a list of `{ bpmnProcessId, taskDefinition, title }`, with `title` a text per
language from `taskDefinitionTitle`. The second answers a list of `{ caseKind }`, the values of
the field of `Q-CASE-KIND`: `bpmnProcessId`, the case title `title` (a text per language), or the
code below `details`. A case without a code gives `null`, which the user interface shows as
"other". The paths
lie below `/gui/api/v1`, so they need a login like every other path of the chain.

The controller asks MongoDB with one aggregation per call: the condition of the list, then a
`$group`. It takes the condition from the library, so the user sees the types of exactly the user
tasks and cases the lists show them. It uses the same visibility as the controller of the task
list and of the case list, see [The controllers of the lists](#the-controllers-of-the-lists), and
the mode `All`, so ended user tasks and cases count too. A type which only ended tasks have is on
the dashboard as well, with an open count of 0.

```java
final var visible = userTaskService.buildUserTasksCriteria(
    UserTaskVisibility.everythingTheUserMayWorkOn(currentUser),
    null,
    UserTaskService.RetrieveItemsMode.All,
    List.of());
final var aggregation = Aggregation.newAggregation(
    Aggregation.match(visible),
    Aggregation.group("bpmnProcessId", "taskDefinition")
        .first("taskDefinitionTitle").as("title"));
return mongoTemplate.aggregate(aggregation, UserTask.class, UserTaskType.class).getMappedResults();
```

For the cases it is `workflowlistService.buildWorkflowlistCriteria` with
`WorkflowVisibility.workflowsAddressedTo(currentUser)`, `null` as the timestamp,
`WorkflowlistService.RetrieveItemsMode.All`, and `null` for the conditions and the business ids,
then a `$group` by the field of `Q-CASE-KIND`. `UserTaskService` and `WorkflowlistService` are
beans of the library.

The answer holds no counts. The user interface asks the count endpoint of the GUI API for them,
up to 20 types in one request, see [behaviour.md](behaviour.md#the-screens). So a count follows the
same rules as the counts next to the primary filters.

With a tile "overdue" (`Q-DASHBOARD`), the same controller also counts the open user tasks whose
`dueDate` lies before now, with the same visibility and the mode `OpenTasksWithFollowUp`:
`GET /gui/api/v1/dashboard/overdue-user-tasks/count`. The integration tests of the backend prove
both lists and the count with two users who see different tasks.

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
  expects an array. Write a list as a map instead, keyed by the id, or as one string separated by
  commas, as `languages` is. The user interface puts the entries in the order it needs. The keys
  stay as they are written, `person-picture-uri` is not turned into `personPictureUri`.
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
  `container/src/main/resources/config/application-mailpit.yaml` is the template. The interval is
  one minute by default. A test that waits for a mail sets it to `1s` in its own properties, so it
  does not wait a minute.
- The mail of the library links the task and the task list with a path per language. Without a
  configuration it is `/aufgabe/<id>` and `/aufgaben` in German, and `/task/<id>` and `/tasks` in
  every other language. With `Q-ROUTES` the application names its own paths,
  `business-cockpit.notification.task-path.<language>` for the page of a task and
  `business-cockpit.notification.task-list-path.<language>` for the list. The key `default` holds
  the path of every language without a key of its own, and a key adds to the library's defaults:

  ```yaml
  business-cockpit:
    notification:
      task-path:
        default: tasks
        de: aufgaben
      task-list-path:
        default: tasks
        de: aufgaben
  ```

  The user interface still answers the library's paths as well, see
  [behaviour.md](behaviour.md#notifications), because a mail sent before the change carries them.
  Templates of its own are needed only for another wording of the mail.

The application tells the rest while it starts. A value it cannot run without ends the start, and
the message lists every missing key with an example. A feature that is not configured yet gives a
warning. So start it once and follow what it prints.

`application-local.yaml` holds the values for `docker-compose.yaml`: the address of MongoDB, of
Kafka, of Mailpit and of Keycloak, the client secret of the local realm, a JWT key for local use
only and the BPMS API user. The `README.md` starts the application with the profile `local`.
With `embedded` and `behind`, the profile `ui-dev-server` comes on top of it for a developer of the
user interface, see [The route to the user interface](#the-route-to-the-user-interface).

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
  `http://localhost:8080/login/oauth2/code/<id>`, the start page `http://localhost:8080/` as its
  post-logout redirect URI (the attribute `post.logout.redirect.uris`), and the secret that
  `application-local.yaml` names. Both lists also hold `http://localhost:*`. The integration tests
  start the application on a free port, and without that entry Keycloak refuses their login and
  their logout as an invalid redirect,
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
- the logout. With `oidc`, `/logout` deletes both cookies and sends the browser to the
  `end_session_endpoint` of Keycloak with `id_token_hint`, and the next request asks for a new login
  (`BE-032`, `AUTH-007`),
- a browser that goes away. The test opens `GET /gui/api/v1/updates` with a login, closes the
  connection and waits for the next ping. The log, read with Spring Boot's `OutputCaptureExtension`,
  then has no `AuthenticationCredentialsNotFoundException`, and no error or warning about the
  closed connection (`BE-033`, `BE-034`),
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

Two settings keep them fast and right:

- A test that adds a user or a group to Keycloak or LDAP first empties the cache of the user
  directory, with `DELETE /actuator/userdirectorycache`. Otherwise the directory answers from its
  cache, for up to 14 hours, and the new user has no groups, see
  [The user directory](#the-user-directory). The test properties expose this endpoint
  (`management.endpoints.web.exposure.include`), and the test sends the request with a login, like
  any request of the chain.
- A test that waits for a notification mail sets `business-cockpit.notification.interval` to `1s`,
  see [The configuration](#the-configuration).

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
- how a new version of the user interface gets in: a new value of `ui.version`,
- with `embedded` and `behind`, a section "Developing the user interface": start the application
  with the profiles `local,ui-dev-server`, for example
  `mvn spring-boot:run -Dspring-boot.run.profiles=local,ui-dev-server`, run `npm run dev` in the
  UI repository, and open the address of the backend, not the one of the dev server. It names the
  address of the dev server in `application-ui-dev-server.yaml` and says that the backend then
  serves no file of the UI jar.


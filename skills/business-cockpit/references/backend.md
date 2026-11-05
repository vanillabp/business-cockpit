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
1. [The layout](#the-layout)
1. [The parent pom](#the-parent-pom)
1. [The application](#the-application)
1. [The controllers of the lists](#the-controllers-of-the-lists)
1. [The login](#the-login)
1. [The BPMS API](#the-bpms-api)
1. [The user directory](#the-user-directory)
1. [The configuration](#the-configuration)
1. [The docker-compose.yaml](#the-docker-composeyaml)
1. [The test that starts the application](#the-test-that-starts-the-application)
1. [The README](#the-readme)

## Spring Boot only

The cockpit backend exists for Spring Boot only. The library is a Spring MVC application on virtual
threads. The Quarkus code in the `business-cockpit` repository belongs to the extension of a
workflow module, not to the cockpit. So the skill creates a Spring Boot backend and asks no question
about the framework. `Q-BACKEND-FRAMEWORK` is Spring Boot whenever the skill creates the backend.

## The layout

```
pom.xml                         the parent: versions, the BOMs, the two modules
types/
  pom.xml                       downloads <scope>/bc-types, writes and compiles UiUriTypes.java
application/
  pom.xml                       business-cockpit, the UI jar, the optional starters
  src/main/java/<package>/
    CockpitApplication.java     the main class
    lists/                      one controller per list of Q-LIST-CONTROLLERS
    security/                   the two security chains and userDetailsProvider
    users/                      the user directory and its two mappers
  src/main/resources/config/
    application.yaml            what every environment shares
    application-local.yaml      the values for docker-compose.yaml
  src/test/java/<package>/      the test that starts the application
docker-compose.yaml             MongoDB, Kafka, Mailpit, and Keycloak for Q-LOGIN-KIND oidc
keycloak/
  realm.json                    the realm Keycloak imports at the start, for Q-LOGIN-KIND oidc
README.md                       how to start it, and the order of the builds
```

The module `types` builds the types jar for the workflow modules, see
[project.md](project.md#the-types-jar). The module `application` is the counterpart of `container`.
`<package>` is the Java package of `Q-BACKEND-COORDINATES`.

## The parent pom

The library's own parent is `io.vanillabp:release-parent`, which carries no Spring. So the
library pins its Spring versions through BOMs, and the application has to use the same ones. Take
them from the root `pom.xml` of the `business-cockpit` repository at the tag of `Q-COCKPIT-VERSION`,
and import them in the `dependencyManagement` of the parent:

- `org.springframework.boot:spring-boot-dependencies`,
- `org.springframework.cloud:spring-cloud-dependencies`, because the library brings
  `spring-cloud-starter-gateway-server-webmvc` for its proxy under `/wm/`,
- `io.vanillabp:vanillabp-bom`, in the version the library's modules import.

Pin `spring-boot-maven-plugin` to the version of `spring-boot-dependencies`, the way the library
does, because no `spring-boot-starter-parent` gives it a version. The parent also sets the Java
version of `Q-JAVA-VERSION` and the property `ui.version`, the version of the user interface. Both
modules read `ui.version`: `types` downloads `<scope>/bc-types` of that version, and `application`
takes the UI jar of that version.

A snapshot of the library pulls snapshots of `com.phactum.mongodb:mongodb-changesets` from GitHub
Packages, which asks for credentials. A release of the library needs no such repository. Propose a
release version in `Q-COCKPIT-VERSION`, and name the repository in the `README.md` only when the
user takes a snapshot.

## The application

The module `application` depends on:

- `io.vanillabp.businesscockpit:business-cockpit` of `Q-COCKPIT-VERSION`,
- the UI jar of `Q-UI-JAR` in the version `ui.version`, with scope `runtime`, when `Q-HOSTING` is
  `embedded`,
- `spring-boot-starter-oauth2-client` when `Q-LOGIN-KIND` is `oidc`,
- `spring-kafka` and `io.vanillabp.businesscockpit:bpms-protobuf-api` of `Q-COCKPIT-VERSION` when
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

| Controller           | Path                       | Abstract class                            | Visibility                                                   | Generated when |
|----------------------|----------------------------|-------------------------------------------|--------------------------------------------------------------|----------------|
| task list            | `/gui/api/v1`              | `AbstractUserTaskListGuiApiController`    | `UserTaskVisibility.everythingTheUserMayWorkOn(currentUser)` | always, it serves `/tasks` |
| the user's own tasks | `/gui/api/v1/current-user` | `AbstractUserTaskListGuiApiController`    | `UserTaskVisibility.onlyWhatIsTheUsersOwn(currentUser)`      | always, it serves `/my-tasks` |
| tasks of the roles   | `/gui/api/v1/user-roles`   | `AbstractUserTaskListGuiApiController`    | `UserTaskVisibility.whatTheUsersGroupsMayTake(currentUser)`  | `Q-SUB-LISTS` has sub-lists by role |
| case list            | `/gui/api/v1`              | `AbstractWorkflowListGuiApiController`    | `WorkflowVisibility.workflowsAddressedTo(currentUser)`       | always |
| workflow modules     | `/gui/api/v1`              | `AbstractWorkflowModulesGuiApiController` | `WorkflowModuleVisibility.modulesAddressedTo(currentUser)`   | always, the loader needs it |

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
Spring Security's `oauth2Login` handles the redirect, and its `securityContextRepository` is the
`JwtSecurityContextRepository`, so the login ends in the cockpit's cookie:

```java
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
    .oauth2Login(login -> login
        .securityContextRepository(jwtSecurityContextRepository))
    // the GUI API answers 401 instead of a redirect to the provider, so the user interface
    // notices a lost login and sends the whole browser to the login
    .exceptionHandling(exceptions -> exceptions
        .defaultAuthenticationEntryPointFor(
            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
            PathPatternRequestMatcher.withDefaults().matcher("/gui/api/**")))
    .logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessHandler(jwtLogoutSuccessHandler))
    .addFilterAfter(new PassiveJwtSecurityFilter(properties.getJwt(), jwtMapper),
        BasicAuthenticationFilter.class)
    .addFilterAfter(new JwtRenewalFilter(properties.getJwt(), jwtMapper, updatesRequestMatcher),
        PassiveJwtSecurityFilter.class);
```

The groups of the user come from a claim of the token, by default `groups` (`Q-OIDC-PROVIDER`).
Map them to authorities with a `GrantedAuthoritiesMapper` of the OAuth2 login, so they reach the
cockpit's token as its groups. The id of the user is the claim `preferred_username` by default, set
as `user-name-attribute` of the provider. The provider is configured with the usual Spring Boot keys
below `spring.security.oauth2.client.registration.<id>` and `.provider.<id>`, the client secret
from an environment variable. The login paths are Spring Security's: `/oauth2/authorization/<id>` and
`/login/oauth2/code/<id>`. They go into `Q-LOGIN`.

A request of the GUI API without a login answers 401, not a redirect. A `fetch` would follow the
redirect to the provider and fail there. With a 401 the user interface sends the whole browser to
the login, see [gui-api.md](gui-api.md#staying-logged-in).

The wiki shows this chain, but the cockpit has no test for an OIDC login yet. Write the
end-to-end test of the login first (`BE-007`), before anything builds on it, see
[rough-edges.md](rough-edges.md#what-needs-a-newer-cockpit).

### A login form

The user logs in with a form of the backend. Spring Security's `formLogin` takes the place of
`oauth2Login`, with the same `securityContextRepository`. The users and their passwords come from
the user directory, see [The user directory](#the-user-directory). The login paths are `/login` and
`/logout`. This way is not proven by a test of the cockpit either.

### The token travels in a cookie

The cockpit reads the token from its cookie only. `PassiveJwtSecurityFilter` knows no
`Authorization` header. So the generated backend always uses the cookie, and `Q-TOKEN` is `cookie`.
If the user asks for the header, tell them that the cockpit cannot read it today. Do not generate
a filter of your own for it. Without the header, `Q-HOSTING` `in-front` has no reason left, see
[SKILL.md](../SKILL.md#where-the-user-interface-runs).

The same configuration class writes the bean `userDetailsProvider`, a `JwtUserDetailsProvider`.
The library needs it and brings none. Without it the application does not start.

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

- `io.vanillabp.cockpit.users.UserDetailsProvider`: finds users for a search, lists all users and
  gets one by id. The user interface needs it to propose a task to a person and to show a person.
- `io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper`: turns a user or a group into the
  `Person` and `Group` of the GUI API.
- `io.vanillabp.cockpit.users.model.PersonAndGroupMapper`: turns them into the person and group
  the cockpit stores.

`DerivedCockpitApplication` in the tests of `container` shows all three on a fixed list of users.
Where the list comes from is `Q-USER-DIRECTORY`:

- `configuration`, the default: a list of users with their groups in `application.yaml`, below a
  prefix of the application, read with `@ConfigurationProperties`. With `Q-LOGIN-KIND` `form` it
  also holds each password as a `bcrypt` hash. It is enough for a start and for the tests. For
  production the organisation replaces it.
- `keycloak`: the users and groups of the Keycloak realm, read through Keycloak's admin REST API
  with a service account. The skill writes the client for it in the application.
- `ldap`: the users and groups of a directory, read with Spring LDAP.

The users of the directory and the users of the login must be the same people with the same ids.
With OIDC the id is the claim of `Q-OIDC-PROVIDER`, by default `preferred_username`.

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
    languages: [ en, de ]
  jwt:
    hmacSHA256-base64: ${COCKPIT_JWT_KEY}
```

- `spring.mongodb.uri` names MongoDB. Spring Boot 4 no longer reads `spring.data.mongodb`.
- `business-cockpit.jwt.hmacSHA256-base64` is the key the cookie's token is signed with, 32 bytes
  in base64. It comes from the environment.
- `business-cockpit.additional-properties` carries the languages of `Q-LANGUAGES`, see
  [gui-api.md](gui-api.md#protection-and-login).
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

| Service  | Image                                | Why                                                                            | When |
|----------|--------------------------------------|--------------------------------------------------------------------------------|------|
| MongoDB  | `mongo`, the version of the template | the storage; a replica set, because change streams and the changesets need one | always |
| Kafka    | `apache/kafka`, in KRaft mode        | the reports of the workflow modules, when they come through Kafka              | always; the application reads it with `Q-KAFKA` |
| Mailpit  | `axllent/mailpit`                    | catches the notification e-mails, web interface on port 8025                   | always; the application sends with `Q-MAIL` |
| Keycloak | `quay.io/keycloak/keycloak`          | the OIDC provider, with the realm of `keycloak/` imported at the start         | `Q-LOGIN-KIND` is `oidc` |

Pin every image to a version. MongoDB runs as a single-node replica set. Start it with
`--replSet` and initiate the set in a health check or a setup container, so nobody has to type
`rs.initiate()`. The application connects with `directConnection=true`, because the member's name
inside the compose network does not resolve outside it.

Keycloak starts with `start-dev --import-realm`. The compose file mounts `keycloak/realm.json`
into `/opt/keycloak/data/import/`, so Keycloak creates the realm at its first start. The skill
writes that file for local development:

- a realm, by default `business-cockpit`,
- a confidential client, by default `business-cockpit`, with the redirect address
  `http://localhost:8080/login/oauth2/code/<id>` and the secret that `application-local.yaml`
  names,
- a mapper that writes the groups of a user into the claim `groups`,
- exactly one group and one user with a password, and the user is a member of the group. They are
  a template, not a set of test users.

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
The end-to-end tests of the UI repository prove the rest, against the running backend of
`docker-compose.yaml`.

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

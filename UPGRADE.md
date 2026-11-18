# Upgrading

What an application built on the Business Cockpit has to change when it moves to a new version of
the cockpit. Each section names one version and lists only what breaks or changes. Everything the
section does not name keeps working.

The wiki page [Releases](https://github.com/vanillabp/business-cockpit/wiki/Releases) tells the
whole story of a version. This file is the short list of what somebody has to touch.

The last release is 0.4.3. If your application is on it or on an older one, start at
[From 0.4.x and earlier](#from-04x-and-earlier).

## From 0.4.x and earlier

The last release of the cockpit is 0.4.3. 0.8.0 was planned as the first release on Spring MVC and
never came out. So the section [0.8.x to 1.0.0](#08x-to-100) is not a step after a release you
know. It is the second half of your way. If your application is on 0.4.x or earlier, work through
this section first and then through every entry of the next one. The one entry there which does not
concern you is the one about the snapshot artifact `support`.

This section names each big change once and links to where it is described in full. The changes
which make your application quietly do less than before are written out here.

### First Spring Boot 4.1 and Java 21, if you are on 0.3.x or earlier

0.4.0 moved the cockpit to Spring Boot 4.1 and Java 21, and 0.4.x is still reactive. An application
on 0.3.x or earlier does this upgrade first, with Spring Boot's own migration guide, and in a commit
of its own. It brings Jackson 3, and most MongoDB keys move from `spring.data.mongodb.*` to
`spring.mongodb.*`. The wiki page
[Releases](https://github.com/vanillabp/business-cockpit/wiki/Releases#up-to-040) has the short
version. An application on 0.4.x has this part behind it.

### The backend runs on Spring MVC instead of WebFlux

The cockpit is no longer reactive. It runs on Spring MVC with virtual threads. What your application
overrides loses `Mono`, `Flux` and `ServerWebExchange`. `SecurityFilterChain` replaces
`SecurityWebFilterChain`, `UserContext` replaces `ReactiveUserContext`, and `MongoTemplate`
replaces `ReactiveMongoTemplate`. The request matchers of `WebSecurityConfiguration` have new
names, and that breaks a security chain of your own when it compiles. The MongoDB data stays as it
is, and the REST APIs keep their OpenAPI files. So a workflow module needs nothing for this step.

The skill `business-cockpit-upgrade-to-blocking` in the
[skills repository](https://github.com/vanillabp/skills) does this step with you. It looks at your
application first and lists what it found, before it changes anything. The wiki page
[Releases](https://github.com/vanillabp/business-cockpit/wiki/Releases#up-to-080) says what the
step costs. The skill covers the move to Spring MVC only. The rest of this file still applies after
it.

### Depend on `business-cockpit`, not on `container`

Up to 0.4.x, `io.vanillabp.businesscockpit:container` was the library and the runnable application
in one jar. Now the library is `io.vanillabp.businesscockpit:business-cockpit`, and
`business-cockpit-bom` names its version. `container` is the application which comes with the
cockpit. Do not depend on it. Read it, because it is the example of everything your application
writes itself now: the main class, the list controllers, the login, the user directory and the
configuration. The wiki page
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit#what-the-container-brings-and-you-write-yourself)
has the full table.

Your main class carries `@SpringBootApplication` and starts the application itself.
`BusinessCockpitApplication` is deprecated and empty. A class which still extends it compiles and
works, but the class goes away in a release after 1.0. So drop the `extends`, see
[Setting the application up](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit#setting-the-application-up).

### The five list controllers are yours to write

Up to 0.4.x, `container` brought five concrete controllers of the GUI API, the API the user
interface calls. They stayed in `container`. The library has only their abstract base classes. So
without controllers of your own, nobody answers the lists. The application starts, the user
interface loads, and it shows no tasks, no cases and no workflow modules. The log does not say why.

Write one subclass for each list your user interface asks for. Each of them answers one question:
who may see what. The five of `container` are the template, and each is a few lines long:

|                   class in `container`                   |     `@RequestMapping`      |                base class                 |                       what it returns                        |
|----------------------------------------------------------|----------------------------|-------------------------------------------|--------------------------------------------------------------|
| `tasklist.api.v1.GuiApiController`                       | `/gui/api/v1`              | `AbstractUserTaskListGuiApiController`    | `UserTaskVisibility.everythingTheUserMayWorkOn(currentUser)` |
| `tasklist.api.v1.CurrentUsersTasksGuiApiController`      | `/gui/api/v1/current-user` | `AbstractUserTaskListGuiApiController`    | `UserTaskVisibility.onlyWhatIsTheUsersOwn(currentUser)`      |
| `tasklist.api.v1.UsersRoleAssignedTasksGuiApiController` | `/gui/api/v1/user-roles`   | `AbstractUserTaskListGuiApiController`    | `UserTaskVisibility.whatTheUsersGroupsMayTake(currentUser)`  |
| `workflowlist.api.v1.GuiApiController`                   | `/gui/api/v1`              | `AbstractWorkflowListGuiApiController`    | `WorkflowVisibility.workflowsAddressedTo(currentUser)`       |
| `workflowmodules.api.v1.GuiApiController`                | `/gui/api/v1`              | `AbstractWorkflowModulesGuiApiController` | `WorkflowModuleVisibility.modulesAddressedTo(currentUser)`   |

The packages start with `io.vanillabp.cockpit`. Give each controller a bean name of its own, as
`container` does, because three of the classes have the same simple name.

The workflow list of 0.4.x showed every workflow to everybody. The one of `container` now shows a
user only the workflows which name them in `accessibleToUsers` or one of their groups in
`accessibleToGroups`, and the workflows which name neither. If your users have to see every
workflow, return `WorkflowVisibility.everyWorkflow()`. The wiki page
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit#the-controllers-you-write-yourself)
says more.

### A list controller answers with a visibility

Up to 0.4.x, the three abstract classes had an abstract method for each read and each action, and a
subclass wrote all of them. Now each class has one abstract method, and it returns a visibility:

|                base class                 |             abstract method             |          returns           |
|-------------------------------------------|-----------------------------------------|----------------------------|
| `AbstractUserTaskListGuiApiController`    | `userTasksVisibleTo(UserDetails)`       | `UserTaskVisibility`       |
| `AbstractWorkflowListGuiApiController`    | `workflowsVisibleTo(UserDetails)`       | `WorkflowVisibility`       |
| `AbstractWorkflowModulesGuiApiController` | `workflowModulesVisibleTo(UserDetails)` | `WorkflowModuleVisibility` |

The base class answers the list, the search, the count, a single entry and every action on one from
that visibility. So a task the list does not show cannot be opened, claimed or assigned by its id
either. Each of the three visibility types names the views the cockpit has ready, and you can build
your own where none of them fits.

What happens to the methods your subclass has from 0.4.x:

- `getUserTask`, `markAsRead`, `claimTask`, `claimTasks`, `assignTask`, `assignTasks`,
  `setFollowUpDate`, `getWorkflow` and `getWorkflowModules(UserDetails)` are no longer hooks. The
  base class does their work. Delete them. A method you leave behind without `@Override` compiles
  and is never called.
- `getUserTasks`, `getUserTasksUpdated` and `kwic` of the task list, and `getWorkflows`,
  `getWorkflowsUpdated`, `getUserTasksOfWorkflow` and `kwic` of the workflow list, are no longer
  abstract. Delete them as well, unless you read the data in a way of your own. An override takes
  the visibility as its first parameter instead of the current user, and returns `Page` or `List`
  instead of `Mono` or `Flux`. `countUserTasks` and `countWorkflows` are new hooks of the same kind.
  Write `@Override` on each override, so the compiler tells you when a parameter changes. The
  entries below name the parameters which changed after 0.4.x: the mode of
  [`OpenTasks` is gone](#opentasks-is-gone), the business ids of
  [The update of the list of workflows takes `businessIds`](#the-update-of-the-list-of-workflows-takes-businessids)
  and the workflow module ids of
  [A list can be limited to workflow modules](#a-list-can-be-limited-to-workflow-modules).

The methods of `UserTaskService` and `WorkflowlistService` which read a list take a
`UserTaskVisibility` or a `WorkflowVisibility` now, instead of the flag for entries which address
nobody and the lists of users and groups. The injected field `userContext` of the base classes is a
`UserContext` instead of a `ReactiveUserContext`.

### The update stream tells each tab only what it may see

`GET /gui/api/v1/updates` sends a different stream than in 0.4.x. A user interface of your own has
to follow four changes:

- An element no longer has `targetGroups`. It has `workflowModuleId` instead, see
  [A list can be limited to workflow modules](#a-list-can-be-limited-to-workflow-modules).
- `type` has the new value `RELOAD`, with an empty `id`. It means: load every list of this kind
  again. The cockpit sends it right after a stream opens, and again when the stream had to forget
  ids the browser may still show. A user interface which ignores it shows old lists after a
  reconnect.
- A stream gets only the changes of entities the user may see in one of their lists, and of entities
  the browser of this tab still shows. Up to 0.4.x every stream got every change. The visibility of
  your list controllers decides this as well.
- The stream ends when the sign-in expires. Without a sign-in it answers 401. The browser's
  `EventSource` opens a new stream after the end.

The cockpit's OpenAPI file describes the stream, see
[The update stream is in the OpenAPI file](#the-update-stream-is-in-the-openapi-file).

If your application publishes a `GuiEvent` of its own, the constructor changed. It was
`GuiEvent(source, targetGroups, event)`. It is `GuiEvent(kindOfEntity, entityId, event)` now. The
first value is the name of the event in the stream, for example `UserTask`. The cockpit decides who
gets an event of the kinds `UserTask` and `Workflow` by the id, with the visibility of the lists.
An event of another kind, and an event without an id, reaches every stream.

## 0.8.x to 1.0.0

### `commons` is gone: `support` and the library module take its classes

The artifact `io.vanillabp.businesscockpit:commons` no longer exists. There is no artifact under the
old name which points at the new ones. What a workflow module can use as well went into the new
artifact `io.vanillabp.businesscockpit:business-cockpit-spring-boot-support`, called `support` below. What only the cockpit uses went into the library
module `io.vanillabp.businesscockpit:business-cockpit`.

A cockpit application depends on `business-cockpit`, which brings `support` along. It changes its
imports and nothing in its POM. A workflow module or another service which depended on `commons`
depends on `support` instead. `support` brings no MongoDB and no Kafka.

`support` brings the cockpit's SPI, `io.vanillabp.businesscockpit:spi-for-java`, and through it the
SPI of VanillaBP. A workflow module which names the SPI itself may keep it or remove it. With
`support` as its only dependency, a workflow module compiles. To run, it still needs the cockpit's
adapter for its BPMS, `business-cockpit-*-adapter`. A workflow module which is a library of its own
takes the adapter in test scope, and the application which runs it brings the adapter.

One line per package. Replace the beginning of each import:

|                                      before                                      |                                   now                                    |      artifact      |
|----------------------------------------------------------------------------------|--------------------------------------------------------------------------|--------------------|
| `io.vanillabp.cockpit.commons.security.jwt`                                      | `io.vanillabp.cockpit.support.security.jwt`                              | `support`          |
| `io.vanillabp.cockpit.commons.security.usercontext.WorkflowModuleGroupHierarchy` | `io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleGroupHierarchy`  | `spi-for-java`     |
| `io.vanillabp.cockpit.commons.security.usercontext`                              | `io.vanillabp.cockpit.support.security.usercontext`                      | `support`          |
| `io.vanillabp.cockpit.commons.rest`                                              | `io.vanillabp.cockpit.support.rest`                                      | `support`          |
| `io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException`                | `io.vanillabp.cockpit.support.security.BcUnauthorizedException`          | `support`          |
| `io.vanillabp.cockpit.users.UserDetailsProvider`                                 | `io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService` | `support`          |
| `io.vanillabp.cockpit.users.UserDetailsImpl`                                     | `io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl`      | `support`          |
| `io.vanillabp.cockpit.commons.exceptions`                                        | `io.vanillabp.cockpit.exceptions`                                        | `business-cockpit` |
| `io.vanillabp.cockpit.commons.kafka`                                             | `io.vanillabp.cockpit.kafka`                                             | `business-cockpit` |
| `io.vanillabp.cockpit.commons.mapstruct`                                         | `io.vanillabp.cockpit.mapstruct`                                         | `business-cockpit` |
| `io.vanillabp.cockpit.commons.mongo`                                             | `io.vanillabp.cockpit.mongo`                                             | `business-cockpit` |
| `io.vanillabp.cockpit.commons.utils.DateTimeUtil`                                | `io.vanillabp.cockpit.mongo.converters.DateTimeUtil`                     | `business-cockpit` |
| `io.vanillabp.cockpit.commons.utils.LoggerFactory`                               | gone, see below                                                          |                    |

`WorkflowModuleGroupHierarchy` went to the cockpit's SPI, which `support` brings, because the
adapter reads it. The section
[`group-hierarchy-bean-name` becomes a bean](#group-hierarchy-bean-name-becomes-a-bean) says
what else changed about it.

`BcUnauthorizedException` left its siblings, because `UserContext` in `support` throws it and
`support` cannot see the library module.

`UserDetailsImpl` moved to `support` as well. It is the plain implementation of `UserDetails`,
which the cockpit and a workflow module both need. Before, a workflow module had to write its own
or depend on the library module.

The directory of the users got a new name on its way to `support`, so it is no longer confused with
the `UserDetailsProvider` which says who is calling. Its methods stayed. A controller which extends
`AbstractUserTaskListGuiApiController` finds it in the field `userDirectoryService` instead of
`userDetailsProvider`.

The two auto-configurations of the MongoDB package have new names as well. An application which
names one of them in `spring.autoconfigure.exclude` replaces the name, for the same reason as the
[changeset auto-configuration](#read-your-springautoconfigureexclude):

|                                    before                                    |                                 now                                  |
|------------------------------------------------------------------------------|----------------------------------------------------------------------|
| `io.vanillabp.cockpit.commons.mongo.BusinessCockpitMongoDbAutoConfiguration` | `io.vanillabp.cockpit.mongo.BusinessCockpitMongoDbAutoConfiguration` |
| `io.vanillabp.cockpit.commons.mongo.ChangesetPropertiesAutoConfiguration`    | `io.vanillabp.cockpit.mongo.ChangesetPropertiesAutoConfiguration`    |

They now come with `business-cockpit` only. A workflow module which had `commons` on its class path
got both of them, and with them the MongoDB settings of the cockpit. With `support` it gets neither.

### The snapshot artifact `support` is now `business-cockpit-spring-boot-support`

This concerns only a build which used an earlier snapshot of `main`. The module `support` got a level below it,
because a Quarkus version of it will follow. Its classes moved into the sub-module
`support/spring-boot`, which is published as
`io.vanillabp.businesscockpit:business-cockpit-spring-boot-support`. The artifact
`io.vanillabp.businesscockpit:support` is gone, and no artifact under the old name points at the new
one. `business-cockpit-bom` manages the new artifact. Replace the `artifactId` in the POM. The
packages stay `io.vanillabp.cockpit.support...`, so no import changes.

The module now also has `BcUnknownTaskException`, `BcTaskNotOpenException` and
`BcTaskClaimedByAnotherUserException` in `io.vanillabp.cockpit.support.usertask`, and
`BcUserTaskExceptionHandler`, which answers the first two with `404 Not Found` and the third with
`409 Conflict`. A workflow module which wrote its own exceptions for a task id it does not know, for
a task which is no longer open, or for a task which another user claimed, can use these. Nothing registers the
handler on its own: import it with `@Import(BcUserTaskExceptionHandler.class)`.

### The cockpit no longer injects a `Logger`

`commons` had a bean which handed an SLF4J `Logger` to every field or parameter of that type, and
the cockpit's auto-configuration imported it. The bean is gone. The cockpit's own classes use
Lombok's `@Slf4j` now. A class of the application which has `@Autowired Logger logger` fails at
the start with a missing bean of type `org.slf4j.Logger`. Write
`private static final Logger logger = LoggerFactory.getLogger(MyClass.class);` or use `@Slf4j`.

### `ClientsConfigurationBase` is gone: REST clients are HTTP service clients

The REST client of `support` built on OpenFeign and OkHttp is gone, with the whole package
`io.vanillabp.cockpit.support.rest.adapter`: `ClientsConfigurationBase`, `Client`,
`Authentication`, `OauthAuthentication`, `Proxy`, `Retry`, `BearerTokenBasedAuthInterceptor`,
`OauthBearerTokenHandler`, `OffsetDateTimeAwareExpander`, `TlsTruststoreUtil` and
`ApiVersionAware`. `support` brings neither OpenFeign nor OkHttp any more, and neither does the
BOM `business-cockpit-bom`. An application which uses OpenFeign or OkHttp for clients of its own
names them, with their versions, in its own POM.

A REST client is built the new way, in the package `io.vanillabp.cockpit.support.rest.httpservice`.
A client is an interface which the generator `spring` writes with the library
`spring-http-interface`, and Spring Framework turns it into a client. The requests go through the
JDK's own `java.net.http.HttpClient`. The section [REST clients](support/README.md#rest-clients) of
the support README shows the way step by step.

`support` brings `spring-boot-starter-restclient`, and the auto-configuration
`HttpServiceClientConventionsAutoConfiguration`. It configures every HTTP service group of the
application, so an application which already has groups of its own gets them checked at the start:
a group without `base-url`, or a key nobody reads, stops the start with a message which names the
key.

To move a client which extended `ClientsConfigurationBase`:

1. Generate the interface with the generator `spring` and the library `spring-http-interface`
   instead of `java` with `feign`.
2. Replace the configuration class which extends `ClientsConfigurationBase` by
   `@ImportHttpServices` and an `HttpServiceGroupPrefix` bean, which names the prefix of the
   properties class the client read before. Delete the properties class.
3. Rename the keys which changed, see the table below.
4. Catch Spring's exceptions instead of Feign's.

|                  Key of `Client`                   |                                    Key of an HTTP service group                                     |
|----------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `base-url`                                         | `base-url`. It has no placeholder default; a missing one stops the start                            |
| `connect-timeout: 1500`                            | `connect-timeout: 1500ms`, a duration                                                               |
| `read-timeout: 10000`                              | `read-timeout: 10s`, a duration                                                                     |
| `log`, `verify-ssl`                                | the same                                                                                            |
| `connection-pool-size`, the idle connections kept  | `connection-pool-size`, the connections open at most. Only Apache HttpClient 5 has a limit to raise |
| `proxy.*`, `retry.*`                               | the same. `proxy.port` has no default                                                               |
| `ssl-truststore-filename`, `-password`             | `ssl.bundle`, naming a Spring Boot SSL bundle `spring.ssl.bundle.jks.<name>.*`                      |
| `authentication.basic`, `.username`, `.password`   | `default-header.Authorization: Basic <base64 of user:password>`                                     |
| `authentication.oauth.base-url`                    | `oauth.token-uri`                                                                                   |
| `authentication.oauth.client-id`, `.client-secret` | `oauth.client-id`, `oauth.client-secret`                                                            |
| `authentication.oauth.basic: true`                 | `oauth.client-authentication-method: client_secret_basic`                                           |
| `authentication.oauth.proxy.*`, TLS keys           | none: token requests use the group's proxy and TLS                                                  |
| `additional-get-parameters`                        | none. It was never read                                                                             |

A token without `expires_in` is no longer used until the server answers `401`. It is kept for
`oauth.lifetime-without-expires-in`, one hour unless set. A token is replaced when a tenth of its
lifetime is left, but at most `oauth.renew-before` before it expires, 30 seconds unless set.

A caller of a moved client gets Spring's exceptions. An answer with an error status comes as
`HttpClientErrorException` or `HttpServerErrorException` instead of `feign.FeignException`, for
example `HttpClientErrorException.NotFound` instead of `FeignException.NotFound`. No answer at all
comes as `ResourceAccessException` instead of `feign.RetryableException`. A refused token request
comes as Spring Security's `OAuth2AuthorizationException`.

### The client of the BPMS API sends with the JDK's `HttpClient`

`bpms-api-client` is generated with the library `native` of the generator `java` instead of
`feign`. It brings no OpenFeign and no OkHttp. The messages keep their package and their names.
`BpmsApi` is a class now, created with `new BpmsApi(apiClient)`, and its methods throw the checked
`ApiException`, which carries the status, the headers and the body of the answer. The client
brings the path of the API along as `BpmsApiPath.PATH`. The REST transport of the adapters uses
the client this way, so a workflow module which only configures `vanillabp.cockpit.rest.*` changes
nothing: the keys, the messages and the token of the OAuth client credentials grant stay as they
were. The transport logs each report it sent at level `DEBUG`, with the logger
`io.vanillabp.cockpit.extension.transport.RestTransport`.

### The MongoDB migration is a library of its own

The cockpit no longer carries the MongoDB changeset mechanism. It uses
`com.phactum.mongodb:mongodb-changesets`, which does the same thing and is published on its own.
The `commons` module brings the dependency along, so an application which depends on the cockpit
gets it without naming it.

Nothing on disk changes. The collection is still called `ChangesetInformation`, a step is still
identified by the class name of its changeset bean plus the name of its method, and the cockpit's
own changeset beans stayed where they were. A database migrated by an earlier version runs no step
a second time.

### The write concern is yours now

The changeset mechanism used to set `WriteConcern.JOURNALED` on the application's `MongoTemplate`
while it migrated and never took it back. Every application built on the cockpit ran with that
value without asking for it. Now the library leaves the template as it found it, so the write
concern is the one the application configures.

The delivered `container` configures it. A custom cockpit adds:

```yaml
mongodb:
  write-concern: majority
  write-concern-journal: true
```

Without it the cockpit writes with one acknowledging node and warns about that on every start. A
write which only the primary has is lost when that primary steps down, and the user task the
cockpit has already confirmed to the BPMS adapter never appears.

Write the value here and not into `spring.mongodb.uri`. The cockpit's `MongoTemplate` checks the
result of every write, and Spring Data replaces the write concern of the connection with a plain
acknowledged write while it does that. `mongodb.write-concern: 0` ends the start.
[The write concern](https://github.com/vanillabp/business-cockpit/wiki/Running-the-Business-Cockpit#the-write-concern)
in the wiki has the list of what each value costs.

### A table of the version 1 Camunda 8 adapter stays behind

The version 1 adapters left this repository with 1.0.0. One of them leaves something in the
database: the Camunda 8 adapter created a table `CAMUNDA8_BC_PROCESS_INSTANCES` through its
Liquibase changelog, and nothing ever wrote a row into it. A changelog which drops the table was
written back then and never switched on, so the table is still there and no version of the cockpit
removes it.

An application which ran the version 1 Camunda 8 adapter can drop the table by hand. Nothing reads
it, nothing writes it, and dropping it costs no data.

### Read your `spring.autoconfigure.exclude`

The auto-configuration has a new fully qualified name.

|                                   before                                   |                             now                             |
|----------------------------------------------------------------------------|-------------------------------------------------------------|
| `io.vanillabp.cockpit.commons.mongo.changesets.ChangesetAutoConfiguration` | `com.phactum.mongodb.changesets.ChangesetAutoConfiguration` |

An application which switched the migration off by the old name switches nothing off any more, and
it starts anyway. Spring Boot refuses an exclude entry only while the class it names is on the
class path. The old class is gone, so the entry is read and ignored, and the migration runs in an
application which had it switched off. Replace the name.

### A changeset bean of your own changes two imports

The annotations are called `DbChangeset` and `DbChangesetConfiguration`. They were called
`Changeset` and `ChangesetConfiguration` before 1.0, and they sit in another package now.

|                                 before                                 |                            now                            |
|------------------------------------------------------------------------|-----------------------------------------------------------|
| `io.vanillabp.cockpit.commons.mongo.changesets.Changeset`              | `com.phactum.mongodb.changesets.DbChangeset`              |
| `io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration` | `com.phactum.mongodb.changesets.DbChangesetConfiguration` |

Nothing else about such a bean changes. It is still a Spring bean, its steps are still public
methods which take a `MongoTemplate` or nothing, and they still answer with the command which
undoes what they did.

### A bean which waits for the migration asks for another type

A bean used to take a parameter of the auto-configuration to say "build me after the migration".
Now the parameter has to be `com.phactum.mongodb.changesets.ChangesetApplier`. The
auto-configuration only does the wiring, and a configuration class is built before the beans it
declares. So a parameter of the auto-configuration's type compiles and starts, but it guarantees
nothing.

### `ChangesetInformation.timestamp` is an `Instant`

It was an `OffsetDateTime`. The document is the same either way, a BSON date, so there is no data
migration. Only code which reads the field in Java is affected.

### The workflow provider API is gone

The artifacts `io.vanillabp.businesscockpit:workflow-provider-api-client` and
`io.vanillabp.businesscockpit:workflow-provider-api-server` held no code, and the cockpit never
called the API. Both artifacts and the OpenAPI file are gone. Remove the artifacts from your POM if
you named them. A workflow module may still send `workflowProviderApiUriPath` when it registers;
the cockpit stores it and does nothing with it.

### No configuration key changes

The library reads `mongodb.changesets.mode`, and the cockpit hands it the value of its own
`mongodb.mode`. So `mongodb.mode` stays the key to set, and `mongodb.changesets.mode` must not be
set: the cockpit binds the prefix `mongodb` strictly, and a key below it which the cockpit does not
know ends the start.

### `WorkflowDetailsProviders` is gone

The annotation `io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProviders` is no longer
published. It was the container of a repeatable annotation, but `@WorkflowDetailsProvider` is not
repeatable, so Java never put a value into it and nothing ever read one. Delete the import if your
code carries it. Nothing else changes, because the type could not be reached from a running
application.

A BPMN process has one workflow details provider, and that stays as it is. The method serves the
whole process, so there is nothing to repeat. `@UserTaskDetailsProvider` is a different case: it
stays repeatable and keeps its container `UserTaskDetailsProviders`.

### Every report names an initiator, and one new key says who answers it

The initiator is the user who caused what is reported: who started a case, who took a task. In
version 1 the field was optional, and most applications left it empty. Now every report carries a
value, and a new key says where the value comes from:

```yaml
vanillabp:
  cockpit:
    initiator-source: by-application
```

The key has no default, so write it before you start the application. `by-application` means your
code sets the initiator in a method annotated with `@WorkflowDetailsProvider` or
`@UserTaskDetailsProvider`. `system` means this workflow module knows no action a user causes, and
every report of it then names `system`. A workflow module may say something of its own at
`vanillabp.workflow-modules.<moduleId>.cockpit.initiator-source`, and a single workflow at
`vanillabp.workflow-modules.<moduleId>.workflows.<process>.cockpit.initiator-source`.

With `by-application` a report which names nobody after your provider ran fails, and it takes the
engine's work with it, because the report is built in the transaction of the event. The constant
`io.vanillabp.spi.cockpit.Initiator.SYSTEM` is the way to say that no user caused this one event. It
is also how you drop a value Camunda 7 prefilled: calling `setInitiator(null)` looks exactly like a
provider which did nothing.

Your existing cases keep what they have. Nothing is filled in afterwards, because nobody can find
out later who started a case which ran without an initiator. That is also why the key has no
default: a value which cannot be filled in later is worth one line of configuration.

### A workflow module keeps its templates in its own directory

A workflow module now finds its title templates in `<workflow-module-id>/business-cockpit-templates`
of the classpath, with nothing configured. All of them go there, also `workflow-title.ftl`,
`workflow-fulltext-search.ftl` and a `task-fulltext-search.ftl` which serves every task. A module
which keeps them somewhere else names the directory in its own
`vanillabp.workflow-modules.<moduleId>.cockpit.template-loader-path`.

`vanillabp.cockpit.template-loader-path` keeps working for every module which names no directory of
its own. While it is set, the default directory is not looked at, so nothing changes for an
application which uses it. To move a module's templates into the default directory, remove the key
from the application, or give the other modules a `template-loader-path` of their own.

One thing may change without a key: a module with Freemarker on its classpath, without the key of
the application, and with a directory `<moduleId>/business-cockpit-templates` which held something
else. Its templates are rendered now. Before, the cockpit showed the BPMN names.

### `uiUriType` is a string, and the address is the user interface's business

`uiUriType` was an enum with two values in the published schemas. It is a plain string now, in
`bpms-api` v1 and v1_1 and in `official-gui-api` v1. `EXTERNAL` and `WEBPACK_MF_REACT` are still
valid strings, so a workflow module which reports one of them needs no change.

The field stays required. For a module whose user interface is federated anyway, leaving it out
looks tempting. But then the database fills up with entries that say nothing. On the day a second
value shows up, nobody could tell what an empty one was meant to say. One line of configuration
answers that question once.

The start of an application still checks `ui-uri-type`, and it only checks that a value is there.
Any string passes. The message where nothing is configured names no values. It sends you to the
cockpit user interface instead.

The names are a convention between a workflow module and the user interface which loads it.
VanillaBP passes the value on and never reads it, so it has no list of names and no class with
constants for them. `WEBPACK_MF_REACT` was the value of the React user interface which came with
the cockpit up to 0.8. It is still accepted, like any other string. Which names a user interface
understands is written down with that user interface. If your user interface comes with a small
jar of constants, your workflow modules can use that.

What changes for a user interface is `uiUri` in the GUI API. Before 1.0 the cockpit built the whole
address: `/wm/<workflowModuleId>` plus the path the workflow module reported. Only for the type
`EXTERNAL` it handed out the reported value unchanged. Now the reported path is handed out in every
case, and the user interface builds the address. A cockpit user interface follows one rule. A path
which starts with `http://` or `https://` is an address already and is opened as it is. Every other
path gets `/wm/<workflowModuleId>/` in front of it. `workflowModuleId` and `workflowModuleUri` are in
the same answer, so nothing has to be looked up for it.

So the type no longer decides whether a path is an address. A workflow module whose pages live in
another application reports the whole address in `ui-uri-path`, or per case through `setUiUriPath`
of `PrefilledUserTaskDetails` and `PrefilledWorkflowDetails`. It is opened without the route in
front, whatever `ui-uri-type` says.

If you built your own user interface against the old answer, there are two ways out.

The first is to do it in the browser. Your code looks at the text it got. Where it starts with
neither `/wm/` nor `http://` or `https://`, it adds the proxy route of the module. That keeps
working against both versions of the cockpit, which matters while the two are rolled out one after
the other.

The second is to change what is stored, so the answer looks like it used to. Report the whole
address as `ui-uri-path`, `/wm/taxi-ride/remoteEntry.js` rather than `/remoteEntry.js`, and update
the entries which are already there in one block. This way only fits a user interface of your own
which does not follow the rule above. One which follows it would put the route in front a second
time. Three things are worth knowing before you do that.

The field exists twice. A user task has one and a workflow has one, so two MongoDB collections carry
it, `UserTask` and `Workflow`.

The database holds the reported path, never the computed address. Only `GuiApiMapper` built the
address, at the moment it answered, so nothing in the database has to be taken apart.

Entries whose path starts with `http://` or `https://` must be left alone. Their path is the whole
address already, the cockpit never prefixed it, and prefixing it now would send the browser to a
route of the cockpit which does not exist. So an update in one block has to leave them out. Up to
0.8 the type `EXTERNAL` said the same thing. If an entry of that type has a path without `http://`
or `https://`, give it the whole address in the same block.

### A report no longer changes who sees a user task

The report which creates a user task in the cockpit sets its assignee, its candidate users, its
candidate groups and its excluded candidate users. Later reports, an update or an end, do not change
them any more. Before, an update replaced the candidate groups and the excluded users, and an update
which named an assignee replaced the assignee. Only taking a task over and assigning it in the
cockpit still change the assignee and the candidate users. `admittedUsers` is still read from every
report. A task which names nobody is shown to everybody, and it takes the names from the first later
report which has some.

A workflow module which handed a task to other people by reporting it again has to end the task and
enter it again, for instance with a boundary event. The new task gets the new values. Nothing else
needs to change: the details provider may keep setting the values for every report.

### The cockpit jar carries no user interface

`io.vanillabp.businesscockpit:business-cockpit` and the runnable jar of `container` no longer
contain the React user interface. The repository holds no user interface any more, and no npm
package of the cockpit is released with 1.0.0. If your application depends on `business-cockpit`,
it serves no user interface, and a path no API knows gets 404 instead of the single-page
application.

If your application brings a user interface of its own, name its `index.html` with
`application.spa-default-file`, for example `classpath:/static/index.html`, and unknown paths get it
as before. The library has no value for the key any more, see "Two values are the application's" below. Decisions 62 and 63 in [DECISIONS.md](./DECISIONS.md) have the details.

### `OpenTasks` is gone

The GUI API knew two names for the same list of user tasks: `OpenTasks` and
`OpenTasksWithFollowUp`. `OpenTasks` is gone. A request which still sends it gets 400. Send
`OpenTasksWithFollowUp` instead. The list is the same: every task which has not ended, with or
without a follow-up date. Its partner is `OpenTasksWithoutFollowUp`, which leaves out the tasks with
a follow-up date in the future.

A request without `mode` now gets the same list for the first page and for the update:

- `POST /usertask` without `mode` shows no ended tasks any more. Before, it showed all tasks.
- `POST /workflow` without `mode` shows no ended workflows any more. Before, it showed all workflows.
- `POST /workflow/{workflowId}/usertasks` reads the whole `mode` now, like `POST /usertask`. Before,
  it only looked for `OpenTasks` and answered every other mode with all tasks of the workflow,
  `ClosedTasksOnly` included. Without `mode` it shows the open tasks of the workflow.

If you need the ended ones, send `All`.

A subclass of `AbstractWorkflowListGuiApiController` which overrides `getUserTasksOfWorkflow` gets a
mode instead of the `boolean` which said whether to leave out the ended tasks. The same goes for a
caller of `UserTaskService.getUserTasksOfWorkflow`.

The dev-shell simulator still shows all tasks and all workflows without `mode`. It only fills the
list a developer picks a task from, and it holds a few tasks only.

### An ended user task refuses claim and assign

Claiming a user task, giving it back, assigning it and unassigning it now answer 409 for a task which
has ended. Before, they answered 200 and changed the task. The list forms do the same: if one of the
tasks the user may see has ended, the request changes none of them and answers 409. A user interface
which relied on 200 has to switch these actions off for a task whose `endedAt` is set.

Marking as read and as unread still work for an ended task, and so does `GET /usertask/{id}` with
`markAsRead=true`. A read marker only changes how the list shows the row.

The list forms never answered 404, although the OpenAPI file said so. The file no longer says it.

### Two count endpoints

`POST /usertask/count` and `POST /workflow/count` are new. Each takes a list of filters, the same
filters as its list, and answers one count per filter, with no rows and without sorting. Nothing
else changes. A user interface which counted with a page of size 1 can switch when it likes. Two new
changesets add indexes on the fields which decide who sees a user task or a workflow. On a large
database the first start after the upgrade takes a little longer while MongoDB builds them.

### Signing in is the application's

The library `business-cockpit` brings no login any more. The two security chains `guiHttpSecurity`
and `bpmsApiHttpSecurity`, the users with the password `test` and the bean `userDetailsProvider`
moved into the module `container`. The runnable jar of `container` behaves as before.

If your application depends on `business-cockpit` and relied on what the library brought, write
these three beans yourself. The classes `GuiSecurityConfiguration` and
`BpmsApiSecurityConfiguration` in `container` are the template, and the wiki page
[Security](https://github.com/vanillabp/business-cockpit/wiki/Security) says what every chain has to
keep.

- Without `userDetailsProvider` the application does not start. The failure report shows the bean
  to write.
- Without any chain of your own, Spring Boot adds its default chain. It asks for a login on every
  path, with a password it generates and prints. Your users and the BPMS adapters cannot log in.
- With some chains of your own but not these two, a request which none of your chains takes is not
  checked at all.

On every start the log names each of the two chains your application does not have. The keys
`bpms-api.realm-name`, `bpms-api.username` and `bpms-api.password` stay. The library no longer
builds a chain from them, the chain of `container` does.

The constants `BPMS_API_ROLE` and `BPMS_API_AUTHORITY` moved from
`io.vanillabp.cockpit.bpms.BpmsApiWebSecurityConfiguration` to
`io.vanillabp.cockpit.bpms.BpmsApiAuthority`. The BPMS API still answers only a caller with the
authority `ROLE_BPMS-API`.

The users of the dev-shell simulator log in with `test` now, as they do at the cockpit. Before, it
was `Secure_123`.

### The dev-shell simulator takes version 1.1 of the BPMS API only

The dev-shell simulator answers `/bpms/api/v1_1` only, and `/bpms/api/v1` answers 404 there. A
workflow module whose adapter still reports through version 1 needs a current adapter to be tried
in the dev shell. The cockpit itself still takes version 1.

### Two values are the application's

The library has no value for `application.spa-default-file` and `business-cockpit.default-locale`
any more. `container` sets both in its `application.yaml`.

- `application.spa-default-file` was `classpath:/static/index.html`. Without it, every path no API
  knows gets 404, and the log says so on every start. If your application has a user interface,
  write the key.
- `business-cockpit.default-locale` was German. `container` sets English now, so the delivered
  application writes a mail in English to a user who has no language of their own. Without the
  key, such a mail uses the template without a language in its name, and the log warns while
  e-mail notifications are switched on. If your application relied on German, write
  `business-cockpit.default-locale: de`.

### The update stream is in the OpenAPI file

`GET /gui/api/v1/updates` is now described in the cockpit's own OpenAPI file,
`business-cockpit/src/main/resources/api/gui/v1.yaml`, with its events and their data. What the
stream sends changed after 0.4.x, see
[The update stream tells each tab only what it may see](#the-update-stream-tells-each-tab-only-what-it-may-see).

Two classes moved to another jar, in the same package `io.vanillabp.cockpit.gui.api.v1`.

The operation no longer declares the header `X-Refresh-Token`. The cockpit never read it. So the
generated Java clients of the official GUI API call `currentUser()` without an argument, and a
controller which implements `OfficialCurrentUserApi` overrides `currentUser()`. The operation is in
the official API only since this version, so only code written against a snapshot has to change.
`UserTaskEvent` and `WorkflowEvent` are no longer in `official-gui-api-server`. They are in
`business-cockpit` now, and their `type` is an enum instead of a string. A client generated from the
official file alone no longer has them. Generate it from both files. `Person` and `Group` used to
be in both jars. Now they are only in `official-gui-api-server`. `UserAllOf` is gone, nothing used
it. The generator of the cockpit's file is now 7.24.0, the same as for the official file.

### A task and a case say why they ended

`UserTask` and `Workflow` of the GUI API carry `endReason`: `COMPLETED` or `CANCELLED`, and nothing
while they are open. The cockpit sets it from the report of the end. Two changesets give the tasks
and cases which ended before a reason, on the first start: `CANCELLED` where `comment` has text,
`COMPLETED` otherwise. That is the rule the old releases left in the data, and it is not exact. A
cancellation without a reason becomes `COMPLETED`, and a completion with a comment becomes
`CANCELLED`. If you need it otherwise, correct `endReason` in the collections `usertask` and
`workflow` by hand after the start. A list can also be filtered and sorted by `endReason`.

### A note on a task and on a case

`UserTask` and `Workflow` of the GUI API carry `note`, a text the users keep about the entry. It is
set with `PATCH /usertask/{userTaskId}/note` and `PATCH /workflow/{workflowId}/note`, and for
several entries at once with `PATCH /usertask/note` and `PATCH /workflow/note`. Nothing else
changes. The full-text filter of the lists now finds an entry by its note as well.

### The settings of a user

`GET`, `PUT` and `DELETE /gui/api/v1/app/current-user/settings/{key}` are new. They keep one setting
of the user signed in, in the new field `customSettings` of the collection `users`. Nothing breaks.
A chain of your own needs nothing new, as long as it keeps `currentUserRequestMatcher` as it is and
does not open `/gui/api/v1/app/current-user/**`.

### The list of workflows says `InProgress` and `Done`

`WorkflowRetrieveMode` has two new names: `InProgress` for the workflows which have not ended, and
`Done` for those which have. A suspended workflow has not ended, so it is in `InProgress`. A request
without `mode` gets `InProgress`, the same list as before.

`Active` and `Inactive` still work and give the same lists as `InProgress` and `Done`. They are
deprecated and go with the next major version at the earliest. Since there is "suspend" and
"activate", `Active` reads like "not suspended", and that is not what it shows. So move a user
interface to the new names when you touch it.

A Java caller has to change. `WorkflowlistService.RetrieveItemsMode` knows only `All`, `InProgress`
and `Done` now. A subclass of `AbstractWorkflowListGuiApiController` which overrides `getWorkflows`,
and a caller of `WorkflowlistService`, use `InProgress` instead of `Active` and `Done` instead of
`Inactive`. The generated `WorkflowRetrieveMode` of the official GUI API has all five values, so code
which reads it gets two more cases in a `switch`.

### `avatar` is gone from `Person` and `User`

`Person` and `User` of the GUI API no longer have the number `avatar`. Nothing said what it meant,
and the cockpit only passed on what your `PersonAndGroupApiMapper` returned. If your mapper set
`avatar`, remove that line, because the generated `Person` has no `avatar(...)` any more. If your
user interface shows a picture of a person, load it from an endpoint of your application, and show
the initials when there is none.

### The groups of a user come from the directory, not from the login token

The login token names the user and the time of the login, but no groups any more. On every request
the cockpit asks your bean of type `UserDirectoryService` for the user, by the subject of the token,
and takes the groups from `getAuthorities()` of the answer. So a group you take away in the
directory is gone on the next request. Before, it stayed until the login ended, which by default is
seven days.

`getUser` now runs on every request of a logged-in user. If your directory asks a remote system,
like an identity provider or LDAP, let it cache the users for a few minutes. If `getUser` throws, the
request fails with an error in the log. A user the directory does not know keeps the login, but has
no groups.

A token written by 0.8.x still names the groups in the claim `authorities`. It stays valid, so
nobody is logged out by the update. The claim is not read, and a renewal drops it.

The subject of a new token is `getName()` of the authentication of the login. For a login by user
name and password that is the user name, as before. For a login by OpenID Connect it is the claim
Spring Security names the user by, `sub` unless you configure `user-name-attribute`. Before, a
login by OpenID Connect got no token at all. The directory has to know the user by this id.

What to change:

- If you use the library's `jwtMapper` bean, nothing. It takes your `UserDirectoryService`.
- If you build a `JwtAuthenticationTokenMapper` yourself, in the cockpit or in a workflow module,
  pass a `UserDirectoryService`:

  ```java
  new JwtAuthenticationTokenMapper(properties, userDirectoryService, groupHierarchyService::resolveGroups)
  ```

  The constructors without a directory are gone. Pass `Function.identity()` if you have no group
  hierarchy. A workflow module which reads the cockpit's token needs a `UserDirectoryService` of
  its own for this.

- `new JwtUserDetailsProvider()` stays as it is. Name, short name and e-mail address now come from
  the directory and, where it has none, from the claims of the token.

- A `GrantedAuthoritiesMapper` which copies the groups of an OpenID Connect login into the
  authentication is no longer needed. Those groups do not reach the token.

### `group-hierarchy-bean-name` becomes a bean

The adapter no longer reads `vanillabp.workflow-modules.<id>.cockpit.group-hierarchy-bean-name`,
and the start ends if the key is set. Write a fixed hierarchy below
`vanillabp.workflow-modules.<id>.cockpit.group-hierarchy`.

If the hierarchy changes while the application runs, keep the bean instead, and remove the key:

- Import `io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleGroupHierarchy`.
- Add the method `getWorkflowModuleId()`. It returns the id of the module the bean answers for.
  The adapter finds the bean by its type and this id, and no longer by its name.
- After a change, publish a `WorkflowModuleGroupHierarchyChangedEvent` with the id of the module.
  On Spring Boot use the `ApplicationEventPublisher`. The adapter then registers the module at the
  cockpit again, with the new hierarchy.
- If your module resolves the groups of a request with this hierarchy, ask the bean on every
  request. The adapter keeps no copy, so there is no cache to clear.

A module with the bean and the key `group-hierarchy` does not start, and neither does a bean for a
module which reports nothing to the cockpit.

### The search takes the text literally

A search in the list of user tasks or of workflows finds the text the user typed, and the
suggestions of a search field do the same. Before, the cockpit sent the text to MongoDB as a regular
expression. Now characters like `.`, `(`, `+`, `*` and `\` find themselves. So `Mei.r` no longer
finds `Meier`, and `4.7` no longer finds `437`. A pattern like `(a+)+$` is searched for as text and
can no longer keep the database busy. Whether the search ignores case stays as it was.

If your user interface or a caller of `UserTaskService` or `WorkflowlistService` sent a pattern,
it gets no result for it any more. Send the plain text instead.
`SearchQuery.query()` is plain text now. If code of your own builds a regular expression from what
a user typed, call `SearchCriteriaHelper.literally(...)` the same way.

### The suggestions of a search field are the most frequent words

`OPTIONS /usertask` and `OPTIONS /workflow` answer with the 21 words found most often, the most
frequent first. Words found equally often come in alphabetical order. Before, the answer held any
21 words, and the rarest came first.

If your user interface sorted the suggestions itself or showed them from the end of the list, show
them in the order they come instead.

### A user directory for Keycloak and for LDAP comes with the cockpit

If your directory reads the users of a Keycloak realm or of an LDAP directory, you can drop it and
let the cockpit bring one. Set `business-cockpit.user-directory.type` to `keycloak` or `ldap`. The
next start names every value the directory still needs, with an example. The directory keeps each
user with its groups for 14 hours, `business-cockpit.user-directory.cache-duration`, and forgets
every user each night at 2 a.m., `business-cockpit.user-directory.cache-clear-cron`. So a group given
or taken away counts the next day at the latest. The actuator endpoint `userdirectorycache` empties
the cache at once, if the application has Spring Boot Actuator and exposes the endpoint.

- The LDAP directory needs `org.springframework.ldap:spring-ldap-core`. The cockpit does not bring
  it along, so add it. The Spring Boot BOM names its version.
- The Keycloak directory needs no library. It logs in with a client whose service account has the
  role `view-users` of `realm-management`. Newer releases of Keycloak also need the client scope
  `kc-admin-api` among the default client scopes of that client.
- Do not keep a bean of type `UserDirectoryService` of your own next to it. The start stops if you
  do, and it stops as well if there is no directory at all. Both messages say what to do.

The wiki page about security says more.

### The update of the list of workflows takes `businessIds`

`PUT /workflow` takes `businessIds` now, the same filter `POST /workflow` has. Before, the update
left the business ids out, so a list filtered by them got workflows which did not belong to it.
The field is optional. A user interface which filters by business ids sends them with the update
too, and one which does not use them changes nothing.

A subclass of `AbstractWorkflowListGuiApiController` which overrides `getWorkflowsUpdated` gets the
business ids as a new parameter after `initialTimestamp`. A caller of
`WorkflowlistService.getWorkflowsUpdated` passes them at the same place, or `null` for all
workflows.

### A list can be limited to workflow modules

Every request of a list takes the optional `workflowModuleIds` now: `POST` and `PUT` of
`/usertask` and `/workflow`, the two count endpoints and the suggestions of a search field. The
list then holds only the entries of these workflow modules. Without the field, or with an empty
array, a list holds the entries of every workflow module, as it always did. A user interface which does not use the field changes
nothing.

Each element of the update stream names the workflow module of its entity in `workflowModuleId`,
next to `event`. A user interface which checks the keys of an element strictly has to accept the
new one.

A Java caller has to change. A subclass of `AbstractUserTaskListGuiApiController` which overrides
`getUserTasks`, `countUserTasks`, `getUserTasksUpdated` or `kwic` gets the workflow module ids as a
new parameter. So does a subclass of `AbstractWorkflowListGuiApiController` which overrides
`getWorkflows`, `countWorkflows`, `getWorkflowsUpdated` or `kwic`. A caller of the methods of the
same names in `UserTaskService` and `WorkflowlistService` passes the ids, or `null` for every
workflow module. In all of these methods the new parameter comes right before `searchQueries`.

An implementation of `UpdateStreamAudience` returns `WhatTheStreamsMaySee` from
`whatEachStreamMaySee` now. It carries the visible ids per stream as before, and the workflow
module of each changed entity. The records `UserTaskVisibility.TaskFacts` and
`WorkflowVisibility.WorkflowFacts` have the new last component `workflowModuleId`.

Two new changesets index `workflowModuleId` together with `endedAt` in the collections `usertask`
and `workflow`.

### Where the rights of a user come from

`GET /gui/api/v1/app/current-user/group-hierarchy` is new. It answers the user signed in with the
groups the user directory gives them, and for each workflow module with a group hierarchy the
entries of the user's groups. So a user interface can show where each group and right comes from.
Nothing breaks. A chain of your own needs nothing new, as long as it does not open
`/gui/api/v1/app/current-user/**`.

### A key the adapter does not read ends the start on Spring Boot too

A workflow module on Spring Boot no longer starts with a key in one of the adapter's sections which
the adapter does not read: below `vanillabp.cockpit`, or in the `cockpit` section of a workflow
module, a workflow or a user task. Version 1 ignored such a key without a word. The message names
the key and the one it is nearest to, or, for a key of version 1, what became of it. Remove the key
or write the one the message names. Quarkus refused these keys before.

### An event of the update stream names who made the change

The `event` of each element of `GET /gui/api/v1/updates` has two new optional fields.
`initiatorKind` says who made the change: `USER`, `SYSTEM` for the workflow system or `COCKPIT` for
the cockpit itself. `initiator` holds the id of the user, and it is there only for `USER`. Both are
missing in a `RELOAD` and when the cockpit no longer holds the entity. If several changes of one
entity are sent at once, each of them names who made it. On Azure Cosmos DB for MongoDB they all
name who made the latest, because a change event there does not say what an update changed.

A page which shows one user task or case can now tell whether somebody else changed it, without
loading it first: `initiatorKind` is `USER` and `initiator` is not the user signed in. A list loads
again for every change, as before. A user interface which checks the keys of an event strictly has
to accept the two new ones. Nothing else breaks.

A note written in the cockpit now records its writer as the `initiator` of a user task, like every
other change made in the cockpit. Marking a task as read or unread, which also happens when a user
opens it, is no change of the task any more: it leaves `initiator`, `updatedAt` and `updatedBy` as
they are, and the update stream sends no event for it. A case keeps who made its latest change in the new field
`initiatorOfLatestChange`, because its `initiator` is what the latest report said. A case nobody
changed since the upgrade has no value there. Nothing reads it before the next change.

Every update of a user task or a case which the cockpit sends now also sets the new field
`latestChange`: who made the change and an id which is new for every write. MongoDB then names the
person in the change event of that update. No changeset fills the field, and nothing but the
change stream reads it. Code of your own which updates the two collections directly should set it
the same way, with `LatestChange.recordIn`, or its events name who made the latest change instead.

An implementation of `UpdateStreamAudience` has the new method `namingItsInitiator`, and
`WhatTheStreamsMaySee` has the new last component `initiatorOfEachEntity`. The records
`UserTaskVisibility.TaskFacts` and `WorkflowVisibility.WorkflowFacts` have the new last components
`initiator` and `initiatorOfLatestChange`.

### A task which is not open any more answers 404

This concerns only a build which used an earlier snapshot of `main`. `BcUserTaskExceptionHandler`
on Spring Boot and `BcUserTaskExceptionMapper` on Quarkus answered `BcTaskNotOpenException` with
`409 Conflict`. They answer it with `404 Not Found` now, like `BcUnknownTaskException`: for a form
there is no open task of that id either way. `409 Conflict` now means that another user claimed the
task. A workflow module says so with the new `BcTaskClaimedByAnotherUserException`, which carries
the ids and, if the module knows it, the user who claimed the task. A user interface which read 409
as "the task is done" has to read 404 for that now.

### The user signed in is part of the official GUI API

`GET /gui/api/v1/app/current-user` moved from the cockpit's own OpenAPI file to the official one,
`apis/official-gui-api/openapi/v1.yaml`, so a workflow module can ask it with a generated client.
The path, the answer and the security chains stay as they are. A user interface which generates
its client from both files finds the operation `currentUser` under the new tag
`officialCurrentUser` instead of `login`. In a TypeScript client that is `OfficialCurrentUserApi`
instead of `LoginApi`. The model `User` moved along: it is in `official-gui-api-server` now, in the
same package `io.vanillabp.cockpit.gui.api.v1`.

### The dev-shell simulator describes what only it answers

The paths only the dev-shell simulator answers, `/simulator-api/**` and `/dev-shell/user/**`, are
described in an OpenAPI file of their own, `apis/dev-shell-simulator-api/openapi/dev-shell-simulator-api.yaml`.
A test of a workflow module reads the report of a user task with the generated client
`io.vanillabp.businesscockpit:dev-shell-simulator-api-client` instead of a call written by hand.
The paths and the answers stay as they were, with one exception: `POST /dev-shell/user/{userId}`
for a user the simulator does not know answers 404. Before, it answered 500.

The simulator now also answers `GET /official-api/gui/api/v1/app/current-user` and the
notification settings below `/official-api/gui/api/v1/notifications`, as the cockpit answers them
below `/gui/api/v1`. A workflow module which asks for the user signed in works with the simulator
as it does with the cockpit.

### The simulators answer the official GUI API at `/official-api/gui/api/v1`

The dev-shell simulator and the simulator of `development/simulator` answer the official GUI API
at `/official-api/gui/api/v1`, no longer at `/official-api/v1`. Each system a simulator plays gets a
prefix of its own, and the path of the original goes below it. The cockpit's official GUI API has
the prefix `/official-api`.

- A workflow module whose client of the official GUI API points at a simulator sets the address of
  the simulator with the prefix, and nothing more:
  `spring.http.serviceclient.official-gui-api.base-url=http://localhost:8079/official-api`, or the
  same value in a group or prefix of its own. The client adds `/gui/api/v1` itself, as it does for
  the cockpit. A client built by hand for the simulator is not needed any more.
- A dev shell which builds its clients from the address `/official-api/v1` takes
  `/official-api/gui/api/v1`. The proxy of its dev server keeps forwarding `/official-api/**`.
- A test or a page which calls `/official-api/v1/...` by hand calls
  `/official-api/gui/api/v1/...`. The old path answers 404.

The BPMS API stays at `/bpms/api/v1_1`, so `vanillabp.cockpit.rest.base-url` stays the bare address
of the simulator.

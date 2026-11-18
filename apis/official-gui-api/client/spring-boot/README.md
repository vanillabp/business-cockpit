# Spring Boot client of the official GUI API

The HTTP interface clients of the official GUI API for a Spring Boot application, published as
`io.vanillabp.businesscockpit:official-gui-api-spring-boot-client`.

The module holds the interfaces the generator `spring` writes with the library
`spring-http-interface`, one per tag of the OpenAPI file: `OfficialTasklistApi`,
`OfficialWorkflowlistApi`, `OfficialWorkflowModulesApi`, `OfficialNotificationConfigApi` and
`OfficialCurrentUserApi`, in the
package `io.vanillabp.cockpit.gui.api.v1.client`, and their models in `...client.model`. Spring
Framework turns each interface into a client. Beside them the generator writes
`OfficialGuiApiPath`, the server path of the OpenAPI file, from the small template in
[`../generator`](../generator).

`OfficialGuiApiClientAutoConfiguration` builds the clients. It imports the five interfaces into
the HTTP service group `official-gui-api`, and the HTTP service helper of the
[support module](../../../../support/README.md#rest-clients) configures the group from keys. So an
application adds the dependency, sets the address of the cockpit and injects an interface:

```xml
<dependency>
  <groupId>io.vanillabp.businesscockpit</groupId>
  <artifactId>official-gui-api-spring-boot-client</artifactId>
</dependency>
```

The version comes from `business-cockpit-bom`. The jar comes with a sources jar, which holds the
generated interfaces and models too.

## Where the keys live

The keys of the group are in Spring Boot's place for it,
`spring.http.serviceclient.official-gui-api`:

```yaml
spring:
  http:
    serviceclient:
      official-gui-api:
        base-url: https://cockpit.example.com
        read-timeout: 2s
```

`base-url` is the address of the cockpit, with its context path if it has one, for example
`https://example.com/cockpit`. It never holds the path of the API: the auto-configuration
declares the server path of the OpenAPI file, `/gui/api/v1`, as an `HttpServiceApiPath`, and the
support module adds it to the address of every group with an interface of this API. So a client of a new version of the API brings its new path along, and switching to it means
switching the dependency only. An address which ends with `/gui/api/v1` stops the start with a
message which shows the address to set.

All other keys of the support module work there as well: timeouts, TLS, proxy, logging, retries,
OAuth. An address which is set is checked at the start, and so is every other key.

A workflow module which keeps its keys below a prefix of its own, for example
`loan-approval.cockpit`, imports the interfaces it needs into a group of its own, see the next
section. An `HttpServiceGroupPrefix` for the group `official-gui-api` stops the start with a
message which says so: the group is decided before any bean exists, so it cannot read a prefix
from a bean.

## Without the address there is no client

The group is there only if `spring.http.serviceclient.official-gui-api.base-url` is set. Without
it the application starts and has no client of this API. So the dependency on the class path asks
for no key: not in a test which does not call the cockpit, and not in an application which runs a
workflow module without a cockpit.

A class which calls the cockpit only if the application set the address takes an
`ObjectProvider` of the interface:

```java
@Component
class Assignment {

  private final ObjectProvider<OfficialTasklistApi> tasklist;

  Assignment(
      final ObjectProvider<OfficialTasklistApi> tasklist) {
    this.tasklist = tasklist;
  }

  boolean asksTheCockpit() {
    return tasklist.getIfAvailable() != null;
  }

}
```

A class which needs the client stops the start without the address. The message names the key to
set and the `ObjectProvider` as the way out. A workflow module with a group of its own does the
same with `@ConditionalOnHttpServiceGroupAddress` and its own prefix, see the section
[A group which only some applications use](../../../../support/README.md#a-group-which-only-some-applications-use)
of the support README.

## Two workflow modules with different keys

There is one group of the auto-configuration, so there is one prefix. If two workflow modules of
one application call the cockpit with different keys, each imports the interface it needs into a
group of its own, with a prefix of its own:

```java
interface LoanApprovalTasklist extends OfficialTasklistApi {
}

@Configuration
@ImportHttpServices(group = "loan-approval-cockpit", types = LoanApprovalTasklist.class)
class LoanApprovalCockpitClient {

  @Bean
  HttpServiceGroupPrefix loanApprovalCockpitKeys() {
    return HttpServiceGroupPrefix.of("loan-approval-cockpit", "loan-approval.cockpit");
  }

}
```

```properties
loan-approval.cockpit.base-url=https://cockpit.example.com
```

The sub-interface gives the module a bean type of its own, and its group gets the path of the API
as well.
As soon as the application imports one of the five interfaces into a group of its own, the group
of the auto-configuration steps back. A module which calls the cockpit only if the application
wants it adds `@ConditionalOnHttpServiceGroupAddress(prefix = "loan-approval.cockpit")` of the
support module to that class and takes an `ObjectProvider<LoanApprovalTasklist>`.

## The user's cookie goes along

The cockpit answers as the user whose login cookie a request carries. A workflow module which
answers a request of the cockpit's user interface usually wants the cockpit to answer as that
user, for example to compare the assignee of a task with the user who sent a form. So every group
with an interface of this API sends the `Cookie` header of the request the current thread answers.
The module needs no key and no user directory for that, because the cockpit checks the login.

A call outside of a request, in a test or in a scheduled job, sends no cookie. A call which sets
the `Cookie` header itself, for example through a `default-header` key, keeps its own. The servlet
API is an optional dependency: an application without it never forwards a cookie.

```java
@Component
class Assignment {

  private final OfficialTasklistApi tasklist;

  Assignment(
      final OfficialTasklistApi tasklist) {
    this.tasklist = tasklist;
  }

  /** Called while the module answers the form's request, so the cockpit sees the user's login. */
  Optional<String> assigneeOf(
      final String userTaskId) {

    try {
      final var userTask = tasklist.getUserTask(userTaskId, null).getBody();
      return Optional.ofNullable(userTask.getAssignee()).map(Person::getId);
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    }

  }

}
```

An answer with an error status comes as Spring's `HttpClientErrorException` or
`HttpServerErrorException`, no answer at all as `ResourceAccessException`.

The user signed in, `GET /gui/api/v1/app/current-user`, is part of this API:
`OfficialCurrentUserApi.currentUser()` answers the user of the forwarded cookie with all
groups, or no body if the request carries no login.

Its tests start the client against a stub server. Its coverage is the report
`test-coverage-report/official-gui-api/spring-boot`, without the generated interfaces and models,
and `CoverageGateTest` holds it at 85 %.

Built as part of the reactor build described in the [root README](../../../../README.md#building-it).

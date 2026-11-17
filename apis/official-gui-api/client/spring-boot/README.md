# Spring Boot client of the official GUI API

The HTTP interface clients of the official GUI API for a Spring Boot application, published as
`io.vanillabp.businesscockpit:official-gui-api-spring-boot-client`.

The module holds the interfaces the generator `spring` writes with the library
`spring-http-interface`, one per tag of the OpenAPI file: `OfficialTasklistApi`,
`OfficialWorkflowlistApi`, `OfficialWorkflowModulesApi`, `OfficialNotificationConfigApi` and
`OfficialCurrentUserApi`, in the
package `io.vanillabp.cockpit.gui.api.v1.client`, and their models in `...client.model`. Spring
Framework turns each interface into a client.

`OfficialGuiApiClientAutoConfiguration` builds the clients. It imports the five interfaces into
the HTTP service group `official-gui-api`, and the HTTP service helper of the
[support module](../../../../support/README.md#rest-clients) configures the group from keys. So an
application adds the dependency, sets the keys and injects an interface:

```xml
<dependency>
  <groupId>io.vanillabp.businesscockpit</groupId>
  <artifactId>official-gui-api-spring-boot-client</artifactId>
</dependency>
```

The version comes from `business-cockpit-bom`.

## Where the keys live

The application chooses the prefix of the keys. Without a choice, the keys are in Spring Boot's
place for the group, `spring.http.serviceclient.official-gui-api`:

```yaml
spring:
  http:
    serviceclient:
      official-gui-api:
        base-url: https://cockpit.example.com/gui/api/v1
```

A workflow module brings all its settings below its own prefix. It says so with an
`HttpServiceGroupPrefix` bean for the group:

```java
@Bean
HttpServiceGroupPrefix cockpitKeys() {
  return HttpServiceGroupPrefix.of(OfficialGuiApiClientAutoConfiguration.GROUP, "loan-approval.cockpit");
}
```

```yaml
loan-approval:
  cockpit:
    base-url: https://cockpit.example.com/gui/api/v1
    read-timeout: 2s
```

`base-url` is the address of the cockpit followed by `/gui/api/v1`, the server path of the
OpenAPI file. All other keys of the support module work there as well: timeouts, TLS, proxy,
logging, retries, OAuth. The start stops with a message if `base-url` is missing.

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

The sub-interface gives the module a bean type of its own. As soon as the application imports one
of the five interfaces into a group of its own, the group of the auto-configuration steps back, so
a missing `base-url` of the unused group does not stop the start.

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
`OfficialCurrentUserApi.currentUser(null)` answers the user of the forwarded cookie with all
groups, or no body if the request carries no login.

Its tests start the client against a stub server. Its coverage counts in no aggregated report,
because it runs in a workflow module and in no cockpit. `CoverageGateTest` names it among the
modules which are left out on purpose.

Built as part of the reactor build described in the [root README](../../../../README.md#building-it).

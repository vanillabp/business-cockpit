# Quarkus client of the official GUI API

The Quarkus REST client interfaces of the official GUI API, published as
`io.vanillabp.businesscockpit:official-gui-api-quarkus-client`. The interfaces and models are
generated from [`openapi/v1.yaml`](../../openapi/v1.yaml) by the generator `jaxrs-spec`, as the
Quarkus blueprints generate their clients: `OfficialTasklistApi`, `OfficialWorkflowlistApi`,
`OfficialWorkflowModulesApi`, `OfficialNotificationConfigApi` and `OfficialCurrentUserApi` in the
package `io.vanillabp.cockpit.gui.api.v1.client`, and their models in `...client.model`. Each
method returns the body. An answer with an error status comes as the REST client's
`WebApplicationException`, no answer at all as `ProcessingException`. The module keeps the
generator's templates in `src/main/openapi-templates`, with one change: `@Path` on the interface
is the server path of the OpenAPI file, and each method holds the whole path of its operation.

Two operations of the OpenAPI file are left out: `getUserTaskKwicResults` and `getKwicResults`,
the suggestions of words for the search in the task list and in the workflow list. They use the
method `OPTIONS` with a body, which Quarkus' REST client warns about while the application is
built. They serve the lists of the cockpit's user interface and no workflow module. The OpenAPI
file and the Spring Boot client keep them. The jar comes with a sources jar, which holds the
generated interfaces and models too.

## Using it

A workflow module adds the dependency and the extension of Quarkus' REST client with JSON. There
is no BOM for Quarkus, because `business-cockpit-bom` imports the Spring Boot BOM, so the module
names the version itself:

```xml
<dependency>
  <groupId>io.vanillabp.businesscockpit</groupId>
  <artifactId>official-gui-api-quarkus-client</artifactId>
  <version>${businesscockpit.version}</version>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-rest-client-jackson</artifactId>
</dependency>
```

It builds the clients it needs with `RestClients` of the
[support module](../../../../support/README.md#rest-clients-on-quarkus), at a prefix of its own,
while the application starts:

```java
@ApplicationScoped
public class LoanApprovalCockpit {

  @Produces
  @Singleton
  @Startup
  @LoanApproval
  OfficialTasklistApi tasklist(
      final RestClients restClients) {
    return restClients.build(OfficialTasklistApi.class, "loan-approval.cockpit");
  }

  @Produces
  @Singleton
  @Startup
  @LoanApproval
  OfficialCurrentUserApi currentUser(
      final RestClients restClients) {
    return restClients.build(OfficialCurrentUserApi.class, "loan-approval.cockpit");
  }

}
```

```properties
loan-approval.cockpit.base-url=https://cockpit.example.com
loan-approval.cockpit.read-timeout=2s
```

`base-url` is the address of the cockpit, with its context path if it has one, for example
`https://example.com/cockpit`. It never holds the path of the API: each interface carries the
server path of the OpenAPI file, `/gui/api/v1`, in `@Path` on its type, and each method the whole
path of its operation. So a client of a new version of the API brings its new path along, and
switching to it means switching the dependency only. An address which ends with `/gui/api/v1`
stops the start with a message which shows the address to set.

All other keys of the support module work there as well: timeouts, TLS, proxy, logging, retries,
OAuth. The start stops with a message if `base-url` is missing.

The qualifier, `@LoanApproval` here, is the module's own. A second workflow module of the same
application builds the same interfaces at its own prefix, for example `onboarding.cockpit`, with
its own qualifier, and so gets its own address and timeouts.

## The user's cookie goes along

The cockpit answers as the user whose login cookie a request carries. `ForwardTheUsersCookie`
sends the `Cookie` header of the HTTP request the application answers with every call of an
interface of this API. So a form endpoint learns whom a task is assigned to and who the user is,
and answers 409 for a task another user claimed:

```java
@Inject
@LoanApproval
OfficialTasklistApi tasklist;

@Inject
@LoanApproval
OfficialCurrentUserApi currentUser;

boolean isClaimedByAnotherUser(
    final String taskId) {

  final var assignee = tasklist.getUserTask(taskId, null).getAssignee();
  return (assignee != null) && !assignee.getId().equals(currentUser.currentUser(null).getId());

}
```

The filter is a `@Provider`, so Quarkus registers it for every REST client, however it is built. It
forwards the cookie only to the methods of the interfaces of this API, so a client of another
service never gets the user's login. A call outside of a request, in a test or in a scheduled job,
sends no cookie, and a call which sets the `Cookie` header itself keeps its own. The module needs no
key and no user directory for that, because the cockpit checks the login.

## Tests

`TheClientsCallTheCockpitTest` is a `@QuarkusTest` against a stub server: the cookie of a request
goes to the cockpit and not to another service, a call outside of a request sends none, two
workflow modules call the cockpit with their own keys, one of them at a cockpit with a context
path, an address with the path of the API stops the start, and an unknown task comes as
`WebApplicationException`. `TheCockpitOfTheTests` sets the keys before the application starts.
`NoMethodSendsABodyWithOptionsTest` checks that no interface has a method with `OPTIONS`.
Its coverage counts in the report `test-coverage-report/support/quarkus`, without the generated
interfaces and models.

Built as part of the reactor build described in the [root README](../../../../README.md#building-it).
Quarkus tests read the modules they use from the local Maven repository, so build them with
`install`.

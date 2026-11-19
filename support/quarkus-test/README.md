# Support for Quarkus, for tests

What the tests of a Quarkus workflow module need from `support`. It is published as
`io.vanillabp.businesscockpit:business-cockpit-quarkus-support-test`. A workflow module adds it in
test scope, in the version of [business-cockpit-quarkus-support](../quarkus):

```xml
<dependency>
  <groupId>io.vanillabp.businesscockpit</groupId>
  <artifactId>business-cockpit-quarkus-support-test</artifactId>
  <version>${business-cockpit.version}</version>
  <scope>test</scope>
</dependency>
```

It holds `io.vanillabp.cockpit.support.test.TestLogin`. A test of a workflow module calls the beans of
the module as different users, one after the other, and `TestLogin` logs each one in:

```java
@QuarkusTest
class RiskAssessmentTest {

  @Inject
  TestLogin login;

  @AfterEach
  void logOut() {
    login.logOut();
  }

  @Test
  void onlyAnAssessorAssessesTheRisk() {
    login.logIn("bob", "LOAN_APPROVAL_ASSESS_RISK");
    // calls of the module as bob
    login.logIn("mallory");
    // calls of the module as mallory, who has no role
  }

}
```

`TestIdentityAssociation` of `quarkus-test-security` sets the user of a test, but it takes the
identity as it is. A login of the application runs every `SecurityIdentityAugmentor` on the
identity, and Quarkus brings the permission checkers of a module (`@PermissionChecker`) to the
identity that way. Without them every check of a permission refuses the call. So `TestLogin` runs
the augmentors of the application as a login does, the one with the highest priority first, and
then sets the identity.

The roles are the ones the test names, together with what the augmentors add. The `GroupResolver`
of the cockpit's login is not applied, because a login of the application's own does not apply it
either. The user holds for the whole application until the next `logIn` or `logOut`, also for a
request a test sends over HTTP without a login of its own. So a test class logs out after each test.

The module brings `quarkus-test-security`. It is a plain jar with a Jandex index, like
`business-cockpit-quarkus-support`, so a `@QuarkusTest` finds `TestLogin` as a bean. Its tests are
in [quarkus-it](../quarkus-it), `ATestLogsAUserInTest`, and their coverage counts in the report
`test-coverage-report/support/quarkus`.

Built as part of the reactor build described in the [root README](../../README.md#building-it).

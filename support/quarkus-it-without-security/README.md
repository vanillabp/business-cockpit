# Support for Quarkus, tested as an application without quarkus-security

A Quarkus application which has the [Quarkus module of support](../quarkus) and Quarkus REST, but
not `quarkus-security`. That is how a workflow module looks which has the support only for the
cockpit's SPI and the exceptions of a user task. Quarkus REST brings the HTTP layer and the API
of Quarkus security, but not the extension `quarkus-security`, so there is no `SecurityIdentity`
bean.

It holds tests only. `AssessRiskForm` in `src/test` tells the user by a header of its own and
throws the exceptions of a user task. `AFormWithoutQuarkusSecurityTest` is a `@QuarkusTest`
without any setting of the cockpit. It shows that the application builds and starts, that the
mappers answer with `401`, `404` and `409`, and that the user context is there and knows nobody.
`ASecretWithoutQuarkusSecurityIsRefusedTest` shows that the start ends if the application sets
the cockpit's secret anyway, because nothing could check a token with it.

The same application with `quarkus-security` and `business-cockpit.jwt.enabled=false` is
`AModuleWithoutTheCheckTest` in [quarkus-it](../quarkus-it).

The coverage of the tests counts in the report `test-coverage-report/support/quarkus`. The module
is neither installed nor published.

It is built as part of the reactor build described in the [root README](../../README.md#building-it).
It reads the Quarkus module from the local Maven repository, so build that one with `install`.

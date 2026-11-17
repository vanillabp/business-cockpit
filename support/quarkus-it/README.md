# Support for Quarkus, tested as an application

A Quarkus application which has the [Quarkus module of support](../quarkus) as a plain dependency,
the way a workflow module has it. The tests of that module build their application out of the
classes they name, so they cannot show that Quarkus finds the classes in the jar on its own. This
module shows it.

It holds tests only. In `src/test` there is what a workflow module with a form writes itself: the
form endpoint `AssessRiskForm`, which allows clerks only and throws the exceptions of a user task,
a `UserDirectory`, and a `GroupHierarchy` in which a loan manager is a clerk as well.
`AFormOfAWorkflowModuleTest` is a `@QuarkusTest`. It sends the cockpit's cookie with tokens which
`LoginTokens` of the core's test jar signs, and it expects the user of the token, or `401`, `403`,
`404` for an unknown task and for a task which is not open any more, and `409` for a task which
another user claimed.

The secret comes as the environment variable `BUSINESS_COCKPIT_JWT_HMACSHA256_BASE64`, which the
Surefire configuration sets, the way a container gets it. So the tests also show that Quarkus finds
the key `business-cockpit.jwt.hmacSHA256-base64` under that name.

The coverage of the tests counts in the report `test-coverage-report/support/quarkus`, for the
classes of `support/quarkus` and `support/core`. The module is neither installed nor published.

It is built as part of the reactor build described in the [root README](../../README.md#building-it).
It reads the Quarkus module from the local Maven repository, so build that one with `install`.

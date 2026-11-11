# What a workflow module shares with the cockpit is the module `support`, and nothing else

Decided on 2026-11-11 by the maintainer.

## What was found

`commons` held two kinds of classes. Some are tools of the cockpit server alone: the MongoDB
helpers for change streams, converters and update information, the Kafka settings, the MapStruct
qualifier and the exceptions with the handler which turns them into HTTP answers. Others are
needed by a workflow module behind the cockpit's proxy as well: the check of the cockpit's login
token, the user and the groups of a request, and a REST client for the API of a BPMS or of the
cockpit. A workflow module which needed only the second kind got the first kind, too, with Spring
Data MongoDB and two auto-configurations which bound the cockpit's MongoDB settings.

After 1.0 such a cut breaks every application which depends on it. So it is made before.

## The decision

The module `commons` is gone, and there is no artifact under the old name which points at the new
ones.

The module `support`, artifact `io.vanillabp.businesscockpit:support`, packages below
`io.vanillabp.cockpit.support`, takes only what the cockpit and a workflow module both need or
can use: the packages `security` (the login token and the user context) and `rest`. Spring Web,
Spring Security and Spring's JWT classes are optional dependencies, and nothing in `support`
needs MongoDB or Kafka.

Everything else moves into the library module `business-cockpit`, below `io.vanillabp.cockpit`:
`exceptions`, `kafka`, `mapstruct`, `mongo`, and `DateTimeUtil`, which only the MongoDB converters
use. The bean which injected an SLF4J logger is gone, and the cockpit's classes use Lombok's
`@Slf4j`.

A class which turns up later is placed by the same rule: into `support` only when a workflow
module needs it or can use it as well.

`support` checks the login token through a small interface, `JwtKeySource`. Today its only
implementation uses the shared HMAC secret. A later switch to the public keys of an identity
provider adds an implementation and does not change the API of `support`.

## Where the table had to give way

`BcUnauthorizedException` is in `support`, in `io.vanillabp.cockpit.support.security`, and not
next to the other exceptions. `UserContext` throws it, and `support` cannot see the library
module.

The two simulators stand in for a workflow module, so they depend on `support` and not on the
library module. They had used `NoMappingMethod` and `RestfulExceptionHandler`. The dev shell
simulator now carries its own copy of `NoMappingMethod`, and the simulator no longer registers the
exception handler. It throws none of the exceptions the handler knows.

## What it costs

Every application changes imports, one line per package in `UPGRADE.md`. A workflow module which
used `RestfulExceptionHandler` or `NoMappingMethod` from `commons` no longer finds them in
`support`, and has to write its own or depend on the library module.

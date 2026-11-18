# APIs

The two interfaces of the Business Cockpit, each described by an OpenAPI specification and
generated into a client and a server:

* **[bpms-api](./bpms-api)**: what a workflow module reports to the cockpit, over REST or, as
  protobuf, over Kafka.
* **[official-gui-api](./official-gui-api)**: what a cockpit user interface reads, and what the user
  interface of a workflow module reads about a task or a case it renders. Its Java client for
  Spring Boot lets a workflow module ask the cockpit, for example whom a task is assigned to.

A third one belongs to no cockpit:

* **[dev-shell-simulator-api](./dev-shell-simulator-api)**: the paths only the dev shell simulator
  answers. A dev shell switches users with them, and a test of a workflow module reads what the
  module reported.

The generators are configured per module. Built as part of the reactor build described in the
[root README](../README.md#building-it), and published under `io.vanillabp.businesscockpit`. The
TypeScript client of the official GUI API is no longer generated, see decisions 62 and 63 in the
repository's [DECISIONS.md](../DECISIONS.md).

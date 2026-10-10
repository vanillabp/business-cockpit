# APIs

The three interfaces of the Business Cockpit, each described by an OpenAPI specification and
generated into a client and a server:

* **[bpms-api](./bpms-api)**: what a workflow module reports to the cockpit, over REST or, as
  protobuf, over Kafka.
* **[official-gui-api](./official-gui-api)**: what a cockpit user interface reads, and what the user
  interface of a workflow module reads about a task or a case it renders.
* **[workflow-provider-api](./workflow-provider-api)**: what a workflow module may implement so that
  the cockpit can ask it something.

The generators are configured per module. Built as part of the reactor build described in the
[root README](../README.md#building-it), and published under `io.vanillabp.businesscockpit`. The
TypeScript client of the official GUI API is no longer generated, see decisions 62 and 63 in the
repository's [DECISIONS.md](../DECISIONS.md).

# BPMS API

What a workflow module reports to the cockpit: a user task, a workflow and the registration of the
module itself, each as it is created, changed, completed or cancelled.

Three modules are built from one specification. `client` is what an integration sends with, `server`
is what the cockpit receives with, and `protobuf` is the same messages for the Kafka transport.

The client names its own dependencies rather than reaching them through the cockpit's `commons`
artifact, so that it drags no web framework into a consumer which has to stay free of one. Decision 1
of the [decision log](../../DECISIONS.md) says why.

Built as part of the reactor build described in the [root README](../../README.md#building-it).

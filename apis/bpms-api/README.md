# BPMS API

What a workflow module reports to the cockpit: a user task, a workflow and the registration of the
module itself, each as it is created, changed, completed or cancelled.

Three modules are built from one specification. `client` is what an integration sends with, `server`
is what the cockpit receives with, and `protobuf` is the same messages for the Kafka transport.

Both specifications, `openapi/v1.yaml` and `openapi/v1_1.yaml`, say what the cockpit answers where a
report does not go through, and what the sender does about it. A `400` means: change the report
first, because the same report is refused again. A `500` or a `503` means: send the same report
again later. The answers are described in words only. A `content` for one of them would change the
Accept header the client sends.

The client names its own dependencies rather than reaching them through the cockpit's `commons`
artifact, so that it drags no web framework into a consumer which has to stay free of one. Decision 1
of the [decision log](../../DECISIONS.md) says why.

Built as part of the reactor build described in the [root README](../../README.md#building-it).

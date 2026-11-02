# BPMS API

What a workflow module reports to the cockpit: a user task, a workflow and the registration of the
module itself, each as it is created, changed, completed or cancelled.

Three modules are built from one specification. `client` is what an integration sends with, `server`
is what the cockpit receives with, and `protobuf` is the same messages for the Kafka transport.

Both specifications, `openapi/v1.yaml` and `openapi/v1_1.yaml`, say what the cockpit answers where a
report does not go through, and what the sender does about it. A `400` means: change the report
first, because the same report is refused again. A `422` means: the cockpit understood the report
but can never store it, so do not send it again as it is. A `500` or a `503` means: send the same
report again later. The answers are described in words only, because a `content` for one of them
would change the Accept header the client sends.

A process instance can be suspended and activated again. Both versions take the reports
`usertask/{userTaskId}/suspended` and `usertask/{userTaskId}/activated`. Version 1.1 adds
`workflow/{workflowId}/suspended` and `workflow/{workflowId}/activated`. On Kafka they are the
messages `userTaskSuspended` (4), `userTaskActivated` (5), `workflowSuspended` (18) and
`workflowActivated` (19) of `BcEvent`. Each report carries an id, the id of the record, a timestamp
and optionally an initiator, a source and a comment. In version 1.1 and on Kafka a report also
carries `suspendedBy`, the person who suspended or activated the instance, where the BPMS says so.
The cockpit marks the record with `suspendedAt` and `suspendedBy` in the GUI API and keeps it in
every list. It orders the reports by their timestamps. Only these reports change the suspension: a
creation, a change or an end leaves it as it is. A report
about a record the cockpit does not know is answered with `200` and changes nothing, because it says
too little to create the record. The descriptions in both specifications have the details.

A key of `details` may contain a dot, like `order.id`. MongoDB reads a dot as a step into a nested
document, so the cockpit stores such a key only if `business-cockpit.mongodb.map-key-dot-replacement`
is set in the cockpit. Without it the report is answered with `422`, and the body names the key and
the property. With it every dot of a key is stored as the replacement, and it comes back as a dot.
That has two costs. Search and sorting find the key only by its stored form, like
`details.order~id` for the replacement `~`. And a key which holds the replacement already comes
back with a dot in its place. Both specifications say the same in the description of `details`.

An end of a user task or a workflow, completed or cancelled, may say when the record began. The
field is `createdAt`, and `created_at` on Kafka. On the messages of version 1.1 it is read only where
they carry an end. The field is optional. The cockpit reads it only if the end is the first report
it gets about the record. That happens when the end overtakes the creation. Without it the record starts at
the timestamp of the end, so every record the user interface shows has a start. A creation which
arrives later still fills in what the end left out, and its own start replaces that of the end.

The client names its own dependencies rather than reaching them through the cockpit's `commons`
artifact, so that it drags no web framework into a consumer which has to stay free of one. Decision 1
of the [decision log](../../DECISIONS.md) says why.

Built as part of the reactor build described in the [root README](../../README.md#building-it).

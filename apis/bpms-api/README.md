# BPMS API

What a workflow module reports to the cockpit: a user task, a workflow and the registration of the
module itself, each as it is created, changed, completed or cancelled.

Three modules are built from one specification. `client` is what an integration sends with, `server`
is what the cockpit receives with, and `protobuf` is the same messages for the Kafka transport.

Both specifications, `openapi/v1.yaml` and `openapi/v1_1.yaml`, say what the cockpit answers where a
report does not go through, and what the sender does about it. A `400` means: change the report
first, because the same report is refused again. A `422` means: the cockpit understood the report
but can never store it, so do not send it again as it is. A `500` or a `503` means: send the same
report again later. A `501` is the answer to a report that a user task was suspended or activated
again. The cockpit does nothing with those two reports, and no integration of the cockpit sends
them, so do not send them again. The answers are described in words only. A `content` for one of them would change the
Accept header the client sends.

A key of `details` may contain a dot, like `order.id`. MongoDB reads a dot as a step into a nested
document, so the cockpit stores such a key only if `business-cockpit.mongodb.map-key-dot-replacement`
is set in the cockpit. Without it the report is answered with `422`, and the body names the key and
the property. With it every dot of a key is stored as the replacement, and it comes back as a dot.
That has two costs. Search and sorting find the key only by its stored form, like
`details.order~id` for the replacement `~`. And a key which holds the replacement already comes
back with a dot in its place. Both specifications say the same in the description of `details`.

An end of a user task or a workflow, completed or cancelled, may say when the record began, in
`createdAt` (`created_at` on Kafka, and on the messages of version 1.1 read only where they carry an
end). The field is optional. The cockpit reads it only where the end is the first report about the
record it gets, which happens when the end overtakes the creation. Without it the record starts at
the timestamp of the end, so every record the user interface shows has a start. A creation which
arrives later still fills in what the end left out, and its own start replaces that of the end.

The client names its own dependencies rather than reaching them through the cockpit's `commons`
artifact, so that it drags no web framework into a consumer which has to stay free of one. Decision 1
of the [decision log](../../DECISIONS.md) says why.

Built as part of the reactor build described in the [root README](../../README.md#building-it).

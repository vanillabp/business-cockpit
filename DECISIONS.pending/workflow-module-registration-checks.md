# The cockpit refuses a registration it cannot route, and the startup passes over a stored one

## What was found

The cockpit built the route of a workflow module from a registration after it had stored it, and
it read a stored group hierarchy as it was. A registration is now checked before anything of it is
stored, and the cockpit starts even if a stored registration cannot be routed.

## The decision

1. The id of a workflow module is a plain segment of a path. It holds only letters, digits, `-`,
   `_`, `.` and `~`, the characters a URI never has to encode, and it is not made of dots only.
2. The `uri` of a registration is an absolute `http` or `https` URI with a host.
3. A registration which breaks 1 or 2 is refused before anything is stored. Both versions of the
   BPMS API answer `400 Bad Request` with a plain text body which names the field. Over Kafka the
   registration is dropped, and an error in the log names the module and the reason.
4. In the group hierarchy of a registration, an entry without a group and two entries for one group
   are refused the same way. A target which is `null` is dropped, and an entry without targets has
   none.
5. The route of a module is built before it replaces the one before. A route which cannot be built
   changes nothing.
6. At startup a stored module which cannot be routed is passed over, with an error in the log. The
   cockpit starts, and the module gets its route when it registers again with valid values. A stored
   hierarchy is read as far as it can be: targets which are `null` and entries without a group are
   dropped, and two entries for one group are joined.
7. The extension checks rules 1 and 2 when the workflow module starts, so a wrong value stops the
   module and not the cockpit.

## What follows

- A workflow module whose id has any other character, a `:` or a space for example, cannot report
  to the cockpit any more, although some of these ids could be routed before. The list of allowed
  characters is short on purpose: such an id reads the same in a path, in a URL the browser builds
  and in the log, with nothing to encode.

## Where it applies

- `business-cockpit`: `WhatARegistrationMustSay`, `WorkflowModuleService`,
  `MicroserviceProxyRegistry`, both `BpmsApiController` classes and `KafkaWorkflowModuleController`
- `extensions-commons/core`: `BusinessCockpitConfiguration`
- the wiki pages "Connecting a workflow module" and "Configuration"


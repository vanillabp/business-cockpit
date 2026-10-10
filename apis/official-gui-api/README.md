# Official GUI API

What a user interface reads from the cockpit: the two lists, one user task, one business case, and
the workflow modules it may load components from. The interface of the user task forms as well.

`server` is what the cockpit implements. The TypeScript client, once published as
`@vanillabp/bc-official-gui-client`, is no longer built here. A user interface generates its own
from `openapi`, see decision 63 in the repository's [DECISIONS.md](../../DECISIONS.md).

Built as part of the reactor build described in the [root README](../../README.md#building-it).

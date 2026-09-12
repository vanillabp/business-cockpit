# Official GUI API

What a user interface reads from the cockpit: the two lists, one user task, one business case, and
the workflow modules it may load components from. The interface of the user task forms as well,
which is why a workflow module's user interface uses the generated TypeScript client rather than
writing requests by hand.

`server` is what the cockpit implements, `client` is the generated TypeScript, published as
`@vanillabp/bc-official-gui-client`.

Built as part of the reactor build described in the [root README](../../README.md#building-it).

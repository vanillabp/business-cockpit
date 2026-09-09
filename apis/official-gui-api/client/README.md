# Official GUI API client

The generated TypeScript client of the [official GUI API](..), published to npm as
`@vanillabp/bc-official-gui-client`. A workflow module's user interface uses it to read what the
cockpit knows about the task or the business case it is rendering.

Nothing here is written by hand: the sources are generated from the specification of the module
above, with the templates of [openapi-generator-fixes](../../../openapi-generator-fixes). Build it
with the reactor build described in the [root README](../../../README.md#building-it), which also
publishes it to the local NPM registry.

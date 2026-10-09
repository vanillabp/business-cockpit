# UI packages

The NPM packages of the Business Cockpit:

* **[bc-types](./bc-types)**: the TypeScript types, published as `@vanillabp/bc-types`.
* **[bc-shared](./bc-shared)**: what a workflow module's user interface and the cockpit both use,
  published as `@vanillabp/bc-shared`.
* **[bc-ui](./bc-ui)**: what a cockpit user interface is built from, published as
  `@vanillabp/bc-ui`.

Maven does not build them any more, and no release publishes them, see decision 62 in the
repository's [DECISIONS.md](../DECISIONS.md). They stay here until they move to repositories of
their own. Somebody who works on them publishes them by hand to the local NPM registry which
[development/README.md](../development/README.md#local-npm-registry) sets up.

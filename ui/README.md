# UI packages

The NPM packages of the Business Cockpit:

* **[bc-types](./bc-types)**: the TypeScript types, published as `@vanillabp/bc-types`.
* **[bc-shared](./bc-shared)**: what a workflow module's user interface and the cockpit both use,
  published as `@vanillabp/bc-shared`.
* **[bc-ui](./bc-ui)**: what a cockpit user interface is built from, published as
  `@vanillabp/bc-ui`.

They are built by Maven modules wrapping npm, so a reactor build publishes them to the local NPM
registry before the modules depending on them are built. That is why a build needs that registry,
which [development/README.md](../development/README.md#local-npm-registry) sets up. Releases go to
npmjs, snapshots to GitHub Packages.

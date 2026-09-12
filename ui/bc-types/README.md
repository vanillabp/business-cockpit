# bc-types

The TypeScript types of the Business Cockpit, published as `@vanillabp/bc-types`. They are what
`bc-shared` and `bc-ui` are written against, and they are separate so that a workflow module can
depend on the types without pulling in components.

```sh
npm run build      # once
npm start          # the same, in watch mode
```

A reactor build publishes this package to the NPM registry given by `-Dnpm.registry`, which
[development/README.md](../../development/README.md#local-npm-registry) sets up for local work.

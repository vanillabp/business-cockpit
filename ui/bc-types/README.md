# bc-types

The TypeScript types of the Business Cockpit, published as `@vanillabp/bc-types`. They are what
`bc-shared` and `bc-ui` are written against, and they are separate so that a workflow module can
depend on the types without pulling in components.

```sh
npm run build      # once
npm start          # the same, in watch mode
```

Maven does not build or publish this package any more, see decision 62 in the repository's
[DECISIONS.md](../../DECISIONS.md). To try a change in another package, publish it by hand to the
local NPM registry which [development/README.md](../../development/README.md#local-npm-registry) sets up:
`npm run publish:snapshot -- --@vanillabp:registry=http://localhost:4873`.

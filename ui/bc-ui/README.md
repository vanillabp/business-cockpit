# bc-ui

The components a Business Cockpit user interface is built from, published as `@vanillabp/bc-ui`.
They can also be used as web components, which is what makes a cockpit user interface in another
framework possible; the wiki describes that under
[Customizing the user interface](https://github.com/vanillabp/business-cockpit/wiki/Customizing-the-user-interface).

```sh
npm run build      # once
npm start          # the same, in watch mode
npm run storybook  # the components on their own
```

Maven does not build or publish this package any more, see decision 62 in the repository's
[DECISIONS.md](../../DECISIONS.md). To try a change in another package, publish it by hand to the
local NPM registry which [development/README.md](../../development/README.md#local-npm-registry) sets up:
`npm run publish:snapshot -- --@vanillabp:registry=http://localhost:4873`.

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

A reactor build publishes this package to the NPM registry given by `-Dnpm.registry`, which
[development/README.md](../../development/README.md#local-npm-registry) sets up for local work.

# Dev shell for React

The web application which renders a workflow module's components without a cockpit, published as
`@vanillabp/bc-dev-shell-react`. It imports the components the ordinary way, so a dev server can
replace them as they are edited, which module federation otherwise prevents.

How a workflow module uses it is in the wiki, under
[Developing UI components locally](https://github.com/vanillabp/business-cockpit/wiki/Developing-UI-components-locally).
This file is about the package.

```sh
npm run build      # once
npm start          # the same, in watch mode
```

A reactor build publishes this package to the NPM registry given by `-Dnpm.registry`, which
[development/README.md](../README.md#local-npm-registry) sets up for local work. The Angular
counterpart is [dev-shell-angular](../dev-shell-angular), and the server which supplies the data
behind the components is [dev-shell-simulator](../dev-shell-simulator).

Until now this file described installing `@vanillabp/bc-shared`, which is the wrong package: it was
copied from that package's README and never adapted. What it said about the Webpack configuration
belongs to `bc-shared` and is in the wiki page linked above.

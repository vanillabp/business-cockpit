# Use in workflow modules

Example:

```js
import { LoadingIndicator } from '@bc/shared/components/LoadingIndicator';
```

Read the next sections for instructions of installation.

## package.json

```sh
npm install @bc/shared
```

### Development

For local development of user-task forms in workflow modules one can

1. [build this package locally](#build)
1. Link the package `npm link`
1. Get current package-version `npm -g list` (e.g. `@bc/shared@0.0.1`)
1. Change to webapp folder of the workflow module
1. Link the global package `npm link @bc/shared@0.0.1` (this needs to be repeated after each `npm install`)

## tsconfig.json

It is necessary to tell the Typescript compiler where to find imports of `@bc/shared`:

```json
{
  "compilerOptions": {
    ...
    "baseUrl": "./",
    "paths": {
      "@bc/shared/*": ["node_modules/@bc/shared/dist/*"]
    }
  },
  "include": [
    "src/**/*",
    "node_modules/@bc/shared/dist/**/*"
  ],
  ...
}
```

### Webpack

Also Webpack needs to learn about the module. If the project is using `create-react-app` scripts one can use the drop-in replacement `craco` instead which allows to modify Webpack configuration.

```sh
install craco --save-dev
```

In the scripts-section of `package.json` replace occurrences of `react-script` by `craco`.

Add the file `craco.config.js` to your root-folder:

```js
const path = require("path");

const aliases = {
  '@bc/shared': path.join(path.resolve(__dirname, '.'), "node_modules", "@bc", "shared", "dist"),
  react: path.join(path.resolve(__dirname, '.'), "node_modules", "react"),
  'react-dom': path.join(path.resolve(__dirname, '.'), "node_modules", "react-dom")
};

module.exports = {
  webpack: {
    alias: aliases
  },
  plugins: [
    {
      plugin: {
        overrideWebpackConfig: ({ webpackConfig, pluginOptions, context: { paths } }) => {
          const moduleScopePlugin = webpackConfig.resolve.plugins.find(plugin => plugin.appSrcs && plugin.allowedFiles);
          if (moduleScopePlugin) {
            Object
                .keys(aliases)
                .map(key => aliases[key])
                .forEach(path => moduleScopePlugin.appSrcs.push(path));
          }
          const ignoreWarnings = [
              { module: /@bc\/shared/ },
              { module: /@microsoft\/fetch-event-source/ }
            ];
          return { ...webpackConfig, ignoreWarnings }
        }
      }
    }
  ]
};
```

*Details:*

In the exports section `webpack.alias` the alias to the module is defined. In addition it is necessary to define aliases for 'react' and 'react-dom' because otherwise Webpack would add these modules twice (one time for the original app and one time for the module we've added).

Create-react-app scripts limit sources to the `./src` folder. Unfortunately, the new aliases point to the `node_modules` directory and will be rejected. In the exports section `plugins[].plugin{}.overrideWebpackConfig` a craco-plugin is defined which is able to modify the webpack configuration. The `ModuleScopePlugin` which is used to block other folders than `./src` is determined and the pathes of the aliases are added.

Last but not least, the craco plugin is also used to suppress warnings regarding missing `.map` files.

# Build

```sh
npm run build
```

## Development

```sh
npm start
```

does the same as normal building but also enters `watch` mode.

## Storybook

```sh
npm storybook
```

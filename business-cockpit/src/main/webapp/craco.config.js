const ModuleFederationPlugin = require("webpack/lib/container/ModuleFederationPlugin");
const { dependencies } = require("./package.json");

const path = require("path");

const aliases = {
  '@bc/shared': path.join(path.resolve(__dirname, '.'), "node_modules", "@bc", "shared", "dist"),
};

module.exports = {
  webpack: {
    alias: aliases,
    plugins: {
      add: [
        new ModuleFederationPlugin({
          name: "business-cockpit",
          shared: {
            ...dependencies,
            react: {
              import: 'react', // the "react" package will be used a provided and fallback module
              shareKey: 'react', // under this name the shared module will be placed in the share scope
              shareScope: 'default', // share scope with this name will be used
              singleton: true,
              requiredVersion: dependencies["react"],
            },
            "react-dom": {
              singleton: true,
              requiredVersion: dependencies["react-dom"],
            },
            "@bc/shared": {
              import: '@bc/shared',
              requiredVersion: '0.0.1'
            }
          },
        }),
      ]
    }
  },
  plugins: [
    {
      plugin: {
        overrideWebpackConfig: ({ webpackConfig, pluginOptions, context: { paths } }) => {
//          webpackConfig.resolve.extensionAlias = {
//                ".js": [".ts", ".tsx", ".js", ".mjs"],
//                ".mjs": [".mts", ".mjs"]
//              };
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

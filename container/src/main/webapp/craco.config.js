const { DefinePlugin } = require("webpack");
const { ModuleFederationPlugin } = require('webpack').container;
const { dependencies } = require('./package.json');
const path = require("path");

const aliases = {
  'styled-components': path.join(path.resolve(__dirname, '.'), "node_modules", "styled-components"),
  'react': path.join(path.resolve(__dirname, '.'), "node_modules", "react"),
  'react-dom': path.join(path.resolve(__dirname, '.'), "node_modules", "react-dom"),
  'react-router-dom': path.join(path.resolve(__dirname, '.'), "node_modules", "react-router-dom"),
  'i18next': path.join(path.resolve(__dirname, '.'), "node_modules", "i18next"),
  'react-i18next': path.join(path.resolve(__dirname, '.'), "node_modules", "react-i18next")
};

module.exports = {
  devServer: {
      devMiddleware: {
          writeToDisk: true,
      },
  },
  webpack: {
    alias: aliases,
    plugins: {
      add: [
        new ModuleFederationPlugin({
          name: "business-cockpit",
          shared: {
            react: {
              eager: true,
              singleton: true,
              requiredVersion: dependencies["react"],
            },
            "react-dom": {
              eager: true,
              singleton: true,
              requiredVersion: dependencies["react-dom"],
            },
            "react-router-dom": {
              eager: true,
              singleton: true,
              requiredVersion: dependencies["react-router-dom"],
            },
          },
        }),
      ]
    }
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
              { module: /@microsoft\/fetch-event-source/ }
            ];
          return { ...webpackConfig, ignoreWarnings }
        }
      }
    }
  ]
};

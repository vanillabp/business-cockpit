const { DefinePlugin } = require("webpack");
const path = require("path");

const aliases = {
  '@bc/shared': path.join(path.resolve(__dirname, '.'), "node_modules", "@bc", "shared", "dist"),
  react: path.join(path.resolve(__dirname, '.'), "node_modules", "react"),
  'react-dom': path.join(path.resolve(__dirname, '.'), "node_modules", "react-dom")
};

module.exports = {
  webpack: {
    alias: aliases,
    plugins: {
      add: [
        new DefinePlugin({
          'process.env.BUILD_TIMESTAMP': `'${new Date().toISOString()}'`,
          'process.env.BUILD_VERSION': `'0.0.1-SNAPSHOT'`,
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
              { module: /@bc\/shared/ },
              { module: /@microsoft\/fetch-event-source/ }
            ];
          return { ...webpackConfig, ignoreWarnings }
        }
      }
    }
  ]
};

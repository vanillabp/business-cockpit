const { parseVersion } = require('./utils');
const { DefinePlugin } = require("webpack");
const { ModuleFederationPlugin } = require('webpack').container;
const { dependencies } = require('./package.json');

module.exports = {
  webpack: {
    configure: {
      output: {
        publicPath: '/wm/TestModule/',
      }
    },
    plugins: {
      remove: [ 'HtmlWebpackPlugin' , 'MiniCssExtractPlugin' ],
      add: [
        new DefinePlugin({
          'process.env.BUILD_TIMESTAMP': `'${new Date().toISOString()}'`,
          'process.env.BUILD_VERSION': `'${parseVersion()}'`,
        }),
        new ModuleFederationPlugin({
          name: "TestModule",
          filename: 'remoteEntry.js',
          exposes: {
            List: './src/List',
            Form: './src/Form',
          },
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
  }
};

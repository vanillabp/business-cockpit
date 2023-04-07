const MicroFrontedPlugin = require('craco-plugin-micro-frontend');
require('react-scripts/config/env');
const { parseVersion } = require('./utils');
const { DefinePlugin } = require("webpack");

module.exports = {
  webpack: {
    plugins: [
      new DefinePlugin({
        options: {
          'process.env': {
            BUILD_TIMESTAMP: `'${new Date().toISOString()}'`,
            BUILD_VERSION: `'${parseVersion()}'`,
          }
        },
      }),
    ],
  },
  plugins: [
    {
      plugin: MicroFrontedPlugin,
      options: {
        orgName: 'vanillabp-bc',
        fileName: 'TestModule',
        entry: 'src/index.ts',
        orgPackagesAsExternal: true, // marks packages that has @vanillabp-bc prefix as external
        reactPackagesAsExternal: true, // marks react and react-dom as external
        externals: [
          'react-router',
          'react-router-dom',
          'react-i18next'
        ],
        minimize: true,
        outputPath: process.env.BUILD_PATH,
      },
    },
  ],
};

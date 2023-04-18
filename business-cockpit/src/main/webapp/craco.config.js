const ModuleFederationPlugin = require("webpack/lib/container/ModuleFederationPlugin");
const { dependencies } = require("./package.json");

module.exports = {
  webpack: {
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
          },
        }),
      ]
    }
  }
};

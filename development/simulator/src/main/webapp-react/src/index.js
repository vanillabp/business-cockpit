// currently in craco.config.js webpack.configure.output.publicPath
// is defined but should be replaced by this
__webpack_public_path__ = document.currentScript.src.replace(/[^/]+$/, ''); // eslint-disable-line
// see: https://webpack.js.org/concepts/module-federation/#dynamic-public-path
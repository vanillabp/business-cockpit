const path = require("path");
module.exports = {
    entry: {
        "bundle.js": [
            path.resolve(
                __dirname,
                "dist/library-web-components/browser/polyfills.js"
            ),
            path.resolve(
                __dirname,
                "dist/library-web-components/browser/styles.css"
            ),
            path.resolve(
                __dirname,
                "dist/library-web-components/browser/main.js"
            )
        ]
    },
    output: { filename: "[name]", path: path.resolve(__dirname, "dist") },
    module: {
        rules: [
            {
                test: /\.css$/i,
                use: ["style-loader", "css-loader"]
            }
        ]
    }
};

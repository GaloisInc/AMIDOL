module.exports = {
    mode: "development",
    entry: {
      'webReact': './index.tsx'
    },
    devtool: 'source-map',
    resolve: {
        extensions: ['.js', '.jsx', '.json', '.ts', '.tsx']
    },
    output: {
      library: 'webReact',
      libraryTarget: 'var',
      filename: 'webReact.js',
    },
    externals: {
      'plotly.js': 'Plotly'
    },
    module: {
        rules: [
            {
                test: /\.(ts|tsx|jsx)$/,
                loader: 'ts-loader'
            },
            {
                enforce: "pre",
                test: /\.js$/,
                loader: "source-map-loader"
            }
        ]
    },
    stats: {
        colors: true
    }
};


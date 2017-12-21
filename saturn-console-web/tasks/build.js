process.env.NODE_ENV = 'production'

var rm = require('rimraf')
var path = require('path')
var chalk = require('chalk')
var webpack = require('webpack')
var appConf = require('./conf/app.conf')
var webpackConfig = require('./conf/webpack.prod.conf')
var utils = require('./utils')


rm(path.join(appConf.buildRoot, appConf.assetsSubDirectory), err => {
  if (err) throw err

  utils.checkLoaderEnable(webpackConfig, 'eslintEnable', 'eslint-loader')
  utils.checkLoaderEnable(webpackConfig, 'babelEnable', 'babel-loader')

  webpack(webpackConfig, function(err, stats) {
    if (err) throw err
    process.stdout.write(stats.toString({
        colors: true,
        modules: false,
        children: false,
        chunks: false,
        chunkModules: false
      }) + '\n\n')

    console.log(chalk.cyan('  Build complete.\n'))
    console.log(chalk.yellow(
      '  Tip: built files are meant to be served over an HTTP server.\n' +
      '  Opening index.html over file:// won\'t work.\n'
    ))
  })
})

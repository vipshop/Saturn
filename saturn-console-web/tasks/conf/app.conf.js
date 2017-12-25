var path = require('path')

module.exports = {
  env: process.env.NODE_ENV,
  entry: './src/index.js',
  devPort: 8083,
  assetsSubDirectory: '',
  assetsPublicPath: '/',
  proxyTable: {
    '/console': {
      target: "http://127.0.0.1:9088",
      changeOrigin: true,
      // pathRewrite: {
      //   '^/api' : '',     // rewrite path
      // }
    },
  },
  buildIndex: path.resolve(__dirname, '../../../saturn-console/src/main/resources/static/index.html'),
  buildRoot: path.resolve(__dirname, '../../../saturn-console/src/main/resources/static'),
  // 关闭eslint
  eslintEnable: true,
  // 关闭babel(需要源码为非转换代码，并且eslint是正确配置才可以)
  babelEnable: true
};
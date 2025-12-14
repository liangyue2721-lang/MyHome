'use strict' // 启用严格模式，增强代码的错误检查。:contentReference[oaicite:6]{index=6}

const path = require('path') // :contentReference[oaicite:8]{index=8}:contentReference[oaicite:10]{index=10}

function resolve(dir) {
  return path.join(__dirname, dir) // :contentReference[oaicite:12]{index=12}
}

const CompressionPlugin = require('compression-webpack-plugin') // :contentReference[oaicite:15]{index=15}:contentReference[oaicite:17]{index=17}

const name = process.env.VUE_APP_TITLE || 'TECHNOLOGY' // :contentReference[oaicite:19]{index=19}:contentReference[oaicite:21]{index=21}

//const baseUrl = 'http://115.190.121.53:6004' // :contentReference[oaicite:23]{index=23}:contentReference[oaicite:25]{index=25}
const baseUrl = 'http://localhost:8085' // :contentReference[oaicite:23]{index=23}:contentReference[oaicite:25]{index=25}

const port = process.env.port || process.env.npm_config_port || 80 // :contentReference[oaicite:27]{index=27}:contentReference[oaicite:29]{index=29}

module.exports = {
  publicPath: process.env.NODE_ENV === "production" ? "/" : "/", // :contentReference[oaicite:31]{index=31}
  outputDir: 'dist', // :contentReference[oaicite:34]{index=34}
  assetsDir: 'static', // :contentReference[oaicite:37]{index=37}
  productionSourceMap: false, // :contentReference[oaicite:40]{index=40}
  transpileDependencies: ['quill'], // :contentReference[oaicite:43]{index=43}:contentReference[oaicite:45]{index=45}
  lintOnSave: false, // 是否开启eslint保存检测，有效值：ture | false | 'error'
  devServer: {
    before: (app) => {
      // 增强代理日志记录
      app.use(process.env.VUE_APP_BASE_API, (req, res, next) => {
        console.log('请求参数 =>', {
          method: req.method,
          query: req.query,
          body: req.body,
          headers: req.headers
        })
        next()
      })
    },
    host: 'localhost', // 允许外部访问。
    port: port, // 开发服务器端口。
    open: true, // 自动打开浏览器。
    proxy: {
      [process.env.VUE_APP_BASE_API]: {
        target: baseUrl, // 代理的目标地址。
        changeOrigin: true, // 是否改变主机头为目标地址。
        pathRewrite: {
          ['^' + process.env.VUE_APP_BASE_API]: '' // 重写路径。
        }
      },
      '^/v3/api-docs/(.*)': {
        target: baseUrl, // 代理 SpringDoc 接口文档。
        changeOrigin: true // 是否改变主机头为目标地址。
      }
    },
    disableHostCheck: true // 关闭主机检查，允许使用 IP 访问。
  },

  css: {
    loaderOptions: {
      sass: {
        sassOptions: {outputStyle: "expanded"} // 设置 Sass 的输出风格为展开。
      }
    }
  },

  configureWebpack: {
    name: name, // 设置应用名称。
    resolve: {
      alias: {
        '@': resolve('src') // 设置路径别名。
      }
    },
    plugins: [
      new CompressionPlugin({
        cache: false, // 不启用文件缓存。
        test: /\.(js|css|html|jpe?g|png|gif|svg)?$/i, // 匹配需要压缩的文件类型。
        filename: '[path][base].gz[query]', // 压缩后的文件名。
        algorithm: 'gzip', // 使用 gzip 压缩。
        minRatio: 0.8, // 只有压缩率小于此值的文件才会被压缩。
        deleteOriginalAssets: false // 是否删除原始文件。
      })
    ],
  },

  chainWebpack(config) {
    config.plugins.delete('preload') // 删除 preload 插件。
    config.plugins.delete('prefetch') // 删除 prefetch 插件。

    config.module
      .rule('svg')
      .exclude.add(resolve('src/assets/icons')) // 排除 icons 目录。
      .end()
    config.module
      .rule('icons')
      .test(/\.svg$/) // 匹配 .svg 文件。
      .include.add(resolve('src/assets/icons')) // 包含 icons 目录。
      .end()
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader') // 使用 svg-sprite-loader。
      .options({
        symbolId: 'icon-[name]' // 设置 symbolId。
      })
      .end()

    config.when(process.env.NODE_ENV !== 'development', config => {
      config
        .plugin('ScriptExtHtmlWebpackPlugin')
        .after('html')
        .use('script-ext-html-webpack-plugin', [{
          inline: /runtime\..*\.js$/ // 内联 runtime 文件。
        }])
        .end()

      config.optimization.splitChunks({
        chunks: 'all',
        cacheGroups: {
          libs: {
            name: 'chunk-libs', // 拆分第三方库。
            test: /[\\/]node_modules[\\/]/, // 匹配 node_modules 目录。
            priority: 10, // 优先级。
            chunks: 'initial' // 只打包初始依赖。
          },
          elementUI: {
            name: 'chunk-elementUI', // 拆分 elementUI。
            test: /[\\/]node_modules[\\/]_?element-ui(.*)/, // 匹配 element-ui。
            priority: 20 // 优先级高于 libs。
          },
          commons: {
            name: 'chunk-commons', // 拆分公共组件。
            test: resolve('src/components'), // 匹配 components 目录。
            minChunks: 3, // 最小引用次数。
            priority: 5, // 优先级。
            reuseExistingChunk: true // 复用已有的 chunk。
          }
        }
      })
      config.optimization.runtimeChunk('single') // 提取 runtime 为单独的 chunk。
    })
  }
}

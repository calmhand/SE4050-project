const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8080",
        ws: true,
        changeOrigin: true
      }
    }, 
    port:"3000"}
})

#项目构建
##新建项目
create-react-app projectName
##弹出配置
npm run eject
##安装express
sudo cnpm i express --save
##安装nodemon
sudo cnpm i -g nodemon
##安装mongodb
brew install mongodb
##启动mongdo
mongod --config /usr/local/etc/mongod.conf
##安装mongoose
sudo cnpm i mongoose --save
##安装antd-mobile
sudo cnpm i antd-mobile@next --save
##安装babel-plugin-import
sudo cnpm i babel-plugin-import --save
##package.json的babel里面添加配置
"plugins":[[
      "import",
      { "libraryName": "antd-mobile", "style": "css"
      }
    ]]
##安装redux
sudo cnpm i redux --save
##安装redux-chunk
sudo cnpm i redux-thunk --save
##安装react-redux
sudo cnpm i react-redux --save
##安装connect插件
sudo cnpm i babel-plugin-transform-decorators-legacy --save-dev
##安装react-router
sudo cnpm i react-router-dom --save
##安装axios
sudo cnpm i axios --save
##配置proxy
"proxy":"http://localhost:9093"
##安装cookie-parser
sudo cnpm i cookie-parser --save
##安装body-parser
sudo cnpm i body-parser --save
##安装utility,进行md5加密
sudo cnpm i utility --save
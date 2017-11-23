const express = require('express');
const mongoose = require('mongoose');
//链接mongo 并使用imooc这个集合
const DB_URL = 'mongodb://127.0.0.1:27017/imooc';
mongoose.connect(DB_URL);
mongoose.connection.on('connected', function () {
    console.log('mongo connect success');
})
//类似于mysql的表 mongo里有文档的概念
const User = mongoose.model('user', new mongoose.Schema({
    user: { type: String, require: true },
    age: { type: Number, require: true }
}))

//Mongoose文档类型
//String，Number等数据结构
//定create,remove,update分别用来增、删、改的操作
//find和findOne用来查数据

//新增数据
// User.create({
//     name: 'imooc',
//     age: 18
// }, function (err, doc) {
//     if (!err) {
//         console.log(doc)
//     } else {
//         console.log(err)
//     }
// })

//删除
// User.remove({ age: 18 }, function (err, doc) {
//     console.log(doc);
// })

//更新
// User.update({'user':'小明'},{'$set':{age:16}},function(err,doc){
//     console.log(doc);
// })

//新建app
const app = express();

app.get('/', function (req, res) {
    res.send('<h1>Hello World</h1>');
})

app.get('/data', function (req, res) {
    //查找,只查找一条信息，使用findOne
    User.find({user:'小明'}, function (err, doc) {
        res.json(doc);
    })
})

app.listen(9093, function () {
    console.log('Node app start at port 9093');
})

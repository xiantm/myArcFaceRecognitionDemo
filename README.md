## 使用虹软人脸识别SDK实现的人脸搜索demo
- [虹软人脸识别官网](http://www.arcsoft.com.cn/ai/arcface.html)
- [虹软官方demo](https://github.com/asdfqwrasdf/ArcFaceDemo)
- [虹软人脸论坛](http://www.arcsoft.com.cn/bbs/forum.php?mod=forumdisplay&fid=42)

### 功能
- 实时录入和检测和搜索人脸
### 遇到的坑

- 多线程人脸对比返回73733,解决办法,使用对比线程的人脸对比引擎，参考 [http://www.arcsoft.com.cn/bbs/forum.php?mod=viewthread&tid=387&highlight=73733](http://www.arcsoft.com.cn/bbs/forum.php?mod=viewthread&tid=387&highlight=73733)

### 项目问题
- 没有加Android6.0动态权限验证，需要自己手动打开相机和存储权限

### 效果图
![](https://github.com/xiantm/myArcFaceRecognitionDemo/blob/master/show.jpg)

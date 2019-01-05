# WechatLuckyMoneyGetter
一个微信红包自动点击工具，免root，目前支持微信6.7.3，自动检测并且拆开红包。<br>
目前仅支持中文，前往[Release](https://github.com/LnJan/WechatLuckyMoneyGetter/releases)下载最新版本。已下载用户会直接推送新版本。<br>

## 特点
### ● 适配最新版本微信6.7.3
      随着微信版本的更新，微信团队也在不断封杀传统的免root抢红包工具，目前基于AccessibilityService的红包工具部分功能已不再适用。
      在AccessibilityService基础上加入Opencv特征识别来识别微信红包的特征信息来解决此问题
### ● 红包识别更加智能
      增加敏感字符不抢的功能，避免落入[专属红包]\[抢到翻倍]等陷阱中。增加延时拆红包、息屏抢红包等的功能
### ● 可支持微信7.0
      由于目前采用的是基于OpenCv的图像识别技术，后续可以用此技术继续支持微信7.0的版本
### ● 方便、安全
      无需ROOT，下载即用。代码公开透明
      
## 使用方法
    1、打开[微信红包工具]，开启插件
    2、静待红包到账
    
## 注意事项
    1、由于国内第三方定制系统如MIUI、Flyme等出于对电量的优化的考虑有可能会关闭抢红包服务，如需长时间运行本服务
    需按软件提示要求在权限管理中运行应用后台运行。
    2、由于目前新版微信重写了聊天及列表页面的文本控件，故无法从AccessibilityService直接获取到文本信息，所以如需
    监控聊天列表页面，需提供本软件截屏权限。
    3、如果需要使用息屏抢红包的功能可能会增加手机耗电量，请慎用

## 技术实现
    详情可浏览这篇文章[AccessibilityService+OpenCV实现微信新版本抢红包插件](https://www.jianshu.com/p/c269a1a1866b)
    
## 更新日志
完整的更新日志请见[VERSIONLOG](https://github.com/LnJan/WechatLuckyMoneyGetter/blob/master/VERSIONLOG.md)
## 版权免责声明
本项目源自小米去年秋季发布会时演示的[MIUI 7抢红包测试](https://github.com/XiaoMi/LuckyMoneyTool)和大神[geeeeeeeeek](https://github.com/geeeeeeeeek/WeChatLuckyMoney)的开源项目<br>
插件可能会在一定程度上改变微信的交互方式。使用本项目中包含的代码及其生成物时，使用者自行承担随之而来的各种风险，包括但不限于“禁用红包功能”、“微信封号”。

## 应用截图
![软件截图](https://github.com/LnJan/WechatLuckyMoneyGetter/blob/master/screenshot.jpg)

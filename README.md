# Android-JL_Bluetooth

The bluetooth SDK for Android

**中文** | [English](https://github.com/Jieli-Tech/Android-JL_Bluetooth/blob/main/README_en.md)

<br/>

<div align="center">

![Android](https://img.shields.io/badge/Android-5.1+-blue.svg)
![Android Studio](https://img.shields.io/badge/Android_Studio-Latest-orange.svg)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)       

<strong style="font:24px;color:#000000">杰理之家SDK(Android)</strong>

**杰理之家SDK**是<strong style="color:#ee2233">珠海市杰理科技股份有限公司</strong>(以下简称“本公司”)开发，专门为本公司音箱耳机类产品提供蓝牙控制开发平台。

</div>


# 一、 快速接入



为了帮助开发者快速接入<strong style="color:red">杰理之家SDK</strong>，请开发前详细阅读: 

- [杰理之家SDK开发文档(Android)](https://doc.zh-jieli.com/Apps/Android/jielihome/zh-cn/master/index.html)
- [开发说明文档](./doc/)



# 二、压缩包文件结构

```tex
apk  ---  测试APK文件夹
 ├── btsmart-V1.13.0-202601231637-113126-debug.apk    --- 杰理之家测试版本
 ├── UpdateContent.txt                                --- 更新说明
 ├── 杰理之家导出打印日志说明.pdf                     --- 杰理之家APP导出打印日志说明
code ---  参考源码工程文件夹
 ├── PiHome_V1.13.0_SDK_V4.2.0.zip                    --- 杰理之家项目源码
 ├── 杰理音频编解码库开发资料_V2.1.0_Android.zip      --- 杰理音频编解码开发资料(OPUS)
doc ---  开发文档文件夹
 ├── JieLi_Home_SDK_V4.2.0_html_zh                    --- 杰理之家SDK开发说明文档(中文版本)
 ├── JieLi_Home_SDK_V4.2.0_html_en                    --- 杰理之家SDK开发说明文档(英文版本)
 ├── ReadMe.txt
 ├── 杰理开放平台接入说明文档.pdf                     --- 接入杰理之家服务器的说明文档
 ├── 杰理之家APP用户手册V1.2.pdf                      --- 杰理之家的操作说明文档
 ├── 杰理OTA(Android)在线开发文档                     --- OTA库的开发说明文档
 ├── 杰理之家SDK(Android)开发文档                     --- 杰理之家SDK开发说明文档(线上版)
 ├── 杰理音频编码库开发说明.pdf                       --- 杰理OPUS编解码开发说明文档
 ├── JLA_V2编解码库开发说明.pdf                       --- 杰理JLA_V2编解码开发说明文档
libs --- 核心库文件夹
 ├── jl_bt_ota_V1.10.0_10931-release.aar              --- 杰理OTA相关
 ├── jl_bluetooth_rcsp_V4.0.0_40015-release.aar       --- 杰理之家SDK相关
 ├── jldecryption_v0.4-release.aar                    --- 加密解密相关
 ├── BmpConvert_V1.6.0_10604-release.aar              --- 转码静态图片(png,jpeg,bmp等)
 ├── GifConvert_V1.3.0_42-release.aar                 --- 转码Gif(动态图片)
 ├── jl_eq_V1.1.0_10101-release.aar                   --- 杰理均衡器曲线算法库
 ├── jl_audio_decode_V2.1.0_20012-release.aar         --- 杰理OPUS编解码库
 └── jl_audio_v2_V1.0.0_9-release.aar                 --- 杰理JLA_V2编解码库
杰理之家SDK(Android)发布记录.pdf
ReadMe.txt
```





# 三、版本说明

| 版本  | 日期       | 修改者 | 修改记录                                                     |
| ----- | ---------- | ------ | ------------------------------------------------------------ |
| 4.2.0 | 2026/01/21 | 钟卓成 | 1. 新增功能<br />1.1 增加LE Audio与RCSP并存功能<br />1.2 增加AI翻译功能<br />1.3 增加Auracast Broadcast功能<br />1.4 增加Gatt Over BR/EDR连接方式的支持<br />2. 优化功能<br />2.1 增加Android 15的兼容处理<br />3. 修复问题<br />3.1 修改彩屏仓本地资源<br />3.2 修复已知的问题 |
| 4.1.0 | 2025/07/18 | 钟卓成 | 1. 增加701N和707N彩屏仓SDK支持<br/>1.1 增加屏幕亮度控制<br/>1.2 增加屏幕保护程序控制<br/>1.3 增加天气同步<br/>1.4 增加消息同步 |
| 4.0.0 | 2025/04/15 | 钟卓成 | 1、分离蓝牙实现和RCSP功能实现<br/>2、优化SDK日志输出<br/>3、优化文件浏览功能<br/>4、增加Android 14的兼容处理 |
| 3.2.0 | 2023/11/23 | 钟卓成 | 1、新增 TWS 耳机一拖二功能和接口  <br />2、修复已知问题      |
| 3.0.8 | 2022/08/12 | 张焕明 | 1、新增辅听耳机的验配功能                                    |
| 3.0.7 | 2022/07/20 | 张焕明 | 1、增加支持挂脖耳机 UI                                       |
| 2.9.1 | 2021/08/25 | 钟卓成 | 1、修复手机兼容性问题                                        |
| 2.9.0 | 2021/05/18 | 陈森华 | 1、增加音箱SDK的闹钟贪睡模式<br>2、增加耳机SDK的主动降噪(ANC)设置功能 |





# 四、技术支持

- 🌐 **官方网站**: [杰理科技](https://www.zh-jieli.com/)
- 📧 **技术支持**: 请通过官方渠道联系





# 五、许可证

本项目采用 [Apache License 2.0](./LICENSE.txt) 开源协议。

```
Copyright 2024 珠海市杰理科技股份有限公司

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

<div align="center">

**© 2024 珠海市杰理科技股份有限公司 | Licensed under Apache License 2.0**

</div>

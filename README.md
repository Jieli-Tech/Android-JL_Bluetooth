[tag download]:https://github.com/Jieli-Tech/Android-JL_Bluetooth/tags
[tag_badgen]:https://img.shields.io/github/v/tag/Jieli-Tech/Android-JL_Bluetooth?style=plastic&logo=android&labelColor=ffffff&color=informational&label=Tag&logoColor=blue

# Android-JL_Bluetooth  [![tag][tag_badgen]][tag download]

<div align="center">

**杰理之家SDK(Android) - 专为杰理音箱耳机类产品提供蓝牙控制开发平台**

[中文](./README.md) · [English](./README_en.md) · [文档中心](https://doc.zh-jieli.com/Apps/Android/jielihome/zh-cn/master/index.html) · [SDK 版本历史](#八版本历史) · [报告问题](https://github.com/Jieli-Tech/Android-JL_Bluetooth/issues)

</div>

---

## 📋 目录

- [一、概述](#一概述)
- [二、运行环境](#二运行环境)
- [三、快速开始](#三快速开始)
- [四、工程结构](#四工程结构)
- [五、配置说明](#五配置说明)
- [六、调试技巧](#六调试技巧)
- [七、社区与支持](#七社区与支持)
- [八、版本历史](#八版本历史)
- [九、许可证](#九许可证)

---



## 一、概述

`Android-JL_Bluetooth` 是**珠海市杰理科技股份有限公司**为杰理音箱耳机类产品提供的蓝牙控制开发平台。本 SDK 基于<strong style="color:red">RCSP协议(远程控制系统协议)</strong>，提供完整的蓝牙控制功能和丰富的应用示例，支持以下应用场景：

| 应用类型 | 典型产品 |
|---------|---------|
| **音箱类产品** | 智能音箱、蓝牙音箱、便携音箱、Auracast音箱 |
| **耳机类产品** | TWS耳机、头戴式耳机、挂脖耳机、彩屏仓、翻译耳机 |
| **音频设备** | 蓝牙音频接收器、音频解码器、声卡、录音笔 |

**杰理之家SDK**提供了丰富的功能接口：

| 功能           | 说明                                                     |
| -------------- | -------------------------------------------------------- |
| **音乐控制**   | 手机音乐播放控制、设备音乐播放控制、ID3音乐信息显示      |
| **设备设置**   | 音量设置、状态查询、重启设备等                           |
| **文件浏览**   | 查看SD卡、U盘等存储器的音乐文件列表                      |
| **闹钟管理**   | 闹钟的增删改查, 闹钟铃声设置                             |
| **FM控制**     | FM收音功能、FM发射功能                                   |
| **灯光控制**   | 灯光闪烁, 频率, 颜色(RGB), 模式等控制, 实现酷炫效果      |
| **音效调节**   | 均衡器音效调节, 轻易打造卓越音质、 混响、高低音设置      |
| **按键设置**   | 耳机按键功能设置, 丰富耳机功能                           |
| **查找设备**   | 查找设备或查找手机                                       |
| **ANC设置**    | 噪声处理模式设置, 支持正常模式、主动降噪模式、通透模式等 |
| **彩屏仓控制** | 亮度调节，壁纸更新，屏幕保护程序更新等                   |
| **AI翻译**     | 录音翻译，面对面翻译，音视频翻译，通话翻译等             |
| **自定义命令** | 支持客户拓展功能                                         |

---



## 二、运行环境

| 类别 | 要求 | 说明 |
|------|------------|-----------|
| **操作系统** | Android 5.1+ | 支持BLE功能 |
| **硬件要求** | 支持RCSP功能的SDK | AC701N、AC707N、AC697N、AC696N、AC695N等 |
| **开发平台** | Android Studio | 建议使用最新版 |
| **语言支持** | Java/Kotlin | 提供完整的API支持 |





---

## 三、快速开始

### 3.1 克隆仓库

```bash
git clone https://github.com/Jieli-Tech/Android-JL_Bluetooth.git
cd Android-JL_Bluetooth
```



### 3.2 导入项目到Android Studio

1. 打开 Android Studio
2. 选择 "Open an existing project"
3. 导航到解压后的 `code/` 目录
4. 打开 `PiHome_V1.13.0_SDK_V4.2.0`  中的项目文件



### 3.3 添加依赖库

- **jl_bluetooth_rcsp_Vxxx-release.aar** : 蓝牙控制与RCSP协议处理相关
- **jldecryption_Vxxx-release.aar** :加密相关

**PS: xxx为版本号**

将 `libs/` 目录下的 AAR 文件添加到项目的 `libs` 目录中，并在 `build.gradle` 中添加依赖：

```gradle
dependencies {
    //1.将上面的aar文件放入工程目录中的对应moudle的lib文件夹下
	//2.在moudlu的build.gradle中添加
	implementation fileTree(include: ['*.aar'], dir: 'libs')

	implementation 'com.google.code.gson:gson:2.13.1'
}
```



### 3.4 权限配置

接入SDK时应在 `AndroidManifest.xml` 申请以下权限:

```xml
<!--使用蓝牙权限-->
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>

<!--高版本安卓系统要求-->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!--定位权限，官方要求使用蓝牙或网络开发，需要位置信息-->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```



### 3.5 运行示例应用

参考 `apk/` 目录中的测试APK，了解SDK功能和使用方法。

---



## 四、工程结构

```
Android-JL_Bluetooth/
├── apk/                                     # 测试APK文件夹
│   ├── btsmart-V1.13.0-202601231637-113126-debug.apk    # 杰理之家测试版本
│   ├── UpdateContent.txt                                # 更新说明
│   └── 杰理之家导出打印日志说明.pdf                          # 杰理之家APP导出打印日志说明
├── code/                                    # 参考源码工程文件夹
│   ├── PiHome_V1.13.0_SDK_V4.2.0                        # 杰理之家项目源码
│   └── 杰理音频编解码库开发资料_V2.1.0_Android               # 杰理音频编解码开发资料(OPUS)
├── doc/                                     # 开发文档文件夹
│   ├── JieLi_Home_SDK_V4.2.0_html_zh                    # 杰理之家SDK开发说明文档(中文版本)
│   ├── JieLi_Home_SDK_V4.2.0_html_en                    # 杰理之家SDK开发说明文档(英文版本)
│   ├── 杰理开放平台接入说明文档.pdf                          # 接入杰理之家服务器的说明文档
│   ├── 杰理之家APP用户手册V1.2.pdf                         # 杰理之家的操作说明文档
│   ├── 杰理OTA(Android)在线开发文档                        # OTA库的开发说明文档
│   ├── 杰理之家SDK(Android)开发文档                        # 杰理之家SDK开发说明文档(线上版)
│   ├── 杰理音频编码库开发说明.pdf                           # 杰理OPUS编解码开发说明文档
│   └── JLA_V2编解码库开发说明.pdf                          # 杰理JLA_V2编解码开发说明文档
├── libs/                                    # 核心库文件夹
│   ├── jl_bt_ota_V1.10.0_10931-release.aar              # 杰理OTA相关
│   ├── jl_bluetooth_rcsp_V4.0.0_40015-release.aar       # 杰理之家SDK相关
│   ├── jldecryption_v0.4-release.aar                    # 加密解密相关
│   ├── BmpConvert_V1.6.0_10604-release.aar              # 转码静态图片(png,jpeg,bmp等)
│   ├── GifConvert_V1.3.0_42-release.aar                 # 转码Gif(动态图片)
│   ├── jl_eq_V1.1.0_10101-release.aar                   # 杰理均衡器曲线算法库
│   ├── jl_audio_decode_V2.1.0_20012-release.aar         # 杰理OPUS编解码库
│   └── jl_audio_v2_V1.0.0_9-release.aar                 # 杰理JLA_V2编解码库
├── 杰理之家SDK(Android)发布记录.pdf            # SDK发布记录
└── ReadMe.txt                               # 说明文件
```

---



## 五、配置说明

### 5.1 SDK配置

```java
BluetoothOption bluetoothOption = BluetoothOption.createDefaultOption();//创建默认配置
bluetoothOption.setPriority(BluetoothOption.PREFER_BLE)//通信方式，支持ble和spp
           .setUseMultiDevice(true) //是否支持多设备管理
           .setTimeoutMs(2000)//命令超时时间, 默认2000ms
           .setMtu(509)       //调节蓝牙MTU
           .setUseDeviceAuth(true);//是否开启设备认证。 与固件工程师确认
    /**
     * 扫描设备策略
     *  - BluetoothConstant#NONE_FILTER : 不过滤设备
     *  - BluetoothConstant#ALL_FILTER : 使用所有过滤规则
     *  - BluetoothConstant#FLAG_FILTER : 仅用标识过滤规则 (音箱)
     *  - BluetoothConstant#HASH_FILTER : 仅用加密过滤规则 (耳机,音箱,等等)
     */
    //bluetoothOption.setBleScanStrategy(BluetoothConstant.ALL_FILTER);

    //修改BLE的通讯uuid
    //bluetoothOption.setBleUUID(serviceUUID, writeCharacteristicUUID, notificationCharacteristicUUID);

    //修改SPP的通讯uuid
    //bluetoothOption.setSppUUID(uuid);

//配置参数
RCSPController.init(context, bluetoothOption);
//JL_BluetoothManager.getInstance(context).configure(bluetoothOption);
```

### 5.2 BluetoothOption

SDK库蓝牙配置

| 字段                  | 描述                   | 备注                                                         |
| --------------------- | ---------------------- | ------------------------------------------------------------ |
| **priority**          | 指定通讯方式           | BluetoothOption.PREFER_BLE - BLE方式, 默认值<br />BluetoothOption.PREFER_SPP - SPP方式 |
| **reconnect**         | 是否需要重连           | 异常断开回连设备。 默认值: true                              |
| **timeoutMs**         | 命令超时时间           | BluetoothConstant.DEFAULT_SEND_CMD_TIMEOUT - 2000ms          |
| **enterLowPowerMode** | 是否进入低功耗模式     | 低功耗模式: 仅连接通讯通道。 默认值: false                   |
| **isUseMultiDevice**  | 是否使用多设备管理     | 多设备管理: 同时能保持多个通讯通道。 默认值: false           |
| **isUseDeviceAuth**   | 是否开启设备认证       | 设备认证: 互相认证设备的身份。 默认值: true 注意: 与固件工程师协商 |
| **isMandatoryUseBLE** | 是否强制使用BLE        | 强制连接BLE。默认值: false                                   |
| **isSkipNoNameDev**   | 是否跳过没有名称的设备 | 默认值: true                                                 |
| **isSupportCTKD**     | 是否支持一键连接功能   | 若支持一键连接功能，双模设备配对，会强制走经典蓝牙配对       |
| **scanFilterData**    | 过滤设备标识           | 标识目标设备的特征值, 用于过滤设备。 默认值: null            |
| **bleScanStrategy**   | 搜索设备策略           | BluetoothConstant#NONE_FILTER - 不使用过滤规则<br />BluetoothConstant#ALL_FILTER - 使用全部过滤规则, 默认值<br />BluetoothConstant#FLAG_FILTER - 仅使用标识过滤规则<br />BluetoothConstant#HASH_FILTER - 仅使用HASH加密过滤规则 |
| **bleScanMode**       | BLE扫描模式            | ScanSettings#SCAN_MODE_LOW_POWER - 低功耗模式<br />ScanSettings#SCAN_MODE_BALANCED - 均衡模式， 默认值<br />ScanSettings#SCAN_MODE_LOW_LATENCY - 低延时模式<br />(高功耗, 仅前台有效)建议前台应用使用【低延时模式】 |
| **mtu**               | BLE通讯MTU             | 取值范围: [20, 514], 默认值是20                              |
| **isUseBleBondWay**   | 是否使用BLE加密        | 默认值: false                                                |
| **bleUUIDMap**        | BLE通讯UUID合集        | BluetoothConstant#KEY_BLE_SERVICE_UUID - 服务UUIDBluetoothConstant#KEY_BLE_WRITE_CHARACTERISTIC_UUID - 写特征值UUIDBluetoothConstant#KEY_BLE_NOTIFICATION_CHARACTERISTIC_UUID - 通知特征值UUID |
| **sppUUID**           | SPP通讯UUID            | BluetoothConstant.UUID_SPP                                   |
| **cmdSnGenerator**    | 命令序列化生成器       | 用于多个杰理RCSP库统一命令序列号                             |

---



## 六、调试技巧

- **日志输出**：SDK提供详细的日志输出，可通过日志查看蓝牙连接状态和数据交互
- **设备调试**：使用**Android Studio**的``Logcat``查看实时日志
- **问题排查**：
  - **SDK：** 参考 [SDK调试说明](https://doc.zh-jieli.com/Apps/Android/jielihome/zh-cn/master/other/debug.html)
  - **杰理之家APP:** 参考[杰理之家导出打印日志说明](./apk/杰理之家导出打印日志说明.pdf)



---



## 七、社区与支持

### 7.1 技术交流

| 平台 | 联系方式 | 状态 |
|------|----------|------|
| **官方网站** | [杰理科技](https://www.zh-jieli.com/) | ✅ 活跃 |
| **GitHub Issues** | [问题反馈](https://github.com/Jieli-Tech/Android-JL_Bluetooth/issues) | ✅ 活跃 |



### 7.2 资源链接

| 资源 | 链接 |
|------|------|
| 📖 **在线文档中心** | [杰理之家SDK开发文档](https://doc.zh-jieli.com/Apps/Android/jielihome/zh-cn/master/index.html) |
| 📄 **数据手册** | [开发说明文档](./doc/) |
| 📚 **版本历史** | [版本历史](#八版本历史) |
| 🐛 **问题反馈** | [GitHub Issues](https://github.com/Jieli-Tech/Android-JL_Bluetooth/issues) |

---



## 八、版本历史

| 版本 | 日期 | 修改记录 |
|------|------|----------|
| 4.2.0 | 2026/01/21 | 1. 新增功能<br />1.1 增加LE Audio与RCSP并存功能<br />1.2 增加AI翻译功能<br />1.3 增加Auracast Broadcast功能<br />1.4 增加Gatt Over BR/EDR连接方式的支持<br />2. 优化功能<br />2.1 增加Android 15的兼容处理<br />3. 修复问题<br />3.1 修改彩屏仓本地资源<br />3.2 修复已知的问题 |
| 4.1.0 | 2025/07/18 | 1. 增加701N和707N彩屏仓SDK支持<br/>1.1 增加屏幕亮度控制<br/>1.2 增加屏幕保护程序控制<br/>1.3 增加天气同步<br/>1.4 增加消息同步 |
| 4.0.0 | 2025/04/15 | 1、分离蓝牙实现和RCSP功能实现<br/>2、优化SDK日志输出<br/>3、优化文件浏览功能<br/>4、增加Android 14的兼容处理 |
| 3.2.0 | 2023/11/23 | 1、新增 TWS 耳机一拖二功能和接口<br />2、修复已知问题 |
| 3.0.8 | 2022/08/12 | 1、新增辅听耳机的验配功能 |
| 3.0.7 | 2022/07/20 | 1、增加支持挂脖耳机 UI |
| 2.9.1 | 2021/08/25 | 1、修复手机兼容性问题 |
| 2.9.0 | 2021/05/18 | 1、增加音箱SDK的闹钟贪睡模式<br>2、增加耳机SDK的主动降噪(ANC)设置功能 |

---



## 九、许可证

本项目采用 [Apache License 2.0](./LICENSE) 开源协议。

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
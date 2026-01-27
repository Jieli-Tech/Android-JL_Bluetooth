# **Android-JL_Bluetooth**

The Bluetooth SDK for Android 

![Android](https://img.shields.io/badge/Android-5.1+-blue.svg)![Android Studio](https://img.shields.io/badge/Android Studio-Latest-orange.svg)![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)                                                                                                             [中文](https://github.com/Jieli-Tech/Android-JL_Bluetooth/blob/main/README.md) | English



<h1 style="text-align:left;font:24px;color:#000000">JieLi Home SDK(Android)</h>

The **JieLi Home SDK** is developed by **Zhuhai JieLi Technology Co., Ltd.** ("the Company") to provide a dedicated Bluetooth control development platform for the Company's speaker and headphone products.



# 1 Quick Integration

To help developers quickly integrate the <strong style="color:red">JieLi Home SDK</strong>, please carefully read the following before development:

- [JieLi Home SDK Documentation (Android)](https://doc.zh-jieli.com/Apps/Android/jielihome/en-us/master/index.htmll)
- [Development Guide](./doc/)



# 2. Compressed File Structure

```tex
apk  --- Test APK folder
 ├── btsmart-V1.13.0-202601231637-113126-debug.apk    --- JieLi Home test version
 ├── UpdateContent.txt                                --- Release notes
 ├── Instructions_for_Exporting_Logs_from_JieLi_Home_APP.pdf --- Log export guide
code --- Reference source code project folder
 ├── PiHome_V1.13.0_SDK_V4.2.0.zip                    --- JieLi Home project source code
 ├── JieLi_Audio_Codec_Development_Guide_V2.1.0_Android.zip --- OPUS codec development resources
doc --- Documentation folder
 ├── JieLi_Home_SDK_V4.2.0_html_zh                    --- SDK documentation (Chinese)
 ├── JieLi_Home_SDK_V4.2.0_html_en                    --- SDK documentation (English)
 ├── ReadMe.txt
 ├── JieLi_Open_Platform_Integration_Guide.pdf        --- Server integration guide
 ├── JieLi_Home_APP_User_Manual_V1.2.pdf              --- User manual
 ├── JieLi_OTA(Android)_Online_Development_Doc        --- OTA library guide
 ├── JieLi_Home_SDK(Android)_Development_Documentation--- Online SDK documentation
 ├── JieLi_Audio_Codec_Development_Guide.pdf          --- OPUS codec guide
 ├── JLA_V2_Codec_Development_Guide.pdf               --- JLA_V2 codec guide
libs --- Core library folder
 ├── jl_bt_ota_V1.10.0_10931-release.aar              --- OTA-related library
 ├── jl_bluetooth_rcsp_V4.0.0_40015-release.aar       --- JieLi Home SDK core library
 ├── jldecryption_v0.4-release.aar                    --- Encryption/decryption utilities
 ├── BmpConvert_V1.6.0_10604-release.aar              --- Static image converter (PNG, JPEG, BMP, etc.)
 ├── GifConvert_V1.3.0_42-release.aar                 --- GIF animation converter
 ├── jl_eq_V1.1.0_10101-release.aar                   --- Equalizer curve algorithm library
 ├── jl_audio_decode_V2.1.0_20012-release.aar         --- OPUS audio codec library
 └── jl_audio_v2_V1.0.0_9-release.aar                 --- JLA_V2 audio codec library
JieLi_Home_SDK(Android)_Release_Notes.pdf
ReadMe.txt
```



# 3. Release History

| Version | Date       | Author          | Changes                                                      |
| :------ | :--------- | :-------------- | :----------------------------------------------------------- |
| 4.2.0   | 2026/01/21 | ZhuoCheng Zhong | **1. New Features** <br />1.1 Added LE Audio & RCSP coexistence <br />1.2 Added AI translation <br />1.3 Added Auracast Broadcast <br />1.4 Added GATT over BR/EDR support <br />**2. Optimizations** <br />2.1 Enhanced Android 15 compatibility <br />**3. Bug Fixes** <br />3.1 Fixed local resource handling for color-screen cases <br />3.2 Resolved known issues |
| 4.1.0   | 2025/07/18 | ZhuoCheng Zhong | 1. Added support for AC701N/AC707N color-screen cases <br />1.1 Screen brightness control <br />1.2 Screensaver control <br />1.3 Weather sync <br />1.4 Message sync |
| 4.0.0   | 2025/04/15 | ZhuoCheng Zhong | 1. Separated Bluetooth implementation from RCSP logic <br />2. Improved SDK logging <br />3. Enhanced file browsing <br />4. Added Android 14 compatibility |
| 3.2.0   | 2023/11/23 | ZhuoCheng Zhong | 1. Added TWS one-to-two pairing feature <br />2. Fixed known issues |
| 3.0.8   | 2022/08/12 | HuanMing Zhang  | 1. Added hearing aid fitting functionality                   |
| 3.0.7   | 2022/07/20 | HuanMing Zhang  | 1. Added UI support for neckband headphones                  |
| 2.9.1   | 2021/08/25 | ZhuoCheng Zhong | 1. Fixed mobile device compatibility issues                  |
| 2.9.0   | 2021/05/18 | SenHua Chen     | 1. Added snooze mode for speaker alarms <br />2. Added ANC settings for headphones |



# 4. Technical Support

- 🌐 **Official Website**: [JieLi Technology](https://www.zh-jieli.com/)
- 📧 **Support**: Please contact via official channels



# 5. License

This project is licensed under the [Apache License 2.0](./LICENSE.txt).

```tex
Copyright 2024 Zhuhai JieLi Technology Co., Ltd.

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

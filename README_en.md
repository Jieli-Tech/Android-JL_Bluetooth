[tag download]:https://github.com/Jieli-Tech/Android-JL_Bluetooth/tags
[tag_badgen]:https://img.shields.io/github/v/tag/Jieli-Tech/Android-JL_Bluetooth?style=plastic&logo=android&labelColor=ffffff&color=informational&label=Tag&logoColor=blue

# Android-JL_Bluetooth  [![tag][tag_badgen]][tag download]

<div align="center">

**JieLi Home SDK (Android) - A Bluetooth Control Development Platform for JieLi Speakers and Earphones**

[Chinese](./README.md) · [English](./README_en.md) · [Documentation Center](https://doc.zh-jieli.com/Apps/Android/jielihome/en-us/master/index.html) · [SDK Changelog](#8-version-history) · [Report Issues](https://github.com/Jieli-Tech/Android-JL_Bluetooth/issues)

</div>

---

## Table of Contents

- [1. Overview](#1-overview)
- [2. Environment Requirements](#2-environment-requirements)
- [3. Quick Start](#3-quick-start)
- [4. Project Structure](#4-project-structure)
- [5. Configuration Guide](#5-configuration-guide)
- [6. Debugging Tips](#6-debugging-tips)
- [7. Community & Support](#7-community--support)
- [8. Version History](#8-version-history)
- [9. License](#9-license)

---

## 1. Overview

`Android-JL_Bluetooth` is a Bluetooth control development platform provided by **Zhuhai Jieli Technology Co., Ltd.** for JieLi speakers and earphones. This SDK is based on the <strong style="color:red">RCSP (Remote Control System Protocol)</strong>, offering comprehensive Bluetooth control features and rich application examples, supporting the following use cases:

| Application Type | Typical Products |
|---------|---------|
| **Speakers** | Smart speakers, Bluetooth speakers, portable speakers, Auracast speakers |
| **Earphones** | TWS earphones, over-ear headphones, neckband earphones, color-screen charging cases, translation earphones |
| **Audio Devices** | Bluetooth audio receivers, audio decoders, sound cards, voice recorders |

**JieLi Home SDK** provides a rich set of functional interfaces:

| Feature | Description |
| -------------- | -------------------------------------------------------- |
| **Music Control** | Phone music playback control, device music playback control, ID3 music info display |
| **Device Settings** | Volume settings, status queries, device restart, etc. |
| **File Browsing** | View music file lists on SD card, USB drive, and other storage devices |
| **Alarm Management** | Add, delete, modify, and query alarms; set alarm ringtones |
| **FM Control** | FM radio reception, FM transmission |
| **Light Control** | Light flashing, frequency, color (RGB), modes, and other controls for cool effects |
| **Sound Effects** | Equalizer adjustments, easy creation of excellent sound quality, reverb, bass/treble settings |
| **Key Settings** | Earphone button function configuration, enriching earphone features |
| **Find Device** | Find the device or find the phone |
| **ANC Settings** | Noise handling mode settings, supporting normal mode, active noise cancellation mode, transparency mode, etc. |
| **Color Screen Case Control** | Brightness adjustment, wallpaper updates, screensaver updates, etc. |
| **AI Translation** | Recording translation, face-to-face translation, audio/video translation, call translation, etc. |
| **Custom Commands** | Supports customer-extended functionality |

---

## 2. Environment Requirements

| Category | Requirement | Description |
|------|------------|------|
| **Operating System** | Android 5.1+ | Supports BLE functionality |
| **Hardware** | SDK with RCSP support | AC701N, AC707N, AC697N, AC696N, AC695N, etc. |
| **Development Platform** | Android Studio | Latest version recommended |
| **Language Support** | Java/Kotlin | Full API support provided |

---

## 3. Quick Start

### 3.1 Clone the Repository

```bash
git clone https://github.com/Jieli-Tech/Android-JL_Bluetooth.git
cd Android-JL_Bluetooth
```

### 3.2 Import the Project into Android Studio

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to the extracted `code/` directory
4. Open the project files in `PiHome_V1.13.0_SDK_V4.2.0`

### 3.3 Add Dependencies

- **jl_bluetooth_rcsp_Vxxx-release.aar**: Bluetooth control and RCSP protocol processing
- **jldecryption_Vxxx-release.aar**: Encryption-related

**Note: xxx represents the version number**

Add the AAR files from the `libs/` directory to your project's `libs` folder, and add the dependencies in `build.gradle`:

```gradle
dependencies {
    // 1. Place the above aar files into the corresponding module's lib folder
    // 2. Add to the module's build.gradle
    implementation fileTree(include: ['*.aar'], dir: 'libs')

    implementation 'com.google.code.gson:gson:2.13.1'
}
```

### 3.4 Permission Configuration

When integrating the SDK, the following permissions should be declared in `AndroidManifest.xml`:

```xml
<!-- Bluetooth permissions -->
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>

<!-- Required for newer Android versions -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Location permission: officially required for Bluetooth or network development -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### 3.5 Run the Sample Application

Refer to the test APKs in the `apk/` directory to learn about SDK features and usage.

---

## 4. Project Structure

```
Android-JL_Bluetooth/
├── apk/                                     # Test APK folder
│   ├── btsmart-V1.13.0-202601231637-113126-debug.apk    # JieLi Home test version
│   ├── UpdateContent.txt                                # Update notes
│   └── JieLi Home App Export Log Instructions.pdf          # Instructions for exporting logs from JieLi Home app
├── code/                                    # Reference source code project folder
│   ├── PiHome_V1.13.0_SDK_V4.2.0                        # JieLi Home project source code
│   └── JieLi Audio Codec Library Dev Resources V2.1.0_Andrian       # JieLi audio codec development resources (OPUS)
├── doc/                                     # Documentation folder
│   ├── JieLi_Home_SDK_V4.2.0_html_zh                    # JieLi Home SDK development docs (Chinese version)
│   ├── JieLi_Home_SDK_V4.2.0_html_en                    # JieLi Home SDK development docs (English version)
│   ├── JieLi Open Platform Integration Guide.pdf             # Guide for integrating with JieLi Home server
│   ├── JieLi Home APP User Manual V1.2.pdf                   # JieLi Home operation guide
│   ├── JieLi OTA (Android) Online Development Docs           # OTA library development guide
│   ├── JieLi Home SDK (Android) Development Docs             # JieLi Home SDK development docs (online version)
│   ├── JieLi Audio Codec Library Dev Guide.pdf                # JieLi OPUS codec development guide
│   └── JLA_V2 Codec Library Dev Guide.pdf                     # JieLi JLA_V2 codec development guide
├── libs/                                    # Core library folder
│   ├── jl_bt_ota_V1.10.0_10931-release.aar              # JieLi OTA related
│   ├── jl_bluetooth_rcsp_V4.0.0_40015-release.aar       # JieLi Home SDK related
│   ├── jldecryption_v0.4-release.aar                    # Encryption/decryption related
│   ├── BmpConvert_V1.6.0_10604-release.aar              # Static image transcoding (png, jpeg, bmp, etc.)
│   ├── GifConvert_V1.3.0_42-release.aar                 # GIF (animated image) transcoding
│   ├── jl_eq_V1.1.0_10101-release.aar                   # JieLi equalizer curve algorithm library
│   ├── jl_audio_decode_V2.1.0_20012-release.aar         # JieLi OPUS codec library
│   └── jl_audio_v2_V1.0.0_9-release.aar                 # JieLi JLA_V2 codec library
├── JieLi Home SDK (Android) Release Notes.pdf  # SDK release notes
└── ReadMe.txt                               # Instructions file
```

---

## 5. Configuration Guide

### 5.1 SDK Configuration

```java
BluetoothOption bluetoothOption = BluetoothOption.createDefaultOption(); // Create default configuration
bluetoothOption.setPriority(BluetoothOption.PREFER_BLE) // Communication method: supports BLE and SPP
           .setUseMultiDevice(true) // Whether to support multi-device management
           .setTimeoutMs(2000) // Command timeout, default 2000ms
           .setMtu(509)       // Adjust Bluetooth MTU
           .setUseDeviceAuth(true); // Whether to enable device authentication. Confirm with firmware engineer.
    /**
     * Device scan strategy
     *  - BluetoothConstant#NONE_FILTER : No device filtering
     *  - BluetoothConstant#ALL_FILTER : Use all filtering rules
     *  - BluetoothConstant#FLAG_FILTER : Use only flag-based filtering (speakers)
     *  - BluetoothConstant#HASH_FILTER : Use only encrypted filtering (earphones, speakers, etc.)
     */
    //bluetoothOption.setBleScanStrategy(BluetoothConstant.ALL_FILTER);

    // Modify BLE communication UUID
    //bluetoothOption.setBleUUID(serviceUUID, writeCharacteristicUUID, notificationCharacteristicUUID);

    // Modify SPP communication UUID
    //bluetoothOption.setSppUUID(uuid);

// Configure parameters
RCSPController.init(context, bluetoothOption);
//JL_BluetoothManager.getInstance(context).configure(bluetoothOption);
```

### 5.2 BluetoothOption

Bluetooth configuration for the SDK library

| Field                  | Description                   | Notes                                                         |
| --------------------- | ---------------------- | ------------------------------------------------------------ |
| **priority**          | Specify communication method | BluetoothOption.PREFER_BLE - BLE mode, default<br />BluetoothOption.PREFER_SPP - SPP mode |
| **reconnect**         | Whether to reconnect   | Reconnect on abnormal disconnection. Default: true                              |
| **timeoutMs**         | Command timeout        | BluetoothConstant.DEFAULT_SEND_CMD_TIMEOUT - 2000ms          |
| **enterLowPowerMode** | Whether to enter low-power mode | Low-power mode: only connect communication channel. Default: false                   |
| **isUseMultiDevice**  | Whether to use multi-device management | Multi-device management: maintain multiple communication channels simultaneously. Default: false           |
| **isUseDeviceAuth**   | Whether to enable device authentication | Device authentication: mutual identity verification. Default: true Note: Coordinate with firmware engineer |
| **isMandatoryUseBLE** | Whether to force BLE usage | Force BLE connection. Default: false                                   |
| **isSkipNoNameDev**   | Whether to skip devices without names | Default: true                                                 |
| **isSupportCTKD**     | Whether to support one-touch connection   | If one-touch connection is supported, dual-mode devices will force classic Bluetooth pairing       |
| **scanFilterData**    | Filter device identifiers  | Characteristic values identifying target devices, used for filtering. Default: null            |
| **bleScanStrategy**   | Device search strategy   | BluetoothConstant#NONE_FILTER - No filtering rules<br />BluetoothConstant#ALL_FILTER - Use all filtering rules, default<br />BluetoothConstant#FLAG_FILTER - Use only flag-based filtering rules<br />BluetoothConstant#HASH_FILTER - Use only HASH encryption filtering rules |
| **bleScanMode**       | BLE scan mode            | ScanSettings#SCAN_MODE_LOW_POWER - Low power mode<br />ScanSettings#SCAN_MODE_BALANCED - Balanced mode, default<br />ScanSettings#SCAN_MODE_LOW_LATENCY - Low latency mode<br />(High power consumption, foreground only) Recommended to use [Low Latency Mode] for foreground apps |
| **mtu**               | BLE communication MTU    | Range: [20, 514], default is 20                              |
| **isUseBleBondWay**   | Whether to use BLE encryption | Default: false                                                |
| **bleUUIDMap**        | BLE communication UUID collection | BluetoothConstant#KEY_BLE_SERVICE_UUID - Service UUID<br />BluetoothConstant#KEY_BLE_WRITE_CHARACTERISTIC_UUID - Write characteristic UUID<br />BluetoothConstant#KEY_BLE_NOTIFICATION_CHARACTERISTIC_UUID - Notification characteristic UUID |
| **sppUUID**           | SPP communication UUID   | BluetoothConstant.UUID_SPP                                   |
| **cmdSnGenerator**    | Command sequence generator | Used to unify command sequence numbers across multiple JieLi RCSP libraries                             |

---

## 6. Debugging Tips

- **Log Output**: The SDK provides detailed log output, allowing you to view Bluetooth connection status and data interactions via logs.
- **Device Debugging**: Use **Android Studio**'s `Logcat` to view real-time logs.
- **Troubleshooting**:
  - **SDK**: Refer to [SDK Debugging Guide](https://doc.zh-jieli.com/Apps/Android/jielihome/en-us/master/other/debug.html)
  - **JieLi Home APP**: Refer to [JieLi Home Export Log Instructions](./apk/杰理之家导出打印日志说明.pdf)

---

## 7. Community & Support

### 7.1 Technical Exchange

| Platform | Contact | Status |
|------|----------|------|
| **Official Website** | [JieLi Technology](https://www.zh-jieli.com/) | ✅ Active |
| **GitHub Issues** | [Issue Tracker](https://github.com/Jieli-Tech/Android-JL_Bluetooth/issues) | ✅ Active |

### 7.2 Resource Links

| Resource | Link |
|------|------|
| 📖 **Online Documentation Center** | [JieLi Home SDK Development Docs](https://doc.zh-jieli.com/Apps/Android/jielihome/en-us/master/index.html) |
| 📄 **Datasheets** | [Development Docs](./doc/) |
| 📚 **Version History** | [Version History](#8-version-history) |
| 🐛 **Issue Tracking** | [GitHub Issues](https://github.com/Jieli-Tech/Android-JL_Bluetooth/issues) |

---

## 8. Version History

| Version | Date | Change Log |
|------|------|----------|
| 4.2.0 | 2026/01/21 | 1. New Features<br />1.1 Added LE Audio and RCSP coexistence functionality<br />1.2 Added AI translation functionality<br />1.3 Added Auracast Broadcast functionality<br />1.4 Added support for Gatt Over BR/EDR connection method<br />2. Optimizations<br />2.1 Added Android 15 compatibility handling<br />3. Bug Fixes<br />3.1 Modified color-screen charging case local resources<br />3.2 Fixed known issues |
| 4.1.0 | 2025/07/18 | 1. Added 701N and 707N color-screen case SDK support<br/>1.1 Added screen brightness control<br/>1.2 Added screensaver control<br/>1.3 Added weather synchronization<br/>1.4 Added message synchronization |
| 4.0.0 | 2025/04/15 | 1. Separated Bluetooth implementation and RCSP functionality<br/>2. Optimized SDK log output<br/>3. Optimized file browsing functionality<br/>4. Added Android 14 compatibility handling |
| 3.2.0 | 2023/11/23 | 1. Added TWS earphone one-drag-two functionality and APIs<br />2. Fixed known issues |
| 3.0.8 | 2022/08/12 | 1. Added fitting functionality for hearing aid earphones |
| 3.0.7 | 2022/07/20 | 1. Added support for neckband earphone UI |
| 2.9.1 | 2021/08/25 | 1. Fixed phone compatibility issues |
| 2.9.0 | 2021/05/18 | 1. Added snooze mode for speaker SDK alarms<br>2. Added active noise cancellation (ANC) settings for earphone SDK |

---

## 9. License

This project is licensed under the [Apache License 2.0](./LICENSE).

```
Copyright 2024 Zhuhai Jieli Technology Co., Ltd.

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
**© 2024 Zhuhai Jieli Technology Co., Ltd. | Licensed under Apache License 2.0**
</div>

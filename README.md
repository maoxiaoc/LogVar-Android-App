# Logvar Android App

在 Android 手机上运行弹幕 API 服务，为兼容播放器提供弹幕接口。

当前版本：**测试版 3.0.0（3.0.0-beta）**。服务随应用在手机本地运行，无需 Termux，也无需另行部署服务器。测试版的接口兼容性、后台运行和弹幕源可用性仍需持续验证。

## 项目来源

本项目基于 [huangxd-/danmu_api](https://github.com/huangxd-/danmu_api) 的服务端代码开发，针对 Android 本地运行增加原生界面、前台服务、弹幕源管理、缓存管理及移动端资源控制。界面设计参考了 [m3e-canvas](https://github.com/lnkiai/m3e-canvas) 和本仓库中的设计稿。

本项目由独立维护者开发，属于非官方 Android 实现，不代表上游作者或任何视频平台。感谢上游项目及其贡献者。

## 功能

- 启动、停止手机本地弹幕服务，查看并复制 API 地址。
- 启用或停用弹幕源、拖动调整优先级，以及配置多源合并。
- 检测弹幕源的搜索、分集读取和弹幕下载流程。
- 查看并清理搜索、弹幕和匹配数据缓存。
- 管理 API Token，配置后台保持及开机启动行为。

弹幕源受平台接口、网络、地区和访问限制影响；一次检测成功不代表持续可用，零条弹幕也不一定表示接口失效。

## 下载与使用

在 [Releases](https://github.com/maoxiaoc/LogVar-Android-App/releases) 中选择 **Logvar 测试版 3.0.0**：

| 安装包 | 适用设备 |
| --- | --- |
| `Logvar-3.0.0-beta-arm64-v8a.apk` | 大多数现代 Android 手机 |
| `Logvar-3.0.0-beta-armeabi-v7a.apk` | 支持 32 位 ARM 应用的设备 |
| `Logvar-3.0.0-beta-x86_64.apk` | x86_64 设备或模拟器 |

最低支持 Android 8.0（API 26）。三个包按架构区分，只需安装匹配设备的一份。

1. 安装并打开应用，启动弹幕服务。
2. 点击首页“复制 API 地址”。
3. 在支持自定义弹幕接口的播放器中填写完整地址，包括 Token。
4. 其他设备使用时，需要与手机网络互通，例如连接同一 Wi-Fi 或手机热点；仅开启移动数据并不意味着其他设备可以通过公网访问手机。
5. 如需后台持续使用，开启“保持前台运行”，并根据系统提示允许相关通知和后台运行权限。

API Token 用于播放器访问；管理 Token 用于管理功能，不应填写到播放器或公开分享。当前 API 使用 HTTP，适合可信的本机或局域网环境，请勿直接暴露到公网。分享截图或日志前，请隐藏完整 API 地址、Token、Cookie 和个人信息。

## 源码与构建

仓库根目录就是 Android 工程：

```text
app/                                  Android 界面、服务及原生桥接
app/src/main/assets/nodejs-project/    APK 内嵌服务代码与入口
app/libnode/                          Node 原生运行库及头文件
server/                               服务端源码、依赖清单与锁文件
scripts/                              构建准备脚本
tests/                                部分功能与资源控制检查
```

构建环境：JDK 17、Android SDK 36、NDK 27.3.13750724、CMake，以及 Node.js/npm 和兼容 Android Gradle Plugin 8.7.3 的 Gradle。当前仓库未提供 Gradle Wrapper，需自行配置 Gradle。

在 Windows PowerShell 中，从工程根目录准备嵌入式服务依赖：

```powershell
./scripts/prepare-android-assets.ps1
gradle :app:assembleDebug
```

依赖通过 `server/package-lock.json` 安装；`node_modules` 不纳入源码管理。APK 实际加载的是 assets 下的服务代码，修改 `server/` 时需同步对应的内嵌代码。

构建签名 release 前，在本地创建 `release-signing.properties`：

```properties
storeFile=path/to/your-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

然后执行 `gradle :app:assembleRelease`。以上均为占位值；签名私钥和密码不随仓库提供。自行签名的 APK 通常不能覆盖安装由维护者签名的版本。

## 开源许可与第三方资源

服务端代码依据上游 GNU AGPL v3 许可证使用和修改，许可全文见 [server/LICENSE](server/LICENSE)，项目来源和第三方依赖说明见 [NOTICE.md](NOTICE.md)。分发修改版本或 APK 时，应遵守适用许可证，包括保留版权声明、标明修改并提供对应源码等要求；第三方组件继续适用各自许可证。

当前沿用上游项目使用的 LogVar 名称及图标。本说明仅交代来源和非官方关系，不表示已取得名称、商标或图标的单独授权，也不向使用者授予相关权利。

本项目提供弹幕接口工具，不授予第三方平台内容的使用或再分发权利。使用者及分发者应遵守适用法律和平台规则；“学习交流”用途不豁免相应义务。

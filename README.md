# Logvar Android App

Logvar 是一个独立的 Android 弹幕客户端，当前仓库对应测试版 3.0.0。它基于 [huangxd-/danmu_api](https://github.com/huangxd-/danmu_api) 的服务能力，并针对手机本地运行、前台服务、缓存和弹幕源检测进行了适配。

## 项目状态

当前版本为测试版 3.0.0。Release 中提供按 CPU 架构区分的 APK；源码仓库不提交 APK、签名文件、构建缓存或本地配置。

Logvar 是个人制作的非官方客户端，不代表上游作者或任何视频平台的官方发行与授权。当前版本沿用上游项目的 LogVar 名称和图标，仅用于项目识别。

## 构建

1. 使用 Android Studio 打开本目录。
2. 准备 Android SDK、NDK 和 Node.js Android 运行库。
3. 运行 `scripts/prepare-android-assets.ps1` 安装嵌入式服务依赖。
4. 使用 Gradle 构建 `app` 模块。

不要把 `local.properties`、签名文件、Token、Cookie、密码或个人日志提交到仓库。

## 许可证与使用说明

服务端代码基于 [danmu_api](https://github.com/huangxd-/danmu_api) 修改，遵循 GNU Affero General Public License v3.0，详见 [LICENSE](LICENSE) 和 [NOTICE.md](NOTICE.md)。

本项目主要用于个人学习和技术交流。弹幕源来自第三方平台，使用时请遵守相关平台规则和当地法律；项目不授权第三方内容的再分发，也不用于公开提供第三方内容服务。

如发现项目中存在不当内容，请联系维护者处理。

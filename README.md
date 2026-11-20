# 焦点岛 Playground

一款用来编辑、预览和发送 HyperOS 焦点通知 / 超级岛参数的 Android 实验台。界面为原生 Android View 实现，无第三方运行库。

## 功能

- **模板预设**：充电、包裹、出行、专注计时、音乐、咖啡、运动、即时消息、日程、外卖、航班及空白自定义，共 12 组。
- **编辑与预览**：修改主标题、前置描述、补充内容和状态栏摘要；预览为应用内示意。
- **JSON 工作流**：生成 `param_v2` JSON；支持系统文件选择器导入 / 导出、复制分享及手工编辑。
- **立即 / 延时发送**：将 JSON 放入 Android 通知 extras 的 `miui.focus.param`；延时任务通过 `AlarmManager.setAndAllowWhileIdle` 安排，可取消。
- **设备状态**：尽力读取焦点协议版本、超级岛系统特性和通知开关；不可读取时会明确显示未检测到。

## 云端构建

GitHub Actions 在推送到 `main`、推送 `v*` 标签或手动触发时执行 Gradle 云构建。`main` 构建可从 Actions 的 `focus-island-playground-debug` artifact 下载；推送版本标签会额外发布 APK Release。

## 使用提醒

1. 安装 APK 后允许通知，再尝试立即或延时发送。
2. HyperOS 是否展示焦点通知或超级岛由系统版本、应用通知 / 焦点通知设置和厂商白名单决定；应用只按公开接口发送参数，不尝试绕过白名单。
3. 延时发送为非精确闹钟，省电 / 待机策略可能带来延迟；重启后系统也可能清除待发送项。
4. 输出的预设字段是可编辑示例；官方 JSON schema 及设备具体渲染支持可能随 HyperOS 版本变化。请先使用普通测试通知验证。
5. Release 附带的是 debug 签名 APK，适用于测试与侧载，不是 Play 商店发行包。

## 技术

- Android Gradle Plugin 8.7.3 / Kotlin 2.0.21 / JDK 17
- compileSdk 35 / minSdk 26 / targetSdk 35
- 包名：`com.leaf.focusislandplayground`

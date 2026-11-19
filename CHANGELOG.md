# Changelog

## [1.7.0] - 2026-11-20

### 新增

- 支持读取 GT 机器的 `IGregTechDeviceInformation`：脚本可通过 `ooInfo`、`ooInfoMap`、
  `ooInfoDecodable` 访问 `writeToNBT()` 之外的信息（例如束流线、扫描仪面板上的数据）。
  全部通过反射实现并逐项探测能力，旧版本 GT 会自动降级，不影响 WAILA 显示。

### 修复

- 修正 NEI / waila / GT5U 的依赖版本：原版本已不在 GTNH Maven ，导致项目无法解析依赖、
  IDE 无法索引。
- 修复脚本合并时正则严重回溯的问题：`mergeConfig()` 中的变长 lookbehind 在长脚本上耗时约 9.6 秒，
  游戏内 `Initialization` 阶段约 23 秒；改为分组匹配后降至约 0.02 秒，输出与原实现完全一致。

### 变更

- 移除内置的 `GTNHScript`：默认不加载的历史实现，其显示内容已由 XML 脚本覆盖。

## 兼容性

| 环境 | GT 版本 | Waila | Nashorn 来源 |
| --- | --- | --- | --- |
| GTNH 2.8 | 5.09.51+ | 1.8.x | JDK 8 自带 |
| GTNH 2.9 | 5.09.54+ | 1.19.x | lwjgl3ify 提供 |
| 其他（Java 11+） | — | — | 首次启动自动下载 |

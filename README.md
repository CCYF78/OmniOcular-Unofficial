# OmniOcular - Unofficial

[OmniOcular](https://github.com/exzhawk/OmniOcular) 的非官方增强版，为 GT New Horizons 提供高度可定制的
WAILA 信息面板。

## 特性

- **XML 脚本驱动**：显示内容完全由脚本决定，支持游戏内热重载
- **服务端 / 客户端脚本**：服务端可统一分发脚本，客户端也可强制使用本地脚本、拒绝服务端脚本
- **显示开关**：可单独关闭各类信息显示，不需要的内容直接隐藏
- **黑名单**：屏蔽 NBT 数据过大的方块，避免查看时卡顿
- **缓存机制**：激进的缓存优化，减少 WAILA 刷新带来的 FPS 损耗
- **Java 脚本扩展**：复杂脚本可以实现 `IScript` 接口用 Java 编写，性能更高
- **GT 设备信息读取**：脚本可访问 GT 机器 `writeToNBT()` 之外的信息

## 安装

1. 将 `OmniOcularUnofficial-<version>.jar` 放入 `mods/` 目录
2. 依赖 NotEnoughItems 与 Waila
3. 首次启动后生成配置 `config/Omni Ocular Unofficial.cfg`

## 使用

### XML 脚本

脚本可参考 [GTNH_OmniOcular](https://github.com/CCYF78/GTNH_OmniOcular)。

1. 将 `.xml` 脚本放入 `config/OmniOcular/` 目录
2. 游戏内执行 `/oor` 即可热重载脚本，无需重启
3. 服务端与客户端脚本的取舍见 `config/Omni Ocular Unofficial.cfg`，也可在游戏内修改

### Java 脚本

实现 `me.exz.omniocular.IScript`，再把类的全名加入配置文件中的 `scriptClassName` 列表。

## 与 GT 的集成

脚本除了 `writeToNBT()` 的内容外，还能读取 GT 机器通过 `IGregTechDeviceInformation`
暴露的设备信息（即 IC2 扫描 / GT 扫描仪显示的内容）。
旧版本 GT 会自动降级。

注入到脚本 NBT 中的键：

| 键 | 内容 | 可用条件 |
| --- | --- | --- |
| `ooInfo` | `getInfoData()` 返回的字符串数组 | 所有 GT 版本 |
| `ooInfoMap` | `getInfoMap()` 返回的键值对 | GT 5.09.51.119 及以上 |
| `ooInfoDecodable` | `true` 表示条目需用 `decode()` 解码；`false` 表示条目已是文本 | 所有 GT 版本 |

示例：

```js
// 解码单条信息
function ooText(i) {
    var raw = nbt['ooInfo'][i];
    if (raw == undefined) return "";
    if (nbt['ooInfoDecodable']) {
        return Java.type('gregtech.api.interfaces.tileentity.IGregTechDeviceInformation').decode(raw);
    }
    return raw;
}

// 按 key 前缀查找，避免依赖条目顺序（仅 ooInfoDecodable 为 true 时可用）
function findInfo(prefix) {
    if (nbt['ooInfo'] == undefined) return null;
    for (var i = 0; i < nbt['ooInfo'].length; i++) {
        var raw = nbt['ooInfo'][i];
        if (raw != undefined && raw.indexOf(prefix) == 0) return raw;
    }
    return null;
}

// 例如取多方块通用的「功率」
var usage = findInfo("GT5U.multiblock.usage");
```

> 不同机器返回的条目数量和顺序并不相同，建议按 key 前缀查找。

## 兼容性

- Minecraft 1.7.10
- GTNH 2.8（GT 5.09.51+、Waila 1.8.x）
- GTNH 2.9（GT 5.09.54+、Waila 1.19.x）
- Java 8（JDK 自带 Nashorn）、Java 17 / 21 / 25（GTNH 由 lwjgl3ify 提供 Nashorn）；其他环境首次启动会自动下载
- 未对低版本 GTNH 进行兼容性测试，不保证对过低版本的支持

## 致谢

- exzhawk（Epix）：原版 OmniOcular
- wohaopa，Taskeren：Unofficial 版维护
- 所有脚本贡献者

## License

见 [LICENSE](LICENSE)。

# Minecraft Disco-like CRPG Mod 技术计划

> 项目定位：在 Minecraft Java Edition 中制作一个类《极乐迪斯科》/ Esoteric Ebb 风格的 CRPG 模组。核心不是战斗，而是“可注视对象 → 高亮 → 近距离交互 → 对话树/行动/内心想法 → 属性检定 → 状态分支 → 可调试的叙事数据”。

最后更新：2026-05-29

---

## 0. 结论摘要

### 推荐路线

**首选：Fabric + Minecraft Java Edition 26.1.2 + JDK 25。**

理由：

- 26.1.2 是当前稳定发布线的热修版本；26.1 开始的技术变化包括 Java 25、数据包版本 101.1、资源包版本 84、游戏可执行文件不再混淆等。
- Fabric 文档已更新到 26.1.2，Fabric API 也已有 26.1.2 发布。
- 本项目的核心能力主要依赖自研逻辑：射线检测、HUD 提示、自定义 Screen、数据驱动对话、服务器端检定、实体 routine。Fabric 对这类轻量但深入的客户端/服务端交互开发比较合适。
- GeckoLib、Cloth Config、YACL、owo-lib 等关键库已经有 26.1.x 或 26.1.2 版本。

### 保守路线

**备选：Fabric + Minecraft Java Edition 1.21.11 + JDK 21。**

适合以下情况：

- 你想减少 26.1.x 新版本变动带来的维护成本。
- 你更看重第三方库的稳定版本，而不是最新 Minecraft。
- 你计划接入 FTB Quests 等现成 RPG/任务生态；FTB Quests 当前在 1.21.11 上更适合作为可选集成目标。

### 不建议路线

**不建议把 Forge 作为新项目主线。** 如果目标是现代版本，优先在 Fabric 与 NeoForge 中二选一。NeoForge 可以作为第二阶段移植目标，尤其当你希望接入 NeoForge 模组包生态时再考虑。

---

## 1. 目标功能拆解

用户需求被拆成四个可交付模块。

### 1.1 注视高亮与交互提示

玩家准星指向 10 米内、无墙遮挡的某个“交互目标”时：

- 若目标是实体：实体边缘高亮。
- 若目标是方块组：整个方块组边缘高亮。
- 当玩家距离目标 2 米内且无遮挡时，屏幕显示 `按 [X] 互动`。
- 不满足条件时不显示提示。
- 按键后打开对话 UI。

这里的“方块组”定义为一组指定方块坐标的集合，而不是 Minecraft 原生 block tag。

### 1.2 对话 UI 与分支

对话 UI 内每一步提供三种选择类型：

1. **对话**：`“……”`
2. **行动**：`（……）`
3. **内心想法**：`【……】`

每种选择都可以进入不同分支。每个选择可以配置：

- 是否需要投 `d20`。
- 检定使用的属性。
- DC / 难度。
- 成功分支。
- 失败分支。
- 成功/失败的后果，例如设置世界 flag、玩家 flag、NPC 状态、物品、冷却、隐藏/显示其他选项。

### 1.3 OP 开发者模式

当玩家拥有 OP 权限并进入开发者模式时，可以查看当前存档内实际加载的：

- 对话树。
- 方块组。
- NPC 与实体绑定。
- routine 设置。
- 数据校验错误。
- 玩家当前叙事状态与 flag。
- 交互目标调试信息。

### 1.4 人形实体与 routine

实体可以是人形 NPC，并支持：

- 自定义模型与动画。
- routine 行为，例如巡逻、站立、坐下、走到某点、播放动画、看向某方向。
- 玩家靠近时是否转头/视线追踪玩家。
- routine 与对话状态联动，例如某段对话后 NPC 改变日程或移动到新地点。

---

## 2. 推荐依赖与前置模组

### 2.1 必需依赖

| 依赖 | 用途 | 推荐级别 | 备注 |
|---|---:|---:|---|
| Fabric Loader | 模组加载器 | 必需 | 推荐主线。 |
| Fabric API | key binding、HUD、Screen、networking、data attachments、resource reload 等 | 必需 | 26.1.2 已有发布版本。 |
| GeckoLib | 人形 NPC 模型、实体动画、动作播放 | 必需/强推荐 | 如果人形实体要有可接受的动画效果，建议直接使用。 |

### 2.2 强推荐但可替换

| 依赖 | 用途 | 推荐级别 | 备注 |
|---|---:|---:|---|
| YACL 或 Cloth Config | 配置界面、开发模式设置页 | 强推荐 | 二选一即可。YACL 更现代，Cloth Config 生态更老牌。 |
| owo-lib | Fabric 侧 GUI/config/utility | 可选强推荐 | 如果确定只做 Fabric，能减少 UI 样板代码；如果未来要跨平台，慎用。 |
| SmartBrainLib | NPC AI/routine 辅助 | 可选 | 26.1.2 版本仍是 alpha；如果追求稳定，routine 可以先自研。 |

### 2.3 可选集成

| 依赖/工具 | 用途 | 建议 |
|---|---:|---|
| WorldEdit | 构建场景与快速标记方块组 | 开发期工具，不作为运行时硬依赖。 |
| FTB Quests | 任务日志、目标追踪、章节化内容 | 只做可选集成，不要让核心对话系统依赖它。 |
| Architectury API | Fabric/NeoForge 跨加载器抽象 | 只有在确定要双平台发布时才引入。MVP 不建议一开始跨平台。 |
| Mod Menu | Fabric 客户端配置入口 | 可选；可以后期加。 |
| Jade/WTHIT 类信息提示模组 | 调试时查看实体/方块信息 | 开发期可用，不做硬依赖。 |

### 2.4 不建议作为核心依赖

| 模组/方案 | 原因 |
|---|---|
| CustomNPCs-Unofficial | 可以参考交互与 NPC 编辑思路，但版本和交互体验不适合作为本项目核心。 |
| 纯数据包/命令方块 | 可做叙事原型，但很难实现可靠的边缘高亮、自定义 UI、服务器权威检定、routine 编辑器。 |
| MCreator | 适合简单方块/物品，不适合此类数据驱动叙事系统和复杂客户端渲染。 |

---

## 3. 技术栈

### 3.1 主线技术栈

```text
Minecraft Java Edition: 26.1.2
Mod Loader: Fabric
JDK: 25
Language: Java
Build: Gradle + Fabric Loom
IDE: IntelliJ IDEA Community / Ultimate
Runtime dependencies:
  - Fabric API
  - GeckoLib
Optional:
  - YACL or Cloth Config
  - owo-lib
  - SmartBrainLib
Data:
  - JSON resources / datapack-style definitions
  - Codecs for serialization/deserialization
  - SavedData / Data Attachments for persistent state
```

### 3.2 保守技术栈

```text
Minecraft Java Edition: 1.21.11
Mod Loader: Fabric
JDK: 21
Language: Java
Build: Gradle + Fabric Loom
Runtime dependencies:
  - Fabric API 1.21.11 line
  - GeckoLib 1.21.11 line
  - SmartBrainLib 1.21.11 line, if using library AI
```

### 3.3 为什么不用 Kotlin 作为默认语言

可以用 Kotlin，但不建议第一版默认使用，原因：

- Minecraft 模组生态的示例、排错、mixin、映射资料仍以 Java 为主。
- 增加 Fabric Language Kotlin 依赖后，发布和排错会多一层变量。
- 对话树、AI、渲染、网络同步本身已经足够复杂，MVP 应降低非必要复杂度。

可以在内容工具链或外部校验器中使用 Kotlin/TypeScript/Python，但核心模组先用 Java。

---

## 4. 系统架构总览

### 4.1 客户端负责

- 每帧或每 2 tick 进行准星目标检测。
- 显示高亮边缘。
- 显示 `按 [X] 互动` HUD。
- 监听自定义按键。
- 渲染对话 UI。
- 渲染开发者查看 UI。
- 向服务器发送“我想互动/我选择了某选项”的请求。

### 4.2 服务器负责

- 最终验证玩家是否真的可交互：
  - 距离 <= 2 米。
  - 目标存在。
  - 目标未被墙遮挡。
  - 玩家状态允许互动。
  - NPC/方块组状态允许互动。
- 决定打开哪个对话树。
- 计算 d20 检定。
- 应用成功/失败后果。
- 保存玩家属性、flag、世界状态、NPC 状态。
- 只把当前玩家应该看见的文本和选项发给客户端。

### 4.3 数据驱动原则

不要把内容写死在 Java 类里。Java 只实现引擎，内容放到 JSON：

```text
data/<modid>/dialogues/*.json
data/<modid>/interactions/block_groups/*.json
data/<modid>/interactions/entity_bindings/*.json
data/<modid>/npc_routines/*.json
data/<modid>/attributes/*.json
```

好处：

- 可用 `/reload` 热加载。
- 可由关卡设计者写内容。
- 开发者模式能查看当前存档实际加载的内容。
- 后期可以做图形化编辑器，而不需要重写引擎。

---

## 5. 模块设计

## 5.1 交互目标系统

### 5.1.1 目标类型

定义统一接口：

```java
public sealed interface InteractionTarget permits EntityTarget, BlockGroupTarget {
    ResourceLocation id();
    InteractionTargetType type();
    Vec3 interactionPoint();
    AABB bounds();
    ResourceLocation dialogueId();
    boolean canHighlight(Player player);
    boolean canInteract(Player player);
}
```

目标分两类：

```java
public record EntityTarget(
    ResourceLocation id,
    UUID entityUuid,
    ResourceLocation dialogueId
) implements InteractionTarget {}

public record BlockGroupTarget(
    ResourceLocation id,
    ResourceKey<Level> dimension,
    List<BlockPos> blocks,
    ResourceLocation dialogueId
) implements InteractionTarget {}
```

### 5.1.2 客户端准星检测

检测周期：

- 默认每 2 tick 执行一次。
- 对话 UI 打开时暂停。
- F3/debug overlay 可显示当前命中信息。

检测流程：

1. 取玩家摄像机位置 `eyePos`。
2. 取视线方向 `lookVec`。
3. 计算 `end = eyePos + lookVec * 10`。
4. 对方块做 raycast。
5. 对实体做 raycast。
6. 比较最近命中距离。
7. 如果命中方块属于某个方块组，则返回该方块组。
8. 如果命中实体且中间没有更近的遮挡方块，则返回实体。
9. 若玩家与目标交互点距离 <= 2 米，则显示交互提示。

### 5.1.3 “不透墙”的判定

对实体：

- 从 `eyePos` 向实体 bounding box 的命中点做 raycast。
- 若方块命中距离小于实体命中距离，则视为被遮挡。
- 客户端可先做预测，但服务器必须再次验证。

对方块组：

- 玩家准星必须实际命中该组内某个方块，或者命中该组的合并 bounding box。
- 若使用 bounding box 命中，仍需从 `eyePos` 到命中点 raycast 验证无遮挡。

### 5.1.4 距离判定

实体：

```text
distance(player.eye/feet, entity.getBoundingBox().center) <= 2m
```

方块组：

```text
distance(player.position, nearestPointOnGroupAABB) <= 2m
```

不建议用“命中点距离”作为唯一依据，因为大型方块组可能导致玩家离可互动点很远但命中边缘。

### 5.1.5 方块组空间索引

不要每 tick 遍历所有方块组。建立索引：

```text
Map<ChunkPos, List<BlockGroupId>>
Map<BlockPos, BlockGroupId>
Map<BlockGroupId, CachedGroupGeometry>
```

加载/重载时构建：

- `blockPos -> groupId` 用于快速确认命中方块是否属于组。
- `chunk -> groups` 用于附近扫描。
- `groupId -> outline mesh/bounds` 用于渲染缓存。

---

## 5.2 边缘高亮渲染

### 5.2.1 MVP 方案

实体：

- 渲染实体 bounding box 外框。
- 只对当前准星命中的实体绘制。
- 颜色、透明度、脉冲速度由目标配置决定。

方块组：

- 对组内每个方块绘制细线框。
- 方块数较少时简单可靠。
- 默认限制每组最多 256 个方块参与逐块描边，超过则使用简化边框。

### 5.2.2 进阶方案

对方块组生成合并轮廓：

- 用 greedy meshing 合并相邻方块面。
- 只渲染外露边。
- 缓存 mesh，方块组不变时不重复生成。
- 如果组内方块被破坏或更新，标记 dirty 后重建。

### 5.2.3 不要直接依赖底层 OpenGL

渲染应使用 Minecraft/Fabric 提供的渲染入口和抽象。26.1 之后 Minecraft Java 的渲染内部变化明显，后续还会继续图形 API 迁移。直接调用底层 GL 状态会增加未来维护成本。

---

## 5.3 HUD 交互提示

### 5.3.1 显示规则

显示 `按 [X] 互动` 的条件：

```text
currentTarget != null
AND target.canInteract(player)
AND distance <= 2.0
AND hasLineOfSight == true
AND player.currentScreen == null
AND player is not spectator
```

不显示的情况：

- 距离 2 米外。
- 中间有墙。
- 准星没有指向目标。
- 目标当前不可用，例如对话已结束或状态锁定。
- 玩家正在打开其他 UI。
- 玩家没有权限查看该目标。

### 5.3.2 按键

默认按键建议：

```text
X 或 R
```

不要默认占用 `E`，因为它是原版物品栏。也不要默认占用鼠标右键，因为会与原版交互冲突。

按键触发流程：

1. 客户端检测到按键。
2. 客户端发送 `ServerboundInteractRequest`。
3. 服务器重新验证距离、无遮挡、目标状态。
4. 服务器返回 `ClientboundOpenDialogue` 或拒绝原因。
5. 客户端打开对话 UI。

---

## 5.4 对话树系统

### 5.4.1 对话节点

基本结构：

```json
{
  "id": "demo:old_woman_intro",
  "start": "start",
  "nodes": {
    "start": {
      "speaker": "old_woman",
      "text": "她抬头看你，像是早就知道你会来。",
      "choices": [
        {
          "id": "ask_name",
          "type": "dialogue",
          "text": "“你知道我是谁吗？”",
          "next": "ask_name_reply"
        },
        {
          "id": "inspect_table",
          "type": "action",
          "text": "（检查桌上的账本）",
          "check": {
            "attribute": "logic",
            "dc": 12,
            "die": "d20",
            "success": "ledger_success",
            "failure": "ledger_failure"
          }
        },
        {
          "id": "inner_smell",
          "type": "thought",
          "text": "【这真是耐人寻味】",
          "next": "thought_smell"
        }
      ]
    }
  }
}
```

### 5.4.2 选择类型

```java
public enum ChoiceType {
    DIALOGUE, // “……”
    ACTION,   // （……）
    THOUGHT   // 【……】
}
```

UI 上应明确区分：

- 对话：正常对白按钮。
- 行动：带括号、可显示检定属性。
- 内心想法：带特殊样式，类似被动/主动思维。

### 5.4.3 检定规则

基础公式：

```text
roll = d20 + attributeScore + modifiers
success = roll >= DC
```

建议默认规则：

- 自然 20：强成功，可走 `critical_success`，没有配置则走 `success`。
- 自然 1：强失败，可走 `critical_failure`，没有配置则走 `failure`。
- 每个 choice 默认只投一次，可通过 `roll_policy` 配置。

检定配置：

```json
"check": {
  "attribute": "empathy",
  "dc": 14,
  "die": "d20",
  "modifiers": [
    { "type": "flag", "flag": "smelled_rust", "value": 2 },
    { "type": "item", "item": "minecraft:spyglass", "value": 1 }
  ],
  "roll_policy": "once_per_player"
}
```

### 5.4.4 对话后果

每个节点或选择都可以有 effects：

```json
"effects": [
  { "type": "set_player_flag", "key": "knows_old_woman_lied", "value": true },
  { "type": "set_world_flag", "key": "tenement_alarm_level", "value": 1 },
  { "type": "give_item", "item": "minecraft:paper", "count": 1 },
  { "type": "set_npc_routine", "npc": "demo:old_woman", "routine": "demo:hide_in_room" }
]
```

### 5.4.5 条件显示

选项可设置条件：

```json
"conditions": [
  { "type": "player_flag", "key": "saw_broken_window", "equals": true },
  { "type": "attribute_at_least", "attribute": "logic", "value": 3 }
]
```

服务器在发送 UI 数据前过滤不可见选项。客户端不应自行决定哪些选项可见。

---

## 5.5 属性与玩家状态

### 5.5.1 属性模型

不要直接复制《极乐迪斯科》的技能名。建议做原创但类似结构：

```text
Cognition
  - logic
  - rhetoric
  - memory

Psyche
  - empathy
  - authority
  - intuition

Body
  - endurance
  - force
  - perception

Motion
  - reaction
  - composure
  - sleight
```

### 5.5.2 数据保存

玩家级：

- 属性值。
- 已触发 thought。
- 已进行的一次性检定结果。
- 对话 flag。
- 当前任务/调查状态。

世界级：

- 全局 flag。
- NPC 状态。
- 方块组运行时覆盖。
- 对话树校验结果缓存。
- 开发者模式标记。

在 Fabric 26.1.2 路线中，玩家/实体数据适合用 Data Attachments；世界级数据适合用 SavedData。

---

## 5.6 开发者模式

### 5.6.1 权限

开发者模式必须服务端授权：

```text
player.hasPermissions(2) 或更高
```

建议：

- 只查看：permission level 2。
- 修改/写入/导出：permission level 4。
- 客户端按钮只负责发请求；服务器检查权限后才返回数据。

### 5.6.2 命令

建议命令：

```text
/ebb dev on
/ebb dev off
/ebb dev inspect
/ebb dev dialogues
/ebb dev blockgroups
/ebb dev npcs
/ebb dev reload
/ebb dev validate
/ebb dev dump
```

### 5.6.3 开发者 UI

第一版不需要做复杂图编辑器。先做可用的查看器：

- 左侧列表：dialogue / block group / npc / routine。
- 中间详情：JSON 摘要、加载来源、当前状态。
- 右侧校验错误：缺失 node、无效 next、找不到 attribute、找不到 NPC、循环分支提示。
- 下方调试栏：当前准星 target、距离、LoS、可交互原因。

### 5.6.4 校验规则

对话树加载时检查：

- `start` 节点存在。
- 所有 `next` 指向有效节点。
- 所有 `success/failure` 指向有效节点。
- choice id 在同一节点内唯一。
- attribute id 存在。
- effect 类型合法。
- conditions 类型合法。
- 没有无法到达的节点，或只作为 warning。
- 没有无限自动跳转循环，或只作为 warning。

---

## 5.7 NPC 与 routine

### 5.7.1 实体设计

定义自定义实体：

```java
public class NarrativeNpcEntity extends PathfinderMob implements GeoEntity {
    private ResourceLocation npcId;
    private ResourceLocation activeRoutineId;
    private boolean lookAtPlayerWhenNear;
}
```

### 5.7.2 模型与动画

使用 GeckoLib：

- 人形模型由 Blockbench 制作。
- 动画包括：
  - idle
  - walk
  - talk
  - inspect
  - sit
  - turn_head
  - startled
  - custom emote
- 对话 UI 打开时可播放 talk/idle 组合。
- 行动分支成功/失败时可触发一次性动画。

### 5.7.3 routine 数据格式

```json
{
  "id": "demo:old_woman_daily",
  "npc": "demo:old_woman",
  "look_at_player_when_near": true,
  "look_range": 6.0,
  "steps": [
    {
      "time": { "from": 0, "to": 4000 },
      "action": "walk_to",
      "pos": [120, 64, -31],
      "speed": 0.8,
      "then": "idle"
    },
    {
      "time": { "from": 4000, "to": 9000 },
      "action": "sit",
      "pos": [122, 64, -28],
      "animation": "sit_reading"
    },
    {
      "condition": { "world_flag": "alarm_level", "gte": 1 },
      "action": "walk_to",
      "pos": [115, 64, -40],
      "speed": 1.1,
      "priority": 10
    }
  ]
}
```

### 5.7.4 routine 执行策略

服务器每 10 tick 更新一次 routine 判断即可，不需要每 tick 重新计算计划。

优先级：

1. 强制状态：对话中、战斗中、受伤、被脚本锁定。
2. 条件 routine：世界 flag / 玩家 flag 触发。
3. 时间 routine：按 day time 执行。
4. fallback：idle。

### 5.7.5 视线追踪玩家

条件：

```text
look_at_player_when_near == true
AND player distance <= look_range
AND lineOfSight == true
AND NPC not performing locked animation
```

行为：

- 只旋转头部/上半身，不一定旋转整个身体。
- 玩家离开范围后，平滑回到 routine 朝向。
- 如果正在对话，始终看向对话玩家。

---

## 6. 网络协议设计

### 6.1 C2S：请求互动

```java
record ServerboundInteractRequestPayload(
    TargetKind kind,
    ResourceLocation targetId,
    Optional<UUID> entityUuid,
    Optional<BlockPos> hitPos
) {}
```

服务器返回：

- `OpenDialoguePayload`
- 或 `InteractionDeniedPayload`

### 6.2 S2C：打开对话

```java
record ClientboundOpenDialoguePayload(
    UUID conversationInstanceId,
    ResourceLocation dialogueId,
    String nodeId,
    Component speakerName,
    Component text,
    List<VisibleChoiceDto> choices
) {}
```

### 6.3 C2S：选择选项

```java
record ServerboundChooseDialogueOptionPayload(
    UUID conversationInstanceId,
    String choiceId
) {}
```

### 6.4 S2C：更新对话

```java
record ClientboundDialogueUpdatePayload(
    UUID conversationInstanceId,
    String nodeId,
    Component text,
    List<VisibleChoiceDto> choices,
    Optional<RollResultDto> rollResult
) {}
```

### 6.5 S2C：关闭对话

```java
record ClientboundCloseDialoguePayload(
    UUID conversationInstanceId,
    CloseReason reason
) {}
```

### 6.6 Dev 模式 payload

```java
record ServerboundDevQueryPayload(
    DevQueryType type,
    Optional<ResourceLocation> id
) {}

record ClientboundDevSnapshotPayload(
    DevQueryType type,
    JsonObject payload,
    List<ValidationMessage> messages
) {}
```

---

## 7. 文件结构建议

```text
src/main/java/com/example/ebb/
  EbbMod.java
  registry/
    ModEntities.java
    ModPackets.java
    ModCommands.java
    ModAttachments.java
  interaction/
    InteractionTarget.java
    InteractionTargetType.java
    EntityTarget.java
    BlockGroupTarget.java
    InteractionService.java
    LineOfSightService.java
    BlockGroupIndex.java
  dialogue/
    DialogueDefinition.java
    DialogueNode.java
    DialogueChoice.java
    DialogueRegistry.java
    DialogueRuntime.java
    DialogueService.java
    ChoiceCondition.java
    DialogueEffect.java
  checks/
    AttributeDefinition.java
    AttributeStore.java
    SkillCheck.java
    SkillCheckService.java
    RollResult.java
  npc/
    NarrativeNpcEntity.java
    NarrativeNpcRenderer.java
    NpcRoutineDefinition.java
    NpcRoutineController.java
    NpcLookController.java
  persistence/
    EbbWorldSavedData.java
    PlayerNarrativeState.java
    NpcNarrativeState.java
  dev/
    DevCommands.java
    DevSnapshotService.java
    DialogueValidator.java

src/client/java/com/example/ebb/client/
  EbbClient.java
  interaction/
    ClientTargetDetector.java
    ClientInteractionState.java
  render/
    HighlightRenderer.java
    BlockGroupOutlineCache.java
    PromptHudRenderer.java
  screen/
    DialogueScreen.java
    DialogueChoiceWidget.java
    DevBrowserScreen.java
    DevDialogueTreeScreen.java

src/main/resources/
  fabric.mod.json
  assets/ebb/lang/en_us.json
  assets/ebb/lang/zh_cn.json
  assets/ebb/geo/
  assets/ebb/animations/
  assets/ebb/textures/entity/
  data/ebb/dialogues/
  data/ebb/interactions/block_groups/
  data/ebb/interactions/entity_bindings/
  data/ebb/npc_routines/
  data/ebb/attributes/
```

---

## 8. 数据格式草案

### 8.1 方块组

```json
{
  "id": "demo:locked_door",
  "dimension": "minecraft:overworld",
  "blocks": [
    [100, 64, -20],
    [100, 65, -20]
  ],
  "interaction_point": [100.5, 64.8, -19.7],
  "dialogue": "demo:locked_door_dialogue",
  "highlight": {
    "color": "#78D7FF",
    "pulse": true
  },
  "conditions": [
    { "type": "world_flag", "key": "door_removed", "equals": false }
  ]
}
```

### 8.2 实体绑定

```json
{
  "id": "demo:old_woman",
  "entity_type": "ebb:narrative_npc",
  "dialogue": "demo:old_woman_intro",
  "routine": "demo:old_woman_daily",
  "highlight": {
    "color": "#F2D27A",
    "pulse": true
  },
  "look_at_player_when_near": true,
  "look_range": 6.0
}
```

### 8.3 属性定义

```json
{
  "attributes": [
    {
      "id": "logic",
      "display": "逻辑",
      "group": "cognition",
      "description": "把碎片拼成结构的能力。"
    },
    {
      "id": "empathy",
      "display": "共情",
      "group": "psyche",
      "description": "读出他人情绪裂缝的能力。"
    }
  ]
}
```

### 8.4 对话树

```json
{
  "id": "demo:locked_door_dialogue",
  "start": "start",
  "nodes": {
    "start": {
      "speaker": "narrator",
      "text": "门框内侧有新鲜刮痕，像是有人用钥匙以外的东西试过。",
      "choices": [
        {
          "id": "knock",
          "type": "action",
          "text": "（敲门）",
          "next": "knock_reply"
        },
        {
          "id": "force",
          "type": "action",
          "text": "（试着撞开它）",
          "check": {
            "attribute": "force",
            "dc": 15,
            "die": "d20",
            "success": "force_success",
            "failure": "force_failure"
          }
        },
        {
          "id": "think",
          "type": "thought",
          "text": "【门不是为了挡住你，是为了挡住里面的东西。】",
          "next": "thought_reply"
        }
      ]
    }
  }
}
```

---

## 9. 开发里程碑

## Phase 0：项目骨架

目标：

- Fabric 26.1.2 项目可运行。
- 客户端、服务端、common 代码分层清楚。
- 基础 packet 注册完成。
- `/ebb` 命令可用。
- 资源 reload listener 可读取空 dialogue registry。

交付物：

- 可运行 dev client。
- 可运行 dedicated server。
- 空 JSON 被正确加载和校验。
- README 内写明 Java / Minecraft / Fabric API 版本。

验收标准：

- `gradlew runClient` 成功。
- `gradlew runServer` 成功。
- 无客户端类加载到 dedicated server 的崩溃。

---

## Phase 1：目标检测、HUD、边缘高亮

目标：

- 准星指向 10 米内实体/方块组时高亮。
- 2 米内显示 `按 [X] 互动`。
- 遮挡时不高亮、不提示或提示不可互动。
- 服务器能验证互动请求。

交付物：

- `ClientTargetDetector`
- `HighlightRenderer`
- `PromptHudRenderer`
- `InteractionService`
- `BlockGroupIndex`

验收标准：

- 隔墙看 NPC 不出现提示。
- 准星移开目标后高亮消失。
- 方块组任意成员被看见时，整组高亮。
- 客户端伪造互动请求会被服务器拒绝。

---

## Phase 2：对话 UI 与基础对话树

目标：

- 按键打开对话 UI。
- 三种选项类型正常显示。
- 无检定分支可跳转。
- 对话结束后关闭 UI。

交付物：

- `DialogueScreen`
- `DialogueRegistry`
- `DialogueService`
- `DialogueRuntime`
- JSON 对话格式 v0

验收标准：

- 同一 NPC 可打开指定对话树。
- 选择“对话/行动/内心想法”进入不同节点。
- `/reload` 后内容更新。
- JSON 错误不会导致游戏崩溃，而是记录校验错误。

---

## Phase 3：d20 检定与玩家属性

目标：

- 支持 d20 + 属性 + modifier。
- 成功/失败进入不同分支。
- 检定结果由服务器生成。
- 玩家属性与检定记录持久化。

交付物：

- `AttributeStore`
- `SkillCheckService`
- `RollResult`
- `PlayerNarrativeState`

验收标准：

- 玩家重进世界后属性保留。
- once-per-player 检定不会重复投。
- 自然 1/20 行为可配置。
- 客户端无法伪造投骰结果。

---

## Phase 4：开发者模式

目标：

- OP 可打开开发者查看器。
- 查看已加载 dialogue、block group、NPC、routine。
- 查看校验错误。
- 查看当前准星交互目标调试信息。

交付物：

- `/ebb dev` 命令组。
- `DevBrowserScreen`
- `DevSnapshotService`
- `DialogueValidator`

验收标准：

- 非 OP 无法打开 dev UI。
- OP 可查看当前存档实际加载的数据。
- 缺失 node、无效 attribute、无效 routine 有明确错误提示。
- 可一键复制目标 ID / dialogue ID。

---

## Phase 5：人形 NPC 与 routine

目标：

- 自定义人形 NPC 可生成。
- GeckoLib 动画可播放。
- routine 可按时间/条件切换。
- 玩家靠近时 NPC 可看向玩家。

交付物：

- `NarrativeNpcEntity`
- `NarrativeNpcRenderer`
- `NpcRoutineDefinition`
- `NpcRoutineController`
- `NpcLookController`

验收标准：

- NPC 能站立、行走、播放 idle/talk。
- NPC 按 day time 走到指定位置。
- NPC 与玩家对话时看向玩家。
- 对话 effect 可切换 NPC routine。

---

## Phase 6：内容生产工具与打磨

目标：

- 对话 JSON schema。
- VS Code schema 自动补全。
- 对话树导出 DOT/Mermaid。
- 方块组 in-game 选择工具。
- routine 路点编辑工具。
- 本地化文本从 JSON 中分离。

交付物：

- `schema/dialogue.schema.json`
- `/ebb dev export`
- `/ebb tool wand` 或开发者选区工具
- 内容示例包

验收标准：

- 内容作者可以不改 Java 添加新对话。
- 方块组可以在游戏中框选并导出。
- routine 可视化检查。
- 对话文本可本地化。

---

## 10. 测试计划

### 10.1 单元测试

重点测：

- dialogue JSON 解析。
- 校验器。
- choice condition。
- d20 检定。
- effect 应用。
- block group 空间索引。

### 10.2 集成测试

场景：

1. 单人世界打开对话。
2. dedicated server 两名玩家同时与不同 NPC 对话。
3. 两名玩家同时与同一 NPC 对话。
4. 玩家在对话中离开距离。
5. NPC 被卸载/区块卸载。
6. `/reload` 后旧 conversation instance 的处理。
7. 非 OP 请求 dev payload。

### 10.3 手工测试清单

- 10 米外看目标：有高亮，无互动提示。
- 2 米内看目标：有高亮，有互动提示。
- 2 米内但隔墙：无提示。
- 高亮不穿墙。
- 对话 UI 不阻塞服务器。
- ESC 关闭对话时服务器清理会话。
- 投骰结果在客户端显示清楚。
- 成功/失败分支正确。
- NPC routine 切换不卡顿。
- dedicated server 不加载客户端渲染类。

---

## 11. 性能预算

### 11.1 客户端

| 系统 | 预算 |
|---|---:|
| 目标检测 | 每 2 tick 一次，单次 < 0.2 ms |
| HUD 文本 | 每帧渲染，忽略不计 |
| 实体高亮 | 只渲染当前目标 |
| 方块组高亮 | MVP 限制 256 方块；大组使用合并轮廓 |
| UI | 只在打开时更新布局 |

### 11.2 服务端

| 系统 | 预算 |
|---|---:|
| 互动验证 | 按键触发时执行，不常驻 |
| routine | 每 10 tick 更新一次 |
| 对话运行时 | 玩家选择时更新 |
| 数据保存 | 使用脏标记，避免每 tick 写盘 |

### 11.3 限制

- 单个方块组默认最多 1024 个方块。
- 单次高亮默认最多绘制 256 个逐块线框。
- 一个玩家同时只能有一个对话会话。
- 一个 NPC 同时可允许多个玩家对话，但状态修改必须串行处理。

---

## 12. 风险与规避

### 12.1 Minecraft 26.1+ 渲染变动

风险：高亮渲染直接依赖内部实现，未来版本容易坏。

规避：

- 使用 Fabric/Minecraft 当前推荐渲染入口。
- 避免直接操作全局 OpenGL 状态。
- 把高亮渲染封装到单独模块，便于迁移。

### 12.2 第三方 AI 库在 26.1.2 仍不稳定

风险：SmartBrainLib 26.1.2 版本为 alpha。

规避：

- MVP 不硬依赖 SmartBrainLib。
- routine 第一版用原版 `PathNavigation` + 自定义 schedule controller。
- 若后续 SmartBrainLib 稳定，再做 adapter。

### 12.3 内容 JSON 变复杂

风险：对话树越来越大，手写 JSON 容易错。

规避：

- 从 Phase 2 就做 validator。
- Phase 6 增加 JSON schema 与导出图。
- Dev UI 显示校验错误和不可达节点。

### 12.4 客户端作弊

风险：客户端伪造“我能互动”“我投出了 20”。

规避：

- 所有互动距离、无遮挡、检定、effect 都在服务器执行。
- 客户端只做预测显示。
- C2S payload 不接受 roll result。

### 12.5 大型方块组高亮性能

风险：逐块渲染大组边框成本过高。

规避：

- 缓存轮廓。
- 大组自动简化成 bounding outline。
- 使用 dev warning 提示设计者拆分方块组。

---

## 13. MVP 内容样例

建议第一版做一个小场景验证完整链路：

### 场景：旅馆走廊

交互目标：

1. 人形 NPC：旅馆老板。
2. 方块组：锁住的门。
3. 方块组：桌上的账本。
4. 实体：一只沉默的猫。

属性：

- logic
- empathy
- force
- perception

对话：

- 老板询问玩家为何回来。
- 账本可用 logic 检定。
- 门可用 force 检定。
- 猫触发 inner thought，不说话但给 perception 提示。

验收：

- 4 个目标都可高亮。
- 3 种选择类型都出现。
- 至少 2 个 d20 检定。
- 至少 1 个成功后改变世界 flag。
- 至少 1 个失败后隐藏选项。
- OP 可查看这 4 个目标的配置。

---

## 14. 开发顺序建议

不要先做人形 NPC。建议顺序：

1. 方块组高亮。
2. 实体 bounding box 高亮。
3. HUD 提示。
4. 按键发送互动请求。
5. 静态对话 UI。
6. JSON 对话树。
7. d20 检定。
8. 状态保存。
9. dev viewer。
10. 人形 NPC。
11. routine。
12. 对话与 routine 联动。

原因：NPC 与动画最容易消耗时间，但不是系统闭环的前提。先把“看见目标 → 互动 → 对话 → 检定 → 分支 → 保存”跑通。

---

## 15. 版本决策表

| 场景 | 建议版本 |
|---|---|
| 从零开始做新模组，目标 2026 年现代版本 | Fabric 26.1.2 + JDK 25 |
| 想减少库兼容性风险 | Fabric 1.21.11 + JDK 21 |
| 想接入 FTB Quests 当前稳定生态 | 1.21.11 优先 |
| 想未来支持大型 NeoForge 模组包 | 先 Fabric MVP，再评估 Architectury/NeoForge 移植 |
| 想快速做叙事原型，不关心动画 | Fabric 26.1.2，不接 GeckoLib，先用原版实体/方块组 |
| 想做人形 NPC 动画 | 加 GeckoLib |

---

## 16. 参考来源

- Minecraft Java Edition 26.1.2 release notes: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1-2
- Minecraft Java Edition 26.1 technical changes: https://feedback.minecraft.net/hc/en-us/articles/44551668333837-Minecraft-Java-Edition-26-1
- Minecraft Java Edition 1.21.11 release notes: https://feedback.minecraft.net/hc/en-us/articles/41809981427213-Minecraft-Java-Edition-1-21-11-Mounts-of-Mayhem
- Fabric Documentation 26.1.2: https://docs.fabricmc.net/
- Fabric setup docs: https://docs.fabricmc.net/develop/getting-started/setting-up
- Fabric custom screens: https://docs.fabricmc.net/develop/rendering/gui/custom-screens
- Fabric HUD rendering: https://docs.fabricmc.net/develop/rendering/hud
- Fabric networking: https://docs.fabricmc.net/develop/networking
- Fabric Data Attachments: https://docs.fabricmc.net/develop/data-attachments
- Fabric Saved Data: https://docs.fabricmc.net/develop/saved-data
- Fabric API 26.1.2 on Modrinth: https://modrinth.com/mod/fabric-api/version/0.149.0%2B26.1.2
- Fabric API 1.21.11 on Modrinth: https://modrinth.com/mod/fabric-api/version/i5tSkVBH
- NeoForge 1.21.11 getting started: https://docs.neoforged.net/docs/1.21.11/gettingstarted/
- GeckoLib: https://www.curseforge.com/minecraft/mc-mods/geckolib
- SmartBrainLib: https://www.curseforge.com/minecraft/mc-mods/smartbrainlib
- Cloth Config API: https://www.curseforge.com/minecraft/mc-mods/cloth-config
- YetAnotherConfigLib: https://www.curseforge.com/minecraft/mc-mods/yacl
- owo-lib: https://www.curseforge.com/minecraft/mc-mods/owo-lib
- Architectury API: https://docs.architectury.dev/api/introduction
- FTB Quests: https://www.curseforge.com/projects/289412
- WorldEdit 1.21.11 compatibility listing: https://staging.modrinth.com/plugin/worldedit/version/yu07Zs1W

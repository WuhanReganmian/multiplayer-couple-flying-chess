# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目状态

**当前阶段：预实现（需求阶段已完成，尚无源码）**

仓库现有文件：
- `需求文档.md` — 完整产品需求（中文）
- `demo.jpeg` — UI 参考效果图

## 产品概述

**多人情侣飞行棋** — 一款专为情侣或多对情侣设计的本地 Android 飞行棋游戏，通过分级任务引导亲密互动。

核心约束：
- 仅限异性互动（2人：1男1女；3-4人：至少1男1女）
- 完全离线，零网络请求，零数据上传
- 5 级任务体系（L1-L5），每级 100 个任务（共 500 条），全部支持自定义（增删改）
- 房主可在开局前修改任务库

## 推荐技术栈

| 层级 | 技术 |
|------|------|
| 游戏逻辑 | Rust（类型安全、高性能） |
| Android UI | Kotlin + Jetpack Compose |
| 跨语言桥接 | Android NDK / JNI（jni-rs crate） |
| 构建工具 | Cargo + cargo-ndk（Android 交叉编译） |
| 本地存储 | Room Database（任务库持久化） |

## 核心游戏机制

### 棋盘与移动
- 环形路径，含随机分布的"任务格"，先到终点者获胜
- 掷骰子 → 移动 → 落在任务格时触发任务
- 玩家可接受任务，或拒绝（回退 1-3 步后重新触发）

### 任务分级

| 等级 | 名称 | 强度 |
|------|------|------|
| L1 | 破冰 | Low |
| L2 | 暧昧挑逗 | Mid-Low |
| L3 | 褪衣相亲 | Mid |
| L4 | 边缘取悦 | Mid-High |
| L5 | 实战冲刺 | High |

- 每位玩家可独立设置当前所处阶段
- 任务触发时，从异性玩家池中随机抽取互动对象
- 界面输出：「【玩家 A】请对【玩家 B】执行 [任务 X]」

## UI/UX 设计方向

- 暗色调：深紫 `#2D1B3D`、暗红 `#4A1A1F`、黑色 `#0A0A0A`、金色点缀 `#D4AF37`
- 任务以"翻牌"动画呈现，增加未知感
- 棋盘背景色随阶段 L1→L5 由浅渐变为深红
- 动画要求：60 FPS，掷骰动画 0.8-1.2s

## 构建命令（项目初始化后使用）

```bash
# 环境准备
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-ndk

# Rust 游戏逻辑交叉编译（生成 .so 供 Android 调用）
cargo ndk -t aarch64-linux-android -o app/src/main/jniLibs build \
  --manifest-path gamelogic/Cargo.toml --release

# Android 构建
./gradlew assembleDebug        # 调试包
./gradlew assembleRelease      # 发布包

# 测试
cargo test                     # Rust 单元测试
./gradlew test                 # Kotlin 单元测试

# 代码检查
cargo clippy                   # Rust lint
./gradlew lint                 # Android lint
```

## 非功能性需求

- 启动时间 < 2 秒，动画 60 FPS
- 内存 < 150MB，存储体积 ~10MB
- Min SDK 21（Android 5.0+），Target SDK 34
- 支持 ABI：arm64-v8a、armeabi-v7a、x86_64
- 本地运行，禁止上传任何玩家数据

## Rust + Android 开发注意事项

- JNI 字符串编解码使用 jni-rs crate，注意 UTF-8 / Modified UTF-8 差异
- `.so` 文件须放在 `app/src/main/jniLibs/{abi}/` 目录下
- 优先在真机测试，模拟器 ABI 可能与发布目标不一致
- Compose 状态管理用 ViewModel + StateFlow，注意配置变更（横竖屏）时的生命周期

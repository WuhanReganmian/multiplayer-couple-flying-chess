# Couple Chess 🎲

一款专为情侣设计的本地多人飞行棋 Android 游戏。玩家通过掷骰子在棋盘上移动，触发不同等级的趣味互动挑战任务，增进感情与默契。

## 特点

- **纯本地运行** — 无需网络，无数据上传，完全离线可玩
- **2-4 人同屏** — 支持情侣或多对好友同屏游戏
- **5 级挑战体系** — 从轻松破冰到深度互动，循序渐进
- **自定义任务** — 每级 100 个默认任务（共 500 条），支持增删改，房主可赛前调整
- **Rust + Kotlin 混合架构** — 核心逻辑 Rust 实现，高性能且类型安全

## 技术栈

| 层级 | 技术 |
|------|------|
| 游戏逻辑 | Rust + JNI (jni-rs) |
| Android UI | Kotlin + Jetpack Compose + Material3 |
| 本地存储 | Room Database |
| 导航 | Navigation Compose |
| 构建 | Gradle 8.7 + cargo-ndk |

## 目录结构

```
multiplayer-couple-flying-chess/
├── app/                              # Android 应用模块
│   ├── build.gradle.kts              # 应用级构建配置
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/couplechess/
│       │   ├── MainActivity.kt       # 入口 Activity
│       │   ├── CoupleChessApp.kt     # 导航路由
│       │   ├── data/
│       │   │   ├── model/GameModels.kt    # 数据模型
│       │   │   ├── db/                    # Room 数据库 (Entity, DAO, Database)
│       │   │   ├── bridge/GameBridge.kt   # Rust JNI 桥接
│       │   │   ├── repository/            # 数据仓库
│       │   │   └── GameStateHolder.kt     # 游戏状态管理
│       │   └── ui/
│       │       ├── theme/            # 主题 (颜色、字体、样式)
│       │       ├── components/       # 可复用 UI 组件
│       │       ├── navigation/       # 路由定义
│       │       └── screens/          # 页面
│       │           ├── home/         # 首页
│       │           ├── playersetup/  # 玩家设置
│       │           ├── game/         # 游戏主界面
│       │           └── taskmanager/  # 任务管理
│       ├── res/                      # 资源文件
│       └── jniLibs/                  # Rust 编译产物 (.so)
├── gamelogic/                        # Rust 游戏逻辑模块
│   ├── Cargo.toml
│   └── src/
│       ├── lib.rs                    # JNI 接口导出
│       ├── models.rs                 # 游戏数据结构
│       └── game.rs                   # 核心游戏逻辑
├── gradle/                           # Gradle wrapper & 版本目录
│   └── libs.versions.toml           # 依赖版本集中管理
├── .github/workflows/android.yml     # CI/CD 自动构建 & 发布
├── build.gradle.kts                  # 项目级构建配置
├── settings.gradle.kts               # 项目设置
└── gradle.properties                 # Gradle 属性
```

## 环境要求

- **JDK 17** (推荐 Eclipse Temurin)
- **Android SDK** (compileSdk 34, minSdk 21)
- **Android NDK 25.2**
- **Rust toolchain** (stable) + `cargo-ndk`

## 构建与调试

### 1. 准备 Rust 交叉编译环境

```bash
rustup target add aarch64-linux-android
cargo install cargo-ndk
```

### 2. 编译 Rust 原生库

```bash
cd gamelogic
cargo ndk -t arm64-v8a -o ../app/src/main/jniLibs build --release
```

### 3. 构建 Android APK

```bash
# 调试包
./gradlew assembleDebug

# 发布包
./gradlew assembleRelease
```

### 4. 运行测试

```bash
# Rust 单元测试
cd gamelogic && cargo test

# Kotlin 单元测试
./gradlew test
```

### 5. 代码检查

```bash
cargo clippy                  # Rust lint
./gradlew lint                # Android lint
```

## CI/CD

每次推送到 `main` 分支时，GitHub Actions 自动：

1. 运行 Rust 测试
2. 交叉编译 Rust → arm64-v8a `.so`
3. 构建 debug APK
4. 运行 Kotlin 测试
5. 创建 GitHub Release（自动递增版本号 `v0.0.x`）

前往 [Releases](../../releases) 页面下载最新 APK。

## 版本规范

- 版本号格式：`v0.0.x`（从 `v0.0.1` 开始）
- 每次 main 分支构建自动递增 patch 版本
- APK 命名：`couple-chess-v{version}-arm64-v8a-debug.apk`

## 最低系统要求

- Android 5.0 (API 21) 及以上
- 64 位 ARM 处理器 (arm64-v8a)
- 约 10MB 存储空间

## License

Private — 仅供个人使用。

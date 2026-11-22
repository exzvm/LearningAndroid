# LearningAndroid

Android 学习记录仓库。每个学习项目都是一个独立的 Android Studio 工程，放在各自的顶层目录中，互不干扰。

## 学习项目

| 项目 | 说明 |
| --- | --- |
| [DiceRoller](DiceRoller/) | 掷骰子应用，使用 Jetpack Compose。 |
| [TipCalculator](TipCalculator/) | 小费计算器，来自 Android Basics with Compose 课程。 |

## 如何打开与构建

本仓库**根目录不是 Gradle 工程**。每个子项目都有自己的 Gradle wrapper 和 `settings.gradle.kts`，请分别打开对应子目录：

1. 在 Android Studio 中选择 `File > Open`，选中子项目目录（例如 `LearningAndroid/DiceRoller`）。
2. 等待 Gradle Sync 完成后即可运行。
3. 多个项目可以各开一个窗口；**不要直接打开 `LearningAndroid/` 根目录进行编译**。

各项目的工具链相互独立：

| 项目 | Gradle | AGP | Kotlin | compileSdk |
| --- | --- | --- | --- | --- |
| DiceRoller | 9.5.0 | 9.3.3 | 2.2.10 | 37.1 |
| TipCalculator | 8.12 | 8.8.0 | 2.1.0 | 35 |

命令行构建时，如果系统未安装独立 JDK，可临时使用 Android Studio 自带的 JBR：

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd DiceRoller && ./gradlew assembleDebug
```

`local.properties` 不纳入版本控制；首次用 Android Studio 打开项目时会自动生成。

## 新增学习项目

在根目录新建项目目录，保持其为一个独立 Gradle 工程，并在此 README 的项目表中补充一行即可。

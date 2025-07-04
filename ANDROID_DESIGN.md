# 安卓项目开发指南

## 1. 项目背景与目标
本项目旨在开发一个功能与现有 iPhone 应用对齐的安卓版本，用于实时获取市场深度数据、进行买卖比率和大单监控，并将结果推送至指定渠道。

## 2. 技术栈选择
*   **开发语言**: Kotlin
*   **UI 框架**: Jetpack Compose (原生 Android 声明式 UI 框架)
*   **网络请求**: Retrofit
*   **本地数据存储**: Room (SQLite 数据库抽象层)
*   **异步处理**: Kotlin Coroutines
*   **依赖注入**: Hilt (可选，用于简化依赖管理)
*   **推送服务**: Firebase Cloud Messaging (FCM)

## 3. 应用架构设计
采用 MVVM (Model-View-ViewModel) 架构模式，以确保代码的可维护性、可测试性和可扩展性。

### 核心模块
*   **数据模型 (Data Models)**:
    *   职责: 定义应用中所有数据结构，包括从 API 获取的数据和内部使用的数据。
    *   技术: Kotlin Data Classes。
    *   对应 iOS: `Models.swift`。安卓版本的数据模型将严格参照 `Models.swift` 中的定义，确保数据结构的一致性。
*   **数据获取模块 (Data Module)**:
    *   职责: 负责与 Binance API 或其他数据源进行交互，获取市场深度数据。
    *   技术: Retrofit, Kotlin Coroutines。
    *   对应 iOS: `BinanceAPIService.swift`。安卓版本将使用 Retrofit 实现与 `BinanceAPIService.swift` 相同的数据请求逻辑和 API 接口定义。
*   **数据存储模块 (Data Storage Module)**:
    *   职责: 负责本地数据缓存和持久化。
    *   技术: Room。
    *   对应 iOS: CoreData。安卓版本将使用 Room 数据库来管理本地数据存储，其结构和逻辑将与 iOS 的 CoreData 使用方式对齐。
*   **分析模块 (Analysis Module)**:
    *   职责: 实现买卖比率计算、大单监控等核心业务逻辑。
    *   技术: 纯 Kotlin 逻辑。
    *   对应 iOS: `MarketDataViewModel.swift`。安卓版本将直接移植 `MarketDataViewModel.swift` 中包含的所有核心分析算法和业务逻辑，确保计算结果的一致性。
*   **UI 模块 (UI Module)**:
    *   职责: 负责用户界面的展示和用户交互。
    *   技术: Jetpack Compose。
    *   对应 iOS: `ContentView.swift`, `HorizontalBarChartView.swift`, `OrderRowWithBarView.swift`, `RatioChartView.swift`。安卓 UI 将使用 Jetpack Compose 重新构建，力求在视觉和交互上与 iOS 版本保持高度一致。
*   **推送模块 (Notification Module)**:
    *   职责: 负责将分析结果通过 FCM 推送至指定渠道 (如 Discord Webhook)。
    *   技术: FCM SDK, 后端服务 (如果需要)。
    *   对应 iOS: (无直接对应文件，但功能上与 iOS 的推送机制对齐)。安卓版本将集成 FCM 实现消息推送功能。

## 4. UI/UX 设计
*   **参考**: 界面风格应严格参考现有 iPhone 应用的 UI/UX，并适配安卓 Material Design 规范。
*   **组件**: 优先使用 Jetpack Compose 提供的 Material Design 组件。
*   **图表**: 考虑使用第三方图表库 (如 MPAndroidChart 或 Compose 兼容的图表库) 来实现类似 iOS 的 `HorizontalBarChartView` 和 `RatioChartView`。具体实现时，将分析 iOS 中图表组件的绘制逻辑和数据绑定方式，并在 Compose 中寻找或实现对应的视觉效果。
*   **列表项**: `OrderRowWithBarView.swift` 中的列表项设计将通过 Compose 的 `Row` 和 `Modifier` 等组件进行精确复刻。
*   **主视图**: `ContentView.swift` 的整体布局和数据展示方式将作为安卓主界面的设计蓝图。

## 5. 开发流程
1.  **项目初始化**: 创建新的 Android Studio 项目，配置 Kotlin 和 Jetpack Compose。
2.  **依赖配置**: 添加 Retrofit, Room, Coroutines, FCM 等必要库。
3.  **数据模型定义**: 根据 API 响应和业务需求定义 Kotlin 数据类，与 `Models.swift` 对齐。
4.  **网络层实现**: 使用 Retrofit 定义 API 接口和数据获取逻辑，与 `BinanceAPIService.swift` 对齐。
5.  **本地存储实现**: 使用 Room 定义数据库实体、DAO 和 Repository，与 CoreData 逻辑对齐。
6.  **业务逻辑实现**: 在 ViewModel 中实现数据分析和处理逻辑，移植 `MarketDataViewModel.swift` 中的算法。
7.  **UI 界面开发**: 使用 Jetpack Compose 构建各个界面和自定义视图，复刻 `ContentView.swift`, `HorizontalBarChartView.swift`, `OrderRowWithBarView.swift`, `RatioChartView.swift` 的设计。
8.  **推送服务集成**: 配置 FCM，实现消息接收和处理。
9.  **测试**: 编写单元测试和 UI 测试。
10. **优化与发布**: 性能优化、适配不同设备、打包发布。

## 6. 任务追踪
所有开发步骤和任务都将在 `TODO.md` 文件中进行记录和追踪。

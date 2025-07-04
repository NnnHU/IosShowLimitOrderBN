# 安卓项目 TODO 列表

## 阶段一：项目初始化与基础架构
- [ ] 创建新的 Android Studio 项目 (Kotlin + Jetpack Compose)
- [ ] 配置 `build.gradle`，添加必要的依赖 (Retrofit, Room, Coroutines, FCM)
- [ ] 定义基础数据模型 (与 iOS `Models.swift` 对齐)
- [ ] 实现网络层：
    - [ ] 定义 Retrofit API 接口 (参考 `BinanceAPIService.swift`)
    - [ ] 实现数据获取逻辑
- [ ] 实现本地数据存储层：
    - [ ] 定义 Room 数据库实体和 DAO
    - [ ] 实现 Repository 模式
- [ ] 设置 MVVM 基础结构 (ViewModel, View)

## 阶段二：核心功能开发
- [ ] 实现数据分析模块：
    - [ ] 移植买卖比率计算逻辑 (参考 `MarketDataViewModel.swift`)
    - [ ] 移植大单监控逻辑
- [ ] UI 界面开发：
    - [ ] 实现主界面布局 (参考 `ContentView.swift`)
    - [ ] 开发自定义图表组件 (参考 `HorizontalBarChartView.swift`, `RatioChartView.swift`)
    - [ ] 实现订单行视图 (参考 `OrderRowWithBarView.swift`)
- [ ] 集成推送模块：
    - [ ] 配置 Firebase 项目和 FCM SDK
    - [ ] 实现消息接收和处理逻辑

## 阶段三：测试、优化与发布
- [ ] 编写单元测试 (针对 ViewModel 和数据层)
- [ ] 编写 UI 测试
- [ ] 性能优化和内存管理
- [ ] 适配不同安卓设备和屏幕尺寸
- [ ] 打包并准备发布

## 待定/待讨论
- [ ] 是否需要依赖注入框架 (如 Hilt)？
- [ ] 具体图表库的选择？
- [ ] Discord 推送的具体实现方式 (直接在客户端推送还是通过后端服务)？

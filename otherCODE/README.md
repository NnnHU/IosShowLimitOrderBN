# 币安市场深度监控系统

一个完整的币安现货和合约市场深度实时监控系统，支持多种输出格式和Discord推送。

## 🌟 主要特性

### 核心功能
- **实时监控**：同时监控现货和合约市场深度数据
- **多币种支持**：目前支持 BTC、ETH、SOL（可轻松扩展）
- **数据统一**：所有输出使用相同的数据源，确保一致性
- **智能过滤**：基于可配置的最小数量阈值过滤大单

### 输出方式
- **文本分析**：详细的市场深度文字报告
- **图表可视化**：2×2布局的深度图表和比率分析
- **Discord推送**：支持分离的文本和图表webhook
- **延迟发送**：防止Discord消息丢失的智能发送队列

### 分析功能
- **多层次深度分析**：0-1%、1-2.5%、2.5-5%、5-10%价格范围
- **买卖比率计算**：实时计算各价格范围的买卖力量对比
- **大单监控**：专注于具有市场影响力的大额订单

## 🚀 项目特性

- **统一数据源**: 确保文本分析和图表显示使用相同的数据
- **模块化设计**: 分离配置、数据管理、文本输出和图表输出
- **多输出方式**: 支持Discord文本消息和图表图片发送
- **实时监控**: WebSocket实时接收币安订单簿数据
- **线程安全**: 多线程环境下的数据安全保证
- **灵活配置**: 通过配置文件轻松调整监控参数

## 📁 项目结构

```
orderRate/
├── config.py              # 统一配置管理
├── data_manager.py         # 数据源管理
├── text_output.py          # 文本输出模块
├── chart_output.py         # 图表输出模块
├── main.py                 # 主程序入口
├── README.md              # 项目说明
├── depthRateSpotAndFuturesChg.py          # 旧版本（兼容）
└── depthRateSpotAndFuturesChgUI.py        # 旧版本（兼容）
```

## 🔧 模块说明

### config.py - 配置管理
- 交易对配置
- 最小数量阈值设置
- Discord Webhook配置
- 发送间隔设置
- 输出选项开关
- 图表参数配置

### data_manager.py - 数据管理
- 统一的订单簿数据获取和维护
- 线程安全的数据访问
- WebSocket消息处理
- 深度比率计算

### text_output.py - 文本输出
- 保持原有的文本分析格式
- 生成市场深度分析报告
- 发送到Discord文本频道

### chart_output.py - 图表输出
- 全新设计的图表可视化
- 2x2布局：现货/合约深度图 + 买卖比率图
- 发送到Discord图片频道

### main.py - 主程序
- 整合所有模块
- 管理WebSocket连接
- 协调数据流和输出

## ⚙️ 安装依赖

```bash
pip install requests websocket-client plotly kaleido pandas aiohttp
```

## 🔧 配置说明

### 1. 交易对配置
```python
SYMBOLS = ["BTCUSDT", "ETHUSDT"]
```

### 2. 最小数量阈值
```python
MIN_QUANTITIES = {
    "BTC": 50.0,
    "ETH": 200.0,
    "SOL": 500.0,
    "DEFAULT": 1000.0
}
```

### 3. Discord Webhook配置
```python
DISCORD_WEBHOOKS = {
    "BTC": {
        "文本输出": ["webhook_url_1", "webhook_url_2"],
        "图表输出": ["webhook_url_3"]
    }
}
```

### 4. 发送间隔
```python
SEND_INTERVALS = {
    "文本输出": 300,  # 文本分析每5分钟发送一次
    "图表输出": 120,  # 图表每2分钟发送一次
}
```

### 5. 输出选项
```python
OUTPUT_OPTIONS = {
    "启用文本输出": True,
    "启用图表输出": True,
    "启用控制台输出": True,
    "保存图表到本地": False,
}
```

## 🚀 使用方法

### 快速开始
```bash
python main.py
```

### 自定义配置
1. 修改 `config.py` 中的配置参数
2. 设置Discord Webhook URLs
3. 调整监控交易对和阈值
4. 运行主程序

## 📊 输出格式

### 文本输出
- 市场深度分析报告
- 订单簿摘要（前10档）
- 多层次买卖比率分析
- 订单变化追踪

### 图表输出
- 2x2布局图表
- 现货市场深度图（横向条形图）
- 合约市场深度图（横向条形图）
- 现货买卖比率图
- 合约买卖比率图

## 🔍 监控功能

### 实时数据
- 币安现货WebSocket订阅
- 币安合约WebSocket订阅
- 订单簿增量更新
- 数据一致性保证

### 分析功能
- 0-1%, 1-2.5%, 2.5-5%, 5-10% 价格范围分析
- 买卖力量对比
- 订单变化追踪
- 大单监控（基于最小数量阈值）

## 🎛️ 高级配置

### 图表自定义
```python
CHART_CONFIG = {
    "显示订单数量": 10,
    "图表宽度": 1200,
    "图表高度": 800,
    "主题": "dark",
    "格式": "png",
    "发送延迟": 3,  # Discord发送间隔延迟（秒），避免消息丢失
}
```

### 分析范围自定义
```python
ANALYSIS_RANGES = [
    (0, 1),      # 0-1%
    (1, 2.5),    # 1-2.5%
    (2.5, 5),    # 2.5-5%
    (5, 10),     # 5-10%
]
```

## 🛠️ 故障排除

### 常见问题

1. **WebSocket连接失败**
   - 检查网络连接
   - 确认币安API可访问性

2. **Discord发送失败**
   - 验证Webhook URL正确性
   - 检查Discord服务器状态

3. **图表生成失败**
   - 确认kaleido已正确安装
   - 检查plotly版本兼容性

### 调试模式
设置 `"启用控制台输出": True` 可查看详细的运行日志。

## 📝 版本历史

### v2.0.0 (当前版本)
- 重新设计模块化架构
- 统一数据源管理
- 分离文本和图表输出
- 改进图表设计
- 增强配置灵活性

### v1.x
- 原始版本（depthRateSpotAndFuturesChg.py）
- 基础文本输出功能

## 🤝 贡献

欢迎提交Issue和Pull Request来改进这个项目。

## 📄 许可证

MIT License

## ⚠️ 免责声明

本软件仅供学习和研究目的使用，不构成投资建议。使用者需自行承担使用风险。

## ⚙️ 配置选项

### 延迟发送配置
为了避免Discord消息丢失，系统使用智能发送队列：

**工作原理**：
- 所有图表发送请求进入队列
- 专用发送线程按顺序处理
- 每次发送之间自动延迟3秒
- 确保多币种图表都能成功送达

### 最小数量阈值
```python
MIN_QUANTITIES = {
    "BTC": 50.0,     # BTC最小数量为50
    "ETH": 200.0,    # ETH最小数量为200  
    "SOL": 500.0,    # SOL最小数量为500
    "DEFAULT": 1000.0  # 其他币种默认
}
```

### 发送间隔控制
```python
SEND_INTERVALS = {
    "文本输出": 300,  # 文本分析每5分钟发送一次
    "图表输出": 120,  # 图表每2分钟发送一次
}
``` 
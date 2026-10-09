# ExpenseTracker — Android 智能记账 APP 架构设计

## 1. 项目概述

一款 Android 语音记账 APP，核心流程：
**双击电源键 → 语音识别 → AI 语义解析 → 自动分类 → 存入本地数据库 → 主页查询/统计**

---

## 2. 技术栈

| 层次 | 技术选型 |
|------|----------|
| 语言 | Kotlin 100% |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture (3层) |
| 数据库 | Room (SQLite) |
| 网络 | Retrofit + OkHttp + Moshi |
| 语音识别 | Android SpeechRecognizer API |
| AI 语义 | **DeepSeek V4 API** (DeepSeek 兼容 OpenAI 接口格式) |
| DI | Hilt (Dagger) |
| 导航 | Compose Navigation |
| 图表 | Vico (Compose 原生图表库) |
| 最低 SDK | Android 8.0 (API 26)，目标 Android 14 (API 34) |

---

## 3. 项目结构

```
ExpenseTracker/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/expensetracker/
│   │   │   ├── ExpenseTrackerApp.kt          # Application 类，Hilt 入口
│   │   │   ├── MainActivity.kt               # 唯一 Activity
│   │   │   │
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── AppDatabase.kt        # Room 数据库定义
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   └── ExpenseDao.kt     # 账单 DAO
│   │   │   │   │   └── entity/
│   │   │   │   │       ├── ExpenseEntity.kt  # 账单表
│   │   │   │   │       └── CategoryEntity.kt # 分类表
│   │   │   │   ├── remote/
│   │   │   │   │   ├── DeepSeekApiService.kt  # DeepSeek Chat API (Retrofit)
│   │   │   │   │   └── dto/
│   │   │   │   │       ├── ChatRequest.kt     # Chat Completions 请求体
│   │   │   │   │       └── ChatResponse.kt    # Chat Completions 响应体
│   │   │   │   └── repository/
│   │   │   │       ├── ExpenseRepository.kt   # 账单数据仓库
│   │   │   │       └── DeepSeekRepository.kt  # DeepSeek AI 语义解析仓库
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Expense.kt            # 领域模型
│   │   │   │   │   └── ExpenseCategory.kt    # 分类枚举
│   │   │   │   └── usecase/
│   │   │   │       ├── ParseVoiceExpenseUseCase.kt   # 语音→结构化账单
│   │   │   │       ├── AddExpenseUseCase.kt          # 添加账单
│   │   │   │       ├── GetExpensesUseCase.kt         # 查询账单
│   │   │   │       └── GetStatisticsUseCase.kt       # 统计
│   │   │   │
│   │   │   ├── ui/
│   │   │   │   ├── home/
│   │   │   │   │   ├── HomeScreen.kt         # 主页：账单列表 + 统计概览
│   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   ├── add/
│   │   │   │   │   ├── AddExpenseScreen.kt   # 手动添加/语音结果确认
│   │   │   │   │   └── AddExpenseViewModel.kt
│   │   │   │   ├── statistics/
│   │   │   │   │   ├── StatisticsScreen.kt   # 图表统计
│   │   │   │   │   └── StatisticsViewModel.kt
│   │   │   │   ├── settings/
│   │   │   │   │   └── SettingsScreen.kt     # 设置(AI配置、分类管理)
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   ├── Type.kt
│   │   │   │   │   └── Theme.kt
│   │   │   │   ├── navigation/
│   │   │   │   │   └── NavGraph.kt           # 导航图
│   │   │   │   └── components/               # 通用 Compose 组件
│   │   │   │       ├── ExpenseCard.kt
│   │   │   │       ├── CategoryIcon.kt
│   │   │   │       └── MonthSummaryCard.kt
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── VoiceInputService.kt      # 前台服务：语音识别
│   │   │   │   └── PowerButtonDetector.kt    # 双击电源键检测
│   │   │   │
│   │   │   └── di/
│   │   │       └── AppModule.kt              # Hilt DI 模块
│   │   │
│   │   └── res/
│   │       ├── values/strings.xml
│   │       └── drawable/                     # 分类图标
│   │
│   └── build.gradle.kts
├── build.gradle.kts                          # 根构建脚本
├── settings.gradle.kts
└── gradle/libs.versions.toml                 # 版本目录
```

---

## 4. 核心数据流

```
┌─────────────────────────────────────────────────────────────────┐
│                    双击电源键 (PowerButtonDetector)              │
│                          │                                       │
│                          ▼                                       │
│              VoiceInputService.startListening()                  │
│                          │                                       │
│                          ▼                                       │
│              语音文本 (SpeechRecognizer)                         │
│                          │                                       │
│                          ▼                                       │
│        ParseVoiceExpenseUseCase.invoke(text)                     │
│           │                                                     │
│           ├─► DeepSeekRepository.parseExpense(text)              │
│           │      │                                              │
│           │      └─► POST /chat/completions (DeepSeek API)      │
│           │         请求: system="记账助手" user="{text}"         │
│           │         响应: {amount, category, note, date}          │
│           │                                                     │
│           └─► AddExpenseUseCase.invoke(expense)                  │
│                  │                                              │
│                  └─► ExpenseRepository.insert(expense)           │
│                         │                                       │
│                         └─► Room DB (expenses 表)                │
│                                                                 │
│  主页查询: GetExpensesUseCase → ExpenseRepository → Room → UI    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. 数据库设计

### expenses 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER (PK, auto) | 主键 |
| amount | REAL | 金额 |
| category | TEXT | 分类 (餐饮/交通/购物/娱乐/居住/医疗/教育/其他) |
| note | TEXT | 备注 |
| date | INTEGER (timestamp) | 消费日期 |
| created_at | INTEGER | 记录创建时间 |
| source | TEXT | 来源 (voice/manual) |

### categories 表 (预置 + 可自定义)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER (PK, auto) | 主键 |
| name | TEXT | 分类名 |
| icon | TEXT | 图标名称 |
| color | TEXT | 颜色 (#RRGGBB) |
| is_default | INTEGER | 是否系统预置 |

---

## 6. AI 语义解析设计

### DeepSeek V4 API 调用

**Endpoint**: `https://api.deepseek.com/chat/completions`  
**接口格式**: 兼容 OpenAI Chat Completions API

### Prompt 模板
```
你是一个记账助手。请将以下语音输入解析为 JSON 格式的消费记录。
输入: "{语音文本}"

请提取:
- amount: 金额(数字，如35.5)
- category: 分类(餐饮/交通/购物/娱乐/居住/医疗/教育/其他/收入)
- date: 日期(ISO格式 yyyy-MM-dd，若未指明日期则用今天)
- note: 简短备注(中文)

请严格返回以下JSON格式，不要包含其他内容:
{"amount": 35.5, "category": "餐饮", "date": "2026-06-23", "note": "午餐外卖"}
```

### DeepSeek API 配置
- **模型**: `deepseek-chat` (DeepSeek V4)
- **API Key**: 用户在设置页输入，EncryptedSharedPreferences 加密存储
- **Base URL**: `https://api.deepseek.com/`
- 兼容 OpenAI SDK，可直接使用 `com.openai:openai-client` 或 Retrofit 手写接口

---

## 7. 双击电源键检测方案

用户已在系统设置中授予 POWER_KEY 监听权限（Android 无障碍服务 / 设备管理员权限）。

**方案：AccessibilityService + 前台 Service**
- 注册 `AccessibilityService` 监听 `TYPE_POWER_KEY` 按键事件
- 在短时间内（<800ms）连续收到 2 次按下+释放 → 判定为双击
- 通过前台 Service 保持监听活跃，防止系统杀死
- 备选方案：`BroadcastReceiver` 监听 `ACTION_SCREEN_OFF`（600ms 内 2 次 OFF）
- 提供 APP 内 FAB 按钮和通知栏快捷方式作为补充入口

---

## 8. 页面设计

### 主页 (HomeScreen)
- 顶部：本月总支出 / 收入概要卡片
- 中间：按日期分组的账单列表 (LazyColumn)
- 底部：浮动按钮 (FAB) → 手动记账 / 语音记账
- 顶部栏：菜单按钮 → 统计 / 设置

### 统计页 (StatisticsScreen)
- 分类饼图 (Vico DonutChart)
- 月度趋势折线图
- 分类排行榜

### 手动添加页 (AddExpenseScreen)
- 金额输入
- 分类选择器 (Chip 横向滚动)
- 日期选择器
- 备注输入
- 保存按钮

### 设置页 (SettingsScreen)
- DeepSeek API Key 配置 (EncryptedSharedPreferences 加密存储)
- DeepSeek API Endpoint 自定义（默认 `https://api.deepseek.com/`）
- 模型名称配置（默认 `deepseek-chat`）
- 自定义分类管理
- 数据导出/导入 (JSON/CSV)
- 关于

---

## 9. AVL 实现步骤

| 步骤 | 内容 | 涉及文件 |
|------|------|----------|
| 1 | 创建 Gradle 项目骨架，配置依赖 | build.gradle.kts, libs.versions.toml |
| 2 | 定义 Room Entity + DAO + Database | data/local/** |
| 3 | 实现 ExpenseRepository | data/repository/ExpenseRepository.kt |
| 4 | 实现 DeepSeek AI API (Retrofit) | data/remote/** |
| 5 | 实现 Domain UseCases | domain/usecase/** |
| 6 | 实现 Hilt DI 模块 | di/AppModule.kt |
| 7 | 实现 Theme + Navigation | ui/theme/**, ui/navigation/** |
| 8 | 实现 HomeScreen + ViewModel | ui/home/** |
| 9 | 实现 AddExpenseScreen | ui/add/** |
| 10 | 实现 StatisticsScreen | ui/statistics/** |
| 11 | 实现 SettingsScreen | ui/settings/** |
| 12 | 实现语音录入服务 | service/VoiceInputService.kt |
| 13 | 实现双击电源键检测 | service/PowerButtonDetector.kt |
| 14 | 集成测试 + 完善 | - |

---

## 10. 已确认决策

1. **电源键双击** ✅ — 用户自行授权，使用 AccessibilityService 监听 `TYPE_POWER_KEY`
2. **AI API Key** ✅ — 用户在设置页自行输入 DeepSeek API Key，加密存储
3. **AI 模型** ✅ — DeepSeek V4 (`deepseek-chat`)，兼容 OpenAI 接口格式

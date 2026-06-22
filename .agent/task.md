# 记账 APP 项目开发文档

## 📋 项目概述

| 项目 | 内容 |
|------|------|
| 项目名称 | 个人记账 APP |
| 开发语言 | Kotlin |
| 构建环境 | JDK 21 |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构模式 | MVVM |
| 数据存储 | 本地 JSON 文件 |
| 最低系统 | Android 8.0（minSdk 26） |
| 目标系统 | Android 最新稳定版 |

---

## 🛠 技术选型清单

| 类别 | 技术方案 |
|------|----------|
| 开发语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository |
| JSON 序列化 | kotlinx.serialization |
| Excel 导出 | Apache POI |
| 图表 | Vico |
| 日历 | kizitonwose/calendar |
| 异步处理 | Coroutines + Flow |
| 依赖注入 | Hilt（第三阶段引入） |
| 金额处理 | Long（分为单位） |
| 时间存储 | Long（时间戳） |

---

## 📁 项目目录结构

```
app/src/main/java/com.example.bookkeeping/
│
├── ui/                          # UI 层
│   ├── home/                    # 首页
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── add/                     # 新增 / 编辑记录页
│   │   ├── AddRecordScreen.kt
│   │   └── AddRecordViewModel.kt
│   ├── detail/                  # 明细列表页
│   │   ├── DetailScreen.kt
│   │   └── DetailViewModel.kt
│   ├── chart/                   # 图表页
│   │   ├── ChartScreen.kt
│   │   └── ChartViewModel.kt
│   ├── calendar/                # 日历页
│   │   ├── CalendarScreen.kt
│   │   └── CalendarViewModel.kt
│   ├── category/                # 分类管理页
│   │   ├── CategoryScreen.kt
│   │   └── CategoryViewModel.kt
│   ├── member/                  # 成员管理页
│   │   ├── MemberScreen.kt
│   │   └── MemberViewModel.kt
│   ├── template/                # 模板管理页
│   │   ├── TemplateScreen.kt
│   │   └── TemplateViewModel.kt
│   ├── settings/                # 设置页
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   ├── export/                  # 导出 / 备份页
│   │   ├── ExportScreen.kt
│   │   └── ExportViewModel.kt
│   └── components/              # 公共 UI 组件
│       ├── StatCard.kt
│       ├── RecordItem.kt
│       ├── FilterSheet.kt
│       ├── NumberKeyboard.kt
│       └── CategoryGrid.kt
│
├── data/                        # 数据层
│   ├── model/                   # 数据模型
│   │   ├── TransactionRecord.kt
│   │   ├── Category.kt
│   │   ├── Member.kt
│   │   ├── Template.kt
│   │   ├── AppSettings.kt
│   │   └── AppData.kt
│   ├── storage/                 # 存储实现
│   │   ├── JsonFileStorage.kt
│   │   └── StorageExtensions.kt
│   └── repository/              # 数据仓库
│       ├── RecordRepository.kt
│       ├── CategoryRepository.kt
│       ├── MemberRepository.kt
│       ├── TemplateRepository.kt
│       └── SettingsRepository.kt
│
├── domain/                      # 业务逻辑层
│   ├── StatisticsCalculator.kt  # 统计计算
│   ├── FilterEngine.kt          # 筛选逻辑
│   └── ExportManager.kt         # 导出逻辑
│
├── util/                        # 工具类
│   ├── DateTimeUtil.kt
│   ├── AmountUtil.kt
│   ├── IconMapper.kt
│   └── DefaultData.kt           # 预置分类 / 成员数据
│
├── navigation/                  # 导航
│   ├── AppNavHost.kt
│   └── Screen.kt
│
└── MainActivity.kt
```

---

## 📊 数据模型设计

### TransactionRecord 账单记录

```kotlin
@Serializable
data class TransactionRecord(
    val id: String,                    // UUID
    val type: RecordType,              // INCOME / EXPENSE
    val amount: Long,                  // 金额（分为单位，正整数）
    val categoryId: String,            // 分类 ID
    val memberId: String,              // 成员 ID
    val remark: String = "",           // 备注
    val dateTime: Long,                // 账单时间戳
    val createdAt: Long,               // 创建时间戳
    val updatedAt: Long                // 更新时间戳
)

enum class RecordType { INCOME, EXPENSE }
```

### Category 分类

```kotlin
@Serializable
data class Category(
    val id: String,
    val name: String,
    val type: RecordType,              // 归属收入或支出
    val iconName: String,              // Material Icon 名称
    val color: String,                 // HEX 颜色
    val sortOrder: Int,
    val enabled: Boolean = true,
    val isSystem: Boolean = false      // 系统预置不可删除
)
```

### Member 成员

```kotlin
@Serializable
data class Member(
    val id: String,
    val name: String,
    val avatarColor: String,           // HEX 颜色
    val iconName: String = "",
    val enabled: Boolean = true
)
```

### Template 模板

```kotlin
@Serializable
data class Template(
    val id: String,
    val name: String,
    val type: RecordType,
    val amount: Long? = null,          // 可为空，套模板后手动填金额
    val categoryId: String,
    val memberId: String,
    val remark: String = ""
)
```

### AppSettings 应用设置

```kotlin
@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lastExpenseCategoryId: String? = null,
    val lastIncomeCategoryId: String? = null,
    val lastMemberId: String? = null,
    val currencySymbol: String = "¥"
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
```

### AppData 根数据结构

```kotlin
@Serializable
data class AppData(
    val version: Int = 1,
    val records: List<TransactionRecord> = emptyList(),
    val categories: List<Category> = emptyList(),
    val members: List<Member> = emptyList(),
    val templates: List<Template> = emptyList(),
    val settings: AppSettings = AppSettings()
)
```

---

## 📄 JSON 存储结构示例

**文件路径**：`filesDir/app_data.json`
**临时写入路径**：`filesDir/app_data.json.tmp`

```json
{
  "version": 1,
  "records": [
    {
      "id": "uuid-001",
      "type": "EXPENSE",
      "amount": 3200,
      "categoryId": "cat-lunch",
      "memberId": "mem-self",
      "remark": "公司楼下午饭",
      "dateTime": 1737000000000,
      "createdAt": 1737000000000,
      "updatedAt": 1737000000000
    }
  ],
  "categories": [
    {
      "id": "cat-lunch",
      "name": "午餐",
      "type": "EXPENSE",
      "iconName": "Restaurant",
      "color": "#FF7043",
      "sortOrder": 3,
      "enabled": true,
      "isSystem": true
    }
  ],
  "members": [
    {
      "id": "mem-self",
      "name": "自己",
      "avatarColor": "#42A5F5",
      "iconName": "",
      "enabled": true
    }
  ],
  "templates": [],
  "settings": {
    "themeMode": "SYSTEM",
    "lastExpenseCategoryId": "cat-lunch",
    "lastIncomeCategoryId": null,
    "lastMemberId": "mem-self",
    "currencySymbol": "¥"
  }
}
```

**安全写入策略**

```
1. 将新内容序列化为字符串
2. 写入 app_data.json.tmp
3. 写入成功后 rename 为 app_data.json
4. 删除临时文件
```

---

## 🗂 预置数据清单

### 支出分类

| ID | 名称 | 图标 | 颜色 |
|----|------|------|------|
| cat-transport | 交通 | DirectionsBus | #42A5F5 |
| cat-snack | 零食 | Cookie | #FFA726 |
| cat-breakfast | 早餐 | FreeBreakfast | #FFCA28 |
| cat-lunch | 午餐 | Restaurant | #FF7043 |
| cat-dinner | 晚餐 | DinnerDining | #EC407A |
| cat-entertainment | 休闲娱乐 | SportsEsports | #AB47BC |
| cat-express | 邮寄 | LocalPostOffice | #26A69A |
| cat-daily | 日用品 | ShoppingCart | #66BB6A |
| cat-rent | 房租 | Home | #5C6BC0 |
| cat-utility | 水电 | ElectricBolt | #29B6F6 |
| cat-telecom | 通讯 | PhoneAndroid | #26C6DA |
| cat-medical | 医疗 | LocalHospital | #EF5350 |
| cat-education | 学习 | MenuBook | #7E57C2 |
| cat-clothing | 服饰 | Checkroom | #EC407A |
| cat-expense-other | 其他 | MoreHoriz | #BDBDBD |

### 收入分类

| ID | 名称 | 图标 | 颜色 |
|----|------|------|------|
| cat-salary | 工资 | Payments | #66BB6A |
| cat-bonus | 奖金 | CardGiftcard | #FFCA28 |
| cat-transfer | 转账 | SwapHoriz | #42A5F5 |
| cat-investment | 理财 | TrendingUp | #26A69A |
| cat-parttime | 兼职 | Work | #FFA726 |
| cat-refund | 退款 | AssignmentReturn | #EC407A |
| cat-income-other | 其他收入 | MoreHoriz | #BDBDBD |

### 预置成员

| ID | 名称 | 颜色 |
|----|------|------|
| mem-self | 自己 | #42A5F5 |
| mem-spouse | 老婆 | #EC407A |
| mem-child | 孩子 | #FFCA28 |
| mem-company | 公司 | #5C6BC0 |
| mem-family | 家庭公用 | #66BB6A |

---

## 📱 页面结构与导航设计

### 底部导航结构

```
首页 (Home)  →  图表 (Chart)  →  日历 (Calendar)  →  设置 (Settings)
                                     ↑
                              悬浮 + 按钮（新增记录）
```

### 页面导航关系

```
MainActivity
└── AppNavHost
    ├── HomeScreen                  底部 Tab 1
    │   └── DetailScreen            点击统计卡片进入
    │       └── AddRecordScreen     点击编辑进入
    ├── AddRecordScreen             点击悬浮 + 进入
    ├── ChartScreen                 底部 Tab 2
    ├── CalendarScreen              底部 Tab 3
    │   └── DetailScreen            点击某天进入
    └── SettingsScreen              底部 Tab 4
        ├── CategoryScreen          分类管理
        ├── MemberScreen            成员管理
        ├── TemplateScreen          模板管理
        └── ExportScreen            导出备份
```

---

## 🖥 页面设计规范

### 首页（HomeScreen）

```
┌─────────────────────────────┐
│  本月统计大卡片              │
│  ┌─────┬─────┬─────┐        │
│  │支出 │收入 │结余  │        │
│  │¥xx  │¥xx  │¥xx  │        │
│  └─────┴─────┴─────┘        │
│  [本月] [上月]  切换按钮     │
├─────────────────────────────┤
│  今日统计卡片               │
│  支出 ¥xx  收入 ¥xx  结余 ¥xx│
├─────────────────────────────┤
│  本周统计卡片               │
│  支出 ¥xx  收入 ¥xx  结余 ¥xx│
├─────────────────────────────┤
│  本年统计卡片               │
│  支出 ¥xx  收入 ¥xx  结余 ¥xx│
├─────────────────────────────┤
│  最近记录                   │
│  [图标] 分类  备注   ¥金额  │
│  [图标] 分类  备注   ¥金额  │
│  ...                        │
├─────────────────────────────┤
│  [首页]  [图表] [日历] [设置]│
│              [+]             │
└─────────────────────────────┘
```

**交互说明**

- 统计卡片点击 → 进入对应时间范围的明细列表页
- 最近记录点击 → 进入编辑页
- 悬浮 `+` 点击 → 进入新增记录页

---

### 新增 / 编辑记录页（AddRecordScreen）

```
┌─────────────────────────────┐
│  ← 返回    记一笔           │
├─────────────────────────────┤
│  [模板]  [支出]  [收入]     │  顶部 Tab 切换
├─────────────────────────────┤
│                             │
│         ¥ 0.00              │  金额大显示区
│                             │
├─────────────────────────────┤
│  分类（宫格图标展示）        │
│  🚌交通  🍪零食  ☕早餐     │
│  🍽午餐  🍽晚餐  🎮娱乐     │
│  ...                        │
├─────────────────────────────┤
│  成员  [自己 ▼]             │
│  时间  [2025-01-16 12:00 ▼] │
│  备注  [请输入备注...]       │
├─────────────────────────────┤
│  [存为模板]     [完成保存]  │
└─────────────────────────────┘
```

**交互说明**

- 顶部切换为"模板"时，展示模板列表供选择
- 金额输入建议使用自定义数字键盘
- 分类默认选中上次使用的分类
- 成员默认选中上次使用的成员
- 时间默认为当前时间
- 保存成功后轻提示 SnackBar，并可继续记下一笔

---

### 明细列表页（DetailScreen）

```
┌─────────────────────────────┐
│  ← 本周明细        [筛选⚙] │
├─────────────────────────────┤
│  2025-01-16 周四            │
│  收入¥100 支出¥56 结余¥44  │
│  ─────────────────────────  │
│  🍽 午餐  公司楼下  ¥32.00  │
│  🚌 交通  回家     ¥24.00  │
├─────────────────────────────┤
│  2025-01-15 周三            │
│  收入¥0 支出¥88 结余-¥88   │
│  ─────────────────────────  │
│  🎮 娱乐  游戏充值  ¥88.00  │
└─────────────────────────────┘
```

**筛选弹窗包含**

```
类型：[全部] [收入] [支出]
分类：多选图标宫格
成员：多选列表
金额：¥最小值 ~ ¥最大值
时间：开始日期 ~ 结束日期
备注：关键词搜索
排序：时间倒序 / 正序 / 金额高低
```

---

### 图表页（ChartScreen）

```
┌─────────────────────────────┐
│  收支图表    [年度 ▼]       │
├─────────────────────────────┤
│  柱状图（12个月）           │
│  绿柱=收入  红柱=支出       │
│  ████████████████████       │
│  1  2  3  4  5  6  7 ...   │
├─────────────────────────────┤
│  年度汇总                   │
│  总收入：¥xx,xxx            │
│  总支出：¥xx,xxx            │
│  结余：  ¥xx,xxx            │
│  最高支出月：X 月           │
│  最高收入月：X 月           │
└─────────────────────────────┘
```

---

### 日历页（CalendarScreen）

```
┌─────────────────────────────┐
│  ← 2025年01月 →            │
├──┬──┬──┬──┬──┬──┬──────────┤
│日│一│二│三│四│五│六         │
├──┼──┼──┼──┼──┼──┼──────────┤
│  │  │  │1 │2 │3 │4          │
│  │  │  │-32│  │+100│       │
├──┼──┼──┼──┼──┼──┼──────────┤
│5 │6 │7 │8 │9 │10│11         │
│  │  │  │  │  │  │           │
└──────────────────────────────┘
│  点击某天 → 进入该日明细    │
└─────────────────────────────┘
```

**颜色规则**

- 收入数字：绿色
- 支出数字：红色
- 结余为正：蓝色
- 结余为负：橙色

---

### 设置页（SettingsScreen）

```
┌─────────────────────────────┐
│  设置                       │
├─────────────────────────────┤
│  外观                       │
│  主题模式  [跟随系统 ▼]     │
├─────────────────────────────┤
│  数据管理                   │
│  分类管理          →        │
│  成员管理          →        │
│  模板管理          →        │
├─────────────────────────────┤
│  导出与备份                 │
│  导出 Excel        →        │
│  导出 JSON 备份    →        │
│  从 JSON 导入      →        │
└─────────────────────────────┘
```

---

## 📦 Excel 导出结构

### Sheet 1：账单明细

| 日期 | 时间 | 类型 | 分类 | 成员 | 金额（元） | 备注 |
|------|------|------|------|------|-----------|------|
| 2025-01-16 | 12:30 | 支出 | 午餐 | 自己 | 32.00 | 公司楼下 |

### Sheet 2：月度汇总

| 月份 | 总收入（元） | 总支出（元） | 结余（元） |
|------|-------------|-------------|-----------|
| 2025-01 | 8000.00 | 3200.00 | 4800.00 |

### Sheet 3：分类汇总

| 分类 | 类型 | 金额（元） | 占比 |
|------|------|-----------|------|
| 午餐 | 支出 | 800.00 | 25% |

---

## 📐 架构分层说明

```
┌─────────────────────────────────────────┐
│              UI 层                       │
│   Compose Screen + ViewModel + UiState  │
└──────────────┬──────────────────────────┘
               │ 调用
┌──────────────▼──────────────────────────┐
│             Domain 层                    │
│   StatisticsCalculator（统计计算）       │
│   FilterEngine（筛选逻辑）               │
│   ExportManager（导出逻辑）             │
└──────────────┬──────────────────────────┘
               │ 调用
┌──────────────▼──────────────────────────┐
│              Data 层                     │
│   Repository（数据仓库）                │
│   JsonFileStorage（JSON 文件读写）       │
│   AppData（内存数据对象）               │
└─────────────────────────────────────────┘
```

**关键规则**

- UI 层只和 ViewModel 通信，不直接访问数据
- ViewModel 持有 UiState，通过 StateFlow 暴露给 Compose
- Repository 持有内存中的 AppData，修改后触发 JSON 持久化
- Domain 层为纯函数，不持有状态

---

## ✅ 开发任务流程

### 第一阶段：MVP 核心可用版

> 目标：能正常记账、查看统计、本地保存

---

#### TASK-001 项目初始化

```
优先级：P0
预估时间：0.5 天

子任务：
□ 创建 Android 项目
□ 配置 build.gradle（Compose、Material3、Kotlin Serialization）
□ 配置 JDK 21
□ 添加基础依赖
□ 配置 minSdk 26 / targetSdk
□ 建立目录结构（ui / data / domain / util / navigation）
□ 创建 MainActivity 并接入 Compose
```

---

#### TASK-002 数据模型

```
优先级：P0
预估时间：0.5 天
依赖：TASK-001

子任务：
□ 创建 RecordType 枚举
□ 创建 ThemeMode 枚举
□ 创建 TransactionRecord 数据类
□ 创建 Category 数据类
□ 创建 Member 数据类
□ 创建 Template 数据类
□ 创建 AppSettings 数据类
□ 创建 AppData 根数据类
□ 所有类添加 @Serializable 注解
□ 创建 FilterParams 筛选参数类
□ 创建 StatResult 统计结果类
```

---

#### TASK-003 JSON 存储层

```
优先级：P0
预估时间：1 天
依赖：TASK-002

子任务：
□ 创建 JsonFileStorage.kt
□ 实现 loadData() 从文件读取 AppData
□ 实现 saveData() 安全写入（先写 .tmp 再 rename）
□ 处理文件不存在时的初始化逻辑
□ 处理 JSON 解析异常（损坏时恢复默认）
□ 创建 DefaultData.kt 预置分类和成员数据
□ 首次启动注入预置数据
□ 编写存储层单元测试
```

---

#### TASK-004 Repository 层

```
优先级：P0
预估时间：1 天
依赖：TASK-003

子任务：
□ 创建 AppDataRepository（核心仓库）
□ 实现内存 AppData 的增删改查
□ 每次修改后异步触发 JSON 保存
□ 实现 RecordRepository（账单相关操作）
□ 实现 CategoryRepository（分类相关操作）
□ 实现 MemberRepository（成员相关操作）
□ 实现 SettingsRepository（设置读写）
□ 暴露 Flow<AppData> 供 ViewModel 订阅
```

---

#### TASK-005 底部导航框架

```
优先级：P0
预估时间：0.5 天
依赖：TASK-001

子任务：
□ 创建 Screen 路由枚举
□ 创建 AppNavHost.kt
□ 实现底部导航栏（首页 / 图表 / 日历 / 设置）
□ 实现悬浮 + 按钮（FAB）
□ FAB 点击跳转新增记录页
□ 各 Tab 页面先用占位页填充
```

---

#### TASK-006 新增记录页

```
优先级：P0
预估时间：2 天
依赖：TASK-004 / TASK-005

子任务：
□ 创建 AddRecordScreen.kt
□ 创建 AddRecordViewModel.kt
□ 实现顶部 Tab 切换（支出 / 收入 / 模板）
□ 实现金额大显示区域
□ 实现自定义数字键盘组件
□ 实现分类宫格选择组件（CategoryGrid）
□ 实现成员下拉选择
□ 实现日期时间选择器
□ 实现备注输入框
□ 实现"完成保存"按钮逻辑
□ 保存成功后 SnackBar 提示
□ 默认选中上次使用的分类和成员
□ 编辑模式：传入 recordId 时加载已有数据
□ 实现"存为模板"按钮逻辑
```

---

#### TASK-007 首页统计

```
优先级：P0
预估时间：2 天
依赖：TASK-004 / TASK-005

子任务：
□ 创建 StatisticsCalculator.kt
□ 实现本月/上月 总收入、总支出、结余计算
□ 实现今日 收入、支出、结余计算
□ 实现本周 收入、支出、结余计算
□ 实现本年 收入、支出、结余计算
□ 创建 HomeViewModel.kt
□ 创建 HomeScreen.kt
□ 实现本月统计大卡片组件（StatCard）
□ 实现今日 / 本周 / 本年 统计卡片
□ 实现本月/上月切换功能
□ 实现最近记录列表（RecordItem 组件）
□ 统计卡片点击跳转明细列表页
```

---

#### TASK-008 明细列表页

```
优先级：P0
预估时间：1.5 天
依赖：TASK-007

子任务：
□ 创建 DetailScreen.kt
□ 创建 DetailViewModel.kt
□ 实现按日分组显示记录列表
□ 每日分组显示当日收入 / 支出 / 结余
□ 每条记录显示：图标、分类名、备注、成员、时间、金额
□ 金额收入绿色、支出红色
□ 点击记录跳转编辑页
□ 列表左滑弹出删除按钮
□ 删除前二次确认弹窗
□ 接收时间范围参数（今日 / 本周 / 本月 / 本年）
□ 空状态页面（无数据时引导提示）
```

---

#### TASK-009 筛选功能

```
优先级：P1
预估时间：1.5 天
依赖：TASK-008

子任务：
□ 创建 FilterEngine.kt
□ 实现按类型筛选（全部 / 收入 / 支出）
□ 实现按分类多选筛选
□ 实现按成员多选筛选
□ 实现按金额范围筛选
□ 实现按时间范围筛选
□ 实现按备注关键词搜索
□ 实现排序（时间正倒序 / 金额高低）
□ 创建 FilterSheet.kt（底部弹出筛选面板）
□ 明细页右上角筛选按钮接入 FilterSheet
□ 筛选条件激活时显示标识
□ 筛选结果为空时显示空状态
```

---

#### TASK-010 主题切换

```
优先级：P1
预估时间：0.5 天
依赖：TASK-005

子任务：
□ 配置 Material 3 LightColorScheme
□ 配置 Material 3 DarkColorScheme
□ 在 AppTheme 中接入 ThemeMode 枚举
□ SettingsViewModel 实现主题设置读写
□ SettingsScreen 实现主题切换 UI
□ 确认收入/支出颜色在深色模式下对比度足够
```

---

### 第二阶段：效率提升版

> 目标：支持成员管理、模板、分类管理

---

#### TASK-011 成员管理页

```
优先级：P1
预估时间：1 天
依赖：TASK-004

子任务：
□ 创建 MemberScreen.kt
□ 创建 MemberViewModel.kt
□ 显示成员列表
□ 实现新增成员（名称 + 颜色选择）
□ 实现编辑成员
□ 实现启用/禁用成员
□ 禁用成员不在记账页显示
□ 系统预置成员不可删除，仅可禁用
□ 删除成员前提示已有账单关联处理
```

---

#### TASK-012 分类管理页

```
优先级：P1
预估时间：1 天
依赖：TASK-004

子任务：
□ 创建 CategoryScreen.kt
□ 创建 CategoryViewModel.kt
□ 支出分类 / 收入分类 Tab 切换
□ 显示分类列表（图标 + 名称 + 启用开关）
□ 实现启用/禁用分类
□ 实现分类排序拖拽（可选，后期优化）
□ 系统预置分类不可删除
□ 后期扩展：支持用户自定义分类
```

---

#### TASK-013 模板功能

```
优先级：P1
预估时间：1.5 天
依赖：TASK-006 / TASK-011

子任务：
□ 创建 TemplateScreen.kt
□ 创建 TemplateViewModel.kt
□ 按收入模板 / 支出模板分组显示
□ 实现新增模板
□ 实现编辑模板
□ 实现删除模板（二次确认）
□ 新增记录页模板 Tab 接入模板列表
□ 点击模板自动填充分类、成员、金额（可选）、备注
□ 新增记录页"存为模板"按钮完整实现
```

---

### 第三阶段：分析展示版

> 目标：上线图表和日历功能

---

#### TASK-014 图表页

```
优先级：P2
预估时间：1.5 天
依赖：TASK-004

子任务：
□ 接入 Vico 图表库
□ 创建 ChartScreen.kt
□ 创建 ChartViewModel.kt
□ 实现年度收支柱状图（12 个月）
□ 收入绿柱 / 支出红柱
□ 顶部切换年度（上一年 / 下一年）
□ 图表下方显示年度汇总数据
□ 显示最高支出月 / 最高收入月
□ 无数据月份显示空柱
```

---

#### TASK-015 日历页

```
优先级：P2
预估时间：1.5 天
依赖：TASK-004

子任务：
□ 接入 kizitonwose/calendar 日历库
□ 创建 CalendarScreen.kt
□ 创建 CalendarViewModel.kt
□ 实现月视图日历
□ 每天格子显示支出金额（红色）
□ 每天格子显示收入金额（绿色）
□ 点击某天跳转该日明细列表
□ 左右滑动切换月份
□ 当天高亮显示
□ 无数据天格子保持干净，不显示 0
```

---

### 第四阶段：导出与完善版

> 目标：Excel 导出、JSON 备份、UI 完善

---

#### TASK-016 Excel 导出

```
优先级：P2
预估时间：1.5 天
依赖：TASK-004

子任务：
□ 接入 Apache POI 依赖
□ 创建 ExportManager.kt
□ 实现 Sheet1 账单明细导出
□ 实现 Sheet2 月度汇总导出
□ 实现 Sheet3 分类汇总导出
□ 金额格式化为元（保留两位小数）
□ 文件名包含导出日期
□ 使用 FileProvider 保存到 Downloads 目录
□ 导出完成后弹出分享/打开选项
□ 创建 ExportScreen.kt（导出设置页）
□ 支持选择导出时间范围
```

---

#### TASK-017 JSON 备份与恢复

```
优先级：P2
预估时间：1 天
依赖：TASK-003

子任务：
□ 实现导出 JSON 到 Downloads 目录
□ 实现从本地 JSON 文件导入（恢复）
□ 导入时校验 JSON 版本和格式
□ 导入前提示"将覆盖当前数据"的确认弹窗
□ 导入成功后刷新全部数据
□ 支持版本迁移逻辑（version 字段）
```

---

#### TASK-018 UI 完善与优化

```
优先级：P3
预估时间：2 天
依赖：全部前置任务

子任务：
□ 统一各页面 TopBar 样式
□ 统一空状态页面样式和引导文案
□ 金额显示统一格式化工具类
□ 日期显示统一格式化工具类
□ 收入绿色 / 支出红色全局统一
□ 列表滑动删除手势体验优化
□ 所有图标添加无障碍语义描述
□ 字体支持系统缩放
□ Loading 状态处理（存储操作时）
□ 错误状态处理（文件损坏时提示）
□ 首页增加"本月最高支出分类"展示
□ 记账页增加最近常用分类快捷显示
□ 深色模式下颜色对比度全面检查
```

---

## 📊 开发进度总览

| 阶段 | 任务编号 | 任务名称 | 预估时间 | 优先级 |
|------|----------|----------|----------|--------|
| 第一阶段 | TASK-001 | 项目初始化 | 0.5 天 | P0 |
| 第一阶段 | TASK-002 | 数据模型 | 0.5 天 | P0 |
| 第一阶段 | TASK-003 | JSON 存储层 | 1 天 | P0 |
| 第一阶段 | TASK-004 | Repository 层 | 1 天 | P0 |
| 第一阶段 | TASK-005 | 底部导航框架 | 0.5 天 | P0 |
| 第一阶段 | TASK-006 | 新增记录页 | 2 天 | P0 |
| 第一阶段 | TASK-007 | 首页统计 | 2 天 | P0 |
| 第一阶段 | TASK-008 | 明细列表页 | 1.5 天 | P0 |
| 第一阶段 | TASK-009 | 筛选功能 | 1.5 天 | P1 |
| 第一阶段 | TASK-010 | 主题切换 | 0.5 天 | P1 |
| 第二阶段 | TASK-011 | 成员管理页 | 1 天 | P1 |
| 第二阶段 | TASK-012 | 分类管理页 | 1 天 | P1 |
| 第二阶段 | TASK-013 | 模板功能 | 1.5 天 | P1 |
| 第三阶段 | TASK-014 | 图表页 | 1.5 天 | P2 |
| 第三阶段 | TASK-015 | 日历页 | 1.5 天 | P2 |
| 第四阶段 | TASK-016 | Excel 导出 | 1.5 天 | P2 |
| 第四阶段 | TASK-017 | JSON 备份与恢复 | 1 天 | P2 |
| 第四阶段 | TASK-018 | UI 完善与优化 | 2 天 | P3 |
| | | **合计** | **约 22 天** | |

---

## ⚠️ 关键开发规范

### 金额规范

```
存储：Long 类型，单位为分
示例：32.50 元 → 存储为 3250
显示：AmountUtil.format(3250) → "¥32.50"
禁止：使用 Double 直接存储金额
```

### 时间规范

```
存储：Long 时间戳，单位毫秒
获取：System.currentTimeMillis()
格式化：DateTimeUtil.formatDate(timestamp) → "2025-01-16"
         DateTimeUtil.formatTime(timestamp) → "12:30"
         DateTimeUtil.formatDateTime(timestamp) → "2025-01-16 12:30"
```

### 存储规范

```
写入流程：
  1. 序列化 AppData 为 JSON 字符串
  2. 写入 app_data.json.tmp
  3. 成功后 rename 为 app_data.json
  4. 失败时保留旧文件不覆盖

读取流程：
  1. 读取 app_data.json
  2. 反序列化为 AppData
  3. 失败时返回 DefaultData（并记录错误日志）
```

### ID 规范

```
所有实体 ID 使用 UUID
生成方式：UUID.randomUUID().toString()
```

---

## 🔗 依赖清单（build.gradle 参考）

```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.xx.xx"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.x")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.x")

// Kotlin Serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.x")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.x")

// 图表
implementation("com.patrykandpatrick.vico:compose-m3:1.x.x")

// 日历
implementation("com.kizitonwose.calendar:compose:2.x.x")

// Excel 导出
implementation("org.apache.poi:poi:5.x.x")
implementation("org.apache.poi:poi-ooxml:5.x.x")
```

---

*文档版本：v1.0 | 最后更新：2026年*
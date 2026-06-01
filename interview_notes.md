# Android 技术面试考点总结 (基于 Inbox-Flow 项目)

这份文档总结了我们在开发 `inbox-flow` 过程中涉及到的核心 Android 与 Compose 技术考点。这些内容在高级 Android 面试中非常高频，涵盖了性能优化、状态管理和架构设计。

---

## 1. Jetpack Compose 核心机制

### 1.1 Surface 的作用
*   **不仅仅是容器**：Surface 负责应用 Material Design 的背景色、形状、海拔（Tonal Elevation）。
*   **颜色传递 (Content Color Propagation)**：通过 `LocalContentColor` 自动设置子项（如 Text/Icon）的前景色，确保对比度。

### 1.2 状态提升 (State Hoisting)
*   **概念**：将状态移至组件外部，使组件变为“无状态”（Stateless），提高可测试性和复用性。
*   **项目实践**：将 `listState` 从 `EmailList` 提升到 `InboxScreen`，以实现搜索栏外观随列表滚动的联动效果。

### 1.3 remember 与 rememberLazyListState
*   **remember**：在重组期间保留对象，防止状态丢失或重复创建昂贵对象。
*   **rememberLazyListState**：特殊的 `remember`，用于持久化列表的滚动位置。它实际上是 `LazyColumn` 的“遥控器”和“状态记录仪”。

---

## 2. Compose 性能优化 (实战高频)

### 2.1 重组作用域最小化 (Recomposition Scope)
*   **痛点**：在父容器读取频繁变化的状态（如滚动位置）会导致整个页面重组。
*   **方案**：将状态读取下移到真正需要的子组件中。例如将 `listState` 传给 `SearchBanner`，在内部使用 `derivedStateOf` 读取 `isScrolled`。
*   **结果**：滚动时只有 SearchBanner 重组，避免了整个邮件列表的无效刷新。

### 2.2 derivedStateOf 的重要性
*   **作用**：将高频率变化的状态（如像素级偏移）转换为低频率变化的状态（如布尔值）。
*   **场景**：判断“是否已滚动”，避免因像素级偏移的变化频繁触发重组。

### 2.3 稳定性 (Stability) 与 Lambda 缓存
*   **跳过重组 (Skipping)**：Compose 编译器会检查参数是否发生变化来决定是否跳过重组。
*   **Lambda 稳定性**：使用 `remember { { ... } }` 包装传给子组件的回调函数。
*   **后果**：如果不使用 `remember`，每次父组件重组都会生成新的 Lambda 对象，导致子组件（如 `EmailList`）的 `Skips` 计数为 0。

### 2.4 避免在 Composable 中进行重计算
*   **反模式**：在 `remember` 块内执行复杂的循环、映射（如 `AvatarView` 优化前出现的 100/500 次循环）。
*   **优化**：直接提取数据，使用简单的哈希算法从预定义列表中获取颜色，显著降低主线程压力。

### 2.5 列表项 Key 的稳定性
*   **实践**：在 `LazyColumn` 的 `items` 块中使用 `key = { it.id }`。
*   **原理**：如果不提供 key，Compose 默认使用 position。当在顶部插入新项时，所有后续项的 position 都会变，导致全列表重绘。使用稳定 ID 后，Compose 仅处理插入动作，其余项触发 Skipping。

---

## 3. Kotlin 协程与线程管理

### 3.1 viewModelScope
*   **生命周期感知**：绑定到 ViewModel，当 ViewModel 清除时自动取消所有协程，防止内存泄漏。

### 3.2 主线程安全 (Main-safety)
*   **Dispatcher 切换**：在 Repository 内部使用 `withContext(Dispatchers.IO)` 处理耗时操作（如列表截取、模拟延时）。
*   **优势**：确保 ViewModel 在调用挂起函数时不会阻塞主线程，即使在 `viewModelScope`（默认 Main）中启动。

---

## 4. UI/UX 与 架构设计

### 4.1 动态 UI 效果
*   **animateDpAsState**：用于实现平滑的属性动画（如搜索栏的海拔浮起）。
*   **animateContentSize**：自动处理容器高度变化的过渡动画（如列表项展开/收起）。

### 4.2 导航系统 (Navigation)
*   **NavHost & NavController**：集成了 Compose 官方导航方案，实现全屏页面跳转。
*   **用户体验**：将邮件撰写从对话框改为全屏页面，符合 Gmail 等主流邮件客户端的设计规范。

### 4.3 状态管理 (UDF/MVI)
*   **InboxUiState**：将整个屏幕的状态聚合在一个不可变的 Data Class 中。
*   **单向数据流**：通过 ID 引用（如 `expandedEmailId`）而非修改数据模型内部状态来实现交互，保持了数据的一致性和可预测性。

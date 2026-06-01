# Android 技术面试考点总结 (基于 Inbox-Flow 项目)

这份文档总结了我们在开发 `inbox-flow` 过程中涉及到的核心 Android 与 Compose 技术考点，这些内容在高级 Android 面试中非常高频。

---

## 1. Jetpack Compose 核心机制

### 1.1 Surface 的作用
*   **不仅仅是容器**：Surface 负责应用 Material Design 的背景色、形状、海拔（Tonal Elevation）。
*   **颜色传递（Content Color Propagation）**：通过 `LocalContentColor` 自动设置子项（如 Text/Icon）的前景色，确保对比度。

### 1.2 状态提升 (State Hoisting)
*   **概念**：将状态移至组件外部，使组件变为“无状态”（Stateless），提高可测试性和复用性。
*   **项目实践**：将 `listState` 从 `EmailList` 提升到 `InboxScreen`，以实现搜索栏与列表滚动的联动。

### 1.3 remember 与 rememberLazyListState
*   **remember**：在重组期间保留对象，防止状态丢失或重复创建昂贵对象。
*   **rememberLazyListState**：特殊的 `remember`，用于持久化列表的滚动位置。它实际上是 `LazyColumn` 的“遥控器”。

---

## 2. Compose 性能优化 (实战高频)

### 2.1 重组作用域最小化 (Recomposition Scope)
*   **痛点**：父容器读取频繁变化的状态（如滚动位置）会导致整个页面重组。
*   **方案**：将状态读取下移到真正需要的子组件中（例如将 `listState` 传给 `SearchBanner`，在内部读取 `isScrolled`）。

### 2.2 derivedStateOf 的重要性
*   **作用**：将高频率变化的状态（像素级偏移）转换为低频率变化的状态（布尔值）。
*   **场景**：判断“是否已滚动”、“是否滚动到底部”，避免像素级触发重组。

### 2.3 稳定性 (Stability) 与 @Immutable
*   **跳过重组 (Skipping)**：Compose 编译器会检查参数是否发生变化。
*   **Lambda 稳定性**：使用 `remember { { ... } }` 包装传给子组件的回调，防止因 Lambda 引用变化导致的无效重组。
*   **数据类标记**：使用 `@Immutable` 或 `@Stable` 标记数据类，告知编译器该类不会发生意外变化，从而安全地跳过重组。

### 2.4 避免在 Composable 中进行重计算
*   **反模式**：在 `remember` 块内执行复杂的循环、映射（如我们在 `AvatarView` 优化前看到的 500 次循环）。
*   **优化**：预定义常量，使用简单的哈希算法，或者将计算逻辑移出 UI 层。

---

## 3. Kotlin 协程与线程管理

### 3.1 viewModelScope
*   **生命周期感知**：绑定到 ViewModel，当 ViewModel 清除时自动取消所有协程，防止内存泄漏。
*   **默认 Dispatcher**：默认运行在 `Dispatchers.Main`，适合直接更新 UI 状态。

### 3.2 挂起 (Suspend) vs 阻塞 (Blocking)
*   **挂起函数**：如 `delay()`，会释放当前线程，允许线程去处理其他任务（如 UI 渲染），直到恢复运行。
*   **主线程安全 (Main-safe)**：Repository 内部应负责切换线程（使用 `withContext(Dispatchers.IO)`），确保 ViewModel 调用时不会卡顿。

---

## 4. UI/UX 交互设计

### 4.1 动态 UI 效果
*   **animateDpAsState / animateColorAsState**：用于实现平滑的属性动画（如搜索栏的海拔浮起、列表项背景色切换）。
*   **animateContentSize**：自动处理容器高度变化的过渡动画，常用于列表项展开/收起。

### 4.2 导航系统 (Navigation)
*   **NavHost & NavController**：Compose 官方导航方案。
*   **页面跳转 vs 对话框**：全屏撰写页提供更好的沉浸感，符合 Gmail 等主流应用的设计规范。

---

## 5. 架构模式 (MVI/MVVM)

### 5.1 单一可信源 (Single Source of Truth)
*   **InboxUiState**：将整个屏幕的状态聚合在一个不可变的 Data Class 中，通过 `StateFlow` 暴露，实现单向数据流 (UDF)。

### 5.2 响应式编程
*   **combine**：利用 Flow 的操作符，将搜索词、邮件列表、展开状态等多个流合并成一个统一的 UI State。

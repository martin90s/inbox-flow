# Jetpack Compose Performance Case Study: Inbox App

This document outlines the intentional performance bottlenecks introduced to this project to simulate real-world "jank" and the corresponding optimizations used to achieve 60/120 FPS scrolling.

## 1. The State Read Pitfall (High-Frequency Recomposition)

### The Problem
Reading a frequently changing state (like `LazyListState.firstVisibleItemScrollOffset`) directly in the body of a Composable function.

```kotlin
// ❌ ANTI-PATTERN: Recomposes the entire list on every pixel scrolled
val scrollOffset = listState.firstVisibleItemScrollOffset
```

### The Fix
Use `derivedStateOf` to "buffer" the state and only trigger recompositions when a specific condition changes.

```kotlin
// ✅ OPTIMIZATION: Only recomposes when the boolean result changes
val isScrolled by remember {
    derivedStateOf { listState.firstVisibleItemScrollOffset > 0 }
}
```

---

## 2. Main Thread Blocking (Computational Stress)

### The Problem
Performing heavy CPU-bound calculations directly inside a Composable without memoization.

```kotlin
// ❌ ANTI-PATTERN: Runs 20,000 iterations on every recomposition
val heavyCalculation = (0..20000).map { ... }.sorted().sum()
```

### The Fix
Wrap expensive operations in `remember` using a stable key (like an item ID) to ensure they only run once.

```kotlin
// ✅ OPTIMIZATION: Calculation only runs once per unique email ID
val heavyCalculation = remember(email.id) {
    (0..20000).map { ... }.sorted().sum()
}
```

---

## 3. Memory Pressure (Allocation Spikes)

### The Problem
Allocating large objects or collections inside a Composable that triggers frequently. This causes the Garbage Collector (GC) to run, causing "hiccups" in the UI.

```kotlin
// ❌ ANTI-PATTERN: Allocates 500 Color objects every frame
val colors = (0..500).map { Color(...) }
```

### The Fix
Use `remember` to cache allocated objects.

```kotlin
// ✅ OPTIMIZATION: List is allocated once and reused
val colors = remember { (0..500).map { Color(...) } }
```

---

## 4. Item Identity Crisis (Missing Keys)

### The Problem
Using `LazyColumn` without providing stable keys for items.

```kotlin
// ❌ ANTI-PATTERN: Lose item identity
items(items = emails) { email -> ... }
```

### The Fix
Provide a unique, stable key (usually from your data model) to allow Compose to track items across recompositions.

```kotlin
// ✅ OPTIMIZATION: Maintain item identity for efficient reuse
items(items = emails, key = { it.id }) { email -> ... }
```

---

## 5. Parameter Instability (The Silent Killer)

### The Problem
Passing "unstable" types like standard Kotlin `List` to a Composable. Compose cannot prove that a `List` won't be modified internally, so it marks the Composable as "Non-Skippable."

### The Fix
Wrap the data in a class marked with `@Immutable` or `@Stable`, or use Kotlinx Immutable Collections.

```kotlin
// ✅ OPTIMIZATION: Explicitly marking the state as Immutable
@Immutable
data class EmailListState(val emails: List<EmailItem>)
```

---

## Summary of Debugging Tools

1.  **Layout Inspector**: Use "Show Recomposition Counts" to find components with high update numbers.
2.  **Recomposition Highlighter**: Visually identify flashing components that update too often.
3.  **Android Studio Profiler**: Identify CPU-bound "Main Thread" blocking and memory allocation spikes.
4.  **Compose Compiler Metrics**: Generate reports to identify which Composables are "Skippable" vs "Restartable."

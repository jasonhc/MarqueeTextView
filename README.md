# MarqueeTextView

一个轻量、高效且支持高度自定义的 Android 跑马灯组件。解决系统原生 `TextView` 跑马灯速度固定不可调、首尾循环衔接生硬、缺乏精细控制等痛点。

---

## ✨ 核心特性

- **自定义速度与停顿**：支持以 `dp/second` 为单位自定义滚动速度，支持配置每轮滚动结束后的暂停时间（毫秒）。
- **动态更新自适应**：动态修改文本、速度或暂停时间时自动重新测量，文字未超出控件宽度时自动降级为静态展示，避免不必要的绘制开销。
- **生命周期安全**：
  - 自动感知窗口焦点变化（`onWindowFocusChanged`）：应用切后台或弹窗遮挡时自动停止动画，切回前台自动恢复，节省 CPU/GPU 资源。
  - 离开窗口（`onDetachedFromWindow`）自动销毁动画并移除回调，防止内存泄漏。
- **简单易用的启停控制**：沿用 Android 原生惯例，仅需通过 `isSelected = true / false` 即可控制跑马灯开始与停止。

---

## 📦 集成步骤

### 1. 拷贝代码文件
将 MarqueeTextView.kt 添加到你的 Android 工程中，并根据所在模块在文件开头添加包名。

### 2. 添加属性定义
把`attrs.xml`文件内容添加到工程的 `res/values/attrs.xml`（若没有可新建）中

---

## 🚀 使用指南

### 1. XML 布局中使用

```xml
<com.example.widget.MarqueeTextView
    android:id="@+id/marqueeTextView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingStart="12dp"
    android:paddingEnd="12dp"
    android:text="这是一条很长很长的跑马灯通知消息，超出屏幕后将自动平滑滚动播放！"
    android:textColor="#333333"
    android:textSize="16sp"
    app:marqueeSpeed="40"
    app:marqueeDelay="1500" />
```

### 2. 代码中控制启停与参数

#### 启动 / 停止滚动
```kotlin
val marqueeView = findViewById<MarqueeTextView>(R.id.marqueeTextView)

// 启动跑马灯
marqueeView.isSelected = true

// 停止跑马灯
marqueeView.isSelected = false
```

#### 动态调整速度与停顿时间
```kotlin
// 设置滚动速度为 50 dp/second（若当前正在滚动，会自动重启并应用新速度）
marqueeView.marqueeSpeed = 50

// 设置每轮滚动结束后的暂停间隔为 800 ms
marqueeView.marqueeDelay = 800
```

#### 动态更新文本内容
```kotlin
// 更新文字后，组件会自动重新计算文字宽度：
// - 若文字超出宽度且 isSelected == true，自动重启跑马灯
// - 若文字未超出宽度，自动保持静态居左展示
marqueeView.text = "新推送的公告内容..."
```

---

## ⚙️ 属性与 API 说明

### XML 属性说明

| 属性名 | 类型 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- |
| `app:marqueeSpeed` | `integer` | `30` | 滚动速度，单位为 `dp/second`。数值越大滚动越快（系统原生默认约 30dp/second）。 |
| `app:marqueeDelay` | `integer` | `1200` | 每轮滚动完成后的停顿时间，单位为 `ms`（系统原生默认约 1200ms）。 |

### 核心属性与方法

| 属性 / 方法 | 类型 / 返回值 | 说明 |
| :--- | :--- | :--- |
| `marqueeSpeed` | `Int` | 动态获取或设置跑马灯速度（dp/second）。赋值后若处于选中状态会立即生效并重启动画。 |
| `marqueeDelay` | `Int` | 动态获取或设置每轮结束停顿时长（ms）。赋值后若处于选中状态会立即生效。 |
| `isSelected` | `Boolean` | 控制跑马灯滚动的开关。设置为 `true` 开始滚动，`false` 停止滚动并复位。 |
| `setText(...)` | `Unit` | 重新设置文字内容，内部会自动重新测量并决定是否需要跑马灯。 |

---

## 💡 实现机制与优化

1. **精确的时间与位移计算**：
   动画时长由总滚动距离（文本宽度 + 间距 `gap`）除以物理位移速度（`marqueeSpeed * density`）计算得出，保证在不同分辨率与屏幕密度（DPI）设备上具有一致的视觉速度。
2. **双文本绘制无缝循环**：
   在单次动画周期内，当第一份文本滑出至右侧出现留白（`currentOffset > totalWidth - viewWidth`）时，第二份文本在偏移量 `paddingLeft - currentOffset + totalWidth` 处预先进入绘制区域，实现无缝连续衔接。
3. **智能边缘渐隐计算**：
   重写了 `getLeftFadingEdgeStrength()`，确保文字在刚开始滚动未位移时不出现左侧渐变遮罩；滚动开始后才渐隐淡出，避免初始状态下的视觉突兀感。

---

## ❓ 常见问题排查

1. **为什么设置了文字但不会滚动？**
   - 检查是否调用了 `view.isSelected = true`。组件通过 `isSelected` 作为滚动开关。
   - 检查文字长度是否**超出**了控件可视宽度。如果文字没有超出宽度，组件默认不触发跑马灯，保持正常文本展示。
   - 检查父容器宽度是否为 `wrap_content` 导致控件被无限拉宽，跑马灯通常需要在固定宽度或 `match_parent` 下才能判定文字超长。
2. **编译报 `R.styleable` 相关错误？**
   - 请检查 `attrs.xml` 中的 `<declare-styleable name="...">` 与类中的获取名称是否完全匹配。

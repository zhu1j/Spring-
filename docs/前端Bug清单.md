# 前端 Bug 清单

> 审查范围：`front/index.html`、`front/js/app.js`、`front/js/api.js`
> 日期：2026-07-05

---

## 🔴 致命（功能无法正常工作）

### Bug 1 — 模态框标题缺少 `id="modalTitle"`

**文件**：`index.html:156`、`app.js:209,220`

HTML 的 `<h5>` 没有 id，JS 里 `document.getElementById('modalTitle')` 返回 `null`：

```html
<!-- index.html:156 -->
<h5>                                    <!-- ← 缺少 id="modalTitle" -->
    <i class="bi bi-person-plus"></i> 新增客户
</h5>
```

```javascript
// app.js:209 → 报错 TypeError: Cannot set properties of null
document.getElementById('modalTitle').innerHTML = '...';

// app.js:220 → 同样报错
document.getElementById('modalTitle').innerHTML = '...';
```

**影响**：点"新增客户"按钮时 JS 直接报错，模态框标题不更新，后续逻辑可能中断。

---

### Bug 2 — 筛选区有两个重置按钮，缺少搜索按钮

**文件**：`index.html:68-78`

```html
<!-- 第一个 -->
<button onclick="resetFilter()">重置</button>

<!-- 第二个：复制粘贴忘改，又是重置 -->
<button onclick="resetFilter()">重置</button>
```

第二个按钮本来是"搜索"按钮，但复制粘贴后没改。导致用户修改筛选条件后无法触发查询。

**影响**：用户改了筛选条件后，只能点"重置"按钮才会触发一次 `searchCustomers(1)`，没有正常的搜索入口。

---

## 🟡 中等（功能正常但体验/效果异常）

### Bug 3 — serviceNeeds 文本框 class 写错

**文件**：`index.html:249`

```html
<!-- ❌ class="form-label" 是标签样式，不是输入框样式 -->
<textarea class="form-label" id="serviceNeeds" rows="3"></textarea>

<!-- ✅ 应为 -->
<textarea class="form-control" id="serviceNeeds" rows="3"></textarea>
```

**影响**：服务需求文本区域没有 Bootstrap 输入框边框，用户看不到这是一个可输入的文本框。

---

### Bug 4 — 保存按钮图标 class 拼写错误

**文件**：`index.html:263`

```html
<i class="bit-chek-lg"></i> 保存
```

`bit-chek-lg` 不是有效的 Bootstrap Icons class。应该是 `bi bi-check-lg`。

**影响**：按钮上的勾号图标不显示。

---

### Bug 5 — "跟进状态" label 属性写到了 class 外面

**文件**：`index.html:53`

```html
<!-- ❌ small 和 mb-1 在 class 外面，无效 -->
<label class="form-label" small mb-1>跟进状态</label>

<!-- ✅ 应该在 class 里面 -->
<label class="form-label small mb-1">跟进状态</label>
```

**影响**：跟进状态标签的字体和间距样式不生效，看起来比其他筛选标签大。

---

### Bug 6 — `d-none` 拼写错误

**文件**：`index.html:26`

```html
<span class="navbar-text small d-done d-md-inline">
```

`d-done` 应该是 `d-none`（`done` → `none`）。Bootstrap 的 `d-none` 表示隐藏，`d-md-inline` 表示中屏及以上显示。

**影响**：顶部导航栏文字在小屏幕上无法隐藏（比如手机端）。

---

### Bug 7 — renderTable 错误覆盖 totalCount

**文件**：`app.js:116`

```javascript
function renderTable(records) {
    const tbody = document.getElementById('customerTableBody');
    document.getElementById('totalCount').textContent = records.length;  // ← 这行多余
```

`records.length` 是当前页条数（如 10），不是总数。虽然在 `renderPagination` 中会用真正的 `total` 覆盖它，但中间有短暂瞬间显示的是错误数字。这行直接删掉即可。

**影响**：分页渲染前，右上角总数瞬间显示当前页条数而不是真正总数。

---

## 总结

| # | 严重度 | 位置 | 问题 |
|---|--------|------|------|
| 1 | 🔴 致命 | `index.html:156` | `<h5>` 缺少 `id="modalTitle"` |
| 2 | 🔴 致命 | `index.html:73-78` | 两个重置按钮，缺少搜索按钮 |
| 3 | 🟡 中等 | `index.html:249` | textarea `class="form-label"` 应为 `form-control` |
| 4 | 🟡 中等 | `index.html:263` | `bit-chek-lg` 应为 `bi bi-check-lg` |
| 5 | 🟡 中等 | `index.html:53` | `small mb-1` 写到 class 属性外面 |
| 6 | 🟡 中等 | `index.html:26` | `d-done` 应为 `d-none` |
| 7 | 🟢 轻微 | `app.js:116` | `totalCount` 被当前页条数覆盖一次 |

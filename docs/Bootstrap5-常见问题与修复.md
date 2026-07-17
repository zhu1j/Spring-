# Bootstrap 5 常见问题与修复

> 持续记录使用 Bootstrap 5 时遇到的样式问题及修复方案。

---

## 1. 模态框打开时页面向右晃动

### 现象

点击详情/新增/编辑按钮，弹窗打开/关闭的瞬间，页面内容会左右晃动一下。

### 原因

Bootstrap 打开模态框时自动做两件事：
1. `body { overflow: hidden }` — 隐藏滚动条
2. `body { padding-right: 17px }` — 补上滚动条消失后的宽度

但不同浏览器滚动条实际宽度不一样（12∼17px），补多了或少了两边宽度就对不齐，产生晃动。

### 修复

```css
html {
    overflow-y: scroll;       /* 滚动条始终占位，不管开不开弹窗 */
}
body {
    padding-right: 0 !important;  /* 禁用 Bootstrap 的补偿 */
}
```

---

## 2. sticky-top 导航栏在模态框打开时跳动

### 现象

页面翻到下方，点开详情弹窗，`sticky-top` 导航栏会往上/往左移一段距离。

### 原因

同上：模态框打开 → 滚动条消失 → 页面宽度变化 → `sticky-top` 导航栏重新计算位置 → 跳动。

### 修复

在上一项修复的基础上再加一句，阻止 Bootstrap 修改滚动条：

```css
html {
    overflow-y: scroll;
}
body {
    padding-right: 0 !important;
}
body.modal-open {
    overflow: visible !important;  /* 不让 Bootstrap 隐藏滚动条 */
}
```

### 原理

`overflow: visible` 覆盖 Bootstrap 的 `overflow: hidden`，模态框照常显示，滚动条还在，导航栏不动。

---

## 3. 详情页/滚动条相关问题统一修复模板

把这三条一起加到 `style.css` 最后：

```css
html {
    overflow-y: scroll;
}
body {
    padding-right: 0 !important;
}
body.modal-open {
    overflow: visible !important;
}
```

三条加完 → 弹窗丝滑、导航栏稳固、页面不晃。

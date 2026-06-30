# 跨境电商客户资料管理系统 — API 详细讲解文档

> **版本**：v1.0.0  
> **Base URL**：`http://localhost:8080`  
> **统一前缀**：`/api/customers`  
> **数据格式**：JSON（请求和响应均为 `application/json`）  
> **编码**：UTF-8

---

## 目录

1. [通用说明](#1-通用说明)
2. [接口索引](#2-接口索引)
3. [接口详解](#3-接口详解)
   - [3.1 新增客户](#31-新增客户)
   - [3.2 更新客户](#32-更新客户)
   - [3.3 删除客户](#33-删除客户)
   - [3.4 查询客户详情](#34-查询客户详情)
   - [3.5 分页查询客户列表](#35-分页查询客户列表)
   - [3.6 导出客户数据为Excel](#36-导出客户数据为excel)
   - [3.7 共享浏览客户列表](#37-共享浏览客户列表)
   - [3.8 获取客户类型枚举](#38-获取客户类型枚举)
4. [错误码说明](#4-错误码说明)
5. [前端调用示例](#5-前端调用示例)
6. [Postman 测试指南](#6-postman-测试指南)

---

## 1. 通用说明

### 1.1 统一响应格式

**所有接口**均返回以下 JSON 结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1719715200000
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 业务状态码。200=成功，400=客户端错误，500=服务端错误 |
| `message` | string | 提示信息，可直接用于前端 Toast 展示 |
| `data` | any | 响应数据，类型视接口而定（对象/数组/null） |
| `timestamp` | long | 响应时间戳（毫秒），便于问题排查 |

### 1.2 HTTP 状态码约定

| HTTP 码 | 含义 | 场景 |
|---------|------|------|
| 200 | OK | 请求成功 |
| 400 | Bad Request | 参数校验失败 / 业务异常 |
| 404 | Not Found | 资源不存在（通过 `Result.code` 返回） |
| 500 | Internal Server Error | 服务器未预期的异常 |

> 注：HTTP 状态码用于宏观分类，具体错误原因看 `Result.message`。

### 1.3 日期格式

所有日期时间字段使用格式：`yyyy-MM-dd HH:mm:ss`  
时区：`GMT+8`（北京时间）

### 1.4 分页请求格式

分页查询使用 POST 方式，JSON Body 传入：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "keyword": "搜索词",
  "customerType": "店铺购买",
  "status": "已签约",
  "country": "中国",
  "sortField": "created_at",
  "sortOrder": "desc"
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `pageNum` | int | 否 | 1 | 页码（从1开始） |
| `pageSize` | int | 否 | 10 | 每页条数（最大500） |
| `keyword` | string | 否 | — | 模糊搜索公司名和联系人 |
| `customerType` | string | 否 | — | 客户类型精确筛选 |
| `status` | string | 否 | — | 跟进状态精确筛选 |
| `country` | string | 否 | — | 国家精确筛选 |
| `sortField` | string | 否 | created_at | 排序字段 |
| `sortOrder` | string | 否 | desc | asc=升序, desc=降序 |

### 1.5 分页响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [ ... ],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 10
  },
  "timestamp": 1719715200000
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `records` | array | 当前页数据列表 |
| `total` | long | 符合条件的总记录数 |
| `pageNum` | long | 当前页码 |
| `pageSize` | long | 每页条数 |
| `pages` | long | 总页数 |

---

## 2. 接口索引

| # | 方法 | 路径 | 说明 | Content-Type |
|---|------|------|------|-------------|
| 1 | **POST** | `/api/customers` | 新增客户 | application/json |
| 2 | **PUT** | `/api/customers/{id}` | 更新客户信息 | application/json |
| 3 | **DELETE** | `/api/customers/{id}` | 删除客户（逻辑删除） | — |
| 4 | **GET** | `/api/customers/{id}` | 查询客户详情 | — |
| 5 | **POST** | `/api/customers/list` | 分页+条件查询 | application/json |
| 6 | **GET** | `/api/customers/export` | 导出Excel文件 | — |
| 7 | **POST** | `/api/customers/share` | 共享浏览（已签约客户） | application/json |
| 8 | **GET** | `/api/customers/types` | 获取客户类型列表 | — |

---

## 3. 接口详解

### 3.1 新增客户

> **POST** `/api/customers`

#### 请求体 (Request Body)

```json
{
  "companyName": "深圳跨境贸易有限公司",
  "contactPerson": "张三",
  "phone": "13800138001",
  "email": "zhangsan@example.com",
  "country": "中国",
  "address": "广东省深圳市南山区科技园路1号",
  "website": "https://szkt.example.com",
  "wechat": "zhangsan_wx",
  "customerType": "店铺购买",
  "serviceNeeds": "需要购买东南亚市场的店铺，月销目标$5000+",
  "source": "官网咨询",
  "status": "潜在客户",
  "remark": "客户比较着急，需要尽快安排",
  "createdBy": "李经理"
}
```

#### 字段约束

| 字段 | 类型 | 必填 | 最大长度 | 约束 |
|------|------|------|---------|------|
| `companyName` | string | **是** | 200 | 不能为空 |
| `contactPerson` | string | **是** | 100 | 不能为空 |
| `customerType` | string | **是** | 50 | 不能为空 |
| `phone` | string | 否 | 30 | — |
| `email` | string | 否 | 100 | 需符合邮箱格式 |
| `country` | string | 否 | 50 | — |
| `address` | string | 否 | 500 | — |
| `website` | string | 否 | 200 | — |
| `wechat` | string | 否 | 50 | — |
| `serviceNeeds` | string | 否 | 文本 | — |
| `source` | string | 否 | 100 | — |
| `status` | string | 否 | 20 | 不传则默认为"潜在客户" |
| `remark` | string | 否 | 文本 | — |
| `createdBy` | string | 否 | 100 | — |

> **注意**：`id`、`createdAt`、`updatedAt`、`isDeleted` 由系统自动填充，**请勿在请求体中传入**。

#### 成功响应 (200)

```json
{
  "code": 200,
  "message": "客户录入成功",
  "data": {
    "id": 9,
    "companyName": "深圳跨境贸易有限公司",
    "contactPerson": "张三",
    "phone": "13800138001",
    "email": "zhangsan@example.com",
    "country": "中国",
    "address": "广东省深圳市南山区科技园路1号",
    "website": "https://szkt.example.com",
    "wechat": "zhangsan_wx",
    "customerType": "店铺购买",
    "serviceNeeds": "需要购买东南亚市场的店铺，月销目标$5000+",
    "source": "官网咨询",
    "status": "潜在客户",
    "remark": "客户比较着急，需要尽快安排",
    "createdBy": "李经理",
    "createdAt": "2026-06-30 14:30:00",
    "updatedAt": "2026-06-30 14:30:00"
  },
  "timestamp": 1719715200000
}
```

#### 失败响应 — 参数校验失败 (400)

```json
{
  "code": 400,
  "message": "公司名称不能为空; 联系人不能为空; 客户类型不能为空",
  "data": null,
  "timestamp": 1719715200000
}
```

#### 前端调用示例

```javascript
const res = await API.create({
    companyName: '深圳跨境贸易有限公司',
    contactPerson: '张三',
    customerType: '店铺购买',
    phone: '13800138001',
    // ... 其他字段
});
// res.code === 200 → 新增成功
// res.code === 400 → 参数有误，提示 res.message
```

---

### 3.2 更新客户

> **PUT** `/api/customers/{id}`

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 客户ID（路径参数，不能为空） |

#### 请求体

同 [3.1 新增客户](#31-新增客户) 的请求体，**传入需要更新的字段即可**（全量更新，未传的字段会被置空）。

#### 成功响应 (200)

```json
{
  "code": 200,
  "message": "客户信息更新成功",
  "data": {
    "id": 9,
    "companyName": "深圳跨境贸易有限公司（已更名）",
    "contactPerson": "张三",
    "status": "意向明确",
    "updatedAt": "2026-06-30 16:00:00",
    "...": "..."
  },
  "timestamp": 1719720000000
}
```

#### 失败响应 — 客户不存在 (400)

```json
{
  "code": 400,
  "message": "客户不存在: id=999",
  "data": null,
  "timestamp": 1719720000000
}
```

#### 前端调用示例

```javascript
const res = await API.update(9, {
    companyName: '深圳跨境贸易有限公司（已更名）',
    contactPerson: '张三',
    customerType: '店铺购买',
    status: '意向明确',
});
```

---

### 3.3 删除客户

> **DELETE** `/api/customers/{id}`

#### 说明

此接口执行**逻辑删除**，数据库中的记录不会被物理清除，而是将 `is_deleted` 字段标记为 `1`。被删除的记录不会出现在任何查询结果中。

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 客户ID |

#### 成功响应 (200)

```json
{
  "code": 200,
  "message": "客户删除成功",
  "data": null,
  "timestamp": 1719720000000
}
```

#### 失败响应 — 客户不存在 (400)

```json
{
  "code": 400,
  "message": "客户不存在: id=999",
  "data": null,
  "timestamp": 1719720000000
}
```

#### 前端调用示例

```javascript
// 建议在前端先弹出确认对话框
if (confirm('确定要删除该客户吗？')) {
    const res = await API.delete(9);
    if (res.code === 200) {
        // 删除成功，刷新列表
    }
}
```

---

### 3.4 查询客户详情

> **GET** `/api/customers/{id}`

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 客户ID |

#### 成功响应 (200)

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "companyName": "深圳跨境贸易有限公司",
    "contactPerson": "张三",
    "phone": "13800138001",
    "email": "zhangsan@example.com",
    "country": "中国",
    "address": "广东省深圳市南山区科技园路1号",
    "website": "https://szkt.example.com",
    "wechat": "zhangsan_wx",
    "customerType": "店铺购买",
    "serviceNeeds": "需要购买东南亚市场的店铺，月销目标$5000+",
    "source": "官网咨询",
    "status": "已签约",
    "remark": "已签署购买协议，店铺已交付",
    "createdBy": "李经理",
    "createdAt": "2026-06-15 10:30:00",
    "updatedAt": "2026-06-28 09:15:00"
  },
  "timestamp": 1719715200000
}
```

#### 失败响应 — 客户不存在

```json
{
  "code": 404,
  "message": "客户不存在",
  "data": null,
  "timestamp": 1719715200000
}
```

#### 前端调用示例

```javascript
const res = await API.getById(1);
if (res.code === 200) {
    const customer = res.data;
    console.log(customer.companyName);  // "深圳跨境贸易有限公司"
}
```

---

### 3.5 分页查询客户列表

> **POST** `/api/customers/list`

#### 请求体

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "keyword": "深圳",
  "customerType": "店铺购买",
  "status": "",
  "country": "",
  "sortField": "created_at",
  "sortOrder": "desc"
}
```

#### 查询逻辑说明

| 筛选条件 | 匹配方式 | SQL 逻辑 |
|---------|---------|---------|
| `keyword` | 模糊匹配 | `WHERE company_name LIKE '%深圳%' OR contact_person LIKE '%深圳%'` |
| `customerType` | 精确匹配 | `AND customer_type = '店铺购买'` |
| `status` | 精确匹配 | `AND status = '已签约'` |
| `country` | 精确匹配 | `AND country = '中国'` |

多个条件同时传入时，为 **AND** 关系。

#### 支持的排序字段

| sortField 值 | 排序依据 |
|-------------|---------|
| `created_at` (默认) | 按创建时间排序 |
| `updated_at` | 按更新时间排序 |
| `company_name` | 按公司名排序 |
| `status` | 按状态排序 |
| `country` | 按国家排序 |

#### 成功响应 (200)

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "companyName": "深圳跨境贸易有限公司",
        "contactPerson": "张三",
        "phone": "13800138001",
        "email": "zhangsan@example.com",
        "country": "中国",
        "customerType": "店铺购买",
        "status": "已签约",
        "source": "官网咨询",
        "createdBy": "李经理",
        "createdAt": "2026-06-15 10:30:00",
        "updatedAt": "2026-06-28 09:15:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  },
  "timestamp": 1719715200000
}
```

#### 前端调用示例

```javascript
const res = await API.list({
    pageNum: 1,
    pageSize: 10,
    keyword: '深圳',
    customerType: '店铺购买',
});

if (res.code === 200) {
    const { records, total, pages } = res.data;
    renderTable(records);   // 渲染表格
    renderPagination(total, pages); // 渲染分页
}
```

---

### 3.6 导出客户数据为 Excel

> **GET** `/api/customers/export`

#### 请求参数（Query String）

| 参数 | 必填 | 说明 |
|------|------|------|
| `customerType` | 否 | 筛选客户类型 |
| `status` | 否 | 筛选跟进状态 |
| `country` | 否 | 筛选国家 |
| `keyword` | 否 | 关键词搜索 |

> 不传任何参数 = 导出全部客户数据。

#### 说明

此接口**不返回 JSON**，而是直接返回 `.xlsx` 文件流。浏览器识别响应头 `Content-Disposition: attachment` 后自动触发文件下载。

#### 响应头

```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename*=UTF-8''%E5%AE%A2%E6%88%B7%E8%B5%84%E6%96%99.xlsx
```

#### 前端调用方式

```javascript
// 方式一：直接打开下载链接
window.open('http://localhost:8080/api/customers/export?status=已签约', '_blank');

// 方式二：使用 API 封装
API.exportExcel({ status: '已签约', customerType: '店铺购买' });
```

#### Excel 文件格式说明

| 列 | 名称 | 来源字段 |
|----|------|---------|
| A | 序号 | 行号（从1开始） |
| B | 公司名称 | company_name |
| C | 联系人 | contact_person |
| D | 电话 | phone |
| E | 邮箱 | email |
| F | 国家/地区 | country |
| G | 客户类型 | customer_type |
| H | 跟进状态 | status |
| I | 客户来源 | source |
| J | 服务需求 | service_needs |
| K | 微信号 | wechat |
| L | 网站 | website |
| M | 地址 | address |
| N | 备注 | remark |
| O | 录入人 | created_by |
| P | 创建时间 | created_at |
| Q | 更新时间 | updated_at |

**样式特性**：
- 表头：加粗、白色字体、深蓝背景、居中对齐
- 数据行：正常字体、左对齐、自动换行
- 首行冻结（滚动时表头始终可见）
- 列宽已根据内容预设置

---

### 3.7 共享浏览客户列表

> **POST** `/api/customers/share`

#### 说明

此接口面向**外部合作方**，用于公开浏览已签约客户信息。与普通列表查询的区别：

| 对比维度 | `/list` | `/share` |
|---------|---------|----------|
| 可见范围 | 所有客户 | **仅"已签约"状态** |
| 用途 | 内部管理 | 外部共享浏览 |
| 安全性 | 返回全字段 | 返回全字段（前端选择性渲染） |

> **注意**：当前版本在后端返回全量字段，脱敏逻辑由前端 `share.html` 控制（仅展示公司名、联系人、国家、类型、签约时间）。后续版本可在后端增加脱敏处理。

#### 请求体

同 [3.5 分页查询](#35-分页查询客户列表)，不同之处是 **status 参数会被忽略**，接口强制筛选 `status = '已签约'`。

```json
{
  "pageNum": 1,
  "pageSize": 12,
  "keyword": ""
}
```

#### 成功响应 (200)

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "companyName": "深圳跨境贸易有限公司",
        "contactPerson": "张三",
        "country": "中国",
        "customerType": "店铺购买",
        "status": "已签约",
        "createdAt": "2026-06-15 10:30:00"
      }
    ],
    "total": 5,
    "pageNum": 1,
    "pageSize": 12
  },
  "timestamp": 1719715200000
}
```

#### 前端调用示例

```javascript
// share.html 中的调用
const res = await fetch('http://localhost:8080/api/customers/share', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ pageNum: 1, pageSize: 12, keyword: '' }),
});
const result = await res.json();
```

---

### 3.8 获取客户类型枚举

> **GET** `/api/customers/types`

#### 说明

返回系统中所有可用的客户类型。前端调用此接口动态填充下拉框，避免硬编码。

#### 请求参数

无

#### 成功响应 (200)

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    "店铺购买",
    "开店培训",
    "合作工厂",
    "物流合作",
    "其他"
  ],
  "timestamp": 1719715200000
}
```

#### 前端调用示例

```javascript
const res = await API.getTypes();
if (res.code === 200) {
    const types = res.data; // ["店铺购买", "开店培训", "合作工厂", "物流合作", "其他"]
    types.forEach(type => {
        // 动态生成 <option> 元素
    });
}
```

---

## 4. 错误码说明

### 4.1 业务错误码

| code | 说明 | 示例 message |
|------|------|-------------|
| 200 | 操作成功 | "操作成功" |
| 400 | 请求参数错误 | "公司名称不能为空" |
| 400 | 业务逻辑错误 | "客户不存在: id=999" |
| 404 | 资源不存在 | "客户不存在" |
| 500 | 服务器内部错误 | "服务器内部错误: ..." |

### 4.2 常见错误场景及处理

| 场景 | 返回 | 前端处理建议 |
|------|------|-------------|
| 必填字段为空 | code=400 "XX不能为空" | 展示 message 给用户 |
| 邮箱格式错误 | code=400 "邮箱格式不正确" | 展示 message，标红邮箱输入框 |
| 客户不存在 | code=400/404 "客户不存在" | Toast 提示，刷新列表 |
| 网络超时 | JS Error "请求超时" | 提示用户检查网络 |
| 后端未启动 | JS Error "无法连接服务器" | 提示用户检查后端状态 |
| 服务器异常 | code=500 | 提示"系统繁忙，请稍后重试" |

---

## 5. 前端调用示例

### 5.1 完整的新增客户流程

```javascript
// 1. 页面加载时获取客户类型
async function initPage() {
    const res = await API.getTypes();
    if (res.code === 200) {
        populateTypeDropdown(res.data);
    }
}

// 2. 用户填写表单并提交
async function handleSubmit() {
    // 收集表单数据
    const customerData = {
        companyName: document.getElementById('companyName').value.trim(),
        contactPerson: document.getElementById('contactPerson').value.trim(),
        customerType: document.getElementById('customerType').value,
        phone: document.getElementById('phone').value.trim(),
        email: document.getElementById('email').value.trim(),
        // ... 其他字段
    };

    // 前端校验
    if (!customerData.companyName) {
        showToast('请输入公司名称', 'warning');
        return;
    }

    // 调用后端
    try {
        const res = await API.create(customerData);
        if (res.code === 200) {
            showToast('客户录入成功', 'success');
            closeModal();
            refreshList();
        } else {
            showToast(res.message, 'danger');
        }
    } catch (error) {
        showToast(error.message, 'danger');
    }
}
```

### 5.2 完整的列表查询 + 导出流程

```javascript
// 查询列表
async function searchCustomers() {
    const query = {
        pageNum: 1,
        pageSize: 10,
        keyword: document.getElementById('searchInput').value,
        customerType: document.getElementById('typeFilter').value,
        status: document.getElementById('statusFilter').value,
    };

    const res = await API.list(query);
    if (res.code === 200) {
        renderTable(res.data.records);
        renderPagination(res.data.total, res.data.pages);
    }
}

// 导出Excel（基于当前筛选条件）
function exportCurrent() {
    const filters = {
        customerType: document.getElementById('typeFilter').value,
        status: document.getElementById('statusFilter').value,
        keyword: document.getElementById('searchInput').value,
    };
    API.exportExcel(filters);
    // 浏览器自动下载文件
}
```

---

## 6. Postman 测试指南

### 6.1 环境配置

在 Postman 中创建环境变量：

| 变量名 | 值 |
|--------|-----|
| `baseUrl` | `http://localhost:8080` |

### 6.2 测试用例

#### TC01 — 新增客户

```
Method:  POST
URL:     {{baseUrl}}/api/customers
Headers: Content-Type: application/json
Body (raw JSON):
{
  "companyName": "测试公司",
  "contactPerson": "测试员",
  "customerType": "店铺购买",
  "phone": "13800138000"
}

期望结果: code=200, data.id 不为空
```

#### TC02 — 新增客户（校验失败）

```
Body (raw JSON):
{
  "companyName": "",
  "contactPerson": "",
  "customerType": ""
}

期望结果: code=400, message 包含校验失败信息
```

#### TC03 — 分页查询

```
Method:  POST
URL:     {{baseUrl}}/api/customers/list
Body (raw JSON):
{
  "pageNum": 1,
  "pageSize": 5,
  "keyword": "深圳"
}

期望结果: code=200, data.records 中公司名包含"深圳"
```

#### TC04 — 导出Excel

```
Method:  GET
URL:     {{baseUrl}}/api/customers/export?status=已签约

期望结果: 返回 .xlsx 文件（Postman 显示为二进制）
```

#### TC05 — 共享浏览

```
Method:  POST
URL:     {{baseUrl}}/api/customers/share
Body (raw JSON):
{
  "pageNum": 1,
  "pageSize": 10
}

期望结果: code=200, data.records 中所有记录 status 均为"已签约"
```

#### TC06 — 获取客户类型

```
Method:  GET
URL:     {{baseUrl}}/api/customers/types

期望结果: code=200, data = ["店铺购买", "开店培训", "合作工厂", "物流合作", "其他"]
```

---

## 附录 A：完整字段对照表

| JSON 字段 (驼峰) | 数据库字段 (蛇形) | 类型 | 必填 |
|-----------------|-------------------|------|------|
| `companyName` | `company_name` | VARCHAR(200) | ✅ |
| `contactPerson` | `contact_person` | VARCHAR(100) | ✅ |
| `customerType` | `customer_type` | VARCHAR(50) | ✅ |
| `phone` | `phone` | VARCHAR(30) | ❌ |
| `email` | `email` | VARCHAR(100) | ❌ |
| `country` | `country` | VARCHAR(50) | ❌ |
| `address` | `address` | VARCHAR(500) | ❌ |
| `website` | `website` | VARCHAR(200) | ❌ |
| `wechat` | `wechat` | VARCHAR(50) | ❌ |
| `serviceNeeds` | `service_needs` | TEXT | ❌ |
| `source` | `source` | VARCHAR(100) | ❌ |
| `status` | `status` | VARCHAR(20) | ❌ (默认"潜在客户") |
| `remark` | `remark` | TEXT | ❌ |
| `createdBy` | `created_by` | VARCHAR(100) | ❌ |
| `createdAt` | `created_at` | DATETIME | 系统自动 |
| `updatedAt` | `updated_at` | DATETIME | 系统自动 |

## 附录 B：MyBatis-Plus 自动映射规则

Java 实体使用**驼峰命名**（如 `companyName`），数据库使用**蛇形命名**（如 `company_name`）。MyBatis-Plus 配置 `map-underscore-to-camel-case: true` 后自动完成转换，开发者无需手动写映射。

## 附录 C：快速问题排查

| 现象 | 可能原因 | 解决方法 |
|------|---------|---------|
| 前端请求一直 pending | 后端未启动 | 启动 Spring Boot 应用 |
| 前端报 "无法连接服务器" | CORS 未配置或端口错误 | 检查 `CorsConfig` 和 `BASE_URL` |
| 导出无反应 | 浏览器拦截弹窗 | 检查弹窗拦截设置 |
| 列表数据不更新 | 查询条件未正确传递 | 打开浏览器 DevTools → Network 检查请求 |
| 中文乱码 | 数据库字符集问题 | 确认数据库使用 `utf8mb4` |

---

> **文档状态**：已发布  
> **最后更新**：2026-06-30  
> **维护人**：CRM 开发团队  
> **反馈方式**：提交 Issue 或联系开发团队

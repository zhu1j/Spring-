# 客户资料管理系统 API 文档

> **Base URL**: `http://localhost:8080`  
> **Content-Type**: `application/json` (所有 POST/PUT 请求)  
> **字符编码**: UTF-8  
> **版本**: v1.0.0 | **更新日期**: 2026-06-30

---

## 通用响应格式

所有接口统一返回以下 JSON 结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { },
  "timestamp": 1719748800000
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | HTTP 状态码，200 表示成功，400 业务异常，500 服务器错误 |
| message | string | 提示信息 |
| data | any | 响应数据（成功时返回，失败时为 null） |
| timestamp | long | 响应时间戳（毫秒） |

---

## 一、客户管理 API

### 1.1 新增客户

> **POST** `/api/customers`

**请求体 (JSON)**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| companyName | string | ✅ | 公司/店铺名称 |
| contactPerson | string | ✅ | 联系人姓名 |
| customerType | string | ✅ | 客户类型（见 [1.8 枚举值](#18-获取客户类型枚举)） |
| phone | string | | 联系电话 |
| email | string | | 邮箱地址（格式校验） |
| country | string | | 国家/地区 |
| address | string | | 详细地址 |
| website | string | | 公司/店铺网址 |
| wechat | string | | 微信号 |
| serviceNeeds | string | | 服务需求描述 |
| source | string | | 客户来源渠道 |
| status | string | | 跟进状态，默认"潜在客户" |
| remark | string | | 备注 |
| createdBy | string | | 录入人 |

**请求示例**：
```json
{
  "companyName": "深圳环球贸易有限公司",
  "contactPerson": "张三",
  "phone": "13800138001",
  "email": "zhangsan@example.com",
  "country": "中国",
  "customerType": "合作工厂",
  "source": "展会",
  "status": "潜在客户",
  "createdBy": "admin"
}
```

**成功响应 (200)**：
```json
{
  "code": 200,
  "message": "客户录入成功",
  "data": {
    "id": 1,
    "companyName": "深圳环球贸易有限公司",
    "contactPerson": "张三",
    "phone": "13800138001",
    "createdAt": "2026-06-30 20:00:00",
    "updatedAt": "2026-06-30 20:00:00"
  },
  "timestamp": 1719748800000
}
```

**失败响应 (400)**：
```json
{
  "code": 400,
  "message": "公司名称不能为空; 联系人不能为空",
  "data": null,
  "timestamp": 1719748800000
}
```

---

### 1.2 根据 ID 查询客户详情

> **GET** `/api/customers/{id}`

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 客户 ID（必须 > 0） |

**成功响应 (200)**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "companyName": "深圳环球贸易有限公司",
    "contactPerson": "张三",
    "phone": "13800138001",
    "email": "zhangsan@example.com",
    "country": "中国",
    "customerType": "合作工厂",
    "status": "已签约",
    "source": "展会",
    "createdBy": "admin",
    "createdAt": "2026-06-30 20:00:00",
    "updatedAt": "2026-06-30 20:00:00"
  },
  "timestamp": 1719748800000
}
```

**失败响应 (404)**：
```json
{
  "code": 404,
  "message": "客户不存在",
  "data": null,
  "timestamp": 1719748800000
}
```

---

### 1.3 分页条件查询客户列表

> **POST** `/api/customers/list`

**请求体 (JSON)**：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| pageNum | int | 1 | 当前页码（从 1 开始） |
| pageSize | int | 10 | 每页条数（最大 500） |
| customerType | string | | 客户类型精确匹配 |
| status | string | | 跟进状态精确匹配 |
| country | string | | 国家/地区精确匹配 |
| keyword | string | | 关键词模糊搜索（匹配公司名或联系人） |
| sortField | string | created_at | 排序字段：company_name / status / country / created_at / updated_at |
| sortOrder | string | desc | 排序方向：asc / desc |

**请求示例**：
```json
{
  "pageNum": 1,
  "pageSize": 10,
  "status": "已签约",
  "keyword": "深圳",
  "sortField": "created_at",
  "sortOrder": "desc"
}
```

**成功响应 (200)**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [ /* Customer 数组 */ ],
    "total": 50,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 5
  },
  "timestamp": 1719748800000
}
```

---

### 1.4 更新客户信息

> **PUT** `/api/customers/{id}`

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 客户 ID |

**请求体 (JSON)**：同 [1.1 新增客户](#11-新增客户)，所有字段选填（只传需要更新的字段）。

**请求示例**：
```json
{
  "phone": "13900139000",
  "status": "意向明确",
  "remark": "客户对报价感兴趣"
}
```

**成功响应 (200)**：
```json
{
  "code": 200,
  "message": "客户信息更新成功",
  "data": { /* 更新后的完整 Customer 对象 */ },
  "timestamp": 1719748800000
}
```

---

### 1.5 删除客户（逻辑删除）

> **DELETE** `/api/customers/{id}`

删除操作为**逻辑删除**，数据库记录仍保留但标记 `is_deleted=1`，之后查询接口不再返回该记录。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 客户 ID |

**成功响应 (200)**：
```json
{
  "code": 200,
  "message": "客户删除成功",
  "data": null,
  "timestamp": 1719748800000
}
```

---

### 1.6 导出客户数据为 Excel

> **GET** `/api/customers/export`

浏览器直接访问即可下载 `.xlsx` 文件。支持可选查询参数进行筛选导出。

**查询参数（均为可选）**：

| 参数 | 类型 | 说明 |
|------|------|------|
| customerType | string | 客户类型筛选 |
| status | string | 状态筛选 |
| country | string | 国家筛选 |
| keyword | string | 关键词搜索 |

**示例**：
```
GET /api/customers/export?status=已签约&country=中国
```

响应为 Excel 文件流，浏览器自动触发下载。导出列包括：序号、公司名称、联系人、电话、邮箱、国家/地区、客户类型、跟进状态、来源、服务需求、微信号、网站、地址、备注、录入人、创建时间、更新时间。

---

### 1.7 共享浏览（只读脱敏）

> **POST** `/api/customers/share`

仅供外部团队成员通过链接查看已签约客户。参数同 [1.3 分页查询](#13-分页条件查询客户列表)，但固定只返回 `status=已签约` 的客户。

> ⚠️ **安全提示**：共享接口返回完整 Customer 对象，前端需要对手机号、邮箱、微信号等敏感字段做脱敏处理后再展示。

---

### 1.8 获取客户类型枚举

> **GET** `/api/customers/types`

**成功响应 (200)**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": ["店铺购买", "开店培训", "合作工厂", "物流合作", "其他"],
  "timestamp": 1719748800000
}
```

---

## 二、枚举值参考

### 2.1 客户类型 (customerType)

| 值 | 说明 |
|------|------|
| 店铺购买 | 跨境电商店铺采购客户 |
| 开店培训 | 需要开店培训服务的客户 |
| 合作工厂 | 供应链合作工厂 |
| 物流合作 | 物流合作伙伴 |
| 其他 | 其他类型客户 |

### 2.2 跟进状态 (status)

| 值 | 说明 |
|------|------|
| 潜在客户 | 待开发的潜在客户 |
| 初步接洽 | 已进行初次沟通 |
| 意向明确 | 客户表达了明确合作意向 |
| 已签约 | 已完成签约 |
| 已流失 | 客户已流失 |

---

## 三、错误码说明

| code | 说明 | 常见原因 |
|------|------|----------|
| 200 | 成功 | — |
| 400 | 请求参数错误 | 必填字段为空、格式校验失败、业务规则不满足 |
| 404 | 资源不存在 | 客户 ID 不存在 |
| 500 | 服务器内部错误 | 数据库连接失败、系统异常 |

---

## 四、注意事项

1. **删除为逻辑删除**：删除的客户数据可通过数据库直接恢复（将 `is_deleted` 改回 0）。
2. **分页上限**：单页最大 500 条，防止恶意请求拖垮数据库。
3. **排序字段白名单**：仅支持 `company_name`、`status`、`country`、`created_at`、`updated_at` 五个字段排序，防止 SQL 注入。
4. **日期格式**：所有日期字段统一使用 `yyyy-MM-dd HH:mm:ss` 格式，时区为 GMT+8。
5. **CORS**：开发环境允许所有来源跨域访问，生产环境需限制为具体域名。

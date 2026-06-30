# Lombok @Builder 类型不匹配：cannot be applied to '(int, String, ...)'

## 问题

编译报错：

```
'Customer(Long, String, String, ...)' cannot be applied to '(int, String, String, ...)'
```

## 原因

`@Builder` 底层调用全参构造器，所有字段方法都是**强类型**的，传入的类型必须和实体类声明的一致。

| 字段类型 | 错误写法 | 正确写法 |
|---------|---------|---------|
| `Long id` | `.id(1)` → `int` | `.id(1L)` → `long`/`Long` |
| `Integer count` | `.count(1L)` → `long` | `.count(1)` → `int`/`Integer` |
| `LocalDateTime time` | `.time("2024-01-01")` → `String` | `.time(LocalDateTime.now())` |

## 解决

1. **主键自增字段不设值**（推荐）
2. **数字加类型后缀**：`Long` → `1L`，`Float` → `1.0F`，`Double` → `1.0D`

```java
// ❌ 错误
Customer.builder().id(1).build();

// ✅ 正确：不设 id，数据库自增
Customer.builder().companyName("测试").build();

// ✅ 正确：加 L 后缀
Customer.builder().id(1L).build();
```

## 规律

> Lombok Builder 不讲类型兼容，只看类型**一模一样**。`int` 不会自动转 `Long`，`String` 不会自动转 `LocalDateTime`。

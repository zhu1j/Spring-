# JUnit 5 测试方法规范与常见错误

## 核心模式：AAA 三段式

每个测试方法都遵循同一个结构：

```
准备数据（Arrange）→ 执行方法（Act）→ 断言结果（Assert）
```

```java
@Test
void shouldCreateCustomer() {
    // 1. Arrange：准备测试数据
    Customer customer = Customer.builder()
            .companyName("测试公司")
            .contactPerson("张三")
            .phone("13800138000")
            .customerType("店铺购买")
            .build();

    // 2. Act：执行被测试的方法
    Customer result = customerService.create(customer);

    // 3. Assert：断言结果是否符合预期
    assertThat(result.getId()).isNotNull();
    assertThat(result.getStatus()).isEqualTo("潜在客户");
}
```

---

## 常见错误清单

### 1. 测试方法返回类型错误

```java
// ❌ 错误：JUnit 5 @Test 方法必须是 void
@Test
public int testUpdate() { ... }

// ✅ 正确
@Test
public void testUpdate() { ... }
```

**规律**：`@Test` 方法必须 `public void`，JUnit 不会执行有返回值的方法。

### 2. 用 System.out.println 代替断言

```java
// ❌ 错误：打印结果靠人眼看，测试没有自动化价值
Customer result = customerService.create(customer);
System.out.println(result);

// ✅ 正确：用断言自动验证
Customer result = customerService.create(customer);
assertThat(result.getId()).isNotNull();
assertThat(result.getCompanyName()).isEqualTo("测试公司");
```

**规律**：没有断言的测试 = 没有测试。跑过了不代表跑对了。

### 3. 在测试里重写业务逻辑

```java
// ❌ 错误：测试不该自己判断"客户是否存在"
Customer existing = customerService.getById(id);
if (existing == null) {
    throw new RuntimeException("客户不存在");
}

// ✅ 正确：让被测试的方法自己处理，测试只管断言结果
assertThatThrownBy(() -> customerService.update(99999L, data))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("客户不存在");
```

**规律**：测试只管**调方法 + 验结果**，业务判断逻辑是被测代码的职责。

### 4. throw 后写代码（不可达代码）

```java
// ❌ 错误：throw 后面的代码永远不会执行
if (existing == null) {
    throw new RuntimeException("...");
    return 0;  // ← 不可达
}

// ✅ 正确：throw 就结束了，不需要 return
if (existing == null) {
    throw new RuntimeException("...");
}
```

### 5. update 测试没有真正修改数据

```java
// ❌ 错误：查出来原样写回去，什么都没测到
Customer customer = customerService.getById(id);
customerService.update(id, customer);

// ✅ 正确：查出来 → 改 → 更新 → 验证改成功了
Customer updateData = Customer.builder()
        .companyName("新名字").build();
Customer result = customerService.update(id, updateData);
assertThat(result.getCompanyName()).isEqualTo("新名字");
```

---

## AssertJ 常用断言速查

```java
import static org.assertj.core.api.Assertions.*;

// 判空/非空
assertThat(obj).isNull();
assertThat(obj).isNotNull();

// 值相等
assertThat(str).isEqualTo("预期值");
assertThat(num).isGreaterThan(0);

// 字符串包含
assertThat(str).contains("关键词");
assertThat(str).startsWith("前缀");

// 异常断言
assertThatThrownBy(() -> someMethod()).isInstanceOf(RuntimeException.class);

// 集合断言
assertThat(list).hasSize(3);
assertThat(list).contains("元素");
assertThat(list).containsExactly("A", "B", "C");  // 顺序严格匹配
```

---

## 规律总结

| 错误 | 后果 | 正确做法 |
|------|------|---------|
| 返回类型非 void | 该方法不会被 JUnit 执行 | `public void` |
| 没有断言 | 测试永远是绿的，BUG 检测不到 | `assertThat()` |
| 测试里写业务逻辑 | 测的不是被测代码，是测试自己的代码 | 只调方法 + 断言 |
| throw 后写 return | 不可达代码，无意义 | 去掉 return |
| 数据没变化就 update | 测不出 update 是否真的生效 | 修改字段再更新 |

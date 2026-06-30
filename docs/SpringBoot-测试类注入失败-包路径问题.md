# SpringBoot 测试类注入失败：No beans of 'XXX' type found

## 问题

测试类写好后，`@Autowired` 报红：

```
Could not autowire. No beans of 'CustomerService' type found.
```

## 原因

测试类放在了 **默认包**（`src/test/java/` 根目录下），没有 `package` 声明。

Spring Boot 的 `@SpringBootTest` 会从测试类所在包向上查找 `@SpringBootApplication` 启动类。默认包无法向上查找，Spring 容器根本没启动，自然扫描不到任何 Bean。

```
❌ src/test/java/CustomerServiceTest.java          → 无 package，找不到启动类
✅ src/test/java/com/crm/service/CustomerServiceTest.java → package com.crm.service，能找到
```

## 解决

1. **把测试文件搬到和主代码相同的包路径下**
2. **添加对应的 `package` 声明**

| main 代码路径 | test 代码路径 |
|--------------|--------------|
| `src/main/java/com/crm/service/CustomerService.java` | `src/test/java/com/crm/service/CustomerServiceTest.java` |

## 规律

> 测试类的文件夹路径必须和 `main` 里一致，`package` 声明也要一致。只要测试类放在启动类包路径（`com.crm`）或它的子包下就行。

```
✅ package com.crm.service;
✅ package com.crm;
❌ 默认包（无 package 声明）
```

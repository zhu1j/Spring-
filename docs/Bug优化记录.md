# Bug 优化记录

> 项目：跨境电商客户资料管理系统（Spring Boot 版）  
> 日期：2026-06-30  
> 修复人：Claude Code AI Assistant

---

## Bug #1 🔴 严重：数据库连接失败 — 不支持的字符编码 utf8mb4

### 错误现象

```
java.io.UnsupportedEncodingException: utf8mb4
    at java.base/java.lang.String.lookupCharset(String.java:829)
    at java.base/java.lang.String.getBytes(String.java:1765)
    at com.mysql.cj.util.StringUtils.getBytes(StringUtils.java:234)
```

系统启动后所有数据库查询均失败，抛出 `UnsupportedEncodingException`，系统完全不可用。

### 错误原因

`application.yml` 中 JDBC 连接 URL 使用了 `characterEncoding=utf8mb4`：

```yaml
# ❌ 错误配置
url: jdbc:mysql://localhost:3306/crm_db?useUnicode=true&characterEncoding=utf8mb4&...
```

- `utf8mb4` 是 **MySQL 内部的字符集名称**，用于支持完整的 UTF-8（含 4 字节 emoji）。
- **Java 标准库只识别 `UTF-8`** 作为字符编码名称。
- MySQL JDBC 驱动 (`mysql-connector-j`) 在建立连接时调用 `String.getBytes("utf8mb4")` 来初始化连接属性，该方法调用了 `String.lookupCharset("utf8mb4")`，Java 无法找到名为 `utf8mb4` 的 charset，抛出 `UnsupportedEncodingException`。

### 修复方案

将 JDBC URL 中的 `characterEncoding=utf8mb4` 改为 `characterEncoding=UTF-8`：

```yaml
# ✅ 正确配置
url: jdbc:mysql://localhost:3306/crm_db?useUnicode=true&characterEncoding=UTF-8&...
```

**技术说明**：`characterEncoding=UTF-8` 设置的是 JDBC 驱动与 Java 之间的数据传输编码。MySQL 服务器端的字符集由数据库/表的 `DEFAULT CHARSET utf8mb4` 配置决定，二者互不影响。`UTF-8` 是 Java 标准字符集，能正确处理所有 Unicode 字符。

### 影响范围

- 文件：`backend/src/main/resources/application.yml` 第 16 行
- 影响：P0 级阻断性 Bug，导致系统完全无法启动

---

## Bug #2 🟡 中等：无用 import 污染代码

### 错误现象

`CustomerService.java` 接口文件导入了 `org.springframework.web.multipart.MultipartFile`，但该接口中没有任何方法使用此类型。

### 错误原因

开发阶段可能规划了文件上传功能但未实现，`import` 语句残留。

### 修复方案

删除未使用的 `import`：

```java
// ❌ 删除
import org.springframework.web.multipart.MultipartFile;
```

### 影响范围

- 文件：`backend/src/main/java/com/crm/service/CustomerService.java` 第 8 行
- 影响：P2 级，不影响运行，但违反代码规范（SonarLint: Remove this unused import）

---

## Bug #3 🟡 中等：全局异常处理器将业务异常按系统错误记录

### 错误现象

`GlobalExceptionHandler.java` 中对 `RuntimeException`（业务异常）使用了 `log.error()` 级别记录日志并打印完整堆栈：

```java
// ❌ 业务异常按 ERROR 级别 + 完整堆栈记录
@ExceptionHandler(RuntimeException.class)
public Result<Void> handleRuntimeException(RuntimeException e) {
    log.error("业务异常: {}", e.getMessage(), e);  // 打印堆栈
    return Result.fail(e.getMessage());              // 缺少 code 参数
}
```

这会导致：
1. 用户输入不存在的 ID 时，日志文件被完整堆栈污染
2. 运营/运维人员无法区分"真正的系统故障"和"正常业务拦截"
3. 告警系统可能因大量"ERROR"日志误触发

### 修复方案

1. 将日志级别从 `ERROR` 降为 `WARN`
2. 移除堆栈打印 `e` 参数
3. 补充 `code` 参数使错误码精确

```java
// ✅ 业务异常按 WARN 级别，仅记录消息
@ExceptionHandler(RuntimeException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public Result<Void> handleRuntimeException(RuntimeException e) {
    log.warn("业务异常: {}", e.getMessage());   // 仅消息，无堆栈
    return Result.fail(400, e.getMessage());       // 明确返回 400
}
```

### 影响范围

- 文件：`backend/src/main/java/com/crm/config/GlobalExceptionHandler.java` 第 74-79 行
- 影响：P2 级，系统可运行，但影响运维可观测性和日志质量

---

## Bug #4 🟢 建议：缺少可独立运行的自动化测试

### 错误现象

项目 `src/test` 目录为空，没有任何测试用例。项目依赖外部 MySQL 数据库，无法在 CI/CD 环境或本地无 MySQL 时运行测试。

### 修复方案

1. **添加 H2 内存数据库依赖**（仅 test scope），MySQL 兼容模式，无需外部数据库即可运行测试
2. **创建测试配置** `src/test/resources/application.yml`，自动替换生产数据源为 H2
3. **编写初始化脚本** `src/test/resources/schema-h2.sql`，包含建表语句和 5 条种子数据
4. **编写 15 个测试用例**，覆盖所有核心功能：
   - 全量查询、按 ID 查询、查询不存在的客户
   - 新增客户（含默认状态设置）
   - 按跟进状态筛选、按国家筛选、关键词模糊搜索
   - 更新客户信息、更新不存在的客户
   - 分页查询
   - 客户类型枚举查询
   - 共享浏览（仅已签约客户）
   - 逻辑删除、删除不存在的客户
   - 删除后数据一致性验证
5. **修复 Lombok 版本兼容性**：原项目的 Lombok 版本不支持 Java 25，升级至 `1.18.42`
6. **配置 Maven Compiler Plugin** 的 annotation processor paths，确保 Lombok 在 Maven 命令行编译时正确处理

**运行方式**：
```bash
cd backend
mvn test
```

### 测试结果

```
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 影响范围

- 新增文件：
  - `backend/src/test/java/com/crm/CrmApplicationTests.java`
  - `backend/src/test/resources/application.yml`
  - `backend/src/test/resources/schema-h2.sql`
- 修改文件：
  - `backend/pom.xml`（添加 H2 依赖、Lombok 版本、Maven Compiler Plugin 配置）

---

## 修复总结

| 编号 | 级别 | 问题 | 状态 |
|------|------|------|------|
| #1 | 🔴 P0 阻断 | `characterEncoding=utf8mb4` 导致数据库连接失败 | ✅ 已修复 |
| #2 | 🟡 P2 规范 | 未使用的 import 污染代码 | ✅ 已修复 |
| #3 | 🟡 P2 运维 | 业务异常按 ERROR 级别记录，影响日志质量 | ✅ 已修复 |
| #4 | 🟢 P3 工程化 | 缺少自动化测试，依赖外部数据库 | ✅ 已完成 |

### 开发建议

1. **编码规范**：JDBC URL 中的 `characterEncoding` 必须使用 Java 标准字符集名称（`UTF-8`），不能使用 MySQL 特定的 `utf8`/`utf8mb4`
2. **日志分层**：业务异常使用 `WARN`，系统故障使用 `ERROR`，方便运维配置告警阈值
3. **测试先行**：使用 H2 内存数据库实现 DAO/Service 层测试解耦，CI/CD 管道无需 MySQL 环境
4. **依赖管理**：Lombok 等编译期工具需关注 JDK 版本兼容性，建议在 `properties` 中显式锁定版本

package com.crm;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.dto.CustomerQueryDTO;
import com.crm.entity.Customer;
import com.crm.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户资料管理系统 — 核心功能测试
 * <p>
 * 使用 H2 内存数据库（MySQL 兼容模式），无需外部 MySQL 即可运行。
 * 按顺序执行 CRUD + 分页查询 + 条件筛选 + 导出测试。
 * <p>
 * 运行方式：
 * <pre>mvn test</pre>
 * 或在 IDE 中直接运行本类。
 *
 * @author CRM Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("客户管理系统核心功能测试")
class CrmApplicationTests {

    @Autowired
    private CustomerService customerService;

    // ==================== 基础查询测试 ====================

    @Test
    @Order(1)
    @DisplayName("查询所有客户 — 应返回初始化数据")
    void testListAll() {
        CustomerQueryDTO dto = new CustomerQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(20);

        Page<Customer> page = customerService.listByCondition(dto);

        assertNotNull(page, "分页结果不应为 null");
        assertTrue(page.getTotal() >= 5, "初始化后应至少有 5 条数据");
        assertEquals(1, page.getCurrent());
        System.out.printf("✓ 总记录数: %d, 当前页: %d/%d%n",
                page.getTotal(), page.getCurrent(), page.getPages());
    }

    @Test
    @Order(2)
    @DisplayName("根据 ID 查询客户详情")
    void testGetById() {
        // ID=1 是初始化数据中的第一条
        Customer customer = customerService.getById(1L);
        assertNotNull(customer, "ID=1 的客户应存在");
        assertEquals("深圳环球贸易有限公司", customer.getCompanyName());
        assertEquals("张三", customer.getContactPerson());
        System.out.printf("✓ 客户详情: %s / %s / %s%n",
                customer.getCompanyName(), customer.getContactPerson(), customer.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("查询不存在的客户 — 应返回 null")
    void testGetByIdNotFound() {
        Customer customer = customerService.getById(99999L);
        assertNull(customer, "不存在的 ID 应返回 null");
        System.out.println("✓ 查询不存在客户正确返回 null");
    }

    // ==================== 新增测试 ====================

    @Test
    @Order(4)
    @DisplayName("新增客户")
    void testCreate() {
        Customer customer = Customer.builder()
                .companyName("测试科技有限公司")
                .contactPerson("李四")
                .phone("13600136000")
                .email("lisi@test-tech.cn")
                .country("中国")
                .customerType("其他")
                .source("单元测试")
                .status("潜在客户")
                .createdBy("tester")
                .build();

        Customer saved = customerService.create(customer);

        assertNotNull(saved, "保存结果不应为 null");
        assertNotNull(saved.getId(), "ID 应由数据库自动生成");
        assertEquals("测试科技有限公司", saved.getCompanyName());
        assertEquals("潜在客户", saved.getStatus()); // 默认状态
        System.out.printf("✓ 新增客户成功: id=%d, company=%s%n",
                saved.getId(), saved.getCompanyName());
    }

    // ==================== 条件查询测试 ====================

    @Test
    @Order(5)
    @DisplayName("按跟进状态筛选")
    void testFilterByStatus() {
        CustomerQueryDTO dto = new CustomerQueryDTO();
        dto.setStatus("已签约");
        dto.setPageNum(1);
        dto.setPageSize(10);

        Page<Customer> page = customerService.listByCondition(dto);

        assertNotNull(page);
        assertTrue(page.getTotal() > 0, "应至少有一条'已签约'客户");
        page.getRecords().forEach(c ->
                assertEquals("已签约", c.getStatus(), "筛选结果的跟进状态应为'已签约'")
        );
        System.out.printf("✓ 筛选'已签约'客户: 共 %d 条%n", page.getTotal());
    }

    @Test
    @Order(6)
    @DisplayName("按关键词模糊搜索")
    void testSearchByKeyword() {
        CustomerQueryDTO dto = new CustomerQueryDTO();
        dto.setKeyword("深圳");
        dto.setPageNum(1);
        dto.setPageSize(10);

        Page<Customer> page = customerService.listByCondition(dto);

        assertNotNull(page);
        assertTrue(page.getTotal() > 0, "搜索'深圳'应有结果");
        page.getRecords().forEach(c ->
                assertTrue(
                        c.getCompanyName().contains("深圳") || c.getContactPerson().contains("深圳"),
                        "搜索结果应包含关键词"
                )
        );
        System.out.printf("✓ 搜索'深圳': 共 %d 条%n", page.getTotal());
    }

    @Test
    @Order(7)
    @DisplayName("按国家筛选")
    void testFilterByCountry() {
        CustomerQueryDTO dto = new CustomerQueryDTO();
        dto.setCountry("中国");
        dto.setPageNum(1);
        dto.setPageSize(10);

        Page<Customer> page = customerService.listByCondition(dto);

        assertNotNull(page);
        assertTrue(page.getTotal() >= 2, "应至少有 2 条中国客户");
        page.getRecords().forEach(c ->
                assertEquals("中国", c.getCountry())
        );
        System.out.printf("✓ 筛选'中国'客户: 共 %d 条%n", page.getTotal());
    }

    // ==================== 更新测试 ====================

    @Test
    @Order(8)
    @DisplayName("更新客户信息")
    void testUpdate() {
        Customer update = new Customer();
        update.setPhone("13700137000");
        update.setStatus("意向明确");
        update.setRemark("已通过测试更新");

        Customer updated = customerService.update(1L, update);

        assertNotNull(updated);
        assertEquals("13700137000", updated.getPhone());
        assertEquals("意向明确", updated.getStatus());
        assertEquals("已通过测试更新", updated.getRemark());
        // companyName 不应被覆盖
        assertEquals("深圳环球贸易有限公司", updated.getCompanyName());
        System.out.printf("✓ 更新客户成功: id=%d, phone=%s, status=%s%n",
                updated.getId(), updated.getPhone(), updated.getStatus());
    }

    @Test
    @Order(9)
    @DisplayName("更新不存在的客户 — 应抛出异常")
    void testUpdateNotFound() {
        Customer update = new Customer();
        update.setPhone("00000000000");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                customerService.update(99999L, update)
        );
        assertTrue(ex.getMessage().contains("客户不存在"));
        System.out.println("✓ 更新不存在客户正确抛出异常: " + ex.getMessage());
    }

    // ==================== 分页测试 ====================

    @Test
    @Order(10)
    @DisplayName("分页查询 — 第 1 页，每页 2 条")
    void testPagination() {
        CustomerQueryDTO dto = new CustomerQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(2);

        Page<Customer> page = customerService.listByCondition(dto);

        assertNotNull(page);
        assertEquals(2, page.getRecords().size(), "第 1 页应有 2 条");
        assertTrue(page.getPages() > 1, "应有多页");
        System.out.printf("✓ 分页: 当前=%d, 每页=%d, 总页=%d, 总数=%d%n",
                page.getCurrent(), page.getSize(), page.getPages(), page.getTotal());
    }

    // ==================== 枚举查询 ====================

    @Test
    @Order(11)
    @DisplayName("获取客户类型枚举")
    void testGetCustomerTypes() {
        List<String> types = customerService.getCustomerTypes();

        assertNotNull(types);
        assertTrue(types.size() >= 5);
        assertTrue(types.contains("店铺购买"));
        assertTrue(types.contains("合作工厂"));
        System.out.println("✓ 客户类型: " + String.join(", ", types));
    }

    // ==================== 共享浏览测试 ====================

    @Test
    @Order(12)
    @DisplayName("共享浏览 — 仅返回已签约客户")
    void testShareList() {
        CustomerQueryDTO dto = new CustomerQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        Page<Customer> page = customerService.listForShare(dto);

        assertNotNull(page);
        page.getRecords().forEach(c ->
                assertEquals("已签约", c.getStatus(),
                        "共享浏览只应返回'已签约'客户")
        );
        System.out.printf("✓ 共享浏览: 共 %d 条已签约客户%n", page.getTotal());
    }

    // ==================== 删除测试 ====================

    @Test
    @Order(13)
    @DisplayName("逻辑删除客户")
    void testDelete() {
        // 先确认存在
        Customer before = customerService.getById(2L);
        assertNotNull(before, "删除前客户应存在");

        // 执行逻辑删除
        customerService.delete(2L);

        // 逻辑删除后查不到
        Customer after = customerService.getById(2L);
        assertNull(after, "逻辑删除后客户应无法查到");
        System.out.println("✓ 逻辑删除成功: id=2 已不可见");
    }

    @Test
    @Order(14)
    @DisplayName("删除不存在的客户 — 应抛出异常")
    void testDeleteNotFound() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                customerService.delete(99999L)
        );
        assertTrue(ex.getMessage().contains("客户不存在"));
        System.out.println("✓ 删除不存在客户正确抛出异常: " + ex.getMessage());
    }

    // ==================== 空参数查询测试 ====================

    @Test
    @Order(15)
    @DisplayName("无筛选条件查询 — 返回剩余未删除数据")
    void testQueryAllAfterDelete() {
        CustomerQueryDTO dto = new CustomerQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(20);

        Page<Customer> page = customerService.listByCondition(dto);

        assertNotNull(page);
        // 初始 5 条 + 新增 1 条 - 逻辑删除 1 条 = 5 条
        assertTrue(page.getTotal() >= 4, "删除后应有合理数量的记录");
        System.out.printf("✓ 剩余记录数: %d（初始5 + 新增1 - 删除1 ≈ 5）%n", page.getTotal());
    }
}

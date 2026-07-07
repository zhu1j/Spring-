package com.crm.service;

import com.crm.dto.CustomerQueryDTO;
import com.crm.entity.Customer;
import com.crm.service.CustomerService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest //加载Spring容器，自动注入Bean
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomerServiceTest {
    //注入业务层Service
    @Autowired
    private CustomerService customerService;

    private static Long savedId; // create 后用，给后续 update/delete/getById 复用

    // ==================== 1. create（新增） ====================
    //测试增加服务接口
    @Test
    @Order(1)
    @DisplayName("新增客户 → 自动填充默认状态，ID 回填")
    public void testCreate() {
        //测试三步法
        // 1. 准备数据
        // Customer customer = new Customer(9,"西拓跨境","朱杰","17756942062","zhujiejava1@gmail.com","中国","龙华区宝能科技园","www.longhua.com","hahaha","合作工厂","","","潜在客户","","", LocalDateTime.of(2026, 6, 30, 14, 30, 0),LocalDateTime.of(2026, 6, 30, 15, 30, 0),0);
        Customer customer = Customer.builder()
                .id(10L)
                .companyName("西拓跨境")
                .contactPerson("朱杰")
                .phone("17756942062")
                .email("zhujiejava1@gmail.com")
                .country("中国")
                .address("龙华区宝能科技园")
                .website("www.baoneng.com")
                .wechat("17756942062")
                .customerType("合作工厂")
                .serviceNeeds("无额外需求")
                .source("线上")
                .status("初步接洽")
                .remark("重要客户")
                .createdBy("方哥")
                .createdAt(LocalDateTime.of(2026, 6, 30, 14, 30, 0))
                .updatedAt(LocalDateTime.of(2026, 6, 30, 14, 30, 1))
                .isDeleted(0)
                .build();
        //2.执行
        //调用service方法
        Customer result = customerService.create(customer);

        System.out.println(result);

        // 3. 断言
        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo("初步接洽");
    }

        @Test
        @Order(2)
        @DisplayName("新增客户 → 手动指定状态，不覆盖")
        void shouldCreateCustomerWithCustomStatus() {
            Customer customer = Customer.builder()
                    .companyName("已签约公司")
                    .contactPerson("李四")
                    .phone("13900139000")
                    .customerType("合作工厂")
                    .status("已签约")           // 手动指定
                    .build();

            Customer result = customerService.create(customer);

            assertThat(result.getStatus()).isEqualTo("已签约");  // 不覆盖

    }

    // ==================== 2. getById（查询） ====================
    //测试更新服务接口
    @Test
    @Order(3)
    @DisplayName("根据ID查询 → 查得到")
    void shouldGetById() {
        Customer result = customerService.getById(savedId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedId);
        assertThat(result.getCompanyName()).isEqualTo("测试科技有限公司");
    }

    @Test
    @Order(4)
    @DisplayName("根据ID查询 → 查不到，返回 null（逻辑删除的也查不到）")
    void shouldGetByIdReturnNullWhenNotFound() {
        Customer result = customerService.getById(99999L);

        assertThat(result).isNull();
    }

    // ==================== 3. update（更新） ====================
    @Test
    @Order(5)
    @DisplayName("更新客户 → 修改公司名和联系人，返回最新数据")
    public void testUpdate(){
        Long id = 10L;
        // 1.找到存在要修改的客户信息
        Customer createdData = customerService.getById(id);
        // 先查是否存在
        if (createdData == null) {
            throw new RuntimeException("客户不存在：id=" + createdData.getId());
        }

        //2. 执行：修改字段后更新
        Customer updateData = Customer.builder()
                .companyName("修改后的公司")
                .build();
        Customer result = customerService.update(createdData.getId(),updateData);

        // 3. 断言
        assertThat(result.getCompanyName()).isEqualTo("修改后的公司");
    }

    @Test
    @Order(6)
    @DisplayName("更新客户 → ID不存在，抛异常")
    void shouidThrowWhenUpdateNonExistentId() {
        Customer updateData = Customer.builder()
                .companyName("不存在的公司")
                .build();
        assertThatThrownBy(() -> customerService.update(99999L,updateData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("客户不存在");
    }


    // ==================== 4. delete（逻辑删除） ====================
    //测试删除接口
    @Test
    @Order(7)
    @DisplayName("逻辑删除 → 删除后getById 查不到")
    public void testDelete(){
        // 先建一个临时客户
        Customer tmp = customerService.create(Customer.builder()
                .companyName("待删除公司")
                .contactPerson("赵六")
                .phone("13600136000")
                .customerType("其他")
                .build());
        Long tmpId = tmp.getId();

        //执行删除
        customerService.delete(tmpId);

        //逻辑删除后查不到
        Customer result = customerService.getById(tmpId);
        assertThat(result).isNull();
    }

    @Test
    @Order(8)
    @DisplayName("逻辑删除 → ID不存在，抛异常")
    void shouldThrowWhenDeleteNonExistenId() {
        assertThatThrownBy(() -> customerService.delete(99999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("客户不存在");
    }

    // ==================== 5. listByCondition（分页条件查询） ====================
    @Test
    @Order(9)
    @DisplayName("分页查询 → 无条件，返回所有记录并按时间倒序")
    void shouldListAllByCondition() {
        // Arrange：先造测试数据
        customerService.create(Customer.builder()
                .companyName("测试A").contactPerson("张三")
                .phone("13800138000").customerType("店铺购买").build());
        customerService.create(Customer.builder()
                .companyName("测试B").contactPerson("李四")
                .phone("13900139000").customerType("合作工厂").build());
        customerService.create(Customer.builder()
                .companyName("测试C").contactPerson("王五")
                .phone("13700137000").customerType("物流合作").build());

        CustomerQueryDTO dto = new CustomerQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(5);

        var page = customerService.listByCondition(dto);
        assertThat(page.getRecords()).isNotEmpty();
        assertThat(page.getTotal()).isGreaterThan(0); //预期拿到的记录数

        /**
         * 注意：这个测试用的H2的虚拟数据库，数据要自己造，测试的是写的逻辑功能. （不依赖外部环境）
        * */
    }

    // ==================== 6. listForShare（共享浏览，只查已签约） ====================
    @Test
    @Order(6)
    void shouldThrowWhenDeleteNonExistent() {
        assertThatThrownBy(() -> customerService.delete(99999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("客户不存在");
    }


    // ==================== 7. getCustomerTypes（枚举查询） ====================
    @Test
    @Order(7)
    void shouldGetCustomerTypes() {
        assertThat(customerService.getCustomerTypes()).isNotNull();
    }
}

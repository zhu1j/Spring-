package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.dto.CustomerQueryDTO;
import com.crm.entity.Customer;
import com.crm.mapper.CustomerMapper;
import com.crm.service.CustomerService;
import com.crm.util.ExcelUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 客户信息服务实现类
 * <p>
 * 核心业务逻辑层，负责客户数据的增删改查、分页、Excel导出。
 * 所有数据库操作通过 MyBatis-Plus 的 LambdaQueryWrapper 构建。
 *
 * @author CRM Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;

    /** 客户类型枚举（业务常量） */
    private static final List<String> CUSTOMER_TYPES = Arrays.asList(
            "店铺购买", "开店培训", "合作工厂", "物流合作", "其他"
    );

    /** 跟进状态枚举 */
    private static final List<String> STATUS_LIST = Arrays.asList(
            "潜在客户", "初步接洽", "意向明确", "已签约", "已流失"
    );

    // ==================== 增删改 ====================

    @Override
    public Customer create(Customer customer) {
        // 如果未设置状态，默认为"潜在客户"
        if (StringUtils.isBlank(customer.getStatus())) {
            customer.setStatus("潜在客户");
        }
        customerMapper.insert(customer);
        log.info("新增客户成功: id={}, company={}", customer.getId(), customer.getCompanyName());
        return customer;
    }

    @Override
    public Customer update(Long id, Customer customer) {
        // 确保更新的是数据库中存在的记录
        Customer existing = customerMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("客户不存在: id=" + id);
        }
        customer.setId(id);                          // 保证ID正确
        customer.setCreatedAt(existing.getCreatedAt()); // 保留原创建时间
        customerMapper.updateById(customer);
        log.info("更新客户成功: id={}, company={}", id, customer.getCompanyName());
        return customerMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        Customer existing = customerMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("客户不存在: id=" + id);
        }
        // MyBatis-Plus 的逻辑删除：实际执行 UPDATE SET is_deleted = 1
        customerMapper.deleteById(id);
        log.info("逻辑删除客户: id={}, company={}", id, existing.getCompanyName());
    }

    // ==================== 查询 ====================

    @Override
    public Customer getById(Long id) {
        return customerMapper.selectById(id);
    }

    @Override
    public Page<Customer> listByCondition(CustomerQueryDTO dto) {
        Page<Customer> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<Customer> wrapper = buildQueryWrapper(dto);
        // 排序
        applySorting(page, wrapper, dto);
        return customerMapper.selectPage(page, wrapper);
    }

    // ==================== Excel 导出 ====================

    @Override
    public void exportExcel(CustomerQueryDTO dto, HttpServletResponse response) {
        // 导出时不使用分页 — 查询全部符合条件的记录
        LambdaQueryWrapper<Customer> wrapper = buildQueryWrapper(dto);
        List<Customer> list = customerMapper.selectList(wrapper);
        log.info("导出Excel，共 {} 条记录", list.size());
        ExcelUtil.export(response, list, "客户资料");
    }

    // ==================== 共享浏览 ====================

    @Override
    public Page<Customer> listForShare(CustomerQueryDTO dto) {
        // 共享浏览：只返回已签约客户，隐藏敏感字段（手机号、微信号脱敏在此不做，留给前端处理）
        Page<Customer> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<Customer> wrapper = buildQueryWrapper(dto);
        // 只展示已签约客户供外部浏览
        wrapper.eq(Customer::getStatus, "已签约");
        applySorting(page, wrapper, dto);
        return customerMapper.selectPage(page, wrapper);
    }

    // ==================== 枚举查询 ====================

    @Override
    public List<String> getCustomerTypes() {
        return CUSTOMER_TYPES;
    }

    // ==================== 私有方法 ====================

    /**
     * 根据查询条件构建 MyBatis-Plus 的 LambdaQueryWrapper。
     * <p>
     * 使用 Lambda 表达式引用字段，避免字符串硬编码，编译期检查字段名。
     */
    private LambdaQueryWrapper<Customer> buildQueryWrapper(CustomerQueryDTO dto) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();

        // 客户类型精确匹配
        if (StringUtils.isNotBlank(dto.getCustomerType())) {
            wrapper.eq(Customer::getCustomerType, dto.getCustomerType());
        }
        // 跟进状态精确匹配
        if (StringUtils.isNotBlank(dto.getStatus())) {
            wrapper.eq(Customer::getStatus, dto.getStatus());
        }
        // 国家/地区精确匹配
        if (StringUtils.isNotBlank(dto.getCountry())) {
            wrapper.eq(Customer::getCountry, dto.getCountry());
        }
        // 关键词模糊搜索：匹配公司名 或 联系人
        if (StringUtils.isNotBlank(dto.getKeyword())) {
            wrapper.and(w -> w
                    .like(Customer::getCompanyName, dto.getKeyword())
                    .or()
                    .like(Customer::getContactPerson, dto.getKeyword())
            );
        }
        // 默认只查未删除的记录（MyBatis-Plus 逻辑删除会自动加上 is_deleted=0）
        return wrapper;
    }

    /**
     * 构建排序条件
     */
    private void applySorting(Page<Customer> page, LambdaQueryWrapper<Customer> wrapper, CustomerQueryDTO dto) {
        boolean isAsc = "asc".equalsIgnoreCase(dto.getSortOrder());
        String field = dto.getSortField();
        // 安全映射：只允许按以下字段排序
        switch (field) {
            case "company_name":
                wrapper.orderBy(true, isAsc, Customer::getCompanyName);
                break;
            case "status":
                wrapper.orderBy(true, isAsc, Customer::getStatus);
                break;
            case "country":
                wrapper.orderBy(true, isAsc, Customer::getCountry);
                break;
            case "updated_at":
                wrapper.orderBy(true, isAsc, Customer::getUpdatedAt);
                break;
            default: // created_at
                wrapper.orderBy(true, isAsc, Customer::getCreatedAt);
        }
    }
}

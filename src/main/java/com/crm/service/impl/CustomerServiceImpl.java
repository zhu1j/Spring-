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
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    //客户类型枚举
    private static final List<String> CUSTOMER_TYPES = Arrays.asList(
            "店铺购买","开店培训","合作工厂","物流合作","其他"
    );

    //=================实现接口=======================

    //新增
    @Override
    public Customer create(Customer customer) {
        //如果前端没传状态，默认设为”潜在客户“
        if (StringUtils.isBlank(customer.getStatus())) {
            customer.setStatus("潜在客户");
        }
        customerMapper.insert(customer);
        //插入执行后，Mybatis-Plus 会自动回填自增 ID 到 customer.id
        log.info("新增客户成功：id={},company={}",customer.getId(),customer.getCompanyName());
        return customer;
    }

    //更新
    @Override
    public Customer update(Long id, Customer customer) {
        // 先查是否存在
        Customer existing = customerMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("客户不存在：id=" + id);
        }
        //确保ID 正确、保留原创建时间
        customer.setId(id);
        customer.setCreatedAt(existing.getCreatedAt());
        customerMapper.updateById(customer);
        log.info("更新客户成功: id={},company={}",id,customer.getCompanyName());
        //返回最新数据
        return customerMapper.selectById(id);
    }

    //删除(逻辑删除)
    @Override
    public void delete(Long id) {
        Customer existing = customerMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("客户不存在: id=" + id);
        }
        //MyBatis-Plus 的逻辑删除: 实际执行 UPDATE SET is_deleted=1
        customerMapper.deleteById(id);
        log.info("逻辑删除客户: id={},company={}",id,existing.getCompanyName());
    }


    //=================查询===================
    //1.根据ID查询
    @Override
    public Customer getById(Long id) {

        return customerMapper.selectById(id);
        // 逻辑删除的记录会被自动过滤（is_delete=1 的查不到）
    }

    //2.根据 条件 查询多条记录集合
    @Override
    public Page<Customer> listByCondition(CustomerQueryDTO queryDTO) {
        // 1. 创建分页对象
        Page<Customer> page = new Page<>(queryDTO.getPageNum(),queryDTO.getPageSize());
        // 2. 构建查询条件
        LambdaQueryWrapper<Customer> wrapper = buildQueryWrapper(queryDTO);
        // 3. 执行分页查询
        customerMapper.selectPage(page,wrapper);
        return page;
    }

    // ==================== Excel 导出 ====================
    @Override
    public void exportExcel(CustomerQueryDTO queryDTO, HttpServletResponse response) {
        // 导出时不使用分页 - 查全部符合条件的记录
        LambdaQueryWrapper<Customer> wrapper = buildQueryWrapper(queryDTO);
        List<Customer> list = customerMapper.selectList(wrapper);
        log.info("导出Excel,共 {} 条记录",list.size());
        ExcelUtil.export(response, list, "客户资料");
    }

    // ==================== 共享浏览 ====================
    @Override
    public Page<Customer> listForShare(CustomerQueryDTO queryDTO) {
        Page<Customer> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Customer> wrapper = buildQueryWrapper(queryDTO);
        // 共享浏览器只看已签约的客户
        wrapper.eq(Customer::getStatus, "已签约");
        customerMapper.selectPage(page, wrapper);
        return page;
    }

    // ==================== 枚举查询 ====================
    @Override
    public List<String> getCustomerTypes() {
        return CUSTOMER_TYPES;//List.of();
    }




    // ==================== 私有方法：构建查询条件 ====================

    //根据 DTO 构建 LambdaQueryWrapper
    private LambdaQueryWrapper<Customer> buildQueryWrapper(CustomerQueryDTO queryDTO) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();

        // 客户类型精准匹配
        if (StringUtils.isNotBlank(queryDTO.getCustomerType())) {
            wrapper.eq(Customer::getCustomerType, queryDTO.getCustomerType());
        }
        // 跟进状态精确匹配
        if (StringUtils.isNotBlank(queryDTO.getStatus())) {
            wrapper.eq(Customer::getStatus,queryDTO.getStatus());
        }
        // 国家/地区精确匹配
        if (StringUtils.isNotBlank(queryDTO.getCountry())) {
            wrapper.eq(Customer::getCountry,queryDTO.getCountry());
        }
        //关键词模糊搜索：同时匹配公司名和联系人
        if (StringUtils.isNotBlank(queryDTO.getKeyword())) {
            wrapper.and(w -> w
                    .like(Customer::getCompanyName,queryDTO.getKeyword())
                    .or()
                    .like(Customer::getContactPerson,queryDTO.getKeyword())
            );
        }

        //排序
        boolean isAsc = "asc".equalsIgnoreCase(queryDTO.getSortOrder());
        switch (queryDTO.getSortField()) {
            case "company_name":
                wrapper.orderBy(true,isAsc,Customer::getCompanyName);
                break;
            case "status":
                wrapper.orderBy(true,isAsc,Customer::getStatus);
                break;
            case "country":
                wrapper.orderBy(true,isAsc,Customer::getCountry);
                break;
            case "updated_at":
                wrapper.orderBy(true,isAsc,Customer::getUpdatedAt);
                break;
            default: //created_at
                wrapper.orderBy(true,isAsc,Customer::getCreatedAt);
        }
        return wrapper;
    }

}

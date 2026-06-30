package com.crm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.dto.CustomerQueryDTO;
import com.crm.entity.Customer;
import com.crm.mapper.CustomerMapper;
import com.crm.service.CustomerService;
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
            throw new RuntimeException("客户不存在: id" + id);
        }
        //MyBatis-Plus 的逻辑删除: 实际执行 UPDATE SET is_deleted=1
        customerMapper.deleteById(id);
        log.info("逻辑删除客户: id={},company={}",id,existing.getCompanyName());
    }


    @Override
    public Customer getById(Long id) {
        return null;
    }

    @Override
    public Page<Customer> listByCondition(CustomerQueryDTO queryDTO) {
        return null;
    }

    @Override
    public void exportExcel(CustomerQueryDTO queryDTO, HttpServletResponse response) {

    }

    @Override
    public Page<Customer> listForShare(CustomerQueryDTO queryDTO) {
        return null;
    }

    @Override
    public List<String> getCustomerTypes() {
        return null;//List.of();
    }
}

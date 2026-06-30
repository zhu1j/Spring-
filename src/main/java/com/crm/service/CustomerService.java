package com.crm.service;

//客户信息服务接口

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.dto.CustomerQueryDTO;
import com.crm.entity.Customer;

import javax.servlet.http.HttpServletResponse;
import java.util.List;


public interface CustomerService {
    //新增客户
    Customer create(Customer customer);
    //根据ID更新客户信息
    Customer update(Long id,Customer customer);
    //逻辑删除客户
    void delete(Long id);
    //根据id查询客户详情
    Customer getById(Long id);
    //分页 + 多条件查询客户列表
    Page<Customer> listByCondition(CustomerQueryDTO queryDTO);
    //导出客户数据为Excel文件
    void exportExcel(CustomerQueryDTO queryDTO, HttpServletResponse response);
    //获取共享浏览的客户列表（仅已签约客户，供外部团队查看）
    Page<Customer> listForShare(CustomerQueryDTO queryDTO);
    //获取所有客户类型枚举值（供前端下拉框使用）
    List<String> getCustomerTypes();
}

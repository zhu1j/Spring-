package com.crm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.dto.CustomerQueryDTO;
import com.crm.entity.Customer;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 客户信息服务接口
 *
 * @author CRM Team
 */
public interface CustomerService {

    /**
     * 新增客户
     *
     * @param customer 客户实体
     * @return 保存后的客户（含自增ID）
     */
    Customer create(Customer customer);

    /**
     * 根据ID更新客户信息
     *
     * @param id       客户ID
     * @param customer 更新的字段
     * @return 更新后的客户
     */
    Customer update(Long id, Customer customer);

    /**
     * 逻辑删除客户
     *
     * @param id 客户ID
     */
    void delete(Long id);

    /**
     * 根据ID查询客户详情
     *
     * @param id 客户ID
     * @return 客户实体，不存在返回 null
     */
    Customer getById(Long id);

    /**
     * 分页 + 条件查询客户列表
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<Customer> listByCondition(CustomerQueryDTO queryDTO);

    /**
     * 导出客户数据为 Excel
     *
     * @param queryDTO 查询条件（为null则导出全部）
     * @param response HTTP响应（用于输出文件流）
     */
    void exportExcel(CustomerQueryDTO queryDTO, HttpServletResponse response);

    /**
     * 获取用于共享浏览的客户列表（只读，脱敏）
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<Customer> listForShare(CustomerQueryDTO queryDTO);

    /**
     * 获取所有可用的客户类型枚举值
     *
     * @return 客户类型列表
     */
    List<String> getCustomerTypes();
}

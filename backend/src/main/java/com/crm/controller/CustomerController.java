package com.crm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.dto.CustomerQueryDTO;
import com.crm.dto.Result;
import com.crm.entity.Customer;
import com.crm.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户信息 RESTful API 控制器
 * <p>
 * 统一前缀 /api/customers，遵循 RESTful 规范。
 * 所有接口返回 Result<T> 统一响应体。
 *
 * @author CRM Team
 */
@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Validated
public class CustomerController {

    private final CustomerService customerService;

    // ==================== C: 新增 ====================

    /**
     * 新增客户
     *
     * @param customer 客户信息 JSON（companyName, contactPerson, customerType 为必填）
     * @return 保存后的客户信息
     */
    @PostMapping
    public Result<Customer> create(@RequestBody @Valid Customer customer) {
        log.info("请求新增客户: {}", customer.getCompanyName());
        Customer saved = customerService.create(customer);
        return Result.ok("客户录入成功", saved);
    }

    // ==================== R: 查询 ====================

    /**
     * 根据ID查询客户详情
     *
     * @param id 客户ID
     * @return 客户详细信息
     */
    @GetMapping("/{id}")
    public Result<Customer> getById(@PathVariable @NotNull Long id) {
        Customer customer = customerService.getById(id);
        if (customer == null) {
            return Result.fail(404, "客户不存在");
        }
        return Result.ok(customer);
    }

    /**
     * 分页+条件查询客户列表
     * <p>
     * 使用 POST 方式（非 GET）以支持复杂查询条件的 JSON 请求体。
     *
     * @param queryDTO 查询条件 + 分页参数
     * @return 分页结果，包含 total、pageNum、pageSize、records
     */
    @PostMapping("/list")
    public Result<Map<String, Object>> list(@RequestBody CustomerQueryDTO queryDTO) {
        Page<Customer> page = customerService.listByCondition(queryDTO);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        result.put("pages", page.getPages());
        return Result.ok(result);
    }

    // ==================== U: 更新 ====================

    /**
     * 更新客户信息
     *
     * @param id       客户ID
     * @param customer 要更新的字段
     * @return 更新后的客户信息
     */
    @PutMapping("/{id}")
    public Result<Customer> update(@PathVariable @NotNull Long id,
                                   @RequestBody Customer customer) {
        Customer updated = customerService.update(id, customer);
        return Result.ok("客户信息更新成功", updated);
    }

    // ==================== D: 删除 ====================

    /**
     * 删除客户（逻辑删除）
     *
     * @param id 客户ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @NotNull Long id) {
        customerService.delete(id);
        return Result.ok("客户删除成功", null);
    }

    // ==================== 导出 ====================

    /**
     * 导出客户数据为 Excel
     * <p>
     * 支持 GET 请求（方便直接通过浏览器下载），可选传入查询参数进行筛选导出。
     * 生成 .xlsx 文件并以附件形式返回浏览器下载。
     *
     * @param customerType 可选：客户类型筛选
     * @param status       可选：状态筛选
     * @param country      可选：国家筛选
     * @param keyword      可选：关键词搜索
     * @param response     HTTP响应对象
     */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String customerType,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String country,
                       @RequestParam(required = false) String keyword,
                       HttpServletResponse response) {
        CustomerQueryDTO dto = new CustomerQueryDTO();
        dto.setCustomerType(customerType);
        dto.setStatus(status);
        dto.setCountry(country);
        dto.setKeyword(keyword);
        // 导出全部：取消分页限制
        dto.setPageNum(1);
        dto.setPageSize(Integer.MAX_VALUE);
        customerService.exportExcel(dto, response);
    }

    // ==================== 共享浏览 ====================

    /**
     * 共享浏览客户列表（只读、脱敏）
     * <p>
     * 供外部团队成员通过链接查看已签约客户信息。
     * 返回数据仅包含公司名、联系人、国家、客户类型，不包含手机/邮箱等私密信息。
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @PostMapping("/share")
    public Result<Map<String, Object>> shareList(@RequestBody CustomerQueryDTO queryDTO) {
        Page<Customer> page = customerService.listForShare(queryDTO);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return Result.ok(result);
    }

    // ==================== 枚举查询 ====================

    /**
     * 获取可用的客户类型列表（用于前端下拉框）
     *
     * @return 客户类型字符串列表
     */
    @GetMapping("/types")
    public Result<List<String>> getTypes() {
        return Result.ok(customerService.getCustomerTypes());
    }
}

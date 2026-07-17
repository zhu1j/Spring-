package com.crm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.dto.CustomerQueryDTO;
import com.crm.dto.Result;
import com.crm.entity.Customer;
import com.crm.mapper.CustomerMapper;
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
 * 统一前缀 /api/customers,遵循 RESTful 设计规范
 * 所有接口返回 Result<T> 统一响应体
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
     * POST /api/customers
     *
     * @param customer 客户 JSON（companyName, contactPerson, customerType 必填）
     * @return 保存后的客户信息
     */
    @PostMapping
    public Result<Customer> create(@RequestBody @Valid Customer customer) {
        log.info("请求新增客户：{}",customer.getCompanyName());
        Customer saved = customerService.create(customer);
        return Result.ok("客户录入成功",saved);
    }
    // ==================== R: 查询 ====================
    /**
     * 根据 ID 查询客户详情
     * GET /api/customers/{id}
     *
     * @param id 客户 ID
     * @return 客户详细信息
     */
    @GetMapping("/{id}")
    public Result<Customer> getById(@PathVariable @NotNull Long id) {
        Customer customer = customerService.getById(id);
        if (customer == null){
            return Result.fail(404,"客户不存在");
        }
        return Result.ok(customer);
    }

    /**
     * 分页 + 条件查询客户列表
     * POST /api/customers/list
     * <p>
     * 使用 POST 方式（非 GET）以支持复杂查询条件的 JSON 请求体。
     *
     * @param queryDTO 查询条件 + 分页参数
     * @return 分页结果 {records, total, pageNum, pageSize, pages}
     */
    @PostMapping("/list")
    public Result<Map<String,Object>> list(@RequestBody CustomerQueryDTO queryDTO) {
        Page<Customer> page = customerService.listByCondition(queryDTO);
        Map<String,Object> result = new HashMap<>();
        result.put("records",page.getRecords()); //当前页数据
        result.put("total",page.getTotal());    //总记录数
        result.put("pageNum",page.getCurrent()); //当前页码
        result.put("pageSize",page.getSize());  //每页条数
        result.put("pages",page.getPages());    //总页数
        return Result.ok(result);
    }
    // ==================== U: 更新 ====================
    /**
     * 更新客户信息
     * PUT /api/customers/{id}
     *
     * @param id       客户 ID
     * @param customer 要更新的字段（传什么更新什么）
     * @return 更新后的客户完整信息
     */
    @PutMapping("/{id}")
    public Result<Customer> update(@PathVariable @NotNull Long id,
                                   @RequestBody Customer customer) {
        Customer updated = customerService.update(id,customer);
        return Result.ok("客户信息更新成功", updated);
    }
    // ==================== D: 删除 ====================
    /**
     * 逻辑删除客户
     * DELETE /api/customers/{id}
     *
     * @param id 客户 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @NotNull Long id) {
        customerService.delete(id);
        return Result.ok("客户删除成功",null);
    }
    // ==================== 导出 ====================
    /**
     * 导出客户数据为 Excel 文件
     * GET /api/customers/export?status=已签约&country=中国
     * <p>
     * 浏览器直接访问即可下载 .xlsx 文件。支持可选查询参数筛选导出。
     *
     * @param customerType 可选：客户类型筛选
     * @param status       可选：状态筛选
     * @param country      可选：国家筛选
     * @param keyword      可选：关键词搜索
     * @param response     HTTP 响应对象
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
        // 导出全部数据，取消分页限制
        dto.setPageNum(1);
        dto.setPageSize(Integer.MAX_VALUE);
        customerService.exportExcel(dto,response);
    }
    // ==================== 共享浏览 ====================
    /**
     * 共享浏览客户列表（只读、仅已签约客户）
     * POST /api/customers/share
     * <p>
     * 供外部团队成员通过链接查看已签约客户信息。
     * 前端应对手机号、邮箱等敏感字段做脱敏处理。
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @PostMapping("/share")
    public Result<Map<String,Object>> shareList(@RequestBody CustomerQueryDTO queryDTO) {
        Page<Customer> page = customerService.listForShare(queryDTO);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum",page.getCurrent());
        result.put("pageSize", page.getSize());
        return Result.ok(result);
    }
    // ==================== 枚举查询 ====================
    /**
     * 获取可用的客户类型列表（供前端下拉框使用）
     * GET /api/customers/types
     *
     * @return 客户类型字符串列表
     */
    @GetMapping("/types")
    public Result<List<String>> getTypes() {
        return Result.ok(customerService.getCustomerTypes());
    }
}

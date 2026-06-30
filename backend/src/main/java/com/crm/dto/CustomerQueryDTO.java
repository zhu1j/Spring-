package com.crm.dto;

import lombok.Data;

/**
 * 客户查询请求 DTO
 * <p>
 * 支持多条件分页查询：客户类型、状态、国家、关键词等。
 *
 * @author CRM Team
 */
@Data
public class CustomerQueryDTO {

    /** 当前页码（从1开始），默认1 */
    private Integer pageNum = 1;

    /** 每页条数，默认10 */
    private Integer pageSize = 10;

    /** 客户类型筛选 */
    private String customerType;

    /** 跟进状态筛选 */
    private String status;

    /** 国家/地区筛选 */
    private String country;

    /** 关键词搜索（匹配公司名或联系人） */
    private String keyword;

    /** 排序字段，默认创建时间 */
    private String sortField = "created_at";

    /** 排序方向：asc / desc */
    private String sortOrder = "desc";
}

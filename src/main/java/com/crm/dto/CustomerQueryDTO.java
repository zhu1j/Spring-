package com.crm.dto;

import lombok.Data;

@Data
public class CustomerQueryDTO {
    //当前页码（从1开始），默认 1
    private Integer pageNum = 1;
    //每页条数，默认10
    private Integer pageSize = 10;
    //客户类型精确匹配（店铺购买/开店培训/合作工厂 等）
    private String customerType;
    //跟进状态精确匹配（潜在客户/初步接洽/意向明确/已签约/已流失）
    private String status;
    //国家/地区精确匹配
    private String country;
    //关键词模糊搜索（同时匹配公司名和联系人）
    private String keyword;
    //排序字段，默认 created_at
    private String sortField = "created_at";
    //排序方向：asc / desc 默认desc
    private String sortOrder = "desc";
}

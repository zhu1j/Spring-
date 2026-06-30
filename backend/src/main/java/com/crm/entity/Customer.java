package com.crm.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 客户信息实体类
 * <p>
 * 对应数据库表 customer，存储跨境电商客户的核心信息。
 * 使用 MyBatis-Plus 注解简化 ORM 映射。
 *
 * @author CRM Team
 * @since 2026-06-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("customer")
public class Customer {

    /** 主键ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司/店铺名称（必填） */
    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    /** 联系人姓名（必填） */
    @NotBlank(message = "联系人不能为空")
    private String contactPerson;

    /** 联系电话 */
    private String phone;

    /** 邮箱地址 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 国家/地区 */
    private String country;

    /** 详细地址 */
    private String address;

    /** 公司/店铺网址 */
    private String website;

    /** 微信号 */
    private String wechat;

    /** 客户类型：店铺购买 / 开店培训 / 合作工厂 / 物流合作 / 其他 */
    @NotBlank(message = "客户类型不能为空")
    private String customerType;

    /** 服务需求描述（富文本） */
    private String serviceNeeds;

    /** 客户来源渠道 */
    private String source;

    /** 跟进状态：潜在客户 / 初步接洽 / 意向明确 / 已签约 / 已流失 */
    private String status;

    /** 备注信息 */
    private String remark;

    /** 录入人 */
    private String createdBy;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 最后更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0=正常, 1=已删除 */
    @TableLogic
    private Integer isDeleted;
}

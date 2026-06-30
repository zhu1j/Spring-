package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户信息 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动获得 CRUD 方法：
 * insert、deleteById、updateById、selectById、selectList 等。
 * 复杂查询通过 MyBatis-Plus 的 LambdaQueryWrapper 在 Service 层构建。
 *
 * @author CRM Team
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
    // 基础 CRUD 由 MyBatis-Plus 自动提供
    // 如需自定义 SQL，可在此添加方法并使用 @Select / XML 映射
}

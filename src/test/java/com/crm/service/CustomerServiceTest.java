package com.crm.service;

import com.crm.entity.Customer;
import com.crm.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;


@SpringBootTest //加载Spring容器，自动注入Bean
public class CustomerServiceTest {
    //注入业务层Service
    @Autowired
    private CustomerService customerService;

    @Test
    public void testGetCustomerById() {
       // Customer customer = new Customer(9,"西拓跨境","朱杰","17756942062","zhujiejava1@gmail.com","中国","龙华区宝能科技园","www.longhua.com","hahaha","合作工厂","","","潜在客户","","", LocalDateTime.of(2026, 6, 30, 14, 30, 0),LocalDateTime.of(2026, 6, 30, 15, 30, 0),0);
        Customer customer = Customer.builder()
                .id(9L)
                .companyName("西拓跨境")
                .contactPerson("朱杰")
                .phone("17756942062")
                .email("zhujiejava1@gmail.com")
                .country("中国")
                .address("龙华区宝能科技园")
                .website("www.baoneng.com")
                .wechat("17756942062")
                .customerType("合作工厂")
                .serviceNeeds("无额外需求")
                .source("线上")
                .status("初步接洽")
                .remark("重要客户")
                .createdBy("方哥")
                .createdAt(LocalDateTime.of(2026, 6, 30, 14, 30, 0))
                .updatedAt(LocalDateTime.of(2026, 6, 30, 14, 30, 1))
                .isDeleted(0)
                .build();
        //调用service方法
        Customer new_customer = customerService.create(customer);

        System.out.println(new_customer);

    }
}

package com.yupi.yupao.service.impl;

import com.yupi.yupao.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

/**
 * @Author:tzy
 * @Description :
 * @Date:2024/3/199:45
 */

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate<String , Object> redisTemplate;


    @Test
    void test(){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //增
        valueOperations.set("tudouString","shabi");
        valueOperations.set("tudouInt",2);
        valueOperations.set("tudouDouble",250.00);
        User user = new User();
        user.setId(1L);
        user.setUsername("笨蛋");
        valueOperations.set("tudouUser",user);

        //查
        Object tudou = valueOperations.get("tudouString");
        Assertions.assertTrue("shabi".equals(tudou));
        tudou = valueOperations.get("tudouInt");

        Assertions.assertTrue(2 == (Integer)tudou);
        tudou = valueOperations.get("tudouDouble");
        Assertions.assertTrue(250.00 == (Double)tudou);
        tudou = valueOperations.get("tudouUser");
        System.out.println(tudou);


    }
}

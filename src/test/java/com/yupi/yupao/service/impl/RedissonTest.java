package com.yupi.yupao.service.impl;

import com.yupi.yupao.config.RedissonConfig;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author:tzy
 * @Description :
 * @Date:2024/3/2219:39
 */
@SpringBootTest
public class RedissonTest {
    @Resource
    private RedissonClient redissonClient;

    @Test
    void test(){
        //list
        List<String> list = new ArrayList<>();
        list.add("yupi");

        System.out.println(list.get(0));
//        list.remove(0);

        RList<String> rList = redissonClient.getList("yupi");
        rList.add("okk");
        System.out.println(rList.get(0));
//        rList.remove(0);

        //map

        //set
    }
}

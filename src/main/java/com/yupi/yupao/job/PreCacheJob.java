package com.yupi.yupao.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupao.model.domain.User;
import com.yupi.yupao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author:tzy
 * @Description : 预热缓存
 * @Date:2024/3/2113:34
 */
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private RedisTemplate<String , Object> redisTemplate;
    @Resource
    private UserService userService;

    private List<Long> mainUserList = Arrays.asList(1L);
    //每天执行，预热加载用户
    @Scheduled( cron = "0 59 14 * * ? ")
    public void doCacheRecommendUser(){

        for (Long userId : mainUserList) {
            ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
            String redisKey = String.format("yupao:user:recommend:%s", userId);
            //如果没缓存，从数据库中wu查询， 写入缓存里面
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            //page 是mybatis里面的分页查询方法，需要传入 翻页对象，封装类   pageNum:页号
            Page userPage = userService.page( new Page<>( 1 , 20)  , queryWrapper);
            try {
                valueOperations.set(redisKey , userPage ,30000, TimeUnit.MILLISECONDS );
            } catch (Exception e) {
                log.error("redis set error",e);
            }
        }

    }
}

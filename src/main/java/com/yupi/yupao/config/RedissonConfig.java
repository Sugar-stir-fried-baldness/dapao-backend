package com.yupi.yupao.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author:tzy
 * @Description : Redisson配置
 * @Date:2024/3/2219:10
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "spring.redis")
public class RedissonConfig {

    private String host;

    private String port;

//    redissonConfig()方法被标记为一个bean，它返回一个RedissonClient实例。在别的地方想要使用 直接依赖注入 @Resource
    @Bean
    public RedissonClient redissonClient() {
        // 1. 创建配置
        Config config = new Config();
        String redissonAddress = String.format("redis://%s:%s" , host,port) ;
//        这里用的单个服务器
        config.useSingleServer().setAddress(redissonAddress).setDatabase(3);

        // 2. 创建redisson实例
        RedissonClient redisson = Redisson.create(config);


        return redisson;
    }
}

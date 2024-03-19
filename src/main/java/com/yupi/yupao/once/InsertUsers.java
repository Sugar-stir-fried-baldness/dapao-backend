package com.yupi.yupao.once;

import com.yupi.yupao.mapper.UserMapper;
import com.yupi.yupao.model.domain.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

/**
 * @Author:tzy
 * @Description : 一次性插入用户数据
 * @Date:2024/3/1619:34
 */
@Component
public class InsertUsers {

    @Resource
    private UserMapper userMapper;

//    @Scheduled(initialDelay = 5000 , fixedRate = Long.MAX_VALUE)
    public void doInsertSearch(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_VALUE = 10000;
        for (int i = 0; i < INSERT_VALUE; i++) {
            User user = new User();

            user.setUsername("假黑大帅");
            user.setUserAccount("fake_da_shuai");
            user.setAvatarUrl("https://mdn.alipayobjects.com/huamei_0prmtq/afts/img/A*9uyGR6gGcI4AAAAAAAAAAAAADvuFAQ/original");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("123");
            user.setEmail("123");
            user.setUserStatus(0);
            user.setTags("");
            user.setUserRole(0);
            user.setPlanetCode("1111");
//            userMapper.insert(user);
        }
        stopWatch.stop();
        System.out.println(stopWatch.getLastTaskTimeMillis());
    }
}

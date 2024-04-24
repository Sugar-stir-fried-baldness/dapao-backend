package com.yupi.yupao.once;

import com.yupi.yupao.mapper.UserMapper;
import com.yupi.yupao.model.domain.User;
import com.yupi.yupao.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author:tzy
 * @Description :
 * @Date:2024/3/1620:43
 */
@SpringBootTest
class InsertUsersTest {

    @Resource
    private UserService userService;

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertSearch(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_VALUE = 1000000;
        ArrayList<User> userList = new ArrayList<>();
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
            userList.add(user);
        }
        // 20s 10万条
        userService.saveBatch(userList , 10000);
        stopWatch.stop();
        System.out.println(stopWatch.getLastTaskTimeMillis());
    }
    private ExecutorService executorService =   new ThreadPoolExecutor(40 , 1000,1000,TimeUnit.MINUTES , new ArrayBlockingQueue<>(10000));
//    private ExecutorService executorService = new ThreadPoolExecutor(16, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    /**
     * 插入 10w条数据 ， 6s
     * 并发批量插入用户
     */
    @Test
    public void doConcurrencyInsertSearch(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_VALUE = 100000;
        //分十组
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        int batchSize = 10000;
        for (int i = 0; i < 10; i++) {
            ArrayList<User> userList = new ArrayList<>();
            while (true){
                j++;
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
                userList.add(user);
                if(j % batchSize == 0){
                    break;
                }
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                userService.saveBatch(userList, batchSize);
            },executorService);

            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        // 20s 10万条
        stopWatch.stop();
        System.out.println(stopWatch.getLastTaskTimeMillis());
    }
}
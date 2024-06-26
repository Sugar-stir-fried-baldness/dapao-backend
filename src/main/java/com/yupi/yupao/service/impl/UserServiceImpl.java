package com.yupi.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yupi.yupao.common.ErrorCode;
import com.yupi.yupao.common.ResultUtils;
import com.yupi.yupao.contant.UserConstant;
import com.yupi.yupao.exception.BusinessException;
import com.yupi.yupao.mapper.UserMapper;
import com.yupi.yupao.model.domain.User;
import com.yupi.yupao.service.UserService;
import com.yupi.yupao.util.AlgorithmUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.apache.commons.math3.util.Pair;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.yupi.yupao.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate redisTemplate;

    // https://www.code-nav.cn/

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "zzh";

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();
    }

    // [加入星球](https://www.code-nav.cn/) 从 0 到 1 项目实战，经验拉满！10+ 原创项目手把手教程、7 日项目提升训练营、60+ 编程经验分享直播、1000+ 项目经验笔记

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8) {
            return null;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setTags(originUser.getTags());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签名查询 用户  (使用内存查询)
     *
     * @param tagsNameList 标签名列表
     * @return
     */
    @Override
    public List<User> searchUserByTags(List<String> tagsNameList) {
        if (CollectionUtils.isEmpty(tagsNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签为空");
        }

        //在内存里面查询
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        //1.查询出所有的用户
        List<User> userList = userMapper.selectList(userQueryWrapper);
        Gson gson = new Gson();
        //2.在内存中判断是否包含要求的标签
        //过滤掉所有 不包含 所给的 tagsNameList 的标签
        return userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            //将json转化成字符串  set判空
            Set<String> tempTagName = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            //set判空  ofNullable封装一个可能为空的对象 | 如果 tempTagName 为空的话，就取orElse里面的值
            tempTagName = Optional.ofNullable(tempTagName).orElse(new HashSet<>());
            for (String tagName : tagsNameList) {
                if (!tempTagName.contains(tagName)) {
                    return false;
                }
            }
            return true;

        }).map(this::getSafetyUser).collect(Collectors.toList());


    }


    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object loginUser = request.getSession().getAttribute(USER_LOGIN_STATE);

        if (loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        return (User) loginUser;


    }

    /**
     * 更新用户
     *
     * @param user
     * @param loginUser
     * @return
     */
    @Override
    public int updateUser(User user, User loginUser) {
        //判断这个
        if (user == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        //todo 补充校验，如果用户没有传任何要更新的值，就直接报错，不执行update语句
        Long id = user.getId();
//        log.info("user.id 和 loginUser.id 的id分别是",id,loginUser.getId());
        if (!isAdmin(loginUser) && id != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        User userResult = userMapper.selectById(id);
        if (userResult == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        int i = userMapper.updateById(user);

        return i;
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {

        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    /**
     * 匹配用户（推荐）
     *
     * @param num
     * @param loginUser
     * @return
     */
    @Override
    public List<User> matchUser(long num, User loginUser) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.last("limit 5000");
        //过滤掉标签为空的用户 ; 只查需要的数据（比如 id 和 tags）
        userQueryWrapper.isNotNull("tags");
        userQueryWrapper.select("id", "tags");
        List<User> userList = this.list(userQueryWrapper);
        //得到用户的标签列表
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());

        //下标相似度：key：表示用户列表下标(userList) ->> value:与当前用户相似度
        // SortedMap<Integer , Long> indexSimilarityMap = new TreeMap<>();
        List<Pair<User, Long>> list = new ArrayList<>();

        //将登录用户的标签和 数据库里面的所有用户匹配
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            //无标签
            if (StringUtils.isBlank(userTags) || Objects.equals(user.getId(), loginUser.getId())) {
                continue;
            }
            List<String> userTagsList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            //计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagsList);
            list.add(new Pair<>(user, distance));
        }

        // 先是根据求得的list ， 将它排序成 topPairUserList ， 取出它的id ， 根据 id查询完整信息（乱序） ， 重建一个list给它附上有序的值；返回
        //按编辑距离从小到大排序 ， 距离从小->大 : 相似度 从大->小
        List<Pair<User, Long>> topPairUserList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        //找到对应的用户id集合
        List<Long> userListVo = topPairUserList.stream().map(topList -> topList.getKey().getId()).collect(Collectors.toList());

        //根据id查询user完整信息 (乱序的)
        userQueryWrapper = new QueryWrapper<User>();
        userQueryWrapper.in("id" , userListVo);
        Map<Long, List<User>> userIdListMap = this.list(userQueryWrapper).stream().map(this::getSafetyUser).collect(Collectors.groupingBy(user -> user.getId()));

        //
        ArrayList<User> finalUserList = new ArrayList<>();
        for (long userId : userListVo){
            finalUserList.add(userIdListMap.get(userId).get(0));
        }
        //我希望返回的是一个脱敏过的用户
        return finalUserList;
    }

    /**
     * 根据标签名查询 用户 (SQL查询版本)
     *
     * @param tagsNameList 标签名列表
     * @return
     */
    @Deprecated
    private List<User> searchUserByTagsBySQL(List<String> tagsNameList) {
        if (CollectionUtils.isEmpty(tagsNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签为空");
        }
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        //
        for (String tagsName : tagsNameList) {
            // select * from user where tags like '%tags%' ;
            userQueryWrapper.like("tags", tagsName);
        }
        List<User> users = userMapper.selectList(userQueryWrapper);

//        将用户列表转化成流 | 对流中的每个元素应用 getSafetyUser方法 | 将转化后的流收集为一个新的列表
        return users.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }


}


package com.yupi.yupao.service.impl;

import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupao.common.ErrorCode;
import com.yupi.yupao.exception.BusinessException;
import com.yupi.yupao.model.domain.Team;
import com.yupi.yupao.model.domain.User;
import com.yupi.yupao.model.domain.UserTeam;
import com.yupi.yupao.model.dto.TeamQuery;
import com.yupi.yupao.model.enums.TeamStatusEnum;
import com.yupi.yupao.model.request.TeamJoinRequest;
import com.yupi.yupao.model.request.TeamQuitRequest;
import com.yupi.yupao.model.request.TeamUpdateRequest;
import com.yupi.yupao.model.vo.TeamUserVO;
import com.yupi.yupao.model.vo.UserVO;
import com.yupi.yupao.service.TeamService;
import com.yupi.yupao.mapper.TeamMapper;
import com.yupi.yupao.service.UserService;
import com.yupi.yupao.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author 唐子怡
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2024-03-24 10:30:15
 */
@Service

public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private UserTeamService userTeamService;
    @Resource
    private UserService userService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public long addTeam(Team team, User loginUser) {
//    1 请求参数是否为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//    2 是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final Long userId = loginUser.getId();

//    3 校验信息
//        a 队伍人数 > 1 且 <= 20  .如果getMaxNum()方法返回的值为null，则将0赋给maxNum变量。
        Integer maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍人数不符合要求 ");
        }
//        b 队伍名称 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍名称不符合要求 ");
        }
//        c 描述 <= 512 , 描述可以为null
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍描述不符合要求 ");
        }
//        d status 是否公开（int）不传默认为 0（公开）

        Integer status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
        if (enumByValue == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不符合要求");
        }
//        e 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(enumByValue)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不符合要求");
            }
        }
//        f 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (expireTime != null && new Date().after(expireTime)) {//现在的时间在超时时间之后 ,也就是已过期

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");
        }
//        g 校验用户最多创建 5 个队伍
        // todo 有 bug，可能同时创建 100 个队伍
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("userId", userId);
        long count = this.count();
        if (count > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍个数不符合要求");
        }
//        4 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "插入队伍失败");
        }
//        5 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        boolean save = userTeamService.save(userTeam);
        if (!save) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "插入用户失败");
        }
        return 0;
    }

    /**
     * 查询所有队伍列表
     *
     * @param teamQuery
     * @return
     */
    @Override
    public List<TeamUserVO> listTeam(TeamQuery teamQuery) {
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();

        if (teamQueryWrapper != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                teamQueryWrapper.eq("id", id);
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                teamQueryWrapper.like("name", name);
            }

            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                teamQueryWrapper.like("description", description);
            }
            //根据关键词搜索，在名称或描述里面搜索
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                teamQueryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                teamQueryWrapper.eq("maxNum", maxNum);
            }
            Integer status = teamQuery.getStatus();
            if (status != null && status > -1) {
                teamQueryWrapper.eq("status", status);
            }

//        // 不展示已过期的队伍
//        // expireTime is null or expireTime > now()
            teamQueryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
            List<Team> list = this.list(teamQueryWrapper);
            if (CollectionUtils.isEmpty(list)) {
                return new ArrayList<>();
            }

        }

        //通过查询条件 teamQueryWrapper 查询所有
        List<Team> teamList = this.list(teamQueryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }

        //最后返回给controller层的集合对象
        List<TeamUserVO> teamUserVOList = new ArrayList<>();

        //关联查询创建人用户信息
        for (Team team : teamList) {

            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            //获取 脱敏后的用户
            User user = userService.getById(userId);
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);

            //获取脱敏后的用户队伍，并给它关联上 队长 userVO
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            teamUserVO.setCreateUser(userVO);

            //
            teamUserVOList.add(teamUserVO);
        }

        return teamUserVOList;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //原始队伍
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //如果当前登录用户  不是管理员 且 不是队伍创建者
        if (oldTeam.getUserId() != loginUser.getId() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        //如果队伍状态为加密，必须要有密码
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());

        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码为空");
            }
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, team);
        return this.updateById(team);

    }

    /**
     * 用户加入队伍  (记得加上事务注解)
     *
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = loginUser.getId();
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        //1.user_team表，用户队伍关系表 里面 userId = 当前登录用户UserId 的个数不能超过五个
        long count = userTeamService.count(userTeamQueryWrapper);
        if (count > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加入队伍不能超过五个");
        }

        //2.队伍必须存在，只能加入未满、未过期的队伍
        Long teamId = teamJoinRequest.getTeamId();
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        //过期时间 在现在之前  ， 抛异常
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId).eq("teamId", teamId);
        long userTeamNum = userTeamService.count(userTeamQueryWrapper);
        if (userTeamNum > team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满员");
        }

        //3.不能加入自己的队伍，不能重复加入已加入的队伍（幂等性）
        if (userId.equals(team.getUserId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能加入自己的队伍");
        }

        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "禁止加入私有的队伍");
        }

        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());

        return userTeamService.save(userTeam);
    }

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        //2.
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        //3.
        Long userId = loginUser.getId();
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(userId);
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>(userTeam);
        long count = userTeamService.count(userTeamQueryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入该队伍");
        }

        //4.
        long hasTeamNum = getTeamNum(teamId);
        if (hasTeamNum == 1) {
            //队伍只有一人
            this.removeById(teamId);
            //删除关系

        }else{
            //队伍有至少两人
            //是队长
            if( team.getUserId() .equals( userId)){
                QueryWrapper<UserTeam> teamQueryWrapper = new QueryWrapper<>();
                teamQueryWrapper.eq("teamId", teamId);
                teamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> list = userTeamService.list(teamQueryWrapper);
                UserTeam userTeam1 = list.get(1);
                Team newTeam = new Team();
                newTeam.setUserId(userTeam1.getUserId());
                newTeam.setId(userTeam1.getTeamId());
                boolean b = this.updateById(newTeam);
                if(!b){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
                //在关系表中删除关系

            }
        }
        return userTeamService.remove(userTeamQueryWrapper);
    }


    /**
     * 解散队伍
     * @param teamId 传入的队伍id ， teamId
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public Boolean deleteTeam(long teamId, User loginUser) {

        Team team = this.getById(teamId);
        if(team == null ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        }

        //是队长，就可以解散队伍
        Long userId = loginUser.getId();
        if(team.getUserId().equals(userId)){
            UserTeam userTeam = new UserTeam();
            userTeam.setTeamId(teamId);
            QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>(userTeam);
            boolean remove = userTeamService.remove(userTeamQueryWrapper);
            if(!remove ){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除队伍关系失败");
            }
        }else {
            throw new BusinessException(ErrorCode.NO_AUTH,"无权限解散队伍");
        }
        return this.removeById(teamId);
    }



    /**
     * 获取队伍人数
     * @param teamId
     * @return
     */
    private long getTeamNum(long teamId){
        QueryWrapper<UserTeam> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("teamId", teamId);
       return userTeamService.count(teamQueryWrapper);
    }


}





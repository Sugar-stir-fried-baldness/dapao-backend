package com.yupi.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupao.common.ErrorCode;
import com.yupi.yupao.exception.BusinessException;
import com.yupi.yupao.model.domain.Team;
import com.yupi.yupao.model.domain.User;
import com.yupi.yupao.model.domain.UserTeam;
import com.yupi.yupao.model.enums.TeamStatusEnum;
import com.yupi.yupao.service.TeamService;
import com.yupi.yupao.mapper.TeamMapper;
import com.yupi.yupao.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

/**
* @author 唐子怡
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2024-03-24 10:30:15
*/
@Service

public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserTeamService userTeamService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public long addTeam(Team team, User loginUser) {
//    1 请求参数是否为空？
        if(team == null ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//    2 是否登录，未登录不允许创建
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final Long userId = loginUser.getId();
//    3 校验信息
//        a 队伍人数 > 1 且 <= 20  .如果getMaxNum()方法返回的值为null，则将0赋给maxNum变量。
        Integer maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if(maxNum < 1 || maxNum > 20){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍人数不符合要求 ");
        }
//        b 队伍名称 <= 20
        String name = team.getName();
        if(StringUtils.isBlank(name) || name.length() > 20){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍名称不符合要求 ");
        }
//        c 描述 <= 512 , 描述可以为null
        String description = team.getDescription();
        if(StringUtils.isNotBlank(description) && description.length() > 512){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍描述不符合要求 ");
        }
//        d status 是否公开（int）不传默认为 0（公开）

        Integer status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
        if( enumByValue == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍状态不符合要求");
        }
//        e 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if( TeamStatusEnum.SECRET .equals(enumByValue )){
            if(StringUtils.isBlank(password) || password.length() > 32){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码长度不符合要求");
            }
        }
//        f 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if(new Date().after(expireTime)){//现在的时间在超时时间之后
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"超时时间 > 当前时间");
        }
//        g 校验用户最多创建 5 个队伍
        // todo 有 bug，可能同时创建 100 个队伍
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq( "userId", userId);
        long count = this.count();
        if(count >= 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"创建队伍个数不符合要求");
        }
//        4 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if(!result || teamId == null ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"插入队伍失败");
        }
//        5 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        boolean save = userTeamService.save(userTeam);
        if(!save){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"插入用户失败");
        }
        return 0;
    }
}





package com.yupi.yupao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import com.yupi.yupao.common.BaseResponse;
import com.yupi.yupao.common.DeleteRequest;
import com.yupi.yupao.common.ErrorCode;
import com.yupi.yupao.common.ResultUtils;
import com.yupi.yupao.exception.BusinessException;
import com.yupi.yupao.model.domain.Team;
import com.yupi.yupao.model.domain.User;
import com.yupi.yupao.model.domain.UserTeam;
import com.yupi.yupao.model.dto.TeamQuery;
import com.yupi.yupao.model.request.TeamAddRequest;
import com.yupi.yupao.model.request.TeamJoinRequest;
import com.yupi.yupao.model.request.TeamQuitRequest;
import com.yupi.yupao.model.request.TeamUpdateRequest;
import com.yupi.yupao.model.vo.TeamUserVO;
import com.yupi.yupao.service.TeamService;
import com.yupi.yupao.service.UserService;

import com.yupi.yupao.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Slf4j
public class TeamController {

    @Resource(name = "")
    private UserService userService;
    @Resource
    private TeamService teamService;
    @Resource
    private UserTeamService userTeamService;


    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long teamId = teamService.addTeam(team, loginUser);

        return ResultUtils.success(teamId);
    }


    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);

        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询队伍失败");
        }
        return ResultUtils.success(team);
    }


    /**
     * 查询所有
     *
     * @param teamQuery
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //1.查询队伍列表
        List<TeamUserVO> teamList = teamService.listTeam(teamQuery, userService.isAdmin(request));
        List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        //2.判断当前用户是否已加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        try {
            User loginUser = userService.getLoginUser(request);
            userTeamQueryWrapper.eq("userId" , loginUser.getId());
            userTeamQueryWrapper.in("teamId",teamIdList);
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
            Set<Long> hasJoinTeamSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team->{
                boolean hasJoin = hasJoinTeamSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
        }catch (Exception e){}

        //3.查看已加入队伍人数
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId",teamIdList);
        List<UserTeam> userTeamJoinList = userTeamService.list(userTeamJoinQueryWrapper);
                //根据teamId 分组
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamJoinList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.forEach(team->{
            team.setHasJoinNum( teamIdUserTeamList.getOrDefault(team.getId() , new ArrayList<>()).size());
        });
        return ResultUtils.success(teamList);
    }


    /**
     * 查询所有
     *
     * @param teamQuery
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> page = new Page(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>(team);

        Page<Team> pageResult = teamService.page(page, teamQueryWrapper);
        return ResultUtils.success(pageResult);
    }


    /**
     * 用户加入队伍
     *
     * @param teamJoinRequest
     * @return
     */
    @PostMapping("/join")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = teamService.joinTeam(teamJoinRequest, loginUser);

        return ResultUtils.success(result);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = teamService.quitTeam(teamQuitRequest, loginUser);

        return ResultUtils.success(result);
    }

    /**
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if ( deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        Boolean result = teamService.deleteTeam(id, loginUser);

        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 获取当前用户创建的队伍
     *
     * @param teamQuery
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);

        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamList = teamService.listTeam(teamQuery, userService.isAdmin(request));
        return ResultUtils.success(teamList);
    }

    /**
     * 获取当前用户加入的队伍
     *
     * @param teamQuery
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //从UserTeam表中获取 队伍id
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);

        //将队伍id转化为一个列表 ,
        //取出不重复的 队伍id
        //teamId  userId
        //1 -> 2,3
        //2 -> 4
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        ArrayList<Long> idList = new ArrayList<>(listMap.keySet());

        teamQuery.setIdList(idList);
        List<TeamUserVO> teamList = teamService.listTeam(teamQuery , true);
        return ResultUtils.success(teamList);
    }


}

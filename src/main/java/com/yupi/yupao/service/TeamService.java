package com.yupi.yupao.service;

import com.yupi.yupao.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupao.model.domain.User;
import com.yupi.yupao.model.dto.TeamQuery;
import com.yupi.yupao.model.request.TeamJoinRequest;
import com.yupi.yupao.model.request.TeamQuitRequest;
import com.yupi.yupao.model.request.TeamUpdateRequest;
import com.yupi.yupao.model.vo.TeamUserVO;
import org.apache.ibatis.annotations.Param;


import java.util.List;


/**
 * @author 唐子怡
 * @description 针对表【team(队伍)】的数据库操作Service
 * @createDate 2024-03-24 10:30:15
 */
public interface TeamService extends IService<Team> {

    /**
     * 添加队伍
     *
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 查询所有队伍列表
     *
     * @param teamQuery
     * @return
     */
    List<TeamUserVO> listTeam(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍
     *
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest , User loginUser);

    /**
     * 用户加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    Boolean joinTeam(TeamJoinRequest teamJoinRequest ,User loginUser);

    /**
     * 用户退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    Boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 删除队伍
     * @param id
     * @param loginUser
     * @return
     */
    Boolean deleteTeam(@Param("id") long id, @Param("loginUser") User loginUser);
}

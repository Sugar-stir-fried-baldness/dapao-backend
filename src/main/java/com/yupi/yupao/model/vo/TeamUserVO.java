package com.yupi.yupao.model.vo;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author:tzy
 * @Description : 用户队伍封装类（脱敏）  --返回给前端
 * @Date:2024/3/268:40
 */
@Data
public class TeamUserVO implements Serializable {
    private static final long serialVersionUID = 8791428956020294645L;
    /**
     * 队伍id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 用户id（队长 id）
     */
    private Long userId;


    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;


    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

//    /**
//     * 已经加入队伍的成员（入队用户列表）
//     */
    /**
     * 创建人用户信息
     */
    private  UserVO createUser;

    /**
     * 已经加入队伍人数
     */
    private Integer hasJoinNum;
    /**
     * 是否已加入队伍
     */
    private boolean hasJoin = false;

}

package com.yupi.yupao.model.request;

// [编程学习交流圈](https://www.code-nav.cn/) 快速入门编程不走弯路！30+ 原创学习路线和专栏、500+ 编程学习指南、1000+ 编程精华文章、20T+ 编程资源汇总

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户退出请求体
 *
 */
@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 用户id
     */
    private Long userId;
    /**
     *  队伍Id
     */
    private Long teamId;


}

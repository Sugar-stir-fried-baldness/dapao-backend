package com.yupi.yupao.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author:tzy
 * @Description :通用分页请求参数
 * @Date:2024/3/259:32
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = -5860707094194210842L;
    /**
     * 页面大小
     */
    protected int pageSize = 10;

    /**
     * 当前是第几页
     */
    protected int pageNum = 1;

}

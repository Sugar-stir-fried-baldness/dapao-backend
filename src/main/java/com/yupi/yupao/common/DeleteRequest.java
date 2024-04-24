package com.yupi.yupao.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author:tzy
 * @Description :通用删除请求参数
 * @Date:2024/3/259:32
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = -5860707094194210842L;

    private long id;

}

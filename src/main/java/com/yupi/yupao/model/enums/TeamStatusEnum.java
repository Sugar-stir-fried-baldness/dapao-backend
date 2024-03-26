package com.yupi.yupao.model.enums;

/**
 * @Author:tzy
 * @Description : 队伍状态枚举
 * @Date:2024/3/2512:09
 */
public enum TeamStatusEnum {

    PUBLIC(0 , "公开"),
    PRIVATE(1,"私有"),
    SECRET(2,"加密");


    /**
     * 枚举值的编号
     */
    private int value;

    private String text;

    /**
     * 通过编号 获取 枚举值
     * @param value
     * @return
     */
    public static TeamStatusEnum getEnumByValue(Integer value){
        if(value == null){
            return null;
        }
        TeamStatusEnum[] values = TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : values) {
            if(teamStatusEnum.getValue() == value){
                return teamStatusEnum;
            }
        }
        return null;
    }

    TeamStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

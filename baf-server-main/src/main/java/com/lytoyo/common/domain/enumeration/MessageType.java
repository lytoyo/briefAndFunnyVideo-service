package com.lytoyo.common.domain.enumeration;

/**
 * Package:com.lytoyo.common.domain
 *
 * @ClassName:MessageType
 * @Create:2026/1/5 14:33
 **/
public enum MessageType {
    SYSTEMNOTICE(1,"系统公告"),
    FOLLOW(2,"关注推送"),
    CHAT(3,"聊天信息"),
    LIKED(4,"点赞信息"),
    ATTENTION(5,"关注信息"),
    COMMENT(6,"评论消息");

    private final Integer code;
    private final String description;



    MessageType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    // 通过code获取枚举
    public static MessageType fromCode(Integer code) {
        for (MessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid message type code: " + code);
    }

    // 通过name获取枚举
    public static MessageType fromName(String name) {
        try {
            return MessageType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid message type name: " + name);
        }
    }

}

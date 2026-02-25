package com.lytoyo.common.domain.enumeration;

/**
 * Package:com.lytoyo.common.domain.enumeration
 *
 * @ClassName:ContentType
 * @Create:2026/1/5 14:35
 **/
public enum ContentType {
    TEXT(1,"文本"),
    IMAGE(2,"图片"),
    VIDEO(3,"视频"),
    IMAGETEXT(4,"图文"),
    VIDEOTEXT(5,"视频文"),
    VOICE(6,"语音");

    private final Integer code;
    private final String description;

    ContentType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    // 检查是否为媒体类型
    public boolean isMediaType() {
        return this == IMAGE || this == VIDEO || this == VOICE;
    }

    // 获取支持的图片格式
    public static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
    public static final String[] VIDEO_EXTENSIONS = {".mp4", ".avi", ".mov", ".wmv", ".flv"};

    public String[] getAllowedExtensions() {
        switch (this) {
            case IMAGE: return IMAGE_EXTENSIONS;
            case VIDEO: return VIDEO_EXTENSIONS;
            case VOICE: return new String[]{".mp3", ".wav", ".aac", ".amr"};
            default: return new String[]{};
        }
    }
}

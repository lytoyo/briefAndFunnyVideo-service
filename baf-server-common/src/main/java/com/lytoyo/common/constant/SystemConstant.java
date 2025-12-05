package com.lytoyo.common.constant;

/**
 * Package:com.lytoyo.common.constant
 *
 * @ClassName:SystemConstant
 * @Create:2025/12/2 9:47
 **/

public class SystemConstant {
    public static String MATCH = "^(\\w+([-.][A-Za-z0-9]+)*){3,18}@\\w+([-.][A-Za-z0-9]+)*\\.\\w+([-.][A-Za-z0-9]+)*$";

    public static String FORMATERROR = "邮箱格式错误";
    public static String EMAILISEMPTY = "邮箱不能为空";

    public static String EMAILTITLE = "邮箱验证码";

    public static long TOKENEXPIRE = 60000*60*24*30;    //token30天有效期

    public static String TOKENSECRET = "COM.LYTOYO";    //token密钥

    public static String TOKENSUBJECT = "BRIEFANDFUNNY";    //token主题

    public static String SUCCESS = "操作成功"; //响应体成功msg信息体

    public static String IMFORMATIONLOSS = "注册信息缺失";

    public static String CODEERROR = "验证码无效";

    public static String USEREXIST = "用户已存在";

    public static String USERUNEXIST = "用户不存在";

    public static String PASSWORDNOTSAME = "密码不相同";

    public static String FUNNYNAME = "趣友";

    public static String LOGINIMFORMATIONLOSS = "登录信息缺失";

    public static String PASSWORDERROR = "密码错误";

    public static String ACCOUNTBANNED = "账号已被封禁";

}

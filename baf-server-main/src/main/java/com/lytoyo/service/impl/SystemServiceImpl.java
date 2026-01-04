package com.lytoyo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.extra.mail.Mail;
import cn.hutool.extra.mail.MailAccount;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lytoyo.common.constant.RedisConstant;
import com.lytoyo.common.constant.SystemConstant;
import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.User;
import com.lytoyo.common.domain.vo.UserVo;
import com.lytoyo.common.properties.EmailProperties;
import com.lytoyo.common.utils.CodeUtil;
import com.lytoyo.common.utils.JwtUtil;
import com.lytoyo.common.utils.PasswordUtil;
import com.lytoyo.service.SystemService;
import com.lytoyo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Package:com.lytoyo.service
 *
 * @ClassName:SystemServiceImpl
 * @Create:2025/12/2 9:31
 **/
@Service
@Slf4j
public class SystemServiceImpl implements SystemService {
    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private TemplateEngine templateEngine;

    @Autowired
    private EmailProperties emailProperties;

    @Resource
    private UserService userService;

    @Resource
    private PasswordUtil passwordUtil;


    /**
     * 获取邮箱验证码
     * @param email
     * @return
     */
    @Override
    public Result getCode(String email) {
        //验证邮箱格式
        if ((email != null) && (!email.isEmpty())){
            boolean matches = Pattern.matches(SystemConstant.MATCH, email);
            if (!matches) return Result.fail(SystemConstant.FORMATERROR);
        }else {
            return Result.fail(SystemConstant.EMAILISEMPTY);
        }
        //生成6位随机数字验证码
        String random = CodeUtil.generateSixDigitCode(true);
        //存入redis，有效期5分钟
        redisTemplate.opsForValue().set(RedisConstant.EMAILCODE+email,random,RedisConstant.CODEPASTDUE, TimeUnit.MINUTES);

        //生成邮件
        String content = generateEmailContent(random);
        //发送邮件
        List<String> list = Collections.singletonList(email);
        sendEmail(list, content);
        //返回成功标识

        return Result.success();
    }

    /**
     * 注册账户
     * @param user
     * @return
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public Result register(User user) {
        String code = (String) user.getParams().get("code");
        String again = (String) user.getParams().get("again");

        //是否缺失注册账户信息
        if (user.getEmail() == null && user.getPassword() == null
        && again == null && code == null){
            return Result.fail(SystemConstant.IMFORMATIONLOSS);
        }

        //是否两次密码输入不相同
        if (!user.getPassword().equals(again))
            return Result.fail(SystemConstant.PASSWORDNOTSAME);

        //判断邮箱格式是否正确
        if (!user.getEmail().isEmpty()){
            boolean matches = Pattern.matches(SystemConstant.MATCH, user.getEmail());
            if (!matches) return Result.fail(SystemConstant.FORMATERROR);
        }else {
            return Result.fail(SystemConstant.EMAILISEMPTY);
        }

        //判断验证码是否过期且是否正确
        String memoryCode = (String) redisTemplate.opsForValue().get(RedisConstant.EMAILCODE + user.getEmail());
        if (memoryCode != null && !memoryCode.equals(code))
            return Result.fail(SystemConstant.CODEERROR);

        //判断是否已有该账户（用email来判断） 是否被封禁
        User one = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, user.getEmail()));
        if (null != one) return Result.fail(SystemConstant.USEREXIST);
        else if (one.getStatus() == 2) return Result.fail(SystemConstant.ACCOUNTBANNED);

        //生成盐值、密码加密
        String salt = passwordUtil.generateSalt();
        String storePassword = passwordUtil.encryptPassword(user.getPassword(), salt);
        user.setSalt(salt)
            .setPassword(storePassword)
            .setCategory(2)
            .setUserName(SystemConstant.FUNNYNAME+code)
            .setSex(1)
            .setStatus(1);
        //数据插入
        this.userService.save(user);
        return Result.success();
    }

    /**
     * 用户登录
     * @param user
     * @return
     */
    @Override
    public Result login(User user) {
        //检查登录信息是否缺失
        if (user.getEmail() == null && user.getPassword() == null) return Result.fail(SystemConstant.LOGINIMFORMATIONLOSS);

        //检查邮箱格式是否正确
        if (!user.getEmail().isEmpty()){
            boolean matches = Pattern.matches(SystemConstant.MATCH, user.getEmail());
            if (!matches) return Result.fail(SystemConstant.FORMATERROR);
        }else {
            return Result.fail(SystemConstant.EMAILISEMPTY);
        }

        //通过email去获取到用户并校验密码是否正确
        User one = this.userService.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, user.getEmail()));
        if (null == one) return Result.fail(SystemConstant.USERUNEXIST);
        boolean exist = passwordUtil.verifyPassword(user.getPassword(), one.getPassword(), one.getSalt());
        if (!exist) return Result.fail(SystemConstant.PASSWORDERROR);

        //将用户基本信息存入redis，时效1小时
        UserVo userVo = new UserVo();
        BeanUtil.copyProperties(one,userVo);
        redisTemplate.opsForValue().set(RedisConstant.USER + one.getId(),userVo,RedisConstant.USEREPASTDUE,TimeUnit.HOURS);
        //将id使用token工具类生成token
        String token = JwtUtil.geneJsonWebToken(one.getId());
        //返回token
        //登录成功后将用户可见信息一起返回
        Map<String, Object> result = new HashMap<>();
        result.put("token",token);
        result.put("user",one);
        return Result.success(result);
    }


    /**
     * 生成邮件内容
     * @param captcha
     * @return
     */
    private String generateEmailContent(String captcha) {
        Context context = new Context();
        context.setVariable("verifyCode", Arrays.asList(captcha.split("")));
        return templateEngine.process("EmailVerificationCode.html", context);
    }

    /**
     * 发送邮件
     * @param list
     * @param content
     */
    private void sendEmail(List<String> list, String content) {
        MailAccount account = createMailAccount();

        try {
            Mail.create(account)
                    .setTos(list.toArray(new String[0]))
                    .setTitle(SystemConstant.EMAILTITLE)
                    .setContent(content)
                    .setHtml(true)
                    .setUseGlobalSession(false)
                    .send();
        } catch (Exception e) { // 捕获更广泛的异常
            throw e;
        }
    }


    /**
     * 邮件账号
     * @return
     */
    private MailAccount createMailAccount() {
        MailAccount account = new MailAccount();
        account.setAuth(true);
        account.setHost(emailProperties.getHost());
        account.setPort(emailProperties.getPort());
        account.setFrom(emailProperties.getUser());
        account.setUser(emailProperties.getUser());
        account.setPass(emailProperties.getPassword());
        account.setSslEnable(true);
        account.setStarttlsEnable(true);
        return account;
    }


}

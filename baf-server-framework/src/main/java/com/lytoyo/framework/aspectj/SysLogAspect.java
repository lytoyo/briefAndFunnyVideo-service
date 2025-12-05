package com.lytoyo.framework.aspectj;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import com.lytoyo.common.constant.RedisConstant;
import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.SysLogBo;
import com.lytoyo.common.domain.User;
import com.lytoyo.common.domain.vo.UserVo;
import com.lytoyo.common.exception.ExceptionEnum;
import com.lytoyo.common.utils.AuthContextHolder;
import com.lytoyo.common.utils.JwtUtil;
import com.lytoyo.framework.mapper.SystemLogMapper;
import com.lytoyo.framework.mapper.UserMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Package:com.lytoyo.framework.aspectj
 *
 * @ClassName:SysLogAspect
 * @Create:2025/12/4 15:48
 **/
@Aspect
@Component
public class SysLogAspect {
    @Resource
    private SystemLogMapper systemLogMapper;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private UserMapper userMapper;


    @Pointcut("@annotation(com.lytoyo.framework.aspectj.SysLog)")
    public void logPointCut() {
    }

    /**
     * 环绕通知 @Around
     *
     * @param point
     * @return
     * @throws Throwable
     */
    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        //从spring容器上下文请求对象中获取userId
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //httpServletRequest.getHeader("token")
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = servletRequestAttributes.getRequest();

        try {
            //获取请求头的Authorization
            String authorization = request.getHeader("Authorization");
            String[] headers = authorization.split(" ");
            String token = headers[1];
            Long id = JwtUtil.checkJWT(token);
            //需要登录的操作
            if (headers[0].equals("Bearer")) {
                //验证token是否过期
                if (null != id) {
                    //获取redis中的userInfo存入ThreadLocal中
                    UserVo userVo = (UserVo) redisTemplate.opsForValue().get(RedisConstant.USER + id);
                    //如果userVo为null，则在数据库中查找出来并存入redis,不为空也刷新时效
                    if (userVo == null) {
                        User user = this.userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, id));
                        BeanUtils.copyProperties(user, userVo, UserVo.class);
                    }
                    redisTemplate.opsForValue().set(RedisConstant.USER + userVo.getId(), userVo, RedisConstant.USEREPASTDUE, TimeUnit.HOURS);
                    AuthContextHolder.setUserVo(userVo);
                    AuthContextHolder.setUserId(userVo.getId());
                } else {
                    //如果token过期，则用户需要重新登录
                    return Result.error(ExceptionEnum.SIGNATURE_NOT_MATCH);
                }
            }
            //不需要登录或已验证登录
            long beginTime = System.currentTimeMillis();
            //方法执行
            Object result = point.proceed();
            long time = System.currentTimeMillis() - beginTime;
            saveLog(point, time);
            return result;
        } catch (Exception e) {
            return Result.error(ExceptionEnum.INTERNAL_SERVER_ERROR);
        } finally {
            AuthContextHolder.removeUserId();
            AuthContextHolder.removeUserVo();
        }
    }

    /**
     * 保存日志
     *
     * @param joinPoint
     * @param time
     */
    public void saveLog(ProceedingJoinPoint joinPoint, Long time) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SysLogBo sysLogBo = new SysLogBo();
        sysLogBo.setExeuTime(time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        sysLogBo.setCreateDate(dateFormat.format(new Date()));
        SysLog sysLog = method.getAnnotation(SysLog.class);
        if (sysLog != null) {
            //注解上的描述
            sysLogBo.setRemark(sysLog.value());
        }
        //请求的 类名、方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        sysLogBo.setClassName(className);
        sysLogBo.setMethodName(methodName);
        //请求的参数
        Object[] args = joinPoint.getArgs();
        try {
            List<String> list = new ArrayList<String>();
            for (Object o : args) {
                list.add(new Gson().toJson(o));
            }
            sysLogBo.setParams(list.toString());
        } catch (Exception e) {
        }
        this.systemLogMapper.insert(sysLogBo);

    }
}

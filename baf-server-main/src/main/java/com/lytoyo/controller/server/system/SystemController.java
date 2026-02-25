package com.lytoyo.controller.server.system;

import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.User;
import com.lytoyo.framework.aspectj.SysLog;
import com.lytoyo.service.SystemService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Package:com.lytoyo.controller.server.system
 *
 * @ClassName:SystemController
 * @Create:2025/12/2 9:33
 **/
@RestController
@RequestMapping("/server/system")
public class SystemController {
    @Resource
    private SystemService systemService;

    @SysLog(value = "获取邮箱验证码",require = true,needLogin = false)
    @GetMapping("/code")
    public Result getCode(@RequestParam("email") String email){
        Result r = systemService.getCode(email);
        return r;
    }

    @SysLog(value = "注册账户",require = true,needLogin = false)
    @PostMapping("/register")
    public Result register(@RequestBody User user){
        Result r = systemService.register(user);
        return r;
    }

    @SysLog(value = "用户登录",require = true,needLogin = false)
    @PostMapping("/login")
    public Result login(@RequestBody User user){
        Result r = systemService.login(user);
        return r;
    }

    @SysLog(value = "退出登录",require = true,needLogin = true)
    @PostMapping("/logoff")
    public Result logoff(@RequestParam("userId") Long userId){
        Result r = systemService.logoff(userId);
        return r;
    }
}

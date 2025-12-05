package com.lytoyo.controller.server.system;

import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.User;
import com.lytoyo.framework.aspectj.SysLog;
import com.lytoyo.service.SystemService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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

    @SysLog("获取邮箱验证码")
    @GetMapping("/code")
    public Result getCode(@RequestParam("email") String email){
        Result r = systemService.getCode(email);
        return r;
    }

    @SysLog("注册账户")
    @PostMapping("/register")
    public Result register(@RequestBody User user){
        Result r = systemService.register(user);
        return r;
    }

    @SysLog("用户登录")
    @PostMapping("/login")
    public Result login(@RequestBody User user){
        Result r = systemService.login(user);
        return r;
    }
}

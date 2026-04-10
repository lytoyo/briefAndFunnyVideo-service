package com.lytoyo.controller.console.auth;

import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.User;
import com.lytoyo.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Package:com.lytoyo.controller.console.auth
 *
 * @ClassName:ConsoleAuthController
 * @Create:2026/4/8 10:42
 **/
@RestController
@RequestMapping("/console/auth")
public class ConsoleAuthController {
    @Resource
    private UserService userService;

    @PostMapping("login")
    public Result login(@RequestBody User user){
        return userService.login(user);
    }
}

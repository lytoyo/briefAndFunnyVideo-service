package com.lytoyo.controller.console.user;

import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.User;
import com.lytoyo.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Package:com.lytoyo.controller.console
 *
 * @ClassName:ConsoleUserController
 * @Create:2025/11/21 15:00
 **/
@RestController
@RequestMapping("/console/user")
public class ConsoleUserController {
    @Resource
    private UserService userService;
    @GetMapping("/list")
    public Result UserList(@RequestParam("current")Integer current, @RequestParam("size")Integer size,
                           @RequestParam("userName")String userName,@RequestParam("status") Integer status,
                           @RequestParam("category") Integer category){
        return userService.userList(current,size,userName,status,category);
    }

    @PostMapping("/update")
    public Result update(@RequestBody User user){
        return userService.userUpdate(user);
    }


}

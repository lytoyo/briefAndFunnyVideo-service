package com.lytoyo.controller.server.user;

import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.User;
import com.lytoyo.framework.aspectj.SysLog;
import com.lytoyo.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.websocket.server.PathParam;

/**
 * Package:com.lytoyo.controller
 *
 * @ClassName:UserController
 * @Create:2025/11/21 14:56
 **/
@RestController
@RequestMapping("/server/user")
public class ServerUserController {

    @Resource
    private UserService userService;

    @GetMapping("/status")
    @SysLog(value = "获取用户状态等信息",require = false,needLogin = true)
    public Result status(@RequestParam("userId")Long userId){
        return userService.getStatus(userId);
    }

    @GetMapping("/attentionUserList")
    @SysLog(value = "分页获取关注的用户列表",require = true,needLogin = true)
    public Result attentionUserList(Long userId,Integer current,Integer size){
        return userService.attentionUserList(userId,current,size);
    }

    @GetMapping("/fansUserList")
    @SysLog(value = "分页获取粉丝用户列表",require = true,needLogin = true)
    public Result fansUserList(Long userId,Integer current,Integer size){
        return userService.fansUserList(userId,current,size);
    }

    @GetMapping("/askPost")
    @SysLog(value = "分页获取用户各项帖子列表",require = true,needLogin = true)
    public Result askPost(@RequestParam("userId") Long userId,@RequestParam("current") Integer current,
                          @RequestParam("size") Integer size){
        return userService.askPost(userId,current,size);
    }

    @GetMapping("/selfPostList")
    @SysLog(value = "分页获取用户发表的贴子",require = true,needLogin = true)
    public Result selfPostList(@RequestParam("userId")Long userId,@RequestParam("current") Integer current,
                               @RequestParam("size")Integer size){
        return userService.selfPostList(userId,current,size);
    }

    @GetMapping("/likedPostList")
    @SysLog(value = "分页获取用户点赞帖子列表",require = true,needLogin = true)
    public Result likedPostList(Long userId,Integer current,Integer size){
        return userService.likedPostList(userId,current,size);
    }



    @GetMapping("/collectPostList")
    @SysLog(value = "分页获取用户收藏的贴子",require = true,needLogin = true)
    public Result collectPostList(@RequestParam("userId")Long userId, @RequestParam("current") Integer current,
                                  @RequestParam("size")Integer size){
        return userService.collectPostList(userId,current,size);
    }

    @GetMapping("/otherUserDetail")
    @SysLog(value = "获取其他用户的信息",require = true)
    public Result otherUserDetail(@RequestParam("userId") Long userId){
        return this.userService.otherUserDetail(userId);
    }

    @GetMapping("/attentionUser")
    @SysLog(value = "关注其他用户",require = true)
    public Result attentionUser(@RequestParam("userId") Long userId){
        return this.userService.attentionUser(userId);
    }

    @GetMapping("/userInfo")
    @SysLog(value = "获取用户详情")
    public Result userInfo(@RequestParam("userId") Long userId){
        return this.userService.userInfo(userId);
    }

    @PostMapping("/saveUserInfo")
    @SysLog(value = "保存用户修改信息")
    public Result saveUserInfo(@RequestBody User user){
        return this.userService.saveUserInfo(user);
    }
}

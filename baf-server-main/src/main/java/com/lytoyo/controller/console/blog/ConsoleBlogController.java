package com.lytoyo.controller.console.blog;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lytoyo.common.domain.Blog;
import com.lytoyo.common.domain.Comment;
import com.lytoyo.common.domain.Result;
import com.lytoyo.service.BlogService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Package:com.lytoyo.controller.console.blog
 *
 * @ClassName:BlogController
 * @Create:2026/4/7 14:08
 **/
@RestController
@RequestMapping("/console/blog")
public class ConsoleBlogController {
    @Resource
    private BlogService blogService;


    @GetMapping("/list")
    public Result blogList(@RequestParam("current")Integer current, @RequestParam("size")Integer size,
                           @RequestParam("keyword")String keyword, @RequestParam("status") Integer status,
                           @RequestParam("fileType") String fileType){
        return blogService.listBlogs(current,size,keyword,status,fileType);
    }

    @GetMapping("/detail/{id}")
    public Result gainPostDetail(@PathVariable("id")Integer id){
        return blogService.gainPostDetail(id);
    }

    @PostMapping("/update")
    public Result update(@RequestBody Blog blog){
        if (blog.getStatus() == 1){
            blogService.approveBlog(blog.getId());
        }
        else{
            blogService.update(new UpdateWrapper<Blog>().eq("id",blog.getId()).set("status",blog.getStatus()));
        }
        return Result.success();
    }

    @GetMapping("/comment/list")
    public Result commentList(@RequestParam("current")Integer current, @RequestParam("size")Integer size,
                              @RequestParam("postId")Long postId,@RequestParam("commentUserId") Long commentUserId,
                              @RequestParam("type")Integer type){
        return this.blogService.commentList(current,size,postId,commentUserId,type);
    }

    @GetMapping("/comment/detail/{id}")
    public Result commentDetail(@PathVariable("id")Long id){
        return this.blogService.commentDetail(id);
    }

    @PostMapping("/comment/update/status")
    public Result commentUpdateStatus(@RequestBody Comment comment){
        return this.blogService.commentUpdateStatus(comment);
    }
}

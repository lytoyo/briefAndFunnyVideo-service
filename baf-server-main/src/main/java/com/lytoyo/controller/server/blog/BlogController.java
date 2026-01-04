package com.lytoyo.controller.server.blog;

import com.lytoyo.common.domain.Blog;
import com.lytoyo.common.domain.Result;
import com.lytoyo.framework.aspectj.SysLog;
import com.lytoyo.service.BlogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Package:com.lytoyo.controller.server.blog
 *
 * @ClassName:BlogController
 * @Create:2025/12/22 11:29
 **/
@RestController
@RequestMapping("/server/blog")
public class BlogController {

    @Resource
    private BlogService blogService;

    @SysLog(value = "博客上传",require = true)
    @PostMapping("/uploadBlog")
    public Result uploadBlog(@RequestBody Blog blog){
        return this.blogService.uploadBlog(blog);
    }

    @SysLog(value = "博客审核通过",require = true)
    @PostMapping("/approve")
    public Result approveBlog(Long id){
        return this.blogService.approveBlog(id);
    }
}

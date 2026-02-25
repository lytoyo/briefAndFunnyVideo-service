package com.lytoyo.controller.server.blog;

import com.lytoyo.common.domain.Blog;
import com.lytoyo.common.domain.Comment;
import com.lytoyo.common.domain.Liked;
import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.vo.BlogVo;
import com.lytoyo.framework.aspectj.SysLog;
import com.lytoyo.service.BlogService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

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

    @SysLog(value = "博客上传",require = true,needLogin = true)
    @PostMapping("/uploadBlog")
    public Result uploadBlog(@RequestBody Blog blog){
        return this.blogService.uploadBlog(blog);
    }

    @SysLog(value = "博客审核通过",require = true,needLogin = true)
    @PostMapping("/approve")
    public Result approveBlog(Long id){
        return this.blogService.approveBlog(id);
    }

    @SysLog(value = "分页获取贴子",require = true)
    @GetMapping("/postList")
    public Result postList(@RequestParam("type")String type,@RequestParam("current") Integer current,
                           @RequestParam("size") Integer size){
        return this.blogService.postList(type,current,size);
    }


    @SysLog(value = "点赞处理",require = true)
    @PostMapping("/likedHandle")
    public Result likedHandle(@RequestBody BlogVo blogVo){
        return this.blogService.likedHandle(blogVo);
    }

    @SysLog(value = "收藏处理",require = true)
    @PostMapping("/collectHandle")
    public Result collectHandle(@RequestBody BlogVo blogVo){
        return this.blogService.collectHandle(blogVo);
    }

    @SysLog(value = "获取贴子详情",require = true)
    @GetMapping("/gainPostDetail")
    public Result gainPostDetail(@RequestParam Integer id){
        return this.blogService.gainPostDetail(id);
    }

    @SysLog(value = "分页获取视频评论",require = true)
    @GetMapping("/gainPostComment")
    public Result gainPostComment(@RequestParam("current")Integer current,@RequestParam("size")Integer size,
                                  @RequestParam("type")Integer type,@RequestParam("postId")Long postId,
                                  @RequestParam("parentId") Long parentId){
        return this.blogService.gainPostComment(current,size,type,postId,parentId);
    }

    @PostMapping("/commentLikedHandle")
    @SysLog(value = "评论点赞处理",require = true)
    public Result commentLikedHandle(@RequestBody Liked liked){
        return this.blogService.commentLikedHandle(liked);
    }

    @GetMapping("/childCommentList")
    @SysLog(value = "获取子评论列表",require = true)
    public Result childCommentList(@RequestParam("current")Integer current,@RequestParam("size")Integer size,
                                   @RequestParam("parentId")Long parentId){
        return this.blogService.childCommentList(current,size,parentId);
    }

    @PostMapping("/comment")
    @SysLog(value = "帖子评论")
    public Result comment(@RequestBody Comment comment){
        return this.blogService.comment(comment);
    }
}

package com.lytoyo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lytoyo.common.domain.Blog;
import com.lytoyo.common.domain.Result;

/**
 * Package:com.lytoyo.service
 *
 * @ClassName:BlogService
 * @Create:2025/12/22 11:30
 **/
public interface BlogService extends IService<Blog> {
    /**
     * 上传博客
     * @param blog
     * @return
     */
    Result uploadBlog(Blog blog);

    /**
     * 博客审核通过
     * @param id
     * @return
     */
    Result approveBlog(Long id);
}

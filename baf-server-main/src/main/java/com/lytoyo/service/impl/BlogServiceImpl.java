package com.lytoyo.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lytoyo.common.constant.RabbitMqConstant;
import com.lytoyo.common.domain.Blog;
import com.lytoyo.common.domain.Result;
import com.lytoyo.common.repository.BlogRepository;
import com.lytoyo.common.utils.AuthContextHolder;
import com.lytoyo.common.utils.RabbitMqUtil;
import com.lytoyo.mapper.BlogMapper;
import com.lytoyo.service.BlogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Package:com.lytoyo.service.impl
 *
 * @ClassName:BlogServiceImpl
 * @Create:2025/12/22 11:30
 **/
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements BlogService {

    @Resource
    private RabbitMqUtil rabbitMqUtil;

    @Resource
    private BlogRepository blogRepository;

    /**
     * 博客上传
     *
     * @param
     * @return
     */
    @Override
    public Result uploadBlog(Blog blog) {
        Long userId = AuthContextHolder.getUserId();
        //刚上传的博客需要审核
        blog.setUserId(userId)
            .setStatus(2)
            .setCommentCount(0)
            .setLikeCount(0)
            .setViewCount(0)
            .setShareCount(0)
            .setPublishTime(DateUtil.date());
        this.save(blog);
        return Result.success();
    }

    @Override
    public Result approveBlog(Long id) {
        Blog blog = this.getById(id);
        blog.setStatus(1);
        this.uploadBlog(blog);

        //将博客上传到elasticsearch
        this.blogRepository.save(blog);


        return Result.success();
    }


}

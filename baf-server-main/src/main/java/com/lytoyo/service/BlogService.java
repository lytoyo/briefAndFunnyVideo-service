package com.lytoyo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lytoyo.common.domain.*;
import com.lytoyo.common.domain.vo.BlogVo;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;
import java.util.Map;

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

    /**
     * 关键字补全
     *
     * @param keyword
     * @return
     */
    SearchHits<Blog> keywordComplement(String keyword);

    /**
     * 关键字综合查询
     * @param keyword
     * @return
     */
    Map<String,SearchHits> comprehensiveSearch(String keyword);

    /**
     * 关键字分页查询
     * @param keyword
     * @param current
     * @param size
     * @return
     */
    SearchHits<Blog> postQueries(String keyword, Integer current, Integer size);

    /**
     * 关键词帖子分类分页查询
     * @param keyword
     * @param type
     * @param current
     * @param size
     * @return
     */
    SearchHits<Blog> categoryQueries(String keyword, String type, Integer current, Integer size);

    /**
     * 关键词用户分页查询
     * @param keyword
     * @param current
     * @param size
     * @return
     */
    SearchHits<User> userQuries(String keyword, Integer current, Integer size);

    /**
     * 获取贴子列表
     * @param type
     * @param current
     * @param size
     * @return
     */
    Result postList(String type,Integer current, Integer size);

    /**
     * 点赞处理
     * @param blogVo
     * @return
     */
    Result likedHandle(BlogVo blogVo);

    /**
     * 收藏处理
     * @param blogVo
     * @return
     */
    Result collectHandle(BlogVo blogVo);

    /**
     * 获取贴子详情
     * @param id
     * @return
     */
    Result gainPostDetail(Integer id);

    /**
     * 分页获取帖子评论
     * @param current
     * @param size
     * @param type
     * @param postId
     * @param parentId
     * @return
     */
    Result gainPostComment(Integer current, Integer size, Integer type,Long postId,Long parentId);

    /**
     * 评论点赞处理
     * @param liked
     * @return
     */
    Result commentLikedHandle(Liked liked);

    /**
     * 获取子评论列表
     * @param current
     * @param size
     * @param parentId
     * @return
     */
    Result childCommentList(Integer current, Integer size, Long parentId);

    /**
     * 帖子评论
     * @param comment
     * @return
     */
    Result comment(Comment comment);
}

package com.lytoyo.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lytoyo.common.constant.RabbitMqConstant;
import com.lytoyo.common.domain.*;
import com.lytoyo.common.domain.vo.BlogVo;
import com.lytoyo.common.domain.vo.CommentVo;
import com.lytoyo.common.domain.vo.UserVo;
import com.lytoyo.common.exception.ExceptionEnum;
import com.lytoyo.common.properties.MinioProperties;
import com.lytoyo.mapper.*;
import com.lytoyo.repository.BlogRepository;
import com.lytoyo.common.utils.AuthContextHolder;
import com.lytoyo.common.utils.RabbitMqUtil;
import com.lytoyo.service.BlogService;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

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

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private MinioProperties minioProperties;

    @Resource
    private BlogMapper blogMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private LikedMapper likedMapper;

    @Resource
    private CollectMapper collectMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private AttentionMapper attentionMapper;

    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 博客上传
     *
     * @param
     * @return
     */
    @Override
    public Result uploadBlog(Blog blog) {

        //穿来的博客有三种形式：1、只有文本没有文件名，
        //                  2、文本可有可无，有图片文件，1~9张
        //                  3、文本可有可无，有视频文件
        Long userId = AuthContextHolder.getUserId();

        String cover = null;
        //只有文本

        if (blog.getFileName() != null){
            //可能是多张图片文件名称组成的文件名
            String[] fileNames = blog.getFileName().split(",");
            FileInfo fileInfo = fileMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileName, fileNames[0]));
            cover = fileInfo.getCover() == null ? fileInfo.getFileName() : fileInfo.getCover();

        }
        //刚上传的博客需要审核
        blog.setUserId(userId)
                .setStatus(2)
                .setCommentCount(0)
                .setLikeCount(0)
                .setCover(cover)
                .setViewCount(0)
                .setCollectCount(0)
                .setPublishTime(DateUtil.date());
        this.save(blog);

        return Result.success();
    }

    @Override
    public Result approveBlog(Long id) {
        Blog blog = this.getById(id);
        blog.setStatus(1);
        this.blogMapper.updateById(blog);

        //将博客上传到elasticsearch
        this.rabbitMqUtil.sendBlogToElasticsearch(RabbitMqConstant.DIRECTEXCHANGE, RabbitMqConstant.BLOG_UP_ROUTINGKEY, blog);

        return Result.success();
    }

    /**
     * 关键字补全
     *
     * @param keyword
     * @return
     */
    @Override
    public Result keywordComplement(String keyword) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "content", "tag"))
                .withHighlightFields(
                        new HighlightBuilder.Field("content").preTags("<em style='color: red;'>").postTags("</em>"),
                        new HighlightBuilder.Field("tag").preTags("<em>").postTags("</em>")
                ).build();
        List<BlogVo> blogVoSearchList = new ArrayList<>();
        SearchHits<Blog> result = elasticsearchRestTemplate.search(searchQuery, Blog.class);
        if (result.getTotalHits() > 0){
            List<SearchHit<Blog>> searchHits = result.getSearchHits();
             blogVoSearchList = searchHits.stream().map(blogSearchHit -> {
                Blog blog = blogSearchHit.getContent();
                BlogVo blogVo = new BlogVo();
                blogVo.setId(blog.getId());
                blogVo.setContent(blogSearchHit.getHighlightField("content").get(0));
                return blogVo;
            }).collect(Collectors.toList());
        }
        return Result.success(blogVoSearchList);
    }

    /**
     * 关键字综合查询
     *
     * @param keyword
     * @return
     */
    @Override
    public Result comprehensiveSearch(String keyword) {
        HashMap<String, Object> result = new HashMap<>();
        //帖子相关
        NativeSearchQuery blogSearchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "content", "tag"))
                .withPageable(PageRequest.of(0, 5))
                .withHighlightFields(
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("tag").preTags("<em>").postTags("</em>")
                ).build();
        SearchHits<Blog> blogResult = elasticsearchRestTemplate.search(blogSearchQuery, Blog.class);
        List<BlogVo> searchContents = new ArrayList<>();
        if (blogResult.getTotalHits() > 0){
            Map<Long, UserVo> userVoMap = new HashMap<>();
            //获取用户userVo信息
            blogResult.getSearchHits().stream().forEach(blogSearchHit -> {
                UserVo userVo = new UserVo();
                Long userId = blogSearchHit.getContent().getUserId();
                if(userVoMap.get(userId) == null) {
                    User user = this.userMapper.selectById(userId);
                    BeanUtils.copyProperties(user,userVo);
                    userVoMap.put(userId,userVo);
                }
            });

            searchContents = blogResult.getSearchHits().stream().map(blogSearchHit -> {
                BlogVo blogVo = new BlogVo();
                Blog blog = blogSearchHit.getContent();
                BeanUtils.copyProperties(blog, blogVo);
                blogVo.setContent(blogSearchHit.getHighlightField("content").get(0));
                if (blogVo.getFileType().equals("image")){
                    String imageFileNames = Arrays.stream(blogVo.getFileName().split(",")).map(fileName -> {
                        return this.minioProperties.getUrl() + fileName;
                    }).collect(Collectors.joining(","));
                    blogVo.setFileName(imageFileNames);
                }else{
                    blogVo.setFileName(this.minioProperties.getUrl() + blogVo.getFileName());
                }
                blogVo.setCover(this.minioProperties.getUrl() + blogVo.getCover());
                UserVo userVo = userVoMap.get(blogVo.getUserId());

                blogVo.setUserId(userVo.getId());
                blogVo.setUserCategory(userVo.getCategory());
                blogVo.setUserName(userVo.getUserName());
                blogVo.setUserAvatar(this.minioProperties.getUrl() + userVo.getAvatar());
                blogVo.setUserStatus(userVo.getStatus());

                return blogVo;
            }).collect(Collectors.toList());
        }
        result.put("searchBlogContents",searchContents);
        return Result.success(result);
    }

    /**
     * 关键词分页查询
     *
     * @param keyword
     * @param current
     * @param size
     * @return
     */
    @Override
    public SearchHits<Blog> postQueries(String keyword, Integer current, Integer size) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "content", "tag"))
                .withPageable(PageRequest.of(current, size))
                .withHighlightFields(
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("tag").preTags("<em>").postTags("</em>")
                )
                .withPageable(PageRequest.of(current, size))
                .build();
        SearchHits<Blog> result = elasticsearchRestTemplate.search(searchQuery, Blog.class);
        return result;
    }

    /**
     * 关键词帖子分类分页查询
     *
     * @param keyword
     * @param type
     * @param current
     * @param size
     * @return
     */
    @Override
    public SearchHits<Blog> categoryQueries(String keyword, String type, Integer current, Integer size) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "content", "tag"))
                .withPageable(PageRequest.of(current, size))
                .withFilter(QueryBuilders.termQuery("type", type))
                .withHighlightFields(
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("tag").preTags("<em>").postTags("</em>")
                )
                .build();
        SearchHits<Blog> result = elasticsearchRestTemplate.search(searchQuery, Blog.class);
        return result;
    }

    /**
     * 关键词用户分页查询
     *
     * @param keyword
     * @param current
     * @param size
     * @return
     */
    @Override
    public SearchHits<User> userQuries(String keyword, Integer current, Integer size) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "userName"))
                .withPageable(PageRequest.of(current, size))
                .withHighlightFields(new HighlightBuilder.Field("userName").preTags("<em>").postTags("</em>"))
                .build();
        SearchHits<User> result = elasticsearchRestTemplate.search(searchQuery, User.class);
        return result;
    }

    /**
     * 分页获取贴子列表
     * @param type
     * @param current
     * @param size
     * @return
     */
    @Override
    public Result postList(String type,Integer current,Integer size) {
        HashMap<String, Object> result = new HashMap<>();
        List<BlogVo> blogVoList = new ArrayList<>();

        Page<Blog> page = new Page<>(current, size);
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq(!type.equals(""),"file_type",type);
        queryWrapper.orderByDesc("publish_time");
        Page<Blog> blogPageResult = this.blogMapper.selectPage(page, queryWrapper);

        if (blogPageResult.getTotal() > 0) {
            List<Blog> records = blogPageResult.getRecords();
            //获取用户列表id同时注入博客信息
            List<Long> userIdList = records.stream().map(blog -> {
                BlogVo blogVo = new BlogVo();
                BeanUtils.copyProperties(blog, blogVo);
                //如果类型是图片，给每张图片添加ip前缀
                if (blog.getFileType().equals("image")) {
                    //图片名称
                    List<String> fileNameList = Arrays.asList(blog.getFileName().split(","));
                    blogVo.setUrl(fileNameList.stream().map(fileName -> {
                        return minioProperties.getUrl() + fileName;
                    }).collect(Collectors.joining(",")));
                } else {
                    blogVo.setUrl(this.minioProperties.getUrl() + blogVo.getFileName());
                }
                blogVo.setCover(minioProperties.getUrl() + blogVo.getCover());
                blogVo.setIsLiked(false);
                blogVo.setIsCollect(false);
                blogVoList.add(blogVo);
                return blog.getUserId();
            }).distinct().collect(Collectors.toList());

            //获取用户信息并注入
            this.userMapper.selectBatchIds(userIdList).stream().forEach(user -> {
                for (BlogVo blogVo : blogVoList) {
                    if (blogVo.getUserId().equals(user.getId())) {
                        blogVo.setUserCategory(user.getCategory());
                        blogVo.setUserName(user.getUserName());
                        blogVo.setUserStatus(user.getStatus());
                        blogVo.setUserAvatar(minioProperties.getUrl() + user.getAvatar());
                    }
                }
            });
            //获取当前用户对贴子的点赞和收藏记录并做填充
            Long userId = AuthContextHolder.getUserId();
            if (null != userId) {
                this.likedMapper.selectList(new LambdaQueryWrapper<Liked>().eq(Liked::getLikedUserId, userId)
                                .eq(Liked::getType,1)
                        .eq(Liked::getStatus, 1)
                        .in(Liked::getObjId, blogVoList.stream().map(blogVo -> {
                            return blogVo.getId();
                        }).collect(Collectors.toList()))).forEach(liked -> {
                    for (BlogVo blogVo : blogVoList) {
                        if (liked.getObjId().equals(blogVo.getId())) {
                            blogVo.setIsLiked(true);
                            break;
                        }
                    }
                });

                this.collectMapper.selectList(new LambdaQueryWrapper<Collect>().eq(Collect::getCollectUserId, userId)
                        .eq(Collect::getStatus, 1)
                        .in(Collect::getPostId, blogVoList.stream().map(blogVo -> {
                            return blogVo.getId();
                        }).collect(Collectors.toList()))).forEach(collect -> {
                    for (BlogVo blogVo : blogVoList) {
                        if (collect.getPostId().equals(blogVo.getId())) {
                            blogVo.setIsCollect(true);
                            break;
                        }
                    }
                });
            }

            List<String> fileName = blogVoList.stream().filter(blogVo -> {
                return blogVo.getFileType().equals("video") ? true : false;
            }).map(blogVo -> {
                return blogVo.getFileName();
            }).collect(Collectors.toList());

            this.fileMapper.selectList(new LambdaQueryWrapper<FileInfo>().in(FileInfo::getFileName, fileName))
                    .stream().forEach(fileInfo -> {
                        for (BlogVo blogVo : blogVoList) {
                            if (fileInfo.getFileName().equals(blogVo.getFileName())) {
                                blogVo.setDuration(fileInfo.getDuration());
                                break;
                            }
                        }
                    });
        }

        result.put("postList", blogVoList);
        result.put("postPages", blogPageResult.getPages());
        result.put("postTotal", blogPageResult.getTotal());
        result.put("postCurrent", blogPageResult.getCurrent());

        return Result.success(result);
    }

    /**
     * 点赞处理
     * @param blogVo
     * @return
     */
    @Override
    @Transactional
    public Result likedHandle(BlogVo blogVo) {
        Long userId = AuthContextHolder.getUserId();
        //查询是否有该点赞记录
        Liked liked = this.likedMapper.selectOne(new LambdaQueryWrapper<Liked>()
                        .eq(Liked::getType,1)
                .eq(Liked::getObjId, blogVo.getId())
                .eq(Liked::getLikedUserId, userId));
        //有则修改
        if (null != liked){
            liked.setStatus(blogVo.getIsLiked()?1:0);
            this.likedMapper.updateById(liked);
        }
        //没有则添加
        else{
            liked = new Liked();
            liked.setObjId(blogVo.getId());
            liked.setLikedUserId(userId);
            liked.setType(1);
            liked.setTimestamp(System.currentTimeMillis());
            liked.setStatus(1);
            this.likedMapper.insert(liked);
        }
        //todo 这里后续修改为使用redis存取，定时任务修改数据库
        //增减点赞数量
        String sql = liked.getStatus() == 1 ? "like_count = like_count + 1" : "like_count = like_count - 1";
        UpdateWrapper<Blog> wrapper = new UpdateWrapper<>();
        wrapper.eq("id",blogVo.getId()).setSql(sql);
        this.update(wrapper);

        return Result.success(liked);
    }

    /**
     * 收藏处理
     * @param blogVo
     * @return
     */
    @Transactional
    @Override
    public Result collectHandle(BlogVo blogVo) {
        Long userId = AuthContextHolder.getUserId();
        //查询是否有收藏记录
        Collect collect = this.collectMapper.selectOne(new LambdaQueryWrapper<Collect>()
                .eq(Collect::getPostId, blogVo.getId())
                .eq(Collect::getCollectUserId, userId));
        if (null != collect){
            collect.setStatus(blogVo.getIsCollect() ? 1 : 0);
            this.collectMapper.updateById(collect);
        }
        else{
            collect = new Collect();
            collect.setPostId(blogVo.getId());
            collect.setCollectUserId(userId);
            collect.setTimestamp(System.currentTimeMillis());
            collect.setStatus(1);
            this.collectMapper.insert(collect);
        }
        //todo 这里后续修改位使用redis存取，定时任务修改数据库
        //增删收藏数量
        String sql = collect.getStatus() == 1 ? "collect_count = collect_count + 1" : "collect_count = collect_count - 1";
        UpdateWrapper<Blog> wrapper = new UpdateWrapper<>();
        wrapper.eq("id",blogVo.getId()).setSql(sql);
        this.update(wrapper);
        return Result.success(collect);
    }

    /**
     * 获取贴子详情
     * @param id
     * @return
     */
    @Override
    public Result gainPostDetail(Integer id) {
        if (id == null || id == 0) return Result.error(ExceptionEnum.BODY_NOT_MATCH);

        Blog blog = this.getById(id);
        if (blog == null) return Result.error(ExceptionEnum.NOT_FOUND);

        BlogVo blogVo = new BlogVo();
        BeanUtils.copyProperties(blog,blogVo);

        //判断贴子类型，为其添加url前缀，如果是图片则为每个图片名称添加url前缀
        if (blogVo.getFileType().equals("image")){
            blogVo.setUrl(Arrays.stream(blogVo.getFileName().split(",")).map(fileName -> {
                return this.minioProperties.getUrl() + fileName;
            }).collect(Collectors.joining(",")));
        }else{
            blogVo.setUrl(this.minioProperties.getUrl() + blogVo.getFileName());
        }

        //获取用户信息并填入
        User user = this.userMapper.selectById(blogVo.getUserId());
        blogVo.setUserCategory(user.getCategory());
        blogVo.setUserStatus(user.getStatus());
        blogVo.setUserAvatar(this.minioProperties.getUrl() + user.getAvatar());
        blogVo.setCover(this.minioProperties.getUrl() + blogVo.getCover());
        blogVo.setUserName(user.getUserName());
        //判断当前用户是否点赞收藏
        Liked liked = this.likedMapper.selectOne(new LambdaQueryWrapper<Liked>().eq(Liked::getObjId, id).eq(Liked::getType,1).eq(Liked::getStatus, 1));
        Collect collect = this.collectMapper.selectOne(new LambdaQueryWrapper<Collect>().eq(Collect::getPostId, id).eq(Collect::getStatus, 1));
        blogVo.setIsLiked(liked == null ? false : true);
        blogVo.setIsCollect(collect == null ? false : true);

        return Result.success(blogVo);
    }

    /**
     * 分页获取帖子评论
     * @param current
     * @param size
     * @param type 1-全部评论 2-只看楼主 3-视频评论
     * @param postId
     * @param parentId  是否查询父评论，如果是则为0，不是则为父评论id
     * @return
     */
    @Override
    public Result gainPostComment(Integer current, Integer size, Integer type,Long postId,Long parentId) {
        HashMap<String, Object> result = new HashMap<>();
        Page<Comment> commentPage = new Page<>(current, size);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId,postId);
        wrapper.eq(Comment::getParentId,parentId);
        Blog blog = this.blogMapper.selectById(postId);
        //是否只查看楼主评论或只获取视频类型评论
        if (type == 2) {
            wrapper.eq(Comment::getCommentUserId, blog.getUserId());
        }else if (type == 3){
            wrapper.eq(Comment::getFileType,"video");
        }
        Page<Comment> commentPageResult = this.commentMapper.selectPage(commentPage, wrapper);
        Long userId = AuthContextHolder.getUserId();

        List<Liked> likedList = this.likedMapper.selectList(new LambdaQueryWrapper<Liked>()
                .in(!commentPageResult.getRecords().isEmpty(),Liked::getObjId,commentPageResult.getRecords().stream().map(f->{
                    return f.getId();
                }).collect(Collectors.toList()))
                .eq(userId != null,Liked::getLikedUserId,userId)
                .eq(Liked::getStatus,1));
        List<CommentVo> commentList = commentPageResult.getRecords().stream().map(comment -> {
            CommentVo commentVo = new CommentVo();
            commentVo.setIsPoster(false);
            commentVo.setIsLiked(false);
            BeanUtils.copyProperties(comment, commentVo);
            //是否是楼主言论
            if (blog.getUserId().equals(commentVo.getCommentUserId())) {
                commentVo.setIsPoster(true);
            }
            //当前用户是否点过赞
            for (Liked liked : likedList) {
                if (commentVo.getCommentUserId().equals(liked.getLikedUserId())
                        && commentVo.getId().equals(liked.getObjId())
                        && liked.getType().equals(2)) {
                    commentVo.setIsLiked(true);
                    break;
                }
            }

            //该评论有多少子评论
            Long count = this.commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, comment.getId()));
            commentVo.setChildCommentCount(count);

            UserVo userVo = new UserVo();
            User user = this.userMapper.selectById(commentVo.getCommentUserId());
            user.setAvatar(this.minioProperties.getUrl() + user.getAvatar());
            BeanUtils.copyProperties(user,userVo);
            commentVo.setUserVo(userVo);
            if (commentVo.getFileName() != null){
                String fileName = null;
                if (commentVo.getFileType().equals("image")){
                    fileName = Arrays.stream(commentVo.getFileName().split(",")).map(f -> {
                        return this.minioProperties.getUrl() + f;
                    }).collect(Collectors.joining(","));
                }else{
                    fileName = this.minioProperties.getUrl() + commentVo.getFileName();
                    if (commentVo.getCover() != null){
                        commentVo.setCover(this.minioProperties.getUrl() + commentVo.getCover());
                    }
                }
                commentVo.setFileName(fileName);
            }
            return commentVo;
        }).collect(Collectors.toList());
        result.put("current",commentPageResult.getCurrent());
        result.put("pages",commentPageResult.getPages());
        result.put("total",commentPageResult.getTotal());
        result.put("commentList",commentList);
        return Result.success(result);
    }

    /**
     * 评论点赞处理
     * @param liked
     * @return
     */
    @Transactional
    @Override
    public Result commentLikedHandle(Liked liked) {
        Long userId = AuthContextHolder.getUserId();
        if (null == userId) return Result.error(ExceptionEnum.SIGNATURE_NOT_MATCH);
        //查询该评论是否还存在
        Comment comment = this.commentMapper.selectOne(new LambdaQueryWrapper<Comment>().eq(Comment::getId, liked.getObjId()));
        if (comment == null) return Result.error(502,"该评论已不存在");
        //先查询该点赞记录是否存在
        Liked one = this.likedMapper.selectOne(new LambdaQueryWrapper<Liked>().eq(Liked::getObjId,liked.getObjId()));
        //不存在直接补充liked并插入
        if (null == one){
            liked.setType(2);
            liked.setStatus(1);
            liked.setLikedUserId(userId);
            liked.setTimestamp(System.currentTimeMillis());
            this.likedMapper.insert(liked);
            BeanUtils.copyProperties(liked,one);
        }
        else {
            //存在则补充
            one.setStatus(liked.getStatus());
            this.likedMapper.updateById(one);
        }

        //todo 这里后续修改为使用redis存取，定时任务修改数据库


        //增减点赞数量
        String sql = one.getStatus() == 1 ? "liked_count = liked_count + 1" : "liked_count = liked_count - 1";
        UpdateWrapper<Comment> wrapper = new UpdateWrapper<>();
        wrapper.eq("id",one.getObjId()).setSql(sql);
        this.commentMapper.update(wrapper);

        return Result.success(one);
    }

    @Override
    public Result childCommentList(Integer current, Integer size, Long parentId) {
        Map<String, Object> result = new HashMap<>();
        Page<Comment> page = new Page<>(current,size);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getParentId,parentId);
        Page<Comment> commentPageResult = this.commentMapper.selectPage(page, queryWrapper);
        List<Comment> commentRecords = commentPageResult.getRecords();
        if (commentRecords.size() == 0){
            result.put("commentList",new ArrayList<CommentVo>());
            result.put("commentCurrent",commentPageResult.getCurrent());
            result.put("commentPages",commentPageResult.getPages());

            return Result.success(result);
        }
        //获取子评论用户信息
        List<User> userList = this.userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getId, commentRecords.stream().map(comment -> {
                    return comment.getCommentUserId();
                }).collect(Collectors.toList())));
        //获取子评论是否被当前用户点赞
        Long userId = AuthContextHolder.getUserId();
        List<Liked> likedList  = this.likedMapper.selectList(new LambdaQueryWrapper<Liked>()
                            .eq(Liked::getType,2)
                            .eq(Liked::getLikedUserId,null == userId ? 0 : userId)
                            .eq(Liked::getStatus,1)
                            .in(Liked::getObjId,commentRecords.stream().map(comment -> {
                                return comment.getId();
                            }).collect(Collectors.toList())));


        List<CommentVo> commentVoList = commentRecords.stream().map(comment -> {
            UserVo userVo = new UserVo();
            CommentVo commentVo = new CommentVo();
            BeanUtils.copyProperties(comment, commentVo);
            for (User user : userList) {
                if (comment.getCommentUserId().equals(user.getId())) {
                    BeanUtils.copyProperties(user, userVo);
                    userVo.setAvatar(this.minioProperties.getUrl() + userVo.getAvatar());
                    break;
                }
            }
            for (Liked liked : likedList){
                if (liked.getObjId().equals(comment.getId())){
                    commentVo.setIsLiked(true);
                }
            }
            commentVo.setUserVo(userVo);
            return commentVo;
        }).collect(Collectors.toList());

        result.put("commentList",commentVoList);
        result.put("commentCurrent",commentPageResult.getCurrent());
        result.put("commentPages",commentPageResult.getPages());

        return Result.success(result);
    }

    /**
     * 帖子评论
     * @param comment
     * @return
     */
    @Override
    @Transactional
    public Result comment(Comment comment) {
        Long userId = AuthContextHolder.getUserId();
        UserVo userVo = AuthContextHolder.getUserVo();

        comment.setCommentUserId(userId);
        comment.setLikedCount(0l);
        comment.setPublicTime(new Date());
        this.commentMapper.insert(comment);

        CommentVo commentVo = new CommentVo();
        BeanUtils.copyProperties(comment,commentVo);
        if (commentVo.getFileType() != null && commentVo.getFileType().equals("image")){
            commentVo.setFileName(Arrays.stream(commentVo.getFileName().split(",")).map(fileName->{
                return this.minioProperties.getUrl() + fileName;
            }).collect(Collectors.joining(",")));
        }else if (commentVo.getFileType() != null && commentVo.getFileType().equals("video")){
            commentVo.setFileName(this.minioProperties.getUrl() + commentVo.getFileName());
        }
        commentVo.setIsLiked(false);
        Blog blog = this.getById(commentVo.getPostId());
        commentVo.setIsPoster(blog.getUserId() == userId ? true : false);
        commentVo.setChildCommentCount(0l);
        userVo.setAvatar(this.minioProperties.getUrl() + userVo.getAvatar());
        commentVo.setUserVo(userVo);
        UpdateWrapper<Blog> wrapper = new UpdateWrapper<>();
        wrapper.setSql("comment_count = comment_count + 1");
        this.update(wrapper);
        return Result.success(commentVo);
    }

    /**
     * 根据条件查询博客列表
     * @param current
     * @param size
     * @param keyword
     * @param status
     * @param fileType
     * @return
     */
    @Override
    public Result listBlogs(Integer current, Integer size, String keyword, Integer status, String fileType) {
        Map<String, Object> result = new HashMap<>();
        Page<Blog> blogPage = new Page<>(current, size);
        Page<Blog> selectPage = this.blogMapper.selectPage(blogPage, new LambdaQueryWrapper<Blog>()
                .eq(status != 0,Blog::getStatus, status)
                .eq(fileType != null && !fileType.equals(""),Blog::getFileType, fileType)
                .like(!keyword.equals("") && keyword != null, Blog::getContent, keyword));
        List<Blog> blogList = selectPage.getRecords();
        result.put("blogList",blogList);
        result.put("total",selectPage.getTotal());
        return Result.success(result);
    }

    @Override
    public Result commentList(Integer current, Integer size, Long postId, Long commentUserId, Integer type) {
        HashMap<String, Object> result = new HashMap<>();
        Page<Comment> page = new Page<>(current, size);
        Page<Comment> commentPage = this.commentMapper.selectPageByCondition(page,postId,commentUserId,type);
        List<Comment> commentList = commentPage.getRecords();
        result.put("commentList",commentList);
        result.put("total",commentPage.getTotal());
        return Result.success(result);
    }

    @Override
    public Result commentDetail(Long id) {
        Comment comment = this.commentMapper.selectByIdAndDeleteFlag(id);
        CommentVo commentVo = new CommentVo();
        BeanUtils.copyProperties(comment,commentVo);
        User user = this.userMapper.selectById(comment.getCommentUserId());
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user,userVo);
        userVo.setAvatar(this.minioProperties.getUrl() + userVo.getAvatar());
        commentVo.setUserVo(userVo);
        if(commentVo.getFileType() != null && commentVo.getFileType().equals("image")){
            String name = Arrays.stream(commentVo.getFileName().split(",")).map(fileName -> {
                return this.minioProperties.getUrl() + fileName;
            }).collect(Collectors.joining(","));
            commentVo.setFileName(name);
        }
        else{
            commentVo.setFileName(this.minioProperties.getUrl() + commentVo.getFileName());
        }

        return Result.success(commentVo);
    }

    @Override
    public Result commentUpdateStatus(Comment comment) {
        this.commentMapper.update(new UpdateWrapper<Comment>()
                .eq("id",comment.getId())
                .set("delete_flag",comment.getDeleteFlag()));
        return Result.success();
    }

    @Override
    public Result attentionPostList(Integer current, Integer size) {
        Map<String, Object> result = new HashMap<>();
        Long userId = AuthContextHolder.getUserId();
        List<Long> userIdList = attentionMapper.selectList(new LambdaQueryWrapper<Attention>().eq(Attention::getFansUserId, userId)).stream().map(attention -> {
            return attention.getUserId();
        }).collect(Collectors.toList());
        if (userIdList.size() == 0) {
            result.put("total",0);
            result.put("blogVoList",Arrays.asList());
            return Result.success(result);
        }
        Page<Blog> blogPage = new Page<>(current, size);
        Page<Blog> pageResult = this.blogMapper.selectPage(blogPage, new LambdaQueryWrapper<Blog>().eq(Blog::getStatus,1).in(Blog::getUserId, userIdList).orderByDesc(Blog::getId));
        Map<String, User> userMap = new HashMap<>();
        this.userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIdList)).stream().forEach(user -> {
            String id = String.valueOf(user.getId());
            userMap.put(id,user);
        });
        List<BlogVo> blogVoList = pageResult.getRecords().stream().map(blog -> {
            BlogVo blogVo = new BlogVo();
            BeanUtils.copyProperties(blog, blogVo);
            User user = new User();
            User one = userMap.get(String.valueOf(blogVo.getUserId()));
            BeanUtils.copyProperties(one,user);
            user.setAvatar(this.minioProperties.getUrl() + user.getAvatar());
            if (blogVo.getFileType() != null && blogVo.getFileType().equals("image")) {
                blogVo.setFileName(Arrays.stream(blog.getFileName().split(",")).map(fileName -> {
                    return this.minioProperties.getUrl() + fileName;
                }).collect(Collectors.joining(",")));
            } else {
                blogVo.setFileName(this.minioProperties.getUrl() + blogVo.getFileName());
            }
            blogVo.setCover(this.minioProperties.getUrl() + blogVo.getCover());
            blogVo.setUserStatus(user.getStatus());
            blogVo.setUserId(user.getId());
            blogVo.setUserAvatar(user.getAvatar());
            blogVo.setUserName(user.getUserName());
            blogVo.setUserCategory(user.getCategory());
            return blogVo;
        }).collect(Collectors.toList());
        result.put("total",pageResult.getTotal());
        result.put("blogVoList",blogVoList);
        return Result.success(result);
    }

}

package com.lytoyo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lytoyo.common.domain.*;
import com.lytoyo.common.domain.vo.BlogVo;
import com.lytoyo.common.domain.vo.UserVo;
import com.lytoyo.common.properties.MinioProperties;
import com.lytoyo.common.utils.AuthContextHolder;
import com.lytoyo.common.utils.PasswordUtil;
import com.lytoyo.mapper.*;
import com.lytoyo.service.BlogService;
import com.lytoyo.service.UserService;
import io.minio.MinioClient;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Package:com.lytoyo.service.impl
 *
 * @ClassName:UserService
 * @Create:2025/12/1 9:32
 **/

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    private PasswordUtil passwordUtil;

    @Resource
    private UserMapper userMapper;

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioProperties minioProperties;

    @Resource
    private BlogService blogService;

    @Resource
    private BlogMapper blogMapper;

    @Resource
    private AttentionMapper attentionMapper;

    @Resource
    private LikedMapper likedMapper;

    @Resource
    private CollectMapper collectMapper;

    @Resource
    private CommentMapper commentMapper;

    /**
     * 用户注册
     */
    public User register(String username, String plainPassword) {
        User user = new User();
        user.setUserName(username);
        // 1. 生成随机盐值
        String salt = passwordUtil.generateSalt();
        user.setSalt(salt);
        // 2. 加密密码（密码+盐值）
        String encryptedPassword = passwordUtil.encryptPassword(plainPassword, salt);
        user.setPassword(encryptedPassword);
        this.save(user);
        return user;
    }

    /**
     * 用户登录验证
     */
    public boolean login(String email, String inputPassword) {
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) return false;

        // 使用存储的盐值验证密码
        return passwordUtil.verifyPassword(inputPassword, user.getPassword(), user.getSalt());
    }

    /**
     * 获取用户状态信息
     *
     * @param userId
     * @return
     */
    @Override
    public Result getStatus(Long userId) {
        HashMap<String, Object> result = new HashMap<>();
        //查询用户获赞信息并整理
        Integer likedReduce = blogMapper.selectList(new LambdaQueryWrapper<Blog>()
        .eq(Blog::getUserId, userId)
        .eq(Blog::getStatus, 1))
        .stream().map(blog -> {
              return blog.getLikeCount();
        }).reduce(Math.toIntExact(this.likedMapper.selectCount(new LambdaQueryWrapper<Liked>().eq(Liked::getType, 2)
              .in(Liked::getObjId, this.commentMapper.selectList(new LambdaQueryWrapper<Comment>()
              .eq(Comment::getCommentUserId, userId))
                      .stream().map(comment -> {
                            return comment.getId();
                      }).collect(Collectors.toList())))), Integer::sum);
        //查询用户关注列表并整理
        Integer attentionReduce = attentionMapper.selectCount(new LambdaQueryWrapper<Attention>()
                .eq(Attention::getFansUserId, userId)
                .eq(Attention::getStatus, 1)).intValue();
        //查询用户粉丝列表并整理
        Integer fansReduce = attentionMapper.selectCount(new LambdaQueryWrapper<Attention>()
                .eq(Attention::getUserId, userId)
                .eq(Attention::getStatus, 1)).intValue();
        //查询用户状态
        Integer userStatus = userMapper.selectById(userId).getStatus();
        result.put("likedCount", likedReduce);
        result.put("attentionCount", attentionReduce);
        result.put("fansCount", fansReduce);
        result.put("userStatus", userStatus);
        return Result.success(result);
    }

    /**
     * 分页获取用户点赞帖子列表
     *
     * @param current
     * @param size
     * @return
     */
    @Override
    public Result likedPostList(Long userId, Integer current, Integer size) {

        HashMap<String, Object> result = new HashMap<>();
        //分页查询点赞的帖子列表
        Page<Liked> likedPage = new Page<>(current, size);
        QueryWrapper<Liked> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("liked_user_id", userId);
        queryWrapper.eq("type",1);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("update_time");
        Page<Liked> likedPageResult = likedMapper.selectPage(likedPage, queryWrapper);

        List<Long> postIdList = likedPageResult.getRecords().stream().map(liked -> {
            return liked.getObjId();
        }).collect(Collectors.toList());

        //根据这个帖子列表中的帖子id去查询相应的帖子
        List<BlogVo> blogList = this.blogMapper.selectBatchIds(postIdList).stream().map(blog -> {
            BlogVo blogVo = new BlogVo();
            BeanUtils.copyProperties(blog, blogVo);
            blogVo.setCover(minioProperties.getUrl() + blogVo.getCover());
            return blogVo;
        }).collect(Collectors.toList());
        result.put("likedList", blogList);
        result.put("likedCurrent", likedPageResult.getCurrent());
        result.put("likedTotal", likedPageResult.getTotal());
        result.put("likedPages", likedPageResult.getPages());
        return Result.success(result);
    }

    /**
     * 分页获取关注用户列表
     *
     * @param current
     * @param size
     * @return
     */
    @Override
    public Result attentionUserList(Long userId, Integer current, Integer size) {

        HashMap<String, Object> result = new HashMap<>();
        //分页获取关注用户列表
        Page<Attention> attentionPage = new Page<>(current, size);
        Page<Attention> attentionPageResult = this.attentionMapper.selectPage(attentionPage, new LambdaQueryWrapper<Attention>()
                .eq(Attention::getFansUserId, userId)
                .eq(Attention::getStatus, 1));
        //提取其中的关注用户id
        List<Long> userIdList = attentionPageResult.getRecords().stream().map(attention -> {
            return attention.getUserId();
        }).collect(Collectors.toList());

        //查询用户列表
        List<UserVo> userVoList = this.userMapper.selectBatchIds(userIdList).stream().map(user -> {
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(user, userVo);
            userVo.setAvatar(minioProperties.getUrl() + userVo.getAvatar());
            return userVo;
        }).collect(Collectors.toList());
        result.put("attentionUserList", userVoList);
        result.put("current", attentionPageResult.getCurrent());
        result.put("total", attentionPageResult.getTotal());
        result.put("pages", attentionPageResult.getPages());
        return Result.success(result);
    }

    /**
     * 分页获取粉丝用户列表
     *
     * @param current
     * @param size
     * @return
     */
    @Override
    public Result fansUserList(Long userId, Integer current, Integer size) {

        HashMap<String, Object> result = new HashMap<>();
        //分页获取粉丝用户列表
        Page<Attention> attentionPage = new Page<>(current, size);
        Page<Attention> attentionPageResult = this.attentionMapper.selectPage(attentionPage, new LambdaQueryWrapper<Attention>()
                .eq(Attention::getUserId, userId)
                .eq(Attention::getStatus, 1));
        //提取其中的粉丝用户id
        List<Long> userIdList = attentionPageResult.getRecords().stream().map(attention -> {
            return attention.getFansUserId();
        }).collect(Collectors.toList());

        //查询用户列表
        List<UserVo> userVoList = this.userMapper.selectBatchIds(userIdList).stream().map(user -> {
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(user, userVo);
            userVo.setAvatar(minioProperties.getUrl() + userVo.getAvatar());
            return userVo;
        }).collect(Collectors.toList());
        result.put("fansUserList", userVoList);
        result.put("current", attentionPageResult.getCurrent());
        result.put("total", attentionPageResult.getTotal());
        result.put("pages", attentionPageResult.getPages());
        return Result.success(result);
    }

    /**
     * 分页获取用户各项帖子列表
     *
     * @param userId
     * @param current
     * @param size
     * @return
     */
    @Override
    public Result askPost(Long userId, Integer current, Integer size) {
        Map<String, Object> result = new HashMap<>();

        List<BlogVo> blogList = new ArrayList<>();
        List<BlogVo> likedList = new ArrayList<>();
        List<BlogVo> collectList = new ArrayList<>();

        Page<Blog> blogPage = new Page<>(current, size);
        Page<Liked> likedPage = new Page<>(current, size);
        Page<Collect> collectPage = new Page<>(current, size);

        //用户作品
        QueryWrapper<Blog> blogQueryWrapper = new QueryWrapper<>();
        blogQueryWrapper.eq("user_id", userId);
        blogQueryWrapper.eq("status", 1);
        blogQueryWrapper.orderByDesc("publish_time");
        Page<Blog> blogPageResult = this.blogMapper.selectPage(blogPage, blogQueryWrapper);
        //转换为vo类减少传输损耗
        if (blogPageResult.getTotal() > 0) {
            blogList = blogPageResult.getRecords().stream().map(blog -> {
                BlogVo blogVo = new BlogVo();
                BeanUtils.copyProperties(blog, blogVo);
                blogVo.setCover(minioProperties.getUrl() + blogVo.getCover());
                return blogVo;
            }).collect(Collectors.toList());
        }

        QueryWrapper<Liked> likedQueryWrapper = new QueryWrapper<>();
        likedQueryWrapper.eq("liked_user_id", userId);
        likedQueryWrapper.eq("status", 1);
        likedQueryWrapper.eq("type",1);
        likedQueryWrapper.orderByDesc("update_time");
        Page<Liked> likedPageResult = this.likedMapper.selectPage(likedPage, likedQueryWrapper);
        if (likedPageResult.getTotal() > 0) {
            likedList = this.blogMapper.selectList(new LambdaQueryWrapper<Blog>()
                            .in(Blog::getId, likedPageResult.getRecords().stream().map(liked -> {
                                return liked.getObjId();
                            }).collect(Collectors.toList())))
                    .stream().map(blog -> {
                        BlogVo blogVo = new BlogVo();
                        BeanUtils.copyProperties(blog, blogVo);
                        blogVo.setCover(minioProperties.getUrl() + blogVo.getCover());
                        return blogVo;
                    }).collect(Collectors.toList());
        }

        QueryWrapper<Collect> collectQueryWrapper = new QueryWrapper<>();
        collectQueryWrapper.eq("collect_user_id", userId);
        collectQueryWrapper.eq("status", 1);
        collectQueryWrapper.orderByDesc("update_time");
        Page<Collect> collectPageResult = this.collectMapper.selectPage(collectPage, collectQueryWrapper);
        if (collectPageResult.getTotal() > 0) {
            collectList = this.blogMapper.selectList(new LambdaQueryWrapper<Blog>()
                            .in(Blog::getId, collectPageResult.getRecords().stream().map(collect -> {
                                return collect.getPostId();
                            }).collect(Collectors.toList())))
                    .stream().map(blog -> {
                        BlogVo blogVo = new BlogVo();
                        BeanUtils.copyProperties(blog, blogVo);
                        blogVo.setCover(minioProperties.getUrl() + blogVo.getCover());
                        return blogVo;
                    }).collect(Collectors.toList());
        }

        result.put("blogList", blogList);
        result.put("blogPages", blogPageResult.getPages());
        result.put("blogTotal", blogPageResult.getTotal());
        result.put("blogCurrent", blogPageResult.getCurrent());

        result.put("likedList", likedList);
        result.put("likedPages", likedPageResult.getPages());
        result.put("likedTotal", likedPageResult.getTotal());
        result.put("likedCurrent", likedPageResult.getCurrent());

        result.put("collectList", collectList);
        result.put("collectPages", collectPageResult.getPages());
        result.put("collectTotal", collectPageResult.getTotal());
        result.put("collectCurrent", collectPageResult.getCurrent());
        return Result.success(result);
    }

    /**
     * 获取用户发表的贴子
     *
     * @param userId
     * @param current
     * @param size
     * @return
     */
    @Override
    public Result selfPostList(Long userId, Integer current, Integer size) {
        Map<String, Object> result = new HashMap<>();
        List<BlogVo> blogList = new ArrayList<>();
        Page<Blog> blogPage = new Page<>(current, size);
        //用户作品
        QueryWrapper<Blog> blogQueryWrapper = new QueryWrapper<>();
        blogQueryWrapper.eq("user_id", userId);
        blogQueryWrapper.eq("status", 1);
        blogQueryWrapper.orderByDesc("publish_time");
        Page<Blog> blogPageResult = this.blogMapper.selectPage(blogPage, blogQueryWrapper);
        //转换为vo类减少传输损耗
        if (blogPageResult.getTotal() > 0) {
            blogList = blogPageResult.getRecords().stream().map(blog -> {
                BlogVo blogVo = new BlogVo();
                BeanUtils.copyProperties(blog, blogVo);
                blogVo.setCover(minioProperties.getUrl() + blogVo.getCover());
                return blogVo;
            }).collect(Collectors.toList());
        }

        result.put("blogList", blogList);
        result.put("blogPages", blogPageResult.getPages());
        result.put("blogTotal", blogPageResult.getTotal());
        result.put("blogCurrent", blogPageResult.getCurrent());

        return Result.success(result);
    }

    /**
     * 分页获取用户收藏的贴子
     *
     * @param userId
     * @param current
     * @param size
     * @return
     */
    @Override
    public Result collectPostList(Long userId, Integer current, Integer size) {
        Map<String, Object> result = new HashMap<>();
        List<BlogVo> collectList = new ArrayList<>();
        Page<Collect> collectPage = new Page<>(current, size);

        QueryWrapper<Collect> collectQueryWrapper = new QueryWrapper<>();
        collectQueryWrapper.eq("collect_user_id", userId);
        collectQueryWrapper.eq("status", 1);
        collectQueryWrapper.orderByDesc("update_time");
        Page<Collect> collectPageResult = this.collectMapper.selectPage(collectPage, collectQueryWrapper);
        if (collectPageResult.getTotal() > 0) {
            collectList = this.blogMapper.selectList(new LambdaQueryWrapper<Blog>()
                            .eq(Blog::getId, collectPageResult.getRecords().stream().map(collect -> {
                                return collect.getPostId();
                            }).collect(Collectors.toList())))
                    .stream().map(blog -> {
                        BlogVo blogVo = new BlogVo();
                        BeanUtils.copyProperties(blog, blogVo);
                        blogVo.setCover(minioProperties.getUrl() + blogVo.getCover());
                        return blogVo;
                    }).collect(Collectors.toList());
        }

        result.put("collectList", collectList);
        result.put("collectPages", collectPageResult.getPages());
        result.put("collectTotal", collectPageResult.getTotal());
        result.put("collectCurrent", collectPageResult.getCurrent());
        return Result.success(result);
    }

    /**
     * 获取其他用户信息
     * @param userId
     * @return
     */
    @Override
    public Result otherUserDetail(Long userId) {
        UserVo userVo = new UserVo();
        User user = this.userMapper.selectById(userId);
        BeanUtils.copyProperties(user,userVo);
        userVo.setAvatar(this.minioProperties.getUrl() + userVo.getAvatar());
        return Result.success(userVo);
    }
}

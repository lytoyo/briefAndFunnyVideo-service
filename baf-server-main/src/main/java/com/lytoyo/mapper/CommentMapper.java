package com.lytoyo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lytoyo.common.domain.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Package:com.lytoyo.mapper
 *
 * @ClassName:CommentMapper
 * @Create:2026/1/7 9:54
 **/
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    @Select("select * from comment  where id = #{id}")
    Comment selectByIdAndDeleteFlag(@Param("id") Long id);

    @Select("<script>" +
            "SELECT * FROM comment " +
            "WHERE delete_flag IN (0, 1) " +  // 包含已删除和未删除
            "<if test='postId != null and postId != 0'>" +
            "   AND post_id = #{postId} " +
            "</if>" +
            "<if test='commentUserId != null and commentUserId != 0'>" +
            "   AND comment_user_id = #{commentUserId} " +
            "</if>" +
            "<if test='type != null'>" +
            "   <choose>" +
            "       <when test='type == 1'>" +  // parent
            "           AND parent_id = 0 " +
            "       </when>" +
            "       <when test='type == 2'>" +  // child
            "           AND parent_id != 0 " +
            "       </when>" +
            "   </choose>" +
            "</if>" +
            "</script>")
    Page<Comment> selectPageByCondition(Page<Comment> page, Long postId, Long commentUserId, Integer type);

    @Select("select count(*) from comment where delete_flag = 1")
    long selectShieldCount();

    @Select("SELECT COUNT(*) \n" +
            "FROM comment \n" +
            "WHERE create_time >= CURDATE() \n" +
            "  AND create_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY);")
    long selectAddCommentCount();
}

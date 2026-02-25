package com.lytoyo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lytoyo.common.domain.Comment;
import com.lytoyo.mapper.CommentMapper;
import com.lytoyo.service.CommentService;
import org.springframework.stereotype.Service;

/**
 * Package:com.lytoyo.service.impl
 *
 * @ClassName:CommentServiceImpl
 * @Create:2026/1/7 9:58
 **/
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
}

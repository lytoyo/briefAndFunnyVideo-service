package com.lytoyo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lytoyo.common.domain.Liked;
import com.lytoyo.mapper.LikedMapper;
import com.lytoyo.service.LikedService;
import org.springframework.stereotype.Service;

/**
 * Package:com.lytoyo.service.impl
 *
 * @ClassName:LikedServiceImpl
 * @Create:2026/1/7 9:58
 **/
@Service
public class LikedServiceImpl extends ServiceImpl<LikedMapper, Liked> implements LikedService {
}

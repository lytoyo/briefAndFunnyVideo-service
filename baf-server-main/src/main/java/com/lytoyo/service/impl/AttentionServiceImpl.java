package com.lytoyo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lytoyo.common.domain.Attention;
import com.lytoyo.mapper.AttentionMapper;
import com.lytoyo.service.AttentionService;
import org.springframework.stereotype.Service;

/**
 * Package:com.lytoyo.service.impl
 *
 * @ClassName:AttentionServiceImpl
 * @Create:2026/1/7 9:57
 **/
@Service
public class AttentionServiceImpl extends ServiceImpl<AttentionMapper, Attention> implements AttentionService {
}

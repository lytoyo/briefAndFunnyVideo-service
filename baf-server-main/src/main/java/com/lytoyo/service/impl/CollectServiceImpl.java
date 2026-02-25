package com.lytoyo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lytoyo.common.domain.Collect;
import com.lytoyo.mapper.CollectMapper;
import com.lytoyo.service.CollectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Package:com.lytoyo.service.impl
 *
 * @ClassName:CollectServiceImpl
 * @Create:2026/1/12 10:50
 **/
@Service
@Slf4j
public class CollectServiceImpl extends ServiceImpl<CollectMapper, Collect> implements CollectService {
}

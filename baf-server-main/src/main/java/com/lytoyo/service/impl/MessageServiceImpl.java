package com.lytoyo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lytoyo.common.domain.Message;
import com.lytoyo.mapper.MessageMapper;
import com.lytoyo.service.MessageService;
import org.springframework.stereotype.Service;

/**
 * Package:com.lytoyo.service.impl
 *
 * @ClassName:MessageServiceImpl
 * @Create:2026/1/6 15:42
 **/
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
}

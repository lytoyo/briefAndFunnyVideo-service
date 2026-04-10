package com.lytoyo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lytoyo.common.domain.Message;
import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.vo.MessageVo;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

/**
 * Package:com.lytoyo.service
 *
 * @ClassName:MessageService
 * @Create:2026/1/6 15:41
 **/
public interface MessageService extends IService<Message> {

    /**
     * 检查p2pid是否存在
     * @param otherId
     * @return
     */
    Result checkP2pId(Long otherId);

    /**
     * 创建私聊窗口
     * @param message
     * @return
     */
    Result createPrivateWindow(Message message);

    /**
     * 根据p2pId获取聊天消息
     * @param p2pId
     * @param otherId
     * @return
     */
    Result gainChatMessage(Long p2pId,Long otherId);

    /**
     * 上传聊天文件
     * @param file
     * @param suffix
     * @param size
     * @param width
     * @param height
     * @param duration
     * @param type
     * @param otherId
     * @return
     */
    Result uploadChatFile(MultipartFile file, String suffix, Long size, Integer width, Integer height,
                          BigDecimal duration,String type,Long otherId,Long p2pId);

    /**
     * 获取历史聊天消息
     * @param historyChatId
     * @param p2pId
     * @return
     */
    Result getHistoryChatMessage(Long historyChatId, Long p2pId,Long otherId);
}

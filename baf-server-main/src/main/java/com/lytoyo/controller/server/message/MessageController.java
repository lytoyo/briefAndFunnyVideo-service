package com.lytoyo.controller.server.message;

import com.lytoyo.common.domain.Message;
import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.vo.MessageVo;
import com.lytoyo.framework.aspectj.SysLog;
import com.lytoyo.service.MessageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * Package:com.lytoyo.controller.server.message
 *
 * @ClassName:MessageController
 * @Create:2026/3/3 17:19
 **/
@RestController
@RequestMapping("/server/message")
public class MessageController {

    @Resource
    private MessageService messageService;

    @SysLog(value = "检查p2pId是否存在")
    @GetMapping("/checkP2pId")
    public Result checkP2pId(@RequestParam("otherId") Long otherId){
        return messageService.checkP2pId(otherId);
    }

    @SysLog(value = "创建私聊窗口")
    @PostMapping("/createPrivateWindow")
    public Result createPrivateWindow(@RequestBody Message message){
        return messageService.createPrivateWindow(message);
    }

    @SysLog(value = "获取聊天消息")
    @GetMapping("/gainChatMessage")
    public Result gainChatMessage(@RequestParam("p2pId")Long p2pId,@RequestParam("otherId")Long otherId){
        return messageService.gainChatMessage(p2pId,otherId);
    }

    @SysLog(value = "上传聊天文件")
    @PostMapping("/uploadChatFile")
    public Result uploadChatFile(@RequestParam("file") MultipartFile file,@RequestParam("suffix") String suffix,
                                 @RequestParam("size") Long size,@RequestParam("type") String type,
                                 @RequestParam(value = "width",required = false) Integer width,
                                 @RequestParam(value = "height",required = false) Integer height,
                                 @RequestParam(value = "duration",required = false) BigDecimal duration,
                                 @RequestParam(value = "otherId") Long otherId,@RequestParam(value = "p2pId")Long p2pId){
        return messageService.uploadChatFile(file,suffix,size,width,height,duration,type,otherId,p2pId);
    }

    @SysLog(value = "获取历史聊天记录")
    @GetMapping("/getHistoryChatMessage")
    public Result getHistoryChatMessage(@RequestParam("historyChatId")Long historyChatId,@RequestParam("p2pId")Long p2pId,
                                        @RequestParam("otherId")Long otherId){
        return messageService.getHistoryChatMessage(historyChatId,p2pId,otherId);
    }
}

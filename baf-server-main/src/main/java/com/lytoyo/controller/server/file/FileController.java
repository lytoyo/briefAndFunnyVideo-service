package com.lytoyo.controller.server.file;

import com.lytoyo.common.domain.Result;
import com.lytoyo.framework.aspectj.SysLog;
import com.lytoyo.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

/**
 * Package:com.lytoyo.controller.server.file
 *
 * @ClassName:FileController
 * @Create:2025/12/10 9:47
 **/
@RestController
@RequestMapping("/server/file")
public class FileController {

    @Resource
    private FileService fileService;

    @SysLog(value = "文件分片上传",require = false,needLogin = true)
    @PostMapping("/uploadZone")
    public Result uploadZone(@RequestParam("file") MultipartFile file,@RequestParam("md5") String md5,
                             @RequestParam("chunkIndex")Integer chunkIndex){
        fileService.uploadZone(file,md5,chunkIndex);
        return Result.success();
    }

    @SysLog(value = "文件分片合并",require = true,needLogin = true)
    @GetMapping("/zoneMerge")
    public Result zoneMerge(@RequestParam("md5")String md5,@RequestParam("suffix")String suffix,
                            @RequestParam("size")Long size,@RequestParam("type")String type,
                            @RequestParam("chunkCount")Integer chunkCount, @RequestParam(value = "duration",required = false) BigDecimal duration,
                            @RequestParam(value = "width",required = false) Integer width,@RequestParam(value = "height",required = false) Integer height) throws Exception {
        boolean flag = fileService.zoneMerge(md5,suffix,size,type,chunkCount,duration,width,height);
        return Result.success(flag);
    }

    @RequestMapping("/zoneRequest")
    public void zoneRequest(@RequestParam("videoName")String videoName,
                            HttpServletRequest request,
                            HttpServletResponse response){
        fileService.zoneRequest(videoName,request,response);
    }

    @SysLog(value = "简单文件上传",require = true,needLogin = true)
    @PostMapping("/smallFileUpload")
    public Result smallFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("suffix") String suffix,
                                  @RequestParam("md5") String md5,@RequestParam("size") Long size,
                                  @RequestParam("type") String type,@RequestParam(value = "width",required = false) Integer width,
                                  @RequestParam(value = "height",required = false) Integer height,@RequestParam(value = "duration",required = false) BigDecimal duration){

        boolean flag = fileService.smallFileUpload(file,md5,suffix,size,type,width,height,duration);
        return Result.success(flag);
    }

    @SysLog(value = "聊天文件上传",require = true)
    @PostMapping("/commentFileUpload")
    public Result commentFileUpload(@RequestParam("file") MultipartFile file,@RequestParam("suffix") String suffix,
                                    @RequestParam("size") Long size,@RequestParam("type") String type,
                                    @RequestParam(value = "width",required = false) Integer width,
                                    @RequestParam(value = "height",required = false) Integer height,
                                    @RequestParam(value = "duration",required = false) BigDecimal duration) throws Exception{
        return this.fileService.commentFileUpload(file,suffix,size,type,width,height,duration);
    }
}

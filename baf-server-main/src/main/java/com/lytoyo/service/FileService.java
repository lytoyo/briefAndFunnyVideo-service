package com.lytoyo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lytoyo.common.domain.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

/**
 * Package:com.lytoyo.service
 *
 * @ClassName:FileService
 * @Create:2025/12/15 10:53
 **/
public interface FileService extends IService<FileInfo> {


    /**
     * 文件分片上传
     */
    void uploadZone(MultipartFile file,String md5,Integer chunkIndex);


    /**
     * 文件分片合并
     * @return
     */
    boolean zoneMerge(String md5, String suffix, Long size, String type, Integer chunkCount, BigDecimal duration, Integer width, Integer height);

    /**
     * 分片获取视频
     * @param videoName
     * @param request
     * @param response
     */
    void zoneRequest(String videoName, HttpServletRequest request, HttpServletResponse response);


    /**
     * 上传简单文件
     * @return
     */
    boolean smallFileUpload(MultipartFile file, String md5, String suffix,
                            Long size, String type, Integer width,
                            Integer height, BigDecimal duration);
}

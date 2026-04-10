package com.lytoyo.controller.console.system;

import com.lytoyo.common.domain.Result;
import com.lytoyo.service.SystemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Package:com.lytoyo.controller.console.system
 *
 * @ClassName:ConsoleSystemController
 * @Create:2026/4/8 11:12
 **/
@RestController
@RequestMapping("/console/system")
public class ConsoleSystemController {
    @Resource
    private SystemService systemService;
    @GetMapping("overview")
    public Result overview(){
        return systemService.overview();
    }
}

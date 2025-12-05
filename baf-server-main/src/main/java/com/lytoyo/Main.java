package com.lytoyo;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Package:com.lytoyo
 *
 * @ClassName:${NAME}
 * @Create:2025/11/21 14:27
 **/
@SpringBootApplication
@Slf4j
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class,args);
        log.info("项目启动完毕(*^_^*)");
    }
}
package com.lytoyo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Package:com.lytoyo
 *
 * @ClassName:${NAME}
 * @Create:2025/11/21 17:48
 **/

@SpringBootApplication
public class SystemMain {
    public static void main(String[] args) {
        SpringApplication.run(SystemMain.class,args);
    }
}
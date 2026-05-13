package com.shigui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
    /**
     * 后端启动入口。Spring Boot 会从这个类所在包开始扫描 Controller、Service、Mapper 等组件。
     */
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}

package com.guide.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动器：聚合全部业务模块。
 */
@SpringBootApplication
public class GuideApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuideApplication.class, args);
    }
}

package com.guide.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动器：聚合全部业务模块。
 */
@SpringBootApplication
@MapperScan({
        "com.guide.auth.mapper",
        "com.guide.chat.mapper",
        "com.guide.kb.mapper",
        "com.guide.feedback.mapper",
        "com.guide.async.mapper"
})
public class GuideApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuideApplication.class, args);
    }
}

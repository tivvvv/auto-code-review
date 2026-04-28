package com.tiv.auto.code.review;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class AutoCodeReviewApplication {

    public static void main(String[] args) {
        // 加载.env文件
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        // 设置.env文件中的变量参数
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(AutoCodeReviewApplication.class, args);
    }

}
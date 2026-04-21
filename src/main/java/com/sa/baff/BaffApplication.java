package com.sa.baff;

import com.sa.baff.config.AccountLinkFeatureProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AccountLinkFeatureProperties.class)
public class BaffApplication {

    @PostConstruct
    public void started() {
        // JVM의 시간대를 한국으로 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(BaffApplication.class, args);
    }

}

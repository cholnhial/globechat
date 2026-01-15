package dev.chol.globechat;

import org.springframework.boot.SpringApplication;

public class TestGlobechatApplication {

    public static void main(String[] args) {
        SpringApplication.from(GlobechatApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}

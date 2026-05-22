package com.mall.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mall.demo.module.**.mapper")
public class MallPrimaryBackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallPrimaryBackEndApplication.class, args);
    }

}

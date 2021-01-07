package com.zhangyang.weixipay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication
@ComponentScan(basePackages={"com.zhangyang"})
public class WeixiPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeixiPayApplication.class, args);
    }

}

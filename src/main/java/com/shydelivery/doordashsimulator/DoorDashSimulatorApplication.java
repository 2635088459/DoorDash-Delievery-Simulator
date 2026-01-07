package com.shydelivery.doordashsimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DoorDash Simulator 主应用启动类
 * 
 * @SpringBootApplication 注解包含：
 * - @Configuration: 标记为配置类
 * - @EnableAutoConfiguration: 启用自动配置
 * - @ComponentScan: 自动扫描组件
 */
@SpringBootApplication
public class DoorDashSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoorDashSimulatorApplication.class, args);
    }

}

package com.shydelivery.doordashsimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DoorDash Simulator Main Application Entry Point
 * 
 * @SpringBootApplication annotation includes:
 * - @Configuration: Marks this as a configuration class
 * - @EnableAutoConfiguration: Enables auto-configuration
 * - @ComponentScan: Automatically scans for components
 * 
 * @EnableScheduling: Enables Spring's scheduled task execution capability
 */
@SpringBootApplication
@EnableScheduling
public class DoorDashSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoorDashSimulatorApplication.class, args);
    }

}

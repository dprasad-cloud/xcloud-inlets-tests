package com.extremecloudiq.inlets.tests;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

/**
 * @author dprasad
 */
@SpringBootApplication
public class InletsTestsApplication {

    public static void main(String[] args) {
        SpringApplication.run(InletsTestsApplication.class, args);
    }

}

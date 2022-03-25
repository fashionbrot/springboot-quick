package com.github.fashionbrot;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.util.Date;

@Slf4j
@SpringBootApplication
public class QuickApplication {

    private static Environment environment;

    @Autowired
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(QuickApplication.class,args);
        log.info("Start to finish:{}  port:{}", DateFormatUtils.format(new Date(),"yyyy-MM-dd HH:mm:ss"),environment.getProperty("server.port"));
    }

}

package com.github.fashionbrot;

import com.github.fashionbrot.tool.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.SimpleFormatter;

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
        log.info("Start to finish:{}  port:{}", DateUtil.formatDate(new Date()),environment.getProperty("server.port"));
    }

}

package com.github.fashionbrot.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "map")
public class MapConfig {

    private Map<String,String> vm;

    private Map<String,String> fixed;

}

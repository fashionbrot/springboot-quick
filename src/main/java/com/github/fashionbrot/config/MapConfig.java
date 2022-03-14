package com.github.fashionbrot.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "vm")
public class MapConfig {

    private Map<String,String> unset;

    private List<String> fixed;

    private List<String> root;

    private List<String> resource;
}

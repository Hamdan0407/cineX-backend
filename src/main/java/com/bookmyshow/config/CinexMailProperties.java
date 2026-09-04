package com.bookmyshow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cinex.mail")
public class CinexMailProperties {

    private String from = "";
    private String fromName = "CineX";
}

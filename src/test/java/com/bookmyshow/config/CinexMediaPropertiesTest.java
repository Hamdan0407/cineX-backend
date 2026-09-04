package com.bookmyshow.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class CinexMediaPropertiesTest {

    @Test
    void dotenvStyleCloudFrontBaseUrlPopulatesTheConfigurationBean() {
        CinexMediaProperties properties = new CinexMediaProperties();
        properties.setEnvironment(new MockEnvironment().withProperty("CLOUDFRONT_MEDIA_BASE_URL", "https://cdn.example.com"));

        properties.applyDotenvFallbacks();

        assertEquals("https://cdn.example.com", properties.getCloudfrontBaseUrl());
    }

    @Test
    void canonicalPropertyTakesPrecedenceOverDotenvFallback() {
        CinexMediaProperties properties = new CinexMediaProperties();
        properties.setCloudfrontBaseUrl("https://configured.example.com");
        properties.setEnvironment(new MockEnvironment().withProperty("CLOUDFRONT_MEDIA_BASE_URL", "https://dotenv.example.com"));

        properties.applyDotenvFallbacks();

        assertEquals("https://configured.example.com", properties.getCloudfrontBaseUrl());
    }
}

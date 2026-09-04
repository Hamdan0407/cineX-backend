package com.bookmyshow.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

@ConfigurationProperties(prefix = "cinex.media")
public class CinexMediaProperties implements EnvironmentAware {

    private String cloudfrontBaseUrl;
    private String s3Bucket = "cinex-trailer-media";
    private String s3Region = "us-east-1";
    private Map<Long, String> trailerMappings = new HashMap<>();
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void applyDotenvFallbacks() {
        if ((cloudfrontBaseUrl == null || cloudfrontBaseUrl.isBlank()) && environment != null) {
            String dotenvValue = environment.getProperty("CLOUDFRONT_MEDIA_BASE_URL");
            if (dotenvValue != null && !dotenvValue.isBlank()) {
                cloudfrontBaseUrl = dotenvValue.trim();
            }
        }
        if (environment != null) {
            String bucket = environment.getProperty("S3_MEDIA_BUCKET");
            if (bucket != null && !bucket.isBlank()) {
                s3Bucket = bucket.trim();
            }
            String region = environment.getProperty("S3_MEDIA_REGION");
            if (region != null && !region.isBlank()) {
                s3Region = region.trim();
            }
        }
    }

    public String getCloudfrontBaseUrl() {
        return cloudfrontBaseUrl;
    }

    public void setCloudfrontBaseUrl(String cloudfrontBaseUrl) {
        this.cloudfrontBaseUrl = cloudfrontBaseUrl;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public String getS3Region() {
        return s3Region;
    }

    public void setS3Region(String s3Region) {
        this.s3Region = s3Region;
    }

    public Map<Long, String> getTrailerMappings() {
        return trailerMappings;
    }

    public void setTrailerMappings(Map<Long, String> trailerMappings) {
        this.trailerMappings = trailerMappings != null ? trailerMappings : new HashMap<>();
    }
}

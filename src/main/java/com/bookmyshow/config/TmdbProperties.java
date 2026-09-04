package com.bookmyshow.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import jakarta.annotation.PostConstruct;

@ConfigurationProperties(prefix = "tmdb")
public class TmdbProperties implements EnvironmentAware {

    private String apiKey;
    private String baseUrl = "https://api.themoviedb.org/3";
    private List<String> languages = List.of("hi", "en", "ta");
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void applyDotenvApiKeyFallback() {
        // A .env file is a regular properties source, so its uppercase key is not relaxed-bound
        // like a true OS environment variable. Preserve that common dotenv convention safely.
        if ((apiKey == null || apiKey.isBlank()) && environment != null) {
            String dotenvApiKey = environment.getProperty("TMDB_API_KEY");
            if (dotenvApiKey != null && !dotenvApiKey.isBlank()) {
                apiKey = dotenvApiKey.trim();
            }
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }
}

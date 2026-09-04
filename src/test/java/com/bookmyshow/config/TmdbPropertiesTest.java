package com.bookmyshow.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TmdbPropertiesTest {

    @Test
    void dotenvStyleTmdbKeyPopulatesTheConfigurationBean() {
        TmdbProperties properties = new TmdbProperties();
        properties.setEnvironment(new MockEnvironment().withProperty("TMDB_API_KEY", "dotenv-key"));

        properties.applyDotenvApiKeyFallback();

        assertEquals("dotenv-key", properties.getApiKey());
    }

    @Test
    void canonicalPropertyTakesPrecedenceOverDotenvFallback() {
        TmdbProperties properties = new TmdbProperties();
        properties.setApiKey("canonical-key");
        properties.setEnvironment(new MockEnvironment().withProperty("TMDB_API_KEY", "dotenv-key"));

        properties.applyDotenvApiKeyFallback();

        assertEquals("canonical-key", properties.getApiKey());
    }
}

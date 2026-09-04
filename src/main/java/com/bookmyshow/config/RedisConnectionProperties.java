package com.bookmyshow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Redis connection inputs. The URL is preferred when supplied by a production provider. */
@ConfigurationProperties(prefix = "cinex.redis")
public class RedisConnectionProperties {
    private String url = "";
    private String host = "localhost";
    private int port = 6379;
    private String username = "";
    private String password = "";
    private boolean sslEnabled;
    private Duration timeout = Duration.ofSeconds(2);

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url == null ? "" : url.trim(); }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username == null ? "" : username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password == null ? "" : password; }
    public boolean isSslEnabled() { return sslEnabled; }
    public void setSslEnabled(boolean sslEnabled) { this.sslEnabled = sslEnabled; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }

    public boolean hasUrl() { return !url.isBlank(); }
    public String safeEndpoint() { return RedisConnectionConfig.resolve(this).safeEndpoint(); }
}

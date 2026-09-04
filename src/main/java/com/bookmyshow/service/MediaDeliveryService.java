package com.bookmyshow.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bookmyshow.config.CinexMediaProperties;

@Service
public class MediaDeliveryService {

    private final CinexMediaProperties mediaProperties;

    public MediaDeliveryService(CinexMediaProperties mediaProperties) {
        this.mediaProperties = mediaProperties;
    }

    /**
     * Resolves a stored media reference to a browser-deliverable URL.
     * Absolute URLs are returned unchanged. S3 object keys are prefixed with the configured CloudFront base URL.
     */
    public String resolveDeliveryUrl(String mediaReference) {
        if (!StringUtils.hasText(mediaReference)) {
            return null;
        }
        String trimmed = mediaReference.trim();
        if (isAbsoluteUrl(trimmed)) {
            return trimmed;
        }
        String baseUrl = normalizeBaseUrl(mediaProperties.getCloudfrontBaseUrl());
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }
        String objectKey = normalizeObjectKey(trimmed);
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        return baseUrl + "/" + encodeObjectKeyForUrl(objectKey);
    }

    public boolean isConfigured() {
        return StringUtils.hasText(mediaProperties.getCloudfrontBaseUrl());
    }

    public String getCloudfrontBaseUrl() {
        return normalizeBaseUrl(mediaProperties.getCloudfrontBaseUrl());
    }

    static boolean isAbsoluteUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    static String encodeObjectKeyForUrl(String objectKey) {
        String[] segments = objectKey.split("/");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return builder.toString();
    }

    static String normalizeObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        String normalized = objectKey.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.isEmpty() ? null : normalized;
    }

    static String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }
        String normalized = baseUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        try {
            URI uri = URI.create(normalized);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return normalized;
    }
}

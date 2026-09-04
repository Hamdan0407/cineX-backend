package com.bookmyshow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bookmyshow.config.CinexMediaProperties;

class MediaDeliveryServiceTest {

    static final String SPIDER_MAN_TRAILER_OBJECT_KEY =
            "trailers/SPIDER-MAN_ BRAND NEW DAY \u2013 New Trailer (4K).mp4";

    private CinexMediaProperties properties;
    private MediaDeliveryService service;

    @BeforeEach
    void setUp() {
        properties = new CinexMediaProperties();
        properties.setCloudfrontBaseUrl("https://d1al8zqo1izqiu.cloudfront.net");
        service = new MediaDeliveryService(properties);
    }

    @Test
    void resolvesExactSpiderManTrailerObjectKeyToCloudFrontUrl() {
        assertEquals(
                "https://d1al8zqo1izqiu.cloudfront.net/"
                        + MediaDeliveryService.encodeObjectKeyForUrl(SPIDER_MAN_TRAILER_OBJECT_KEY),
                service.resolveDeliveryUrl(SPIDER_MAN_TRAILER_OBJECT_KEY));
    }

    @Test
    void encodesSpacesParenthesesAndEnDashInObjectKeySegments() {
        assertEquals(
                "trailers/SPIDER-MAN_%20BRAND%20NEW%20DAY%20%E2%80%93%20New%20Trailer%20%284K%29.mp4",
                MediaDeliveryService.encodeObjectKeyForUrl(SPIDER_MAN_TRAILER_OBJECT_KEY));
    }

    @Test
    void normalizesLeadingSlashesOnObjectKeys() {
        assertEquals(
                "https://d1al8zqo1izqiu.cloudfront.net/trailers/example.mp4",
                service.resolveDeliveryUrl("/trailers/example.mp4"));
    }

    @Test
    void preservesAbsoluteUrls() {
        String youtube = "https://www.youtube.com/watch?v=abc123";
        assertEquals(youtube, service.resolveDeliveryUrl(youtube));
    }

    @Test
    void returnsNullForMissingMediaReference() {
        assertNull(service.resolveDeliveryUrl(null));
        assertNull(service.resolveDeliveryUrl("   "));
    }

    @Test
    void returnsNullWhenCloudFrontBaseUrlIsNotConfigured() {
        properties.setCloudfrontBaseUrl("");
        assertNull(service.resolveDeliveryUrl("trailers/example.mp4"));
    }

    @Test
    void stripsTrailingSlashFromConfiguredBaseUrl() {
        properties.setCloudfrontBaseUrl("https://cdn.example.com/");
        assertEquals("https://cdn.example.com/posters/foo.jpg", service.resolveDeliveryUrl("posters/foo.jpg"));
    }

    @Test
    void detectsAbsoluteUrls() {
        assertTrue(MediaDeliveryService.isAbsoluteUrl("https://example.com/a.mp4"));
        assertFalse(MediaDeliveryService.isAbsoluteUrl("trailers/a.mp4"));
    }
}

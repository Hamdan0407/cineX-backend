package com.bookmyshow.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MovieResponse {
    private Long id;
    private String title;
    private String description;
    private Integer duration;
    private String language;
    private String genre;
    private LocalDate releaseDate;
    private String posterPath;
    /** Stored S3 object key or external trailer URL reference. */
    private String trailerUrl;
    /** Resolved browser playback URL (CloudFront or external). */
    private String trailerPlaybackUrl;
    /** CineX-hosted S3 object key when available. */
    private String trailerObjectKey;
    private String cast;
    private String certification;
    private String status;
    private Long tmdbId;
}

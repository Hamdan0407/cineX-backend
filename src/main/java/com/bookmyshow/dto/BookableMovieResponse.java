package com.bookmyshow.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookableMovieResponse {
    private Long backendMovieId;
    private Long tmdbId;
    private String title;
    private String description;
    private String genre;
    private Integer duration;
    private String posterPath;
    private String backdropPath;
    /** Resolved browser playback URL (CloudFront or external). */
    private String trailerPlaybackUrl;
    /** CineX-hosted S3 object key when available. */
    private String trailerObjectKey;
    private List<String> screeningLanguages;
    /** Cinema formats available for this movie in the selected city (e.g. 2D, IMAX 2D, 4DX). */
    private List<String> formats;
}

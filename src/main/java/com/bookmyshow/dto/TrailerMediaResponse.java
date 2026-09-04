package com.bookmyshow.dto;

import lombok.Data;

@Data
public class TrailerMediaResponse {
    private Long tmdbId;
    private Long backendMovieId;
    private String trailerObjectKey;
    private String trailerPlaybackUrl;
    private boolean available;

    public static TrailerMediaResponse unavailable() {
        TrailerMediaResponse response = new TrailerMediaResponse();
        response.setAvailable(false);
        return response;
    }
}

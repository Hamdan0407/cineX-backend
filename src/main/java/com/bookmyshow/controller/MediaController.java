package com.bookmyshow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyshow.dto.TrailerMediaResponse;
import com.bookmyshow.service.MovieTrailerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Media API", description = "CineX-hosted media delivery metadata")
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MovieTrailerService movieTrailerService;

    public MediaController(MovieTrailerService movieTrailerService) {
        this.movieTrailerService = movieTrailerService;
    }

    @Operation(summary = "Resolve CineX trailer media for a TMDB movie id")
    @GetMapping("/movies/{tmdbId}/trailer")
    public ResponseEntity<TrailerMediaResponse> getTrailerForMovie(@PathVariable Long tmdbId) {
        return ResponseEntity.ok(movieTrailerService.resolveTrailerForTmdbId(tmdbId));
    }
}

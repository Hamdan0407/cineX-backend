package com.bookmyshow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitySearchResponse {
    private String id;
    private String name;
    private String normalizedName;
    private String state;
    private String country;
  /** True when CineX currently has theatres/shows in this city. */
    private boolean cinexAvailable;
  /** Optional landmark key for curated CineX city artwork (frontend). */
    private String landmarkId;
}

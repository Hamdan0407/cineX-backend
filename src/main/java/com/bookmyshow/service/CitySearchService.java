package com.bookmyshow.service;

import com.bookmyshow.dto.CitySearchResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitySearchService {

    private static final int MAX_RESULTS = 20;

    private static final Map<String, String> LANDMARK_BY_CITY = Map.of(
            "Chennai", "chennai",
            "Mumbai", "mumbai",
            "Bengaluru", "bengaluru",
            "Hyderabad", "hyderabad",
            "Delhi NCR", "delhi-ncr"
    );

    private final TheatreService theatreService;
    private final ObjectMapper objectMapper;

    private List<CityRecord> catalog = List.of();

    @PostConstruct
    void loadCatalog() {
        try (InputStream input = new ClassPathResource("data/indian-cities.json").getInputStream()) {
            catalog = objectMapper.readValue(input, new TypeReference<List<CityRecord>>() {});
            log.info("Loaded {} Indian cities for search", catalog.size());
        } catch (IOException ex) {
            log.error("Failed to load indian-cities.json: {}", ex.getMessage());
            catalog = List.of();
        }
    }

    public List<CitySearchResponse> search(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        Set<String> operationalCities = new HashSet<>(theatreService.getAllCities());

        return catalog.stream()
                .map(record -> toMatch(record, normalizedQuery))
                .filter(MatchScore::matches)
                .sorted(Comparator.comparingInt(MatchScore::score).reversed()
                        .thenComparing(ms -> ms.record().name()))
                .limit(MAX_RESULTS)
                .map(ms -> toResponse(ms.record(), operationalCities))
                .collect(Collectors.toList());
    }

    /** The public city catalog is independent of current CineX availability. */
    public List<CitySearchResponse> list() {
        Set<String> operationalCities = new HashSet<>(theatreService.getAllCities());
        return catalog.stream()
                .sorted(Comparator.comparing(CityRecord::name))
                .map(record -> toResponse(record, operationalCities))
                .collect(Collectors.toList());
    }

    private CitySearchResponse toResponse(CityRecord record, Set<String> operationalCities) {
        boolean available = operationalCities.stream()
                .anyMatch(city -> city.equalsIgnoreCase(record.name()));
        String landmarkId = LANDMARK_BY_CITY.get(record.name());
        return CitySearchResponse.builder()
                .id(record.id())
                .name(record.name())
                .normalizedName(normalize(record.name()))
                .state(record.state())
                .country("India")
                .cinexAvailable(available)
                .landmarkId(landmarkId)
                .build();
    }

    private static MatchScore toMatch(CityRecord record, String query) {
        int score = 0;
        String name = normalize(record.name());
        String state = normalize(record.state());

        if (name.equals(query)) {
            score = 100;
        } else if (name.startsWith(query)) {
            score = 80;
        } else if (name.contains(query)) {
            score = 60;
        } else if (state.startsWith(query)) {
            score = 40;
        } else if (state.contains(query)) {
            score = 30;
        }

        if (record.aliases() != null) {
            for (String alias : record.aliases()) {
                String normalizedAlias = normalize(alias);
                if (normalizedAlias.equals(query)) {
                    score = Math.max(score, 90);
                } else if (normalizedAlias.startsWith(query)) {
                    score = Math.max(score, 70);
                } else if (normalizedAlias.contains(query)) {
                    score = Math.max(score, 50);
                }
            }
        }

        return new MatchScore(record, score);
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private record CityRecord(String id, String name, String state, List<String> aliases) {}

    private record MatchScore(CityRecord record, int score) {
        boolean matches() {
            return score > 0;
        }
    }
}

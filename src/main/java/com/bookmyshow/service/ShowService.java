package com.bookmyshow.service;

import com.bookmyshow.dto.BookableMovieResponse;
import com.bookmyshow.dto.CityShowAvailabilityResponse;
import com.bookmyshow.dto.ShowDto;
import com.bookmyshow.dto.ShowRequest;
import com.bookmyshow.dto.ShowResponse;
import com.bookmyshow.dto.ShowSeatDto;
import com.bookmyshow.dto.mapper.ShowMapper;
import com.bookmyshow.entity.BookingSeat;
import com.bookmyshow.entity.Movie;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Seat;
import com.bookmyshow.entity.Show;
import com.bookmyshow.repository.BookingSeatRepository;
import com.bookmyshow.repository.MovieRepository;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.SeatRepository;
import com.bookmyshow.repository.ShowRepository;
import com.bookmyshow.repository.TheatreRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import com.bookmyshow.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@Service
public class ShowService {
    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final TheatreRepository theatreRepository;
    private final TmdbCatalogSyncService tmdbCatalogSyncService;
    private final ShowInventoryService showInventoryService;
    private final MovieTrailerService movieTrailerService;

    public ShowService(ShowRepository showRepository, MovieRepository movieRepository,
                       ScreenRepository screenRepository, SeatRepository seatRepository,
                       BookingSeatRepository bookingSeatRepository, TheatreRepository theatreRepository,
                       TmdbCatalogSyncService tmdbCatalogSyncService,
                       ShowInventoryService showInventoryService,
                       MovieTrailerService movieTrailerService) {
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.theatreRepository = theatreRepository;
        this.tmdbCatalogSyncService = tmdbCatalogSyncService;
        this.showInventoryService = showInventoryService;
        this.movieTrailerService = movieTrailerService;
    }

    @CacheEvict(value = "shows", allEntries = true)
    public ShowResponse addShow(ShowRequest request) {
        log.info("Adding show for movie id {} on {}", request.getMovieId(), request.getShowDate());
        Movie movie = movieRepository.findById(request.getMovieId())
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
        Screen screen = screenRepository.findById(request.getScreenId())
            .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));
        Show savedShow = showRepository.save(ShowMapper.toNewEntity(request, movie, screen));
        return toResponse(savedShow);
    }

    @Deprecated
    @CacheEvict(value = "shows", allEntries = true)
    public ShowResponse addShow(ShowDto dto) {
        return addShow(ShowMapper.toRequest(dto));
    }

    @CacheEvict(value = "shows", allEntries = true)
    public ShowResponse updateShow(Long id, ShowRequest request) {
        log.info("Updating show id: {}", id);
        Show show = showRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Show not found"));
        Movie movie = null;
        Screen screen = null;
        if (request.getMovieId() != null && !request.getMovieId().equals(show.getMovie().getId())) {
            movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
        }
        if (request.getScreenId() != null && !request.getScreenId().equals(show.getScreen().getId())) {
            screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));
        }
        ShowMapper.applyUpdate(show, request, movie, screen);
        return toResponse(showRepository.save(show));
    }

    @Deprecated
    @CacheEvict(value = "shows", allEntries = true)
    public ShowResponse updateShow(Long id, ShowDto dto) {
        return updateShow(id, ShowMapper.toRequest(dto));
    }

    @CacheEvict(value = "shows", allEntries = true)
    public void deleteShow(Long id) {
        log.info("Deleting show id: {}", id);
        if (!showRepository.existsById(id)) {
            throw new ResourceNotFoundException("Show not found");
        }
        showRepository.deleteById(id);
    }

    @Cacheable(value = "shows", key = "'all'")
    public List<ShowResponse> getAllShows() {
        log.info("Cache Miss: Fetching all shows from MySQL database");
        return showRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Cacheable(value = "shows", key = "'movie_' + #movieId")
    public List<ShowResponse> getShowsByMovie(Long movieId) {
        return getShowsByMovie(movieId, null, null);
    }

    public List<ShowResponse> getShowsByMovie(Long movieId, String city, String language) {
        log.info("Fetching shows for movie id {} city={} language={}", movieId, city, language);
        LocalDate today = LocalDate.now();
        String resolvedCity = resolveCity(city);
        String resolvedLanguage = blankToNull(language);
        List<Show> shows;
        if (resolvedCity != null) {
            shows = showRepository.findByMovieIdAndCity(movieId, resolvedCity, today, resolvedLanguage);
        } else {
            shows = showRepository.findByMovieId(movieId).stream()
                    .filter(show -> !show.getShowDate().isBefore(today))
                    .collect(Collectors.toList());
            if (resolvedLanguage != null) {
                shows = shows.stream()
                        .filter(show -> resolvedLanguage.equals(show.getScreeningLanguage()))
                        .collect(Collectors.toList());
            }
        }
        return shows.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ShowResponse> getShowsByTmdbId(Long tmdbId, String city, String language) {
        String resolvedCity = resolveCity(city);
        if (resolvedCity == null) {
            throw new IllegalArgumentException("City is required");
        }
        log.info("Fetching shows for tmdb id {} city={} language={}", tmdbId, resolvedCity, language);

        Movie movie = tmdbCatalogSyncService.syncMovieByTmdbId(tmdbId);
        if (movie != null && Boolean.TRUE.equals(movie.getCatalogActive())) {
            showInventoryService.ensureShowsForTmdbMovieInCity(movie, resolvedCity);
        }

        return showRepository.findByTmdbIdAndCity(tmdbId, resolvedCity, LocalDate.now(), blankToNull(language))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CityShowAvailabilityResponse getCityAvailability(String city, String language) {
        String resolvedCity = resolveCity(city);
        if (resolvedCity == null) {
            throw new IllegalArgumentException("City is required");
        }
        LocalDate today = LocalDate.now();
        CityShowAvailabilityResponse response = new CityShowAvailabilityResponse();
        response.setCity(resolvedCity);
        response.setTmdbIds(showRepository.findDistinctTmdbIdsByCity(resolvedCity, today, blankToNull(language)));
        response.setLanguages(showRepository.findDistinctScreeningLanguagesByCity(resolvedCity, today));
        return response;
    }

    public List<BookableMovieResponse> getBookableMovies(String city, String language) {
        String resolvedCity = resolveCity(city);
        if (resolvedCity == null) {
            throw new IllegalArgumentException("City is required");
        }
        LocalDate today = LocalDate.now();
        String resolvedLanguage = blankToNull(language);
        List<Movie> movies = showRepository.findDistinctMoviesByCity(resolvedCity, today, resolvedLanguage);
        return movies.stream().map(movie -> {
            BookableMovieResponse dto = new BookableMovieResponse();
            dto.setBackendMovieId(movie.getId());
            dto.setTmdbId(movie.getTmdbId());
            dto.setTitle(movie.getTitle());
            dto.setDescription(movie.getDescription());
            dto.setGenre(movie.getGenre());
            dto.setDuration(movie.getDuration());
            dto.setPosterPath(movie.getPosterPath());
            dto.setBackdropPath(movie.getPosterPath());
            if (movie.getTmdbId() != null) {
                List<Show> cityShows = showRepository.findByTmdbIdAndCity(
                        movie.getTmdbId(), resolvedCity, today, null);
                List<String> langs = cityShows.stream()
                        .map(Show::getScreeningLanguage)
                        .filter(lang -> lang != null && !lang.isBlank())
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
                dto.setScreeningLanguages(langs);
                dto.setFormats(extractPresentationFormats(cityShows));
            } else {
                dto.setScreeningLanguages(List.of());
                dto.setFormats(List.of("2D"));
            }
            movieTrailerService.enrichBookableMovieResponse(dto, movie);
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Derives cinema format badges from theatre amenities and screen names for shows in a city.
     */
    static List<String> extractPresentationFormats(List<Show> shows) {
        if (shows == null || shows.isEmpty()) {
            return List.of();
        }
        Set<String> formats = new LinkedHashSet<>();
        formats.add("2D");
        for (Show show : shows) {
            if (show.getScreen() == null) {
                continue;
            }
            String screenName = show.getScreen().getScreenName() != null
                    ? show.getScreen().getScreenName() : "";
            String amenities = show.getScreen().getTheatre() != null
                    && show.getScreen().getTheatre().getAmenities() != null
                    ? show.getScreen().getTheatre().getAmenities() : "";
            String haystack = (screenName + " " + amenities).toUpperCase(Locale.ROOT);
            if (haystack.contains("IMAX")) {
                formats.add("IMAX 2D");
            }
            if (haystack.contains("4DX")) {
                formats.add("4DX");
            }
            if (haystack.contains("3D") || haystack.contains("REALD")) {
                formats.add("3D");
            }
        }
        return new ArrayList<>(formats);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String resolveCity(String city) {
        if (city == null || city.isBlank()) {
            return null;
        }
        String trimmed = city.trim();
        return theatreRepository.findAll().stream()
                .map(theatre -> theatre.getCity())
                .filter(theatreCity -> theatreCity != null && theatreCity.equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(trimmed);
    }

    public Page<ShowResponse> getShowsPaginated(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return showRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<ShowResponse> searchShowsPaginated(Long movieId, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        if (movieId != null && movieId > 0) {
            return showRepository.findByMovieId(movieId, pageable).map(this::toResponse);
        }
        return showRepository.findAll(pageable).map(this::toResponse);
    }

    public List<ShowSeatDto> getShowSeats(Long showId) {
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new ResourceNotFoundException("Show not found"));

        List<Seat> allSeats = seatRepository.findByScreenId(show.getScreen().getId());
        List<BookingSeat> bookedSeatsEntities = bookingSeatRepository.findByBookingShowId(showId);

        Set<Long> bookedSeatIds = bookedSeatsEntities.stream()
                .filter(bs -> "BOOKED".equals(bs.getStatus()))
                .map(bs -> bs.getSeat().getId())
                .collect(Collectors.toSet());

        return allSeats.stream().map(seat -> {
            ShowSeatDto dto = new ShowSeatDto();
            dto.setSeatId(seat.getId());
            dto.setSeatNumber(seat.getSeatNumber());
            dto.setSeatType(seat.getSeatType());
            dto.setRowLabel(seat.getRowLabel());
            dto.setRowIndex(seat.getRowIndex());
            dto.setColumnIndex(seat.getColumnIndex());
            dto.setWheelchairAccessible(Boolean.TRUE.equals(seat.getWheelchairAccessible()));
            double seatPrice = show.getPrice();
            if (seat.getPrice() != null) {
                seatPrice += seat.getPrice();
            } else if ("PREMIUM".equals(seat.getSeatType())) {
                seatPrice += 50.0;
            }
            dto.setPrice(seatPrice);
            dto.setStatus(bookedSeatIds.contains(seat.getId()) ? "BOOKED" : "AVAILABLE");
            return dto;
        }).sorted(Comparator
                .comparing((ShowSeatDto dto) -> dto.getRowIndex() == null ? Integer.MAX_VALUE : dto.getRowIndex())
                .thenComparing(dto -> dto.getColumnIndex() == null ? Integer.MAX_VALUE : dto.getColumnIndex())
                .thenComparing(ShowSeatDto::getSeatNumber, Comparator.nullsLast(String::compareTo)))
        .collect(Collectors.toList());
    }

    private ShowResponse toResponse(Show show) {
        int total = show.getScreen().getTotalSeats() != null ? show.getScreen().getTotalSeats() : 100;
        long booked = bookingSeatRepository.findByBookingShowId(show.getId()).stream()
                .filter(bs -> "BOOKED".equals(bs.getStatus())).count();
        return ShowMapper.toResponse(show, (int) (total - booked));
    }
}

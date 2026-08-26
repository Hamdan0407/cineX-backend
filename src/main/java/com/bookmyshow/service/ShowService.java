package com.bookmyshow.service;

import com.bookmyshow.dto.ShowDto;
import com.bookmyshow.dto.ShowSeatDto;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import com.bookmyshow.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
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

    public ShowService(ShowRepository showRepository, MovieRepository movieRepository, 
                       ScreenRepository screenRepository, SeatRepository seatRepository, 
                       BookingSeatRepository bookingSeatRepository) {
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.bookingSeatRepository = bookingSeatRepository;
    }

    @CacheEvict(value = "shows", allEntries = true)
    public ShowDto addShow(ShowDto dto) {
        log.info("Adding show for movie id {} on {}", dto.getMovieId(), dto.getShowDate());
        Movie movie = movieRepository.findById(dto.getMovieId())
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
        Screen screen = screenRepository.findById(dto.getScreenId())
            .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));

        Show show = new Show();
        show.setMovie(movie);
        show.setScreen(screen);
        show.setShowTime(dto.getShowTime());
        show.setShowDate(dto.getShowDate());
        show.setPrice(dto.getPrice());
        show.setAvailableSeats(screen.getTotalSeats() != null ? screen.getTotalSeats() : 100);

        Show savedShow = showRepository.save(show);
        return mapToDto(savedShow);
    }

    @CacheEvict(value = "shows", allEntries = true)
    public ShowDto updateShow(Long id, ShowDto dto) {
        log.info("Updating show id: {}", id);
        Show show = showRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Show not found"));
        if (dto.getMovieId() != null && !dto.getMovieId().equals(show.getMovie().getId())) {
            Movie movie = movieRepository.findById(dto.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
            show.setMovie(movie);
        }
        if (dto.getScreenId() != null && !dto.getScreenId().equals(show.getScreen().getId())) {
            Screen screen = screenRepository.findById(dto.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));
            show.setScreen(screen);
        }
        if (dto.getShowTime() != null) show.setShowTime(dto.getShowTime());
        if (dto.getShowDate() != null) show.setShowDate(dto.getShowDate());
        if (dto.getPrice() != null) show.setPrice(dto.getPrice());

        Show updated = showRepository.save(show);
        return mapToDto(updated);
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
    public List<ShowDto> getAllShows() {
        log.info("Cache Miss: Fetching all shows from MySQL database");
        return showRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Cacheable(value = "shows", key = "'movie_' + #movieId")
    public List<ShowDto> getShowsByMovie(Long movieId) {
        log.info("Cache Miss: Fetching shows for movie id {} from MySQL database", movieId);
        return showRepository.findByMovieId(movieId).stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }

    public Page<ShowDto> getShowsPaginated(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return showRepository.findAll(pageable).map(this::mapToDto);
    }

    public Page<ShowDto> searchShowsPaginated(Long movieId, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        if (movieId != null && movieId > 0) {
            return showRepository.findByMovieId(movieId, pageable).map(this::mapToDto);
        }
        return showRepository.findAll(pageable).map(this::mapToDto);
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
            double seatPrice = seat.getPrice() != null ? seat.getPrice() : (show.getPrice() + ("PREMIUM".equals(seat.getSeatType()) ? 50.0 : 0.0));
            dto.setPrice(seatPrice);
            dto.setStatus(bookedSeatIds.contains(seat.getId()) ? "BOOKED" : "AVAILABLE");
            return dto;
        }).collect(Collectors.toList());
    }

    private ShowDto mapToDto(Show show) {
        ShowDto dto = new ShowDto();
        dto.setId(show.getId());
        dto.setMovieId(show.getMovie().getId());
        dto.setScreenId(show.getScreen().getId());
        dto.setShowTime(show.getShowTime());
        dto.setShowDate(show.getShowDate());
        dto.setPrice(show.getPrice());
        
        int total = show.getScreen().getTotalSeats() != null ? show.getScreen().getTotalSeats() : 100;
        long booked = bookingSeatRepository.findByBookingShowId(show.getId()).stream()
                .filter(bs -> "BOOKED".equals(bs.getStatus())).count();
        dto.setAvailableSeats((int) (total - booked));
        return dto;
    }
}

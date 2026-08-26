import os

base_path = 'c:/Users/Hamdaan/Downloads/bookmyshow/bookmyshow/src/main/java/com/bookmyshow'

files = {
    'dto/ShowDto.java': '''package com.bookmyshow.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ShowDto {
    private Long id;
    private Long movieId;
    private Long screenId;
    private LocalTime showTime;
    private LocalDate showDate;
    private Double price;
}
''',
    'dto/SeatDto.java': '''package com.bookmyshow.dto;

import lombok.Data;

@Data
public class SeatDto {
    private Long id;
    private String seatNumber;
    private String seatType;
    private Long screenId;
}
''',
    'dto/ShowSeatDto.java': '''package com.bookmyshow.dto;

import lombok.Data;

@Data
public class ShowSeatDto {
    private Long seatId;
    private String seatNumber;
    private String seatType;
    private String status;
    private Double price;
}
''',
    'repository/ShowRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByMovieId(Long movieId);
}''',
    'repository/SeatRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScreenId(Long screenId);
}''',
    'repository/BookingSeatRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    List<BookingSeat> findByBookingShowId(Long showId);
}''',
    'service/SeatService.java': '''package com.bookmyshow.service;

import com.bookmyshow.dto.SeatDto;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Seat;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.SeatRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatService {
    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;

    public SeatService(SeatRepository seatRepository, ScreenRepository screenRepository) {
        this.seatRepository = seatRepository;
        this.screenRepository = screenRepository;
    }

    public List<SeatDto> bulkCreateSeats(Long screenId) {
        Screen screen = screenRepository.findById(screenId)
            .orElseThrow(() -> new RuntimeException("Screen not found"));

        int totalSeats = screen.getTotalSeats();
        int seatsPerRow = 10;
        int rows = (int) Math.ceil((double) totalSeats / seatsPerRow);
        
        List<Seat> seats = new ArrayList<>();
        char rowChar = 'A';
        int seatCounter = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 1; c <= seatsPerRow; c++) {
                if (seatCounter >= totalSeats) break;
                
                Seat seat = new Seat();
                seat.setSeatNumber(rowChar + String.valueOf(c));
                seat.setSeatType(r < rows / 2 ? "STANDARD" : "PREMIUM");
                seat.setScreen(screen);
                seats.add(seat);
                
                seatCounter++;
            }
            rowChar++;
        }

        List<Seat> savedSeats = seatRepository.saveAll(seats);
        return savedSeats.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<SeatDto> getSeatsByScreen(Long screenId) {
        return seatRepository.findByScreenId(screenId).stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }

    private SeatDto mapToDto(Seat seat) {
        SeatDto dto = new SeatDto();
        dto.setId(seat.getId());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setSeatType(seat.getSeatType());
        dto.setScreenId(seat.getScreen().getId());
        return dto;
    }
}
''',
    'service/ShowService.java': '''package com.bookmyshow.service;

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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public ShowDto addShow(ShowDto dto) {
        Movie movie = movieRepository.findById(dto.getMovieId())
            .orElseThrow(() -> new RuntimeException("Movie not found"));
        Screen screen = screenRepository.findById(dto.getScreenId())
            .orElseThrow(() -> new RuntimeException("Screen not found"));

        Show show = new Show();
        show.setMovie(movie);
        show.setScreen(screen);
        show.setShowTime(dto.getShowTime());
        show.setShowDate(dto.getShowDate());
        show.setPrice(dto.getPrice());

        Show savedShow = showRepository.save(show);
        return mapToDto(savedShow);
    }

    public List<ShowDto> getShowsByMovie(Long movieId) {
        return showRepository.findByMovieId(movieId).stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }

    public List<ShowSeatDto> getShowSeats(Long showId) {
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new RuntimeException("Show not found"));

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
            dto.setPrice(show.getPrice() + ("PREMIUM".equals(seat.getSeatType()) ? 50.0 : 0.0));
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
        return dto;
    }
}
''',
    'controller/SeatController.java': '''package com.bookmyshow.controller;

import com.bookmyshow.dto.SeatDto;
import com.bookmyshow.service.SeatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping("/screen/{screenId}")
    public ResponseEntity<?> bulkCreateSeats(@PathVariable Long screenId) {
        try {
            return new ResponseEntity<>(seatService.bulkCreateSeats(screenId), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/screen/{screenId}")
    public ResponseEntity<List<SeatDto>> getSeatsByScreen(@PathVariable Long screenId) {
        return ResponseEntity.ok(seatService.getSeatsByScreen(screenId));
    }
}
''',
    'controller/ShowController.java': '''package com.bookmyshow.controller;

import com.bookmyshow.dto.ShowDto;
import com.bookmyshow.dto.ShowSeatDto;
import com.bookmyshow.service.ShowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping
    public ResponseEntity<?> createShow(@RequestBody ShowDto showDto) {
        try {
            return new ResponseEntity<>(showService.addShow(showDto), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowDto>> getShowsByMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(showService.getShowsByMovie(movieId));
    }

    @GetMapping("/{showId}/seats")
    public ResponseEntity<?> getShowSeats(@PathVariable Long showId) {
        try {
            return ResponseEntity.ok(showService.getShowSeats(showId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
'''
}

for path, content in files.items():
    full_path = os.path.join(base_path, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w') as f:
        f.write(content)

print("All seat and show files generated successfully.")
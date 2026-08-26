import os

base_path = 'c:/Users/Hamdaan/Downloads/bookmyshow/bookmyshow/src/main/java/com/bookmyshow'

files = {
    'dto/MovieDto.java': '''package com.bookmyshow.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MovieDto {
    private Long id;
    private String title;
    private String description;
    private Integer duration;
    private String language;
    private String genre;
    private LocalDate releaseDate;
}
''',
    'dto/TheatreDto.java': '''package com.bookmyshow.dto;

import lombok.Data;

@Data
public class TheatreDto {
    private Long id;
    private String name;
    private String city;
    private String address;
}
''',
    'dto/ScreenDto.java': '''package com.bookmyshow.dto;

import lombok.Data;

@Data
public class ScreenDto {
    private Long id;
    private String screenName;
    private Integer totalSeats;
    private Long theatreId;
}
''',
    'repository/MovieRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByTitleContainingIgnoreCase(String title);
}''',
    'repository/TheatreRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {
    List<Theatre> findByCityIgnoreCase(String city);
}''',
    'repository/ScreenRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
    List<Screen> findByTheatreId(Long theatreId);
}''',
    'service/MovieService.java': '''package com.bookmyshow.service;

import com.bookmyshow.dto.MovieDto;
import com.bookmyshow.entity.Movie;
import com.bookmyshow.repository.MovieRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {
    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public MovieDto addMovie(MovieDto dto) {
        Movie movie = new Movie();
        movie.setTitle(dto.getTitle());
        movie.setDescription(dto.getDescription());
        movie.setDuration(dto.getDuration());
        movie.setLanguage(dto.getLanguage());
        movie.setGenre(dto.getGenre());
        movie.setReleaseDate(dto.getReleaseDate());
        
        Movie saved = movieRepository.save(movie);
        return mapToDto(saved);
    }

    public List<MovieDto> getAllMovies() {
        return movieRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public MovieDto getMovieById(Long id) {
        return movieRepository.findById(id).map(this::mapToDto)
            .orElseThrow(() -> new RuntimeException("Movie not found"));
    }

    public List<MovieDto> searchMoviesByTitle(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title)
            .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private MovieDto mapToDto(Movie movie) {
        MovieDto dto = new MovieDto();
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setDescription(movie.getDescription());
        dto.setDuration(movie.getDuration());
        dto.setLanguage(movie.getLanguage());
        dto.setGenre(movie.getGenre());
        dto.setReleaseDate(movie.getReleaseDate());
        return dto;
    }
}
''',
    'service/TheatreService.java': '''package com.bookmyshow.service;

import com.bookmyshow.dto.TheatreDto;
import com.bookmyshow.entity.Theatre;
import com.bookmyshow.repository.TheatreRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TheatreService {
    private final TheatreRepository theatreRepository;

    public TheatreService(TheatreRepository theatreRepository) {
        this.theatreRepository = theatreRepository;
    }

    public TheatreDto addTheatre(TheatreDto dto) {
        Theatre theatre = new Theatre();
        theatre.setName(dto.getName());
        theatre.setCity(dto.getCity());
        theatre.setAddress(dto.getAddress());
        
        Theatre saved = theatreRepository.save(theatre);
        return mapToDto(saved);
    }

    public List<TheatreDto> getAllTheatres() {
        return theatreRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public TheatreDto getTheatreById(Long id) {
        return theatreRepository.findById(id).map(this::mapToDto)
            .orElseThrow(() -> new RuntimeException("Theatre not found"));
    }

    public List<TheatreDto> getTheatresByCity(String city) {
        return theatreRepository.findByCityIgnoreCase(city)
            .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private TheatreDto mapToDto(Theatre theatre) {
        TheatreDto dto = new TheatreDto();
        dto.setId(theatre.getId());
        dto.setName(theatre.getName());
        dto.setCity(theatre.getCity());
        dto.setAddress(theatre.getAddress());
        return dto;
    }
}
''',
    'service/ScreenService.java': '''package com.bookmyshow.service;

import com.bookmyshow.dto.ScreenDto;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Theatre;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.TheatreRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScreenService {
    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;

    public ScreenService(ScreenRepository screenRepository, TheatreRepository theatreRepository) {
        this.screenRepository = screenRepository;
        this.theatreRepository = theatreRepository;
    }

    public ScreenDto addScreen(ScreenDto dto) {
        Theatre theatre = theatreRepository.findById(dto.getTheatreId())
            .orElseThrow(() -> new RuntimeException("Theatre not found"));

        Screen screen = new Screen();
        screen.setScreenName(dto.getScreenName());
        screen.setTotalSeats(dto.getTotalSeats());
        screen.setTheatre(theatre);
        
        Screen saved = screenRepository.save(screen);
        return mapToDto(saved);
    }

    public List<ScreenDto> getScreensByTheatreId(Long theatreId) {
        return screenRepository.findByTheatreId(theatreId)
            .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private ScreenDto mapToDto(Screen screen) {
        ScreenDto dto = new ScreenDto();
        dto.setId(screen.getId());
        dto.setScreenName(screen.getScreenName());
        dto.setTotalSeats(screen.getTotalSeats());
        dto.setTheatreId(screen.getTheatre().getId());
        return dto;
    }
}
''',
    'controller/MovieController.java': '''package com.bookmyshow.controller;

import com.bookmyshow.dto.MovieDto;
import com.bookmyshow.service.MovieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {
    
    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    public ResponseEntity<MovieDto> addMovie(@RequestBody MovieDto dto) {
        return new ResponseEntity<>(movieService.addMovie(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<MovieDto>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMovieById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(movieService.getMovieById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<MovieDto>> searchMovies(@RequestParam String title) {
        return ResponseEntity.ok(movieService.searchMoviesByTitle(title));
    }
}
''',
    'controller/TheatreController.java': '''package com.bookmyshow.controller;

import com.bookmyshow.dto.TheatreDto;
import com.bookmyshow.service.TheatreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/theatres")
public class TheatreController {
    
    private final TheatreService theatreService;

    public TheatreController(TheatreService theatreService) {
        this.theatreService = theatreService;
    }

    @PostMapping
    public ResponseEntity<TheatreDto> addTheatre(@RequestBody TheatreDto dto) {
        return new ResponseEntity<>(theatreService.addTheatre(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TheatreDto>> getAllTheatres() {
        return ResponseEntity.ok(theatreService.getAllTheatres());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTheatreById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(theatreService.getTheatreById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<TheatreDto>> getTheatresByCity(@PathVariable String city) {
        return ResponseEntity.ok(theatreService.getTheatresByCity(city));
    }
}
''',
    'controller/ScreenController.java': '''package com.bookmyshow.controller;

import com.bookmyshow.dto.ScreenDto;
import com.bookmyshow.service.ScreenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/screens")
public class ScreenController {
    
    private final ScreenService screenService;

    public ScreenController(ScreenService screenService) {
        this.screenService = screenService;
    }

    @PostMapping
    public ResponseEntity<?> addScreen(@RequestBody ScreenDto dto) {
        try {
            return new ResponseEntity<>(screenService.addScreen(dto), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/theatre/{theatreId}")
    public ResponseEntity<List<ScreenDto>> getScreensByTheatreId(@PathVariable Long theatreId) {
        return ResponseEntity.ok(screenService.getScreensByTheatreId(theatreId));
    }
}
'''
}

for path, content in files.items():
    full_path = os.path.join(base_path, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w') as f:
        f.write(content)

print("All files generated successfully.")
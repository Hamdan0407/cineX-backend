import os

base_path = 'c:/Users/Hamdaan/Downloads/bookmyshow/bookmyshow/src/main/java/com/bookmyshow'

files = {
    'entity/User.java': '''package com.bookmyshow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users") // User is reserved in H2
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String role;
    private LocalDateTime createdAt = LocalDateTime.now();
}
''',
    'entity/Movie.java': '''package com.bookmyshow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private Integer duration;
    private String language;
    private String genre;
    private LocalDate releaseDate;
}
''',
    'entity/Theatre.java': '''package com.bookmyshow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Theatre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String city;
    private String address;
}
''',
    'entity/Screen.java': '''package com.bookmyshow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Screen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String screenName;
    private Integer totalSeats;
    @ManyToOne
    private Theatre theatre;
}
''',
    'entity/Show.java': '''package com.bookmyshow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "shows")
@Data
public class Show {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Movie movie;
    @ManyToOne
    private Screen screen;
    private LocalTime showTime;
    private LocalDate showDate;
    private Double price;
}
''',
    'entity/Seat.java': '''package com.bookmyshow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String seatNumber;
    private String seatType;
    @ManyToOne
    private Screen screen;
}
''',
    'entity/Booking.java': '''package com.bookmyshow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private User user;
    @ManyToOne
    private Show show;
    private String status;
    private Double totalAmount;
    private LocalDateTime bookingTime = LocalDateTime.now();
}
''',
    'entity/BookingSeat.java': '''package com.bookmyshow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class BookingSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Booking booking;
    @ManyToOne
    private Seat seat;
    private String status;
}
''',
    'repository/UserRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User, Long> {}''',
    'repository/MovieRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MovieRepository extends JpaRepository<Movie, Long> {}''',
    'repository/TheatreRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TheatreRepository extends JpaRepository<Theatre, Long> {}''',
    'repository/ScreenRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ScreenRepository extends JpaRepository<Screen, Long> {}''',
    'repository/ShowRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ShowRepository extends JpaRepository<Show, Long> {}''',
    'repository/SeatRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SeatRepository extends JpaRepository<Seat, Long> {}''',
    'repository/BookingRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BookingRepository extends JpaRepository<Booking, Long> {}''',
    'repository/BookingSeatRepository.java': '''package com.bookmyshow.repository;
import com.bookmyshow.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {}''',
    'service/package-info.java': 'package com.bookmyshow.service;',
    'controller/package-info.java': 'package com.bookmyshow.controller;',
    'dto/package-info.java': 'package com.bookmyshow.dto;',
    'config/package-info.java': 'package com.bookmyshow.config;'
}

for path, content in files.items():
    full_path = os.path.join(base_path, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w') as f:
        f.write(content)

print("All files generated successfully.")
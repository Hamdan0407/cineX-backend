package com.bookmyshow.repository;
import com.bookmyshow.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByTitleContainingIgnoreCase(String title);
    Page<Movie> findByTitleContainingIgnoreCaseOrGenreContainingIgnoreCaseOrLanguageIgnoreCase(String title, String genre, String language, Pageable pageable);
}
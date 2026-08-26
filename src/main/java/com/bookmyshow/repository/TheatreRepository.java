package com.bookmyshow.repository;
import com.bookmyshow.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {
    List<Theatre> findByCityIgnoreCase(String city);
    Page<Theatre> findByNameContainingIgnoreCaseOrCityIgnoreCaseOrAddressContainingIgnoreCase(String name, String city, String address, Pageable pageable);
}
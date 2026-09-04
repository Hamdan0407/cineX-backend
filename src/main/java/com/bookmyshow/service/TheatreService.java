package com.bookmyshow.service;

import com.bookmyshow.dto.TheatreDto;
import com.bookmyshow.dto.TheatreRequest;
import com.bookmyshow.dto.TheatreResponse;
import com.bookmyshow.dto.mapper.TheatreMapper;
import com.bookmyshow.entity.Theatre;
import com.bookmyshow.repository.TheatreRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.bookmyshow.exception.ResourceNotFoundException;

@Slf4j
@Service
public class TheatreService {
    private final TheatreRepository theatreRepository;

    public TheatreService(TheatreRepository theatreRepository) {
        this.theatreRepository = theatreRepository;
    }

    @Caching(evict = {
        @CacheEvict(value = "theatres", allEntries = true),
        @CacheEvict(value = "cities", allEntries = true)
    })
    public TheatreResponse addTheatre(TheatreRequest request) {
        log.info("Adding new theatre: {} in {}", request.getName(), request.getCity());
        Theatre saved = theatreRepository.save(TheatreMapper.toNewEntity(request));
        return TheatreMapper.toResponse(saved);
    }

    @Deprecated
    @Caching(evict = {
        @CacheEvict(value = "theatres", allEntries = true),
        @CacheEvict(value = "cities", allEntries = true)
    })
    public TheatreResponse addTheatre(TheatreDto dto) {
        return addTheatre(TheatreMapper.toRequest(dto));
    }

    @Caching(evict = {
        @CacheEvict(value = "theatres", allEntries = true),
        @CacheEvict(value = "cities", allEntries = true)
    })
    public TheatreResponse updateTheatre(Long id, TheatreRequest request) {
        log.info("Updating theatre id: {}", id);
        Theatre theatre = theatreRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Theatre not found"));
        theatre.setName(request.getName());
        theatre.setCity(request.getCity());
        theatre.setAddress(request.getAddress());
        if (request.getAmenities() != null) {
            theatre.setAmenities(request.getAmenities());
        }
        return TheatreMapper.toResponse(theatreRepository.save(theatre));
    }

    @Deprecated
    @Caching(evict = {
        @CacheEvict(value = "theatres", allEntries = true),
        @CacheEvict(value = "cities", allEntries = true)
    })
    public TheatreResponse updateTheatre(Long id, TheatreDto dto) {
        return updateTheatre(id, TheatreMapper.toRequest(dto));
    }

    @Caching(evict = {
        @CacheEvict(value = "theatres", allEntries = true),
        @CacheEvict(value = "cities", allEntries = true)
    })
    public void deleteTheatre(Long id) {
        log.info("Deleting theatre id: {}", id);
        if (!theatreRepository.existsById(id)) {
            throw new ResourceNotFoundException("Theatre not found");
        }
        theatreRepository.deleteById(id);
    }

    @Cacheable(value = "theatres", key = "'all'")
    public List<TheatreResponse> getAllTheatres() {
        log.info("Cache Miss: Fetching all theatres from MySQL database");
        return theatreRepository.findAll().stream().map(TheatreMapper::toResponse).collect(Collectors.toList());
    }

    @Cacheable(value = "theatres", key = "#id")
    public TheatreResponse getTheatreById(Long id) {
        log.info("Cache Miss: Fetching theatre id {} from MySQL database", id);
        return theatreRepository.findById(id).map(TheatreMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Theatre not found"));
    }

    @Cacheable(value = "theatres", key = "'city_' + #city.toLowerCase()")
    public List<TheatreResponse> getTheatresByCity(String city) {
        log.info("Cache Miss: Fetching theatres for city {} from MySQL database", city);
        return theatreRepository.findByCityIgnoreCase(city)
            .stream().map(TheatreMapper::toResponse).collect(Collectors.toList());
    }

    @Cacheable(value = "cities", key = "'all'")
    public List<String> getAllCities() {
        log.info("Cache Miss: Fetching all distinct cities from MySQL database");
        return theatreRepository.findAll().stream()
            .map(Theatre::getCity)
            .filter(c -> c != null && !c.trim().isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    public Page<TheatreResponse> getTheatresPaginated(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return theatreRepository.findAll(pageable).map(TheatreMapper::toResponse);
    }

    public Page<TheatreResponse> searchTheatresPaginated(String query, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return theatreRepository.findByNameContainingIgnoreCaseOrCityIgnoreCaseOrAddressContainingIgnoreCase(query, query, query, pageable)
                .map(TheatreMapper::toResponse);
    }
}

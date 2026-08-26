package com.bookmyshow.service;

import com.bookmyshow.dto.ScreenDto;
import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Theatre;
import com.bookmyshow.repository.ScreenRepository;
import com.bookmyshow.repository.TheatreRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

import com.bookmyshow.exception.ResourceNotFoundException;
@Slf4j
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
            .orElseThrow(() -> new ResourceNotFoundException("Theatre not found"));

        Screen screen = new Screen();
        screen.setScreenName(dto.getScreenName());
        screen.setTotalSeats(dto.getTotalSeats());
        screen.setTotalRows(dto.getTotalRows() != null ? dto.getTotalRows() : 10);
        screen.setTotalColumns(dto.getTotalColumns() != null ? dto.getTotalColumns() : 10);
        screen.setSeatCategories(dto.getSeatCategories() != null ? dto.getSeatCategories() : "VIP,Executive,Premium,Recliner,Gold,Silver");
        screen.setTheatre(theatre);
        
        Screen saved = screenRepository.save(screen);
        return mapToDto(saved);
    }

    public ScreenDto updateScreen(Long id, ScreenDto dto) {
        Screen screen = screenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));
        screen.setScreenName(dto.getScreenName());
        screen.setTotalSeats(dto.getTotalSeats());
        if (dto.getTotalRows() != null) screen.setTotalRows(dto.getTotalRows());
        if (dto.getTotalColumns() != null) screen.setTotalColumns(dto.getTotalColumns());
        if (dto.getSeatCategories() != null) screen.setSeatCategories(dto.getSeatCategories());

        if (dto.getTheatreId() != null && !dto.getTheatreId().equals(screen.getTheatre().getId())) {
            Theatre theatre = theatreRepository.findById(dto.getTheatreId())
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found"));
            screen.setTheatre(theatre);
        }

        Screen updated = screenRepository.save(screen);
        return mapToDto(updated);
    }

    public void deleteScreen(Long id) {
        if (!screenRepository.existsById(id)) {
            throw new ResourceNotFoundException("Screen not found");
        }
        screenRepository.deleteById(id);
    }

    public List<ScreenDto> getAllScreens() {
        return screenRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
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
        dto.setTotalRows(screen.getTotalRows());
        dto.setTotalColumns(screen.getTotalColumns());
        dto.setSeatCategories(screen.getSeatCategories());
        dto.setTheatreId(screen.getTheatre().getId());
        return dto;
    }
}

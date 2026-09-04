package com.bookmyshow.service;

import com.bookmyshow.dto.ScreenDto;
import com.bookmyshow.dto.ScreenRequest;
import com.bookmyshow.dto.ScreenResponse;
import com.bookmyshow.dto.mapper.ScreenMapper;
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

    public ScreenResponse addScreen(ScreenRequest request) {
        Theatre theatre = theatreRepository.findById(request.getTheatreId())
            .orElseThrow(() -> new ResourceNotFoundException("Theatre not found"));
        Screen saved = screenRepository.save(ScreenMapper.toNewEntity(request, theatre));
        return ScreenMapper.toResponse(saved);
    }

    @Deprecated
    public ScreenResponse addScreen(ScreenDto dto) {
        return addScreen(ScreenMapper.toRequest(dto));
    }

    public ScreenResponse updateScreen(Long id, ScreenRequest request) {
        Screen screen = screenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));
        Theatre theatre = null;
        if (request.getTheatreId() != null && !request.getTheatreId().equals(screen.getTheatre().getId())) {
            theatre = theatreRepository.findById(request.getTheatreId())
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found"));
        }
        ScreenMapper.applyUpdate(screen, request, theatre);
        return ScreenMapper.toResponse(screenRepository.save(screen));
    }

    @Deprecated
    public ScreenResponse updateScreen(Long id, ScreenDto dto) {
        return updateScreen(id, ScreenMapper.toRequest(dto));
    }

    public void deleteScreen(Long id) {
        if (!screenRepository.existsById(id)) {
            throw new ResourceNotFoundException("Screen not found");
        }
        screenRepository.deleteById(id);
    }

    public List<ScreenResponse> getAllScreens() {
        return screenRepository.findAll().stream().map(ScreenMapper::toResponse).collect(Collectors.toList());
    }

    public List<ScreenResponse> getScreensByTheatreId(Long theatreId) {
        return screenRepository.findByTheatreId(theatreId)
            .stream().map(ScreenMapper::toResponse).collect(Collectors.toList());
    }
}

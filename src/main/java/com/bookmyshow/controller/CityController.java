package com.bookmyshow.controller;

import com.bookmyshow.dto.CitySearchResponse;
import com.bookmyshow.service.CitySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CitySearchService citySearchService;

    @GetMapping
    public List<CitySearchResponse> list() {
        return citySearchService.list();
    }

    @GetMapping("/search")
    public List<CitySearchResponse> search(@RequestParam("query") String query) {
        return citySearchService.search(query);
    }
}

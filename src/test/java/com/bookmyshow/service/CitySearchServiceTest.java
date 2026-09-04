package com.bookmyshow.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CitySearchServiceTest {

    @Autowired
    private CitySearchService citySearchService;

    @Test
    @DisplayName("Search finds Coimbatore by partial name")
    void searchCoimbatore() {
        var results = citySearchService.search("coimbatore");
        assertFalse(results.isEmpty());
        assertEquals("Coimbatore", results.get(0).getName());
        assertEquals("Tamil Nadu", results.get(0).getState());
        assertEquals("India", results.get(0).getCountry());
        assertEquals("coimbatore", results.get(0).getNormalizedName());
    }

    @Test
    @DisplayName("Search finds Ambur from the supported city dataset")
    void searchAmbur() {
        var results = citySearchService.search(" ambur ");
        assertFalse(results.isEmpty());
        assertEquals("Ambur", results.get(0).getName());
        assertEquals("Tamil Nadu", results.get(0).getState());
    }

    @Test
    @DisplayName("Public catalog includes supported cities and cities without CineX shows")
    void listIncludesSupportedAndUnsupportedCities() {
        var cities = citySearchService.list();
        assertTrue(cities.stream().anyMatch(city -> "Chennai".equals(city.getName()) && city.isCinexAvailable()));
        assertTrue(cities.stream().anyMatch(city -> "Pune".equals(city.getName()) && !city.isCinexAvailable()));
    }

    @Test
    @DisplayName("Search resolves Trichy alias to Tiruchirappalli")
    void searchTrichyAlias() {
        var results = citySearchService.search("trichy");
        assertFalse(results.isEmpty());
        assertEquals("Tiruchirappalli", results.get(0).getName());
    }

    @Test
    @DisplayName("Search marks CineX supported cities as available")
    void searchChennaiIsAvailable() {
        var results = citySearchService.search("chennai");
        assertFalse(results.isEmpty());
        assertEquals("Chennai", results.get(0).getName());
        assertTrue(results.get(0).isCinexAvailable());
        assertEquals("chennai", results.get(0).getLandmarkId());
    }

    @Test
    @DisplayName("Search marks unsupported cities without landmark artwork")
    void searchPuneUnsupported() {
        var results = citySearchService.search("pune");
        assertFalse(results.isEmpty());
        assertEquals("Pune", results.get(0).getName());
        assertFalse(results.get(0).isCinexAvailable());
        assertNull(results.get(0).getLandmarkId());
    }

    @Test
    @DisplayName("Blank query returns no results")
    void blankQueryReturnsEmpty() {
        assertTrue(citySearchService.search("").isEmpty());
        assertTrue(citySearchService.search("   ").isEmpty());
    }

    @Test
    @DisplayName("Unknown query returns no results")
    void unknownQueryReturnsEmpty() {
        assertTrue(citySearchService.search("xyznotacity").isEmpty());
    }
}

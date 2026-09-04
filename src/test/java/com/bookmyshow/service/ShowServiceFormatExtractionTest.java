package com.bookmyshow.service;

import com.bookmyshow.entity.Screen;
import com.bookmyshow.entity.Show;
import com.bookmyshow.entity.Theatre;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShowServiceFormatExtractionTest {

    @Test
    void extractPresentationFormats_detectsImaxAnd4dxFromAmenities() {
        Theatre theatre = new Theatre();
        theatre.setAmenities("4DX • Dolby Atmos • RealD 3D");

        Screen screen = new Screen();
        screen.setScreenName("Screen 1");
        screen.setTheatre(theatre);

        Show show = new Show();
        show.setScreen(screen);

        List<String> formats = ShowService.extractPresentationFormats(List.of(show));

        assertTrue(formats.contains("2D"));
        assertTrue(formats.contains("4DX"));
        assertTrue(formats.contains("3D"));
    }

    @Test
    void extractPresentationFormats_detectsImaxFromScreenName() {
        Theatre theatre = new Theatre();
        theatre.setAmenities("Laser Projection");

        Screen screen = new Screen();
        screen.setScreenName("IMAX Auditorium");
        screen.setTheatre(theatre);

        Show show = new Show();
        show.setScreen(screen);

        List<String> formats = ShowService.extractPresentationFormats(List.of(show));

        assertEquals(List.of("2D", "IMAX 2D"), formats);
    }
}

package com.bookmyshow.controller;

import com.bookmyshow.entity.Booking;
import com.bookmyshow.entity.Show;
import com.bookmyshow.repository.BookingRepository;
import com.bookmyshow.support.ShowInventoryTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DtoArchitectureTest extends ShowInventoryTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private BookingRepository bookingRepository;

    @BeforeEach
    void seedInventory() {
        ensureCatalogMovies();
        ensureSampleShowInventory();
    }

    @Test
    @DisplayName("REST controllers do not expose JPA entities in signatures")
    void controllersDoNotExposeEntities() {
        Set<String> violations = new HashSet<>();
        for (Class<?> controllerClass : findRestControllers()) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!method.getDeclaringClass().getPackageName().startsWith("com.bookmyshow.controller")) {
                    continue;
                }
                for (Type type : collectTypes(method.getGenericReturnType())) {
                    if (isEntityType(type)) {
                        violations.add(controllerClass.getSimpleName() + "#" + method.getName() + " return type " + type);
                    }
                }
                for (Parameter parameter : method.getParameters()) {
                    for (Type type : collectTypes(parameter.getParameterizedType())) {
                        if (isEntityType(type)) {
                            violations.add(controllerClass.getSimpleName() + "#" + method.getName()
                                    + " parameter " + parameter.getName() + " type " + type);
                        }
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), "Entity exposure found: " + violations);
    }

    @Test
    @DisplayName("Movie API returns DTO fields and hides sensitive persistence data")
    void movieApiReturnsDtoNotEntity() throws Exception {
        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].title").isString())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].hibernateLazyInitializer").doesNotExist());
    }

    @Test
    @DisplayName("Show and seat APIs preserve existing response contract")
    void showAndSeatApisRemainCompatible() throws Exception {
        Show show = requireSampleShow();

        mockMvc.perform(get("/api/shows/movie/{movieId}", show.getMovie().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(show.getId()))
                .andExpect(jsonPath("$[0].movieId").value(show.getMovie().getId()))
                .andExpect(jsonPath("$[0].screenId").value(show.getScreen().getId()))
                .andExpect(jsonPath("$[0].movie").doesNotExist())
                .andExpect(jsonPath("$[0].screen").doesNotExist());

        mockMvc.perform(get("/api/shows/{showId}/seats", show.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatId").isNumber())
                .andExpect(jsonPath("$[0].seatNumber").isString())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].seat").doesNotExist());
    }

    @Test
    @DisplayName("Theatre API returns response DTOs without nested entity graphs")
    void theatreApiReturnsDtoNotEntity() throws Exception {
        mockMvc.perform(get("/api/theatres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].screens").doesNotExist());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("Booking API does not expose user passwords or nested entities")
    void bookingApiDoesNotExposeSensitiveFields() throws Exception {
        Booking booking = bookingRepository.findAll().stream()
                .filter(existing -> "user".equals(existing.getClerkUserId()))
                .findFirst()
                .orElseGet(() -> {
            Show show = requireSampleShow();
            Booking created = new Booking();
            created.setShow(show);
            created.setClerkUserId("user");
            created.setBookingStatus("BOOKED");
            created.setPaymentStatus("SUCCESS");
            created.setSeatIds("A1");
            created.setAmount(250.0);
            return bookingRepository.save(created);
        });

        mockMvc.perform(get("/api/bookings/{bookingId}", booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(booking.getId()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.signature").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.show").doesNotExist());
    }

    private static Set<Class<?>> findRestControllers() {
        Set<Class<?>> controllers = new HashSet<>();
        for (Class<?> candidate : new Class<?>[] {
                MovieController.class,
                TheatreController.class,
                ScreenController.class,
                SeatController.class,
                ShowController.class,
                BookingController.class,
                PaymentController.class,
                UserController.class,
                TicketController.class,
                AdminDashboardController.class,
                CacheController.class,
                TmdbController.class
        }) {
            if (candidate.isAnnotationPresent(RestController.class)) {
                controllers.add(candidate);
            }
        }
        return controllers;
    }

    private static Set<Type> collectTypes(Type type) {
        Set<Type> types = new HashSet<>();
        collectTypesRecursive(type, types);
        return types;
    }

    private static void collectTypesRecursive(Type type, Set<Type> types) {
        if (type == null || types.contains(type)) {
            return;
        }
        types.add(type);
        if (type instanceof ParameterizedType parameterizedType) {
            types.add(parameterizedType.getRawType());
            for (Type arg : parameterizedType.getActualTypeArguments()) {
                collectTypesRecursive(arg, types);
            }
        } else if (type instanceof Class<?> clazz) {
            for (Type iface : clazz.getGenericInterfaces()) {
                collectTypesRecursive(iface, types);
            }
            Type superType = clazz.getGenericSuperclass();
            if (superType != null) {
                collectTypesRecursive(superType, types);
            }
        }
    }

    private static boolean isEntityType(Type type) {
        if (!(type instanceof Class<?> clazz)) {
            return false;
        }
        return clazz.getPackageName().equals("com.bookmyshow.entity");
    }
}

package com.bookmyshow.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic CineX screen seating layouts.
 * PVR Sathyam Royapettah is a 6-screen multiplex (Wikipedia / PVR venue list).
 * Layouts are scaled for a bookable UI, not a random generator, and differ per screen.
 */
public final class ScreenLayoutCatalog {

    private ScreenLayoutCatalog() {}

    public static final String SATHYAM_SCREEN_1 = "sathyam-screen-1-dolby";
    public static final String SATHYAM_SCREEN_2 = "sathyam-screen-2";
    public static final String SATHYAM_SCREEN_3 = "sathyam-screen-3";
    public static final String SATHYAM_SCREEN_4 = "sathyam-screen-4";
    public static final String SATHYAM_SCREEN_5 = "sathyam-screen-5";
    public static final String SATHYAM_ELITE = "sathyam-elite-recliner";
    public static final String STANDARD_AUDITORIUM = "standard-auditorium";

    public static final String SCREEN_NAME_1 = "Screen 1 (Dolby Atmos)";
    public static final String SCREEN_NAME_2 = "Screen 2";
    public static final String SCREEN_NAME_3 = "Screen 3";
    public static final String SCREEN_NAME_4 = "Screen 4";
    public static final String SCREEN_NAME_5 = "Screen 5";
    public static final String SCREEN_NAME_ELITE = "Elite";

    /** 4 | aisle | 8 | aisle | 4 */
    private static final int[] WIDE_16 = {0, 1, 2, 3, 6, 7, 8, 9, 10, 11, 12, 13, 16, 17, 18, 19};
    /** 3 | aisle | 6 | aisle | 3 */
    private static final int[] MEDIUM_12 = {0, 1, 2, 5, 6, 7, 8, 9, 10, 13, 14, 15};
    /** 4 | aisle | 5 | aisle | 4 */
    private static final int[] MEDIUM_13 = {1, 2, 3, 4, 7, 8, 9, 10, 11, 14, 15, 16, 17};
    /** 3 | aisle | 4 | aisle | 3 */
    private static final int[] COMPACT_10 = {0, 1, 2, 5, 6, 7, 8, 11, 12, 13};
    /** 2 | aisle | 4 | aisle | 2 recliner */
    private static final int[] ELITE_8 = {1, 2, 6, 7, 8, 9, 13, 14};

    public static Map<String, String> sathyamScreens() {
        Map<String, String> screens = new LinkedHashMap<>();
        screens.put(SCREEN_NAME_1, SATHYAM_SCREEN_1);
        screens.put(SCREEN_NAME_2, SATHYAM_SCREEN_2);
        screens.put(SCREEN_NAME_3, SATHYAM_SCREEN_3);
        screens.put(SCREEN_NAME_4, SATHYAM_SCREEN_4);
        screens.put(SCREEN_NAME_5, SATHYAM_SCREEN_5);
        screens.put(SCREEN_NAME_ELITE, SATHYAM_ELITE);
        return screens;
    }

    public static List<SeatBlueprint> seatsForProfile(String profile) {
        return switch (profile) {
            case SATHYAM_SCREEN_1 -> sathyamScreen1Dolby();
            case SATHYAM_SCREEN_2 -> sathyamScreen2();
            case SATHYAM_SCREEN_3 -> sathyamScreen3();
            case SATHYAM_SCREEN_4 -> sathyamScreen4();
            case SATHYAM_SCREEN_5 -> sathyamScreen5();
            case SATHYAM_ELITE -> sathyamElite();
            case STANDARD_AUDITORIUM -> standardAuditorium();
            default -> standardAuditorium();
        };
    }

    public static int expectedSeatCount(String profile) {
        return seatsForProfile(profile).size();
    }

    /** Flagship hall: 10 rows, 16-seat blocks, wheelchair ends on last row. */
    private static List<SeatBlueprint> sathyamScreen1Dolby() {
        List<SeatBlueprint> seats = new ArrayList<>();
        int rowIdx = 0;
        for (char row = 'A'; row <= 'B'; row++) {
            addRow(seats, String.valueOf(row), rowIdx++, WIDE_16, "ROYALE", 120.0);
        }
        for (char row = 'C'; row <= 'F'; row++) {
            addRow(seats, String.valueOf(row), rowIdx++, WIDE_16, "CLUB", 60.0);
        }
        for (char row = 'G'; row <= 'I'; row++) {
            addRow(seats, String.valueOf(row), rowIdx++, WIDE_16, "CLASSIC", 0.0);
        }
        addRow(seats, "J", rowIdx, WIDE_16, "CLASSIC", 0.0);
        seats.add(blueprint("J", rowIdx, -1, "J-W1", "WHEELCHAIR", 0.0, true));
        seats.add(blueprint("J", rowIdx, 20, "J-W2", "WHEELCHAIR", 0.0, true));
        return seats;
    }

    /** Mid hall: 8 rows of 12. */
    private static List<SeatBlueprint> sathyamScreen2() {
        List<SeatBlueprint> seats = new ArrayList<>();
        int rowIdx = 0;
        for (char row = 'A'; row <= 'B'; row++) {
            addRow(seats, String.valueOf(row), rowIdx++, MEDIUM_12, "CLUB", 40.0);
        }
        for (char row = 'C'; row <= 'H'; row++) {
            addRow(seats, String.valueOf(row), rowIdx++, MEDIUM_12, "CLASSIC", 0.0);
        }
        return seats;
    }

    /** Slightly offset 13-seat rows, premium first row. */
    private static List<SeatBlueprint> sathyamScreen3() {
        List<SeatBlueprint> seats = new ArrayList<>();
        int rowIdx = 0;
        addRow(seats, "A", rowIdx++, MEDIUM_13, "PREMIUM", 50.0);
        for (char row = 'B'; row <= 'G'; row++) {
            addRow(seats, String.valueOf(row), rowIdx++, MEDIUM_13, "CLASSIC", 0.0);
        }
        return seats;
    }

    /** Compact 6 × 12. */
    private static List<SeatBlueprint> sathyamScreen4() {
        List<SeatBlueprint> seats = new ArrayList<>();
        int rowIdx = 0;
        for (char row = 'A'; row <= 'B'; row++) {
            addRow(seats, String.valueOf(row), rowIdx++, MEDIUM_12, "CLUB", 30.0);
        }
        for (char row = 'C'; row <= 'F'; row++) {
            addRow(seats, String.valueOf(row), rowIdx++, MEDIUM_12, "CLASSIC", 0.0);
        }
        return seats;
    }

    /** Smallest standard hall: 6 × 10. */
    private static List<SeatBlueprint> sathyamScreen5() {
        List<SeatBlueprint> seats = new ArrayList<>();
        int rowIdx = 0;
        addRow(seats, "A", rowIdx++, COMPACT_10, "CLUB", 20.0);
        for (char row = 'B'; row <= 'F'; row++) {
            addRow(seats, String.valueOf(row), rowIdx++, COMPACT_10, "CLASSIC", 0.0);
        }
        return seats;
    }

    /** Elite recliner lounge — all ROYALE, wheelchair spaces on row E. */
    private static List<SeatBlueprint> sathyamElite() {
        List<SeatBlueprint> seats = new ArrayList<>();
        int rowIdx = 0;
        for (char row = 'A'; row <= 'D'; row++) {
            addRow(seats, String.valueOf(row), rowIdx++, ELITE_8, "ROYALE", 150.0);
        }
        addRow(seats, "E", rowIdx, ELITE_8, "ROYALE", 150.0);
        seats.add(blueprint("E", rowIdx, 0, "E-W1", "WHEELCHAIR", 0.0, true));
        seats.add(blueprint("E", rowIdx, 15, "E-W2", "WHEELCHAIR", 0.0, true));
        return seats;
    }

    private static List<SeatBlueprint> standardAuditorium() {
        List<SeatBlueprint> seats = new ArrayList<>();
        int rowIdx = 0;
        for (char row = 'A'; row <= 'F'; row++) {
            String type = row <= 'B' ? "PREMIUM" : "CLASSIC";
            double premium = row <= 'B' ? 50.0 : 0.0;
            addRow(seats, String.valueOf(row), rowIdx++, WIDE_16, type, premium);
        }
        return seats;
    }

    private static void addRow(List<SeatBlueprint> seats, String rowLabel, int rowIndex, int[] columns,
                               String seatType, double priceOffset) {
        int seatNum = 1;
        for (int col : columns) {
            seats.add(blueprint(rowLabel, rowIndex, col, rowLabel + seatNum, seatType, priceOffset, false));
            seatNum++;
        }
    }

    private static SeatBlueprint blueprint(String rowLabel, int rowIndex, int columnIndex, String seatNumber,
                                           String seatType, double priceOffset, boolean wheelchair) {
        return new SeatBlueprint(rowLabel, rowIndex, columnIndex, seatNumber, seatType, priceOffset, wheelchair);
    }

    public record SeatBlueprint(
            String rowLabel,
            int rowIndex,
            int columnIndex,
            String seatNumber,
            String seatType,
            double priceOffset,
            boolean wheelchairAccessible
    ) {}
}

package com.backend.event.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class EventCsvFixtureLoader {
    private static final LocalDate CSV_REFERENCE_DATE = LocalDate.of(2026, 10, 16);

    private final JdbcTemplate jdbcTemplate;

    public EventCsvFixtureLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void loadIfEmpty() throws Exception {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event",
                Integer.class
        );

        if (count != null && count > 0) {
            return;
        }

        long shiftDays = ChronoUnit.DAYS.between(CSV_REFERENCE_DATE, LocalDate.now());
        var resource = new ClassPathResource("testdata/events.csv");

        try (Reader reader = new InputStreamReader(
                resource.getInputStream(),
                StandardCharsets.UTF_8
        );
             CSVParser parser = CSVParser.parse(
                     reader,
                     CSVFormat.DEFAULT.builder()
                             .setHeader()
                             .setSkipHeaderRecord(true)
                             .build()
             )) {

            for (CSVRecord record : parser) {
                Long brandId = jdbcTemplate.queryForObject(
                        "SELECT id FROM brand WHERE name = ?",
                        Long.class,
                        record.get("brand_name").trim()
                );

                jdbcTemplate.update("""
                        INSERT INTO event (
                            id, title, description, url, img,
                            start_date, end_date, view_count,
                            is_active, brand
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        Long.parseLong(record.get("id")),
                        record.get("title"),
                        emptyToNull(record.get("description")),
                        record.get("url"),
                        record.get("img"),
                        Timestamp.valueOf(parseDate(record.get("start_date")).plusDays(shiftDays)),
                        nullableTimestamp(record.get("end_date"), shiftDays),
                        Long.parseLong(record.get("view_count")),
                        parseBoolean(record.get("is_active")),
                        brandId
                );
            }
        }
    }

    private static boolean isNullLiteral(String value) {
        return value == null
                || value.isBlank()
                || value.trim().equalsIgnoreCase("NULL");
    }

    private static String emptyToNull(String value) {
        return isNullLiteral(value) ? null : value;
    }

    private static LocalDateTime parseDate(String value) {
        return LocalDateTime.parse(value.trim().replace(' ', 'T'));
    }

    private static Timestamp nullableTimestamp(String value, long shiftDays) {
        if (isNullLiteral(value)) {
            return null;
        }
        return Timestamp.valueOf(parseDate(value).plusDays(shiftDays));
    }

    private static boolean parseBoolean(String value) {
        String normalized = value.trim()
                .replace("'", "")
                .replace("\"", "")
                .toLowerCase();

        if (normalized.startsWith("b")) {
            normalized = normalized.substring(1);
        }

        return normalized.equals("1") || normalized.equals("true");
    }
}
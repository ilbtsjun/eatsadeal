package com.backend.event.service;

import com.backend.event.dto.EventSearchRequest;
import com.backend.event.dto.GetEventListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventSearchIntegrationTest {

    private static final int MIN_SEARCHABLE = 30;

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("event_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private EventCsvFixtureLoader fixtureLoader;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EventService eventService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        fixtureLoader.loadIfEmpty();
    }

    // searchEvents 의 조건(활성 + 미종료)을 JPQL 과 별개로 SQL 로 계산한 기대값
    private int countSearchable() {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM event
                        WHERE is_active = 1
                          AND (end_date IS NULL OR end_date >= ?)
                        """,
                Integer.class,
                Timestamp.valueOf(LocalDateTime.now())
        );
        return count == null ? 0 : count;
    }

    private static EventSearchRequest request(int page, int size) {
        return new EventSearchRequest(null, null, null, null, "latest", page, size);
    }

    @Test
    void eventFixtureIsLoaded() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM event", Integer.class);
        assertTrue(count != null && count > 0, "테스트용 이벤트 데이터가 적재되어야 합니다.");
    }

    @Test
    void fixtureHasEnoughSearchableEvents() {
        int searchable = countSearchable();
        assertTrue(searchable >= MIN_SEARCHABLE,
                "검색 조건(활성+미종료)에 맞는 이벤트가 " + MIN_SEARCHABLE + "건 이상이어야 합니다. 현재: " + searchable
                        + " (CSV 날짜 이동 기준일을 확인하세요)");
    }

    @Test
    void searchEvents_firstPage_returnsRequestedSize() {
        int expected = countSearchable();

        Page<GetEventListResponse> page = eventService.searchEvents(request(0, 20));

        assertEquals(expected, page.getTotalElements());
        assertEquals(20, page.getContent().size());
        assertEquals(20, page.getContent().stream().map(GetEventListResponse::id).distinct().count(),
                "한 페이지 안에 중복 이벤트가 없어야 합니다.");
        page.getContent().forEach(event -> {
            assertNotNull(event.brandName());
            assertTrue(event.isActive());
        });
    }

    @Test
    void searchEvents_lastPage_returnsRemainder() {
        int expected = countSearchable();

        Page<GetEventListResponse> page = eventService.searchEvents(request(1, 20));

        assertEquals(Math.min(20, expected - 20), page.getContent().size());
    }

    @Test
    void searchEvents_pageOutOfRange_returnsEmptyButKeepsTotal() {
        int expected = countSearchable();

        Page<GetEventListResponse> page = eventService.searchEvents(request(100, 20));

        assertTrue(page.getContent().isEmpty());
        assertEquals(expected, page.getTotalElements());
    }

    @Test
    void searchEvents_overHttp_returnsJsonPage() throws Exception {
        int expected = countSearchable();

        mockMvc.perform(get("/api/events")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalElements").value(expected))
                .andExpect(jsonPath("$.content[0].brandName").isNotEmpty())
                .andExpect(jsonPath("$.content[0].eventCodes").isArray());
    }
}
package com.backend.event.service;

import com.backend.common.log.QueryCounter;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventSearchIntegrationTest {

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
        registry.add("app.query-trace.enabled", () -> "true");
    }

    @Autowired
    private EventFixtureLoader fixtureLoader;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EventService eventService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fixtureLoader.loadIfEmpty();
    }

    // searchEvents 조건(브랜드 활성 + 이벤트 활성 + 미종료)을 JPQL과 별개로 SQL로 계산한 기대값
    private int countSearchable() {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM event e
                        JOIN brand b ON b.id = e.brand
                        WHERE b.is_active = 1
                          AND e.is_active = 1
                          AND (e.end_date IS NULL OR e.end_date >= ?)
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
    void fixtureHasExpectedSearchableEvents() {
        assertEquals(EventFixtureLoader.SEARCHABLE_COUNT, countSearchable(),
                "종료·비활성·비활성 브랜드 이벤트는 검색 대상에서 빠져야 합니다.");
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

    private Long brandWithMostSearchable() {
        return jdbcTemplate.queryForObject("""
                SELECT b.id FROM brand b
                JOIN event e ON e.brand = b.id
                WHERE b.is_active = 1
                  AND e.is_active = 1
                  AND (e.end_date IS NULL OR e.end_date >= ?)
                GROUP BY b.id
                ORDER BY COUNT(*) DESC
                LIMIT 1
                """, Long.class, Timestamp.valueOf(LocalDateTime.now()));
    }

    private void setBrandActive(Long brandId, boolean active) {
        jdbcTemplate.update("UPDATE brand SET is_active = ? WHERE id = ?", active, brandId);
    }

    @Test
    void searchEvents_excludesInactiveBrandEvents() {
        Long brandId = brandWithMostSearchable();
        String brandName = jdbcTemplate.queryForObject(
                "SELECT name FROM brand WHERE id = ?", String.class, brandId);
        int before = countSearchable();
        Integer brandCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM event e
                WHERE e.brand = ? AND e.is_active = 1
                  AND (e.end_date IS NULL OR e.end_date >= ?)
                """, Integer.class, brandId, Timestamp.valueOf(LocalDateTime.now()));

        setBrandActive(brandId, false);
        try {
            Page<GetEventListResponse> page = eventService.searchEvents(request(0, 20));

            assertEquals(before - brandCount, page.getTotalElements(),
                    "비활성 브랜드 이벤트 수만큼 전체 건수가 줄어야 합니다.");
            assertTrue(page.getContent().stream().noneMatch(e -> brandName.equals(e.brandName())),
                    "비활성 브랜드 이벤트가 목록에 노출되면 안 됩니다.");
        } finally {
            setBrandActive(brandId, true);
        }
    }

    @Test
    void getEvent_inactiveBrand_returnsNotFound() throws Exception {
        Long brandId = brandWithMostSearchable();
        Long eventId = jdbcTemplate.queryForObject("""
                SELECT e.id FROM event e
                WHERE e.brand = ? AND e.is_active = 1
                  AND (e.end_date IS NULL OR e.end_date >= ?)
                LIMIT 1
                """, Long.class, brandId, Timestamp.valueOf(LocalDateTime.now()));

        mockMvc.perform(get("/api/events/{id}", eventId)).andExpect(status().isOk());

        setBrandActive(brandId, false);
        try {
            mockMvc.perform(get("/api/events/{id}", eventId)).andExpect(status().isNotFound());
        } finally {
            setBrandActive(brandId, true);
        }
    }

    @Test
    void searchEvents_queryCount_doesNotGrowWithPageSize() {
        eventService.searchEvents(request(0, 12)); // 워밍업(최초 로딩 쿼리 제외)

        for (int size : new int[]{5, 12, 20}) {
            QueryCounter.begin();
            Page<GetEventListResponse> page;
            Map<String, Integer> executed;
            try {
                page = eventService.searchEvents(request(0, size));
            } finally {
                executed = QueryCounter.end();
            }

            assertEquals(size, page.getContent().size());
            assertTrue(QueryCounter.total(executed) <= 3,
                    "size=" + size + " 쿼리 수가 3개를 넘으면 안 됩니다: " + executed);
        }
    }
}
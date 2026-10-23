package com.backend.event.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class EventFixtureLoader {
    static final int SEARCHABLE_COUNT = 45;
    static final int EXPIRED_COUNT = 5;
    static final int INACTIVE_COUNT = 3;
    static final int INACTIVE_BRAND_COUNT = 2;

    private static final String[] TITLE_PATTERNS = {
            "테스트 세트 %d번 1+1 행사",
            "가상 메뉴 %d번 30%% 할인",
            "샘플 이벤트 %d번 포장 주문 3,000원 할인",
            "임시 프로모션 %d번 신규 회원 쿠폰",
            "더미 이벤트 %d번 주말 한정 증정",
    };

    private static final String[][] EVENT_CODES = {
            {"BUY_ONE_GET_ONE"},
            {"DISCOUNT_RATE"},
            {"TAKE_OUT", "DISCOUNT_PRICE"},
            {"MEMBERSHIP"},
            {"TIME_SALE", "GIFT_PROMO"},
    };

    private final JdbcTemplate jdbcTemplate;

    public EventFixtureLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void loadIfEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM event", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        List<Long> activeBrands = jdbcTemplate.queryForList(
                "SELECT id FROM brand WHERE is_active = 1 ORDER BY id", Long.class);
        Long inactiveBrand = jdbcTemplate.queryForObject(
                "SELECT id FROM brand WHERE is_active = 0 ORDER BY id LIMIT 1", Long.class);
        LocalDateTime now = LocalDateTime.now().withNano(0);

        int seq = 1;
        for (int i = 0; i < SEARCHABLE_COUNT; i++, seq++) {
            // 4건에 1건은 종료일 없는 상시 이벤트
            LocalDateTime endDate = i % 4 == 0 ? null : now.plusDays(7 + i);
            insert(seq, activeBrands.get(i % activeBrands.size()), now.minusDays(i), endDate, true, 10L * i);
        }
        for (int i = 0; i < EXPIRED_COUNT; i++, seq++) {
            insert(seq, activeBrands.get(i % activeBrands.size()), now.minusDays(60), now.minusDays(1 + i), true, 0L);
        }
        for (int i = 0; i < INACTIVE_COUNT; i++, seq++) {
            insert(seq, activeBrands.get(i % activeBrands.size()), now.minusDays(10), now.plusDays(10), false, 0L);
        }
        for (int i = 0; i < INACTIVE_BRAND_COUNT; i++, seq++) {
            insert(seq, inactiveBrand, now.minusDays(3), now.plusDays(30), true, 0L);
        }
    }

    private void insert(int seq, Long brandId, LocalDateTime startDate, LocalDateTime endDate,
                        boolean active, long viewCount) {
        int pattern = seq % TITLE_PATTERNS.length;
        jdbcTemplate.update("""
                        INSERT INTO event (
                            id, title, description, url, img,
                            start_date, end_date, view_count,
                            is_active, brand
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                (long) seq,
                TITLE_PATTERNS[pattern].formatted(seq),
                null,
                "https://example.com/events/" + seq,
                "https://example.com/images/event-" + seq + ".png",
                Timestamp.valueOf(startDate),
                endDate == null ? null : Timestamp.valueOf(endDate),
                viewCount,
                active,
                brandId
        );
        for (String code : EVENT_CODES[pattern]) {
            jdbcTemplate.update("INSERT INTO event_event_codes (event_id, event_code) VALUES (?, ?)", (long) seq, code);
        }
    }
}

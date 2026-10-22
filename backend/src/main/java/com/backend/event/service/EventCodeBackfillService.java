package com.backend.event.service;

import com.backend.event.repository.EventRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventCodeBackfillService {
    private static final int MAX_LIMIT = 200;

    private final EventRepository eventRepository;
    private final EventService eventService;
    private final AnthropicEventCodeAi eventCodeAi;

    @Schema(description = "이벤트 코드 일괄 분류 결과")
    public record Result(
            @Schema(description = "이번에 살펴본 이벤트 수") int processed,
            @Schema(description = "코드가 정해진 이벤트 수") int classified,
            @Schema(description = "다음 호출에 넘길 afterId (더 처리할 이벤트가 없으면 null)") Long nextAfterId,
            @Schema(description = "오늘 사용한 AI 호출 수") int aiCallsToday,
            @Schema(description = "하루 AI 호출 상한") int aiDailyLimit
    ) {
    }

    public Result run(int limit, Long afterId) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        long cursor = afterId == null ? 0L : afterId;

        List<Long> ids = eventRepository.findActiveIdsWithoutEventCodes(cursor, PageRequest.of(0, safeLimit));
        int classified = 0;
        for (Long id : ids) {
            if (eventService.applyEventCodes(id)) {
                classified++;
            }
        }
        Long nextAfterId = ids.size() < safeLimit ? null : ids.get(ids.size() - 1);
        return new Result(ids.size(), classified, nextAfterId,
                eventCodeAi.usedToday(), eventCodeAi.dailyLimit());
    }
}

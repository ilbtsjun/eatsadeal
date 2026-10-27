package com.backend.event.service;

import com.backend.event.dto.EventCodeBackFillResult;
import com.backend.event.repository.EventRepository;
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

    public EventCodeBackFillResult run(int limit, Long afterId) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        long cursor = afterId == null ? 0L : afterId;

        List<Long> ids = eventRepository.findActiveIdsNeedingClassification(cursor, PageRequest.of(0, safeLimit));
        int classified = 0;
        for (Long id : ids) {
            if (eventService.applyEventCodes(id)) {
                classified++;
            }
        }
        Long nextAfterId = ids.size() < safeLimit ? null : ids.get(ids.size() - 1);
        return new EventCodeBackFillResult(ids.size(), classified, nextAfterId,
                eventCodeAi.usedToday(), eventCodeAi.dailyLimit());
    }
}

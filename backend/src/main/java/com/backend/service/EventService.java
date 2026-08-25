package com.backend.service;

import com.backend.crawler.common.EventCreateDto;
import com.backend.entity.Event;
import com.backend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    @Transactional
    public void createEvent(EventCreateDto eventCreateDto){
        if(eventRepository.existsByUrl(eventCreateDto.url())){
            return;
        }
        Event event = Event.builder()
                .title(eventCreateDto.title())
                .description(eventCreateDto.description())
                .url(eventCreateDto.url())
                .img(eventCreateDto.img())
                .startDate(eventCreateDto.startDate())
                .endDate(eventCreateDto.endDate())
//                .brand(eventCreateDto.brand())
                .eventCodes(eventCreateDto.eventCodes())
                .build();
        eventRepository.save(event);
    }
}
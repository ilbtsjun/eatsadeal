package com.backend.service;

import com.backend.crawler.common.EventCreateDto;
import com.backend.entity.Brand;
import com.backend.entity.Event;
import com.backend.repository.BrandRepository;
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
    private final BrandRepository brandRepository;

    @Transactional
    public void createEvent(EventCreateDto eventCreateDto){
        if(eventRepository.existsByUrl(eventCreateDto.url())){
            return;
        }
        Brand brand = brandRepository.findByName(eventCreateDto.brandName());
        if(brand == null){
            throw new IllegalArgumentException("브랜드가 없습니다.");
        }
        Event event = Event.builder()
                .title(eventCreateDto.title())
                .description(eventCreateDto.description())
                .url(eventCreateDto.url())
                .img(eventCreateDto.img())
                .startDate(eventCreateDto.startDate())
                .endDate(eventCreateDto.endDate())
                .brand(brand)
                .eventCodes(eventCreateDto.eventCodes())
                .build();
        eventRepository.save(event);
    }
}
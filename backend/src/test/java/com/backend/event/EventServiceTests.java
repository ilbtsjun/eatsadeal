package com.backend.event;

import com.backend.auth.service.CurrentUserService;
import com.backend.brand.entity.Brand;
import com.backend.brand.repository.BrandRepository;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.event.dto.CreateEvent;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.event.service.EventService;
import com.backend.favorite.repository.FavoriteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTests {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private EventService eventService;

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("invalidEventProvider")
    @DisplayName("이벤트 생성 유효성 검증 실패 테스트")
    void testCreateEventValidation(String description, CreateEvent createEvent, ErrorCode expectedErrorCode) {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> eventService.createEvent(createEvent)
        );
        assertEquals(expectedErrorCode, exception.getErrorCode());
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("invalidEventProvider")
    @DisplayName("이벤트 생성 유효성 검증 실패 테스트(크롤러)")
    void testUpsertCrawledEventValidation(String description, CreateEvent createEvent, ErrorCode expectedErrorCode) {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> eventService.upsertCrawledEvent(createEvent)
        );
        assertEquals(expectedErrorCode, exception.getErrorCode());
    }

    private static Stream<Arguments> invalidEventProvider() {
        LocalDateTime now = LocalDateTime.now();
        return Stream.of(
                Arguments.of("타이틀이 null인 경우",
                        new CreateEvent(null, null, null, null, null, null, null, true, null),
                        ErrorCode.INVALID_REQUEST),
                Arguments.of("URL이 null인 경우",
                        new CreateEvent("temp", null, null, null, null, null, null, true, null),
                        ErrorCode.INVALID_REQUEST),
                Arguments.of("이미지 URL이 null인 경우",
                        new CreateEvent("temp", null, "temp", null, null, null, null, true, null),
                        ErrorCode.INVALID_REQUEST),
                Arguments.of("시작일이 null인 경우",
                        new CreateEvent("temp", null, "temp", "temp", null, null, null, true, null),
                        ErrorCode.INVALID_REQUEST),
                Arguments.of("브랜드 ID가 null인 경우",
                        new CreateEvent("temp", null, "temp", "temp", now, null, null, true, null),
                        ErrorCode.NOT_FOUND),
                Arguments.of("브랜드 ID가 존재하지 않는 경우",
                        new CreateEvent("temp", null, "temp", "temp", now, null, 2L, true, null),
                        ErrorCode.NOT_FOUND)
        );
    }

    @Test
    @DisplayName("이미 존재하는 이벤트 URL일 경우 예외 발생")
    void testCreateEventAlreadyExists() {
        String duplicateUrl = "https://www.bhc.co.kr/event/currentEvent/96";

        CreateEvent alreadyExistsEvent = new CreateEvent(
                "temp",
                null,
                duplicateUrl,
                "temp",
                LocalDateTime.now(),
                null,
                1L,
                true,
                null
        );

         when(eventRepository.existsByUrl(duplicateUrl)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> eventService.createEvent(alreadyExistsEvent)
        );

        assertEquals(ErrorCode.ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미 존재하는 이벤트 URL일 경우 업데이트(크롤러)")
    void testUpsertCreateEventAlreadyExists() {
        String duplicateUrl = "https://www.bhc.co.kr/event/currentEvent/96";
// given
        LocalDateTime now = LocalDateTime.now();
        CreateEvent alreadyExistsEvent = new CreateEvent(
                "temp",
                null,
                duplicateUrl,
                "temp",
                LocalDateTime.now(),
                null,
                1L,
                true,
                null
        );

        // 1. findByUrl 했을 때 기존 Event가 존재한다고 가정 (Mock 객체 사용)
        Event existingEvent = mock(Event.class);
        when(eventRepository.findByUrl(alreadyExistsEvent.url())).thenReturn(Optional.of(existingEvent));

        // when
        eventService.upsertCrawledEvent(alreadyExistsEvent);

        // then
        verify(existingEvent, times(1)).update(
                eq(alreadyExistsEvent.title()),
                eq(alreadyExistsEvent.description()),
                eq(alreadyExistsEvent.url()),
                eq(alreadyExistsEvent.img()),
                eq(alreadyExistsEvent.startDate()),
                eq(alreadyExistsEvent.endDate()),
                eq(true)
        );

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("정상적인 값으로 이벤트 생성 성공")
    void testCreateEventSuccess() {
        // given
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);
        Long brandId = 1L;

        CreateEvent validEventDto = new CreateEvent(
                "정상 이벤트 제목",
                "이벤트 설명",
                "https://www.bhc.co.kr/event/currentEvent/new",
                "https://image.url",
                start,
                end,
                brandId,
                true,
                null
        );

        // 1. 중복 URL이 존재하지 않음 (false 리턴)
        when(eventRepository.existsByUrl(validEventDto.url()))
                .thenReturn(false);

        // 2. 브랜드 조회 성공 설정 (실제 코드에 brandRepository.findById가 있으므로 필수!)
        Brand mockBrand = Brand.builder().build();
        when(brandRepository.findById(brandId))
                .thenReturn(Optional.of(mockBrand));

        // when & then (예외가 발생하지 않고 정상 통과하는지 확인)
        eventService.createEvent(validEventDto);

        // verify: eventRepository.save()가 정확히 1번 호출되었는지 검증
        verify(eventRepository, org.mockito.Mockito.times(1))
                .save(any(Event.class));
    }

    @Test
    @DisplayName("정상적인 값으로 이벤트 생성 성공(크롤러)")
    void testUpsertCreateEventSuccess() {
        // given
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);
        Long brandId = 1L;

        CreateEvent validEventDto = new CreateEvent(
                "정상 이벤트 제목",
                "이벤트 설명",
                "https://www.bhc.co.kr/event/currentEvent/new",
                "https://image.url",
                start,
                end,
                brandId,
                true,
                null
        );

        // 1. 중복 URL이 존재하지 않음 (false 리턴)
        when(eventRepository.existsByUrl(validEventDto.url()))
                .thenReturn(false);

        // 2. 브랜드 조회 성공 설정 (실제 코드에 brandRepository.findById가 있으므로 필수!)
        Brand mockBrand = Brand.builder().build();
        when(brandRepository.findById(brandId))
                .thenReturn(Optional.of(mockBrand));

        // when & then (예외가 발생하지 않고 정상 통과하는지 확인)
        eventService.createEvent(validEventDto);

        // verify: eventRepository.save()가 정확히 1번 호출되었는지 검증
        verify(eventRepository, org.mockito.Mockito.times(1))
                .save(any(Event.class));
    }

    @Test
    @DisplayName("존재하지 않는 이벤트 비활성화 시 예외 발생")
    void testDeactivate() {
        when(eventRepository.findById(999L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> eventService.deactivateEvent(999L)
        );

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }
}
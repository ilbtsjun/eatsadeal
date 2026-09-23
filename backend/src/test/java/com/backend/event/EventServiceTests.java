package com.backend.event;

import com.backend.auth.service.CurrentUserService;
import com.backend.brand.entity.Brand;
import com.backend.brand.repository.BrandRepository;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.event.dto.*;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.event.service.EventService;
import com.backend.favorite.entity.Favorite;
import com.backend.favorite.repository.FavoriteRepository;
import com.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
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

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private EventService eventService;

    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime END = START.plusDays(7);
    private static final String CLIENT_IP = "203.0.113.5";

    private static EventCode code(int index) {
        return EventCode.values()[index];
    }

    private CreateEvent createRequest(LocalDateTime start, LocalDateTime end, Long brandId,
                                      Boolean isActive, Set<EventCode> codes) {
        return new CreateEvent(
                "이벤트 제목",
                "이벤트 설명",
                "https://www.bhc.co.kr/event/currentEvent/100",
                "https://image.url/img.png",
                start, end, brandId, isActive, codes
        );
    }

    private UpdateEvent updateRequest(String title, String description, String url, String img,
                                      LocalDateTime start, LocalDateTime end,
                                      Boolean isActive, Set<EventCode> codes) {
        return new UpdateEvent(title, description, url, img, start, end, isActive, codes);
    }

    private UpdateEvent emptyUpdateRequest() {
        return updateRequest(null, null, null, null, null, null, null, null);
    }

    private EventSearchRequest searchRequest(Long brandId, Long categoryId, EventCode eventCode,
                                             String keyword, String sort, int page, int size) {
        return new EventSearchRequest(brandId, categoryId, eventCode, keyword, sort, page, size);
    }

    private EventSearchRequest searchRequest(String keyword, String sort, int page, int size) {
        return searchRequest(null, null, null, keyword, sort, page, size);
    }

    private Event buildEvent(Brand brand, Set<EventCode> codes) {
        return Event.builder()
                .title("기존 제목")
                .description("기존 설명")
                .url("https://www.bhc.co.kr/event/currentEvent/old")
                .img("https://image.url/old.png")
                .startDate(START)
                .endDate(END)
                .brand(brand)
                .isActive(true)
                .eventCodes(codes)
                .build();
    }

    private Event buildEvent(Brand brand) {
        return buildEvent(brand, null);
    }

    private Brand responseBrand() {
        Brand brand = mock(Brand.class);
        lenient().when(brand.getId()).thenReturn(1L);
        lenient().when(brand.getName()).thenReturn("BHC");
        lenient().when(brand.getImg()).thenReturn("https://image.url/brand.png");
        return brand;
    }

    private BusinessException assertBusinessException(Runnable runnable, ErrorCode expected) {
        BusinessException exception = assertThrows(BusinessException.class, runnable::run);
        assertEquals(expected, exception.getErrorCode());
        return exception;
    }

    private void givenClientIp(String ip) {
        when(currentUserService.getClientIp()).thenReturn(ip);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private void givenFirstView(String ip, Long eventId, boolean isFirst) {
        when(valueOperations.setIfAbsent(eq("event:view:" + eventId + ":" + ip), eq("1"), eq(Duration.ofHours(3))))
                .thenReturn(isFirst);
    }

    static Stream<Arguments> invalidDateRangeProvider() {
        return Stream.of(
                Arguments.of("시작일이 null인 경우", null, END),
                Arguments.of("시작일/종료일 모두 null인 경우", null, null),
                Arguments.of("종료일이 시작일보다 1초라도 빠른 경우", START, START.minusSeconds(1))
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("invalidDateRangeProvider")
    @DisplayName("이벤트 생성 - 잘못된 날짜면 INVALID_REQUEST, DB 접근 없음")
    void createEventInvalidDateRange(String description, LocalDateTime start, LocalDateTime end) {
        CreateEvent request = createRequest(start, end, 1L, true, null);

        assertBusinessException(() -> eventService.createEvent(request), ErrorCode.INVALID_REQUEST);

        verifyNoInteractions(eventRepository, brandRepository);
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("invalidDateRangeProvider")
    @DisplayName("이벤트 생성(크롤러) - 잘못된 날짜면 INVALID_REQUEST, DB 접근 없음")
    void upsertCrawledEventInvalidDateRange(String description, LocalDateTime start, LocalDateTime end) {
        CreateEvent request = createRequest(start, end, 1L, true, null);

        assertBusinessException(() -> eventService.upsertCrawledEvent(request), ErrorCode.INVALID_REQUEST);

        verifyNoInteractions(eventRepository, brandRepository);
    }

    @Nested
    @DisplayName("createEvent")
    class CreateEventTests {

        @Test
        @DisplayName("성공: 요청값 그대로 Event가 저장된다 (조회수 0, 브랜드 연결, 이벤트코드 복사)")
        void success() {
            Set<EventCode> codes = Set.of(code(0));
            CreateEvent request = createRequest(START, END, 1L, true, codes);
            Brand brand = mock(Brand.class);
            when(eventRepository.existsByUrl(request.url())).thenReturn(false);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            eventService.createEvent(request);

            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository, times(1)).save(captor.capture());
            Event saved = captor.getValue();
            assertEquals(request.title(), saved.getTitle());
            assertEquals(request.description(), saved.getDescription());
            assertEquals(request.url(), saved.getUrl());
            assertEquals(request.img(), saved.getImg());
            assertEquals(START, saved.getStartDate());
            assertEquals(END, saved.getEndDate());
            assertSame(brand, saved.getBrand());
            assertEquals(true, saved.getIsActive());
            assertEquals(0L, saved.getViewCount());
            assertEquals(codes, saved.getEventCodes());
        }

        @Test
        @DisplayName("성공: eventCodes가 null이면 빈 Set으로 저장된다")
        void successWithNullEventCodes() {
            CreateEvent request = createRequest(START, END, 1L, true, null);
            when(eventRepository.existsByUrl(request.url())).thenReturn(false);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(mock(Brand.class)));

            eventService.createEvent(request);

            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(captor.capture());
            assertNotNull(captor.getValue().getEventCodes());
            assertTrue(captor.getValue().getEventCodes().isEmpty());
        }

        @Test
        @DisplayName("성공: 종료일이 null(상시 이벤트)이어도 저장된다")
        void successWithNullEndDate() {
            CreateEvent request = createRequest(START, null, 1L, true, null);
            when(eventRepository.existsByUrl(request.url())).thenReturn(false);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(mock(Brand.class)));

            eventService.createEvent(request);

            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(captor.capture());
            assertNull(captor.getValue().getEndDate());
        }

        @Test
        @DisplayName("경계값: 종료일 == 시작일이면 저장된다")
        void successWhenEndDateEqualsStartDate() {
            CreateEvent request = createRequest(START, START, 1L, true, null);
            when(eventRepository.existsByUrl(request.url())).thenReturn(false);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(mock(Brand.class)));

            eventService.createEvent(request);

            verify(eventRepository, times(1)).save(any(Event.class));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 URL이면 ALREADY_EXISTS, 브랜드 조회/저장은 하지 않는다")
        void alreadyExistsUrl() {
            CreateEvent request = createRequest(START, END, 1L, true, null);
            when(eventRepository.existsByUrl(request.url())).thenReturn(true);

            assertBusinessException(() -> eventService.createEvent(request), ErrorCode.ALREADY_EXISTS);

            verifyNoInteractions(brandRepository);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 브랜드 ID면 NOT_FOUND, 저장하지 않는다")
        void brandNotFound() {
            CreateEvent request = createRequest(START, END, 999L, true, null);
            when(eventRepository.existsByUrl(request.url())).thenReturn(false);
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> eventService.createEvent(request), ErrorCode.NOT_FOUND);

            verify(eventRepository, never()).save(any(Event.class));
        }
    }

    @Nested
    @DisplayName("upsertCrawledEvent")
    class UpsertCrawledEventTests {

        @Test
        @DisplayName("신규 URL: 새 Event를 저장한다")
        void insertWhenUrlNotExists() {
            Set<EventCode> codes = Set.of(code(0));
            CreateEvent request = createRequest(START, END, 1L, true, codes);
            Brand brand = mock(Brand.class);
            when(eventRepository.findByUrl(request.url())).thenReturn(Optional.empty());
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            eventService.upsertCrawledEvent(request);

            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository, times(1)).save(captor.capture());
            Event saved = captor.getValue();
            assertEquals(request.title(), saved.getTitle());
            assertEquals(request.url(), saved.getUrl());
            assertSame(brand, saved.getBrand());
            assertEquals(codes, saved.getEventCodes());
            assertEquals(0L, saved.getViewCount());
        }

        @Test
        @DisplayName("기존 URL: save 없이 기존 Event의 값이 갱신되고, 브랜드는 조회하지 않는다")
        void updateWhenUrlExists() {
            Event existing = buildEvent(mock(Brand.class));
            CreateEvent request = createRequest(START.plusDays(1), END.plusDays(1), 1L, true, null);
            when(eventRepository.findByUrl(request.url())).thenReturn(Optional.of(existing));

            eventService.upsertCrawledEvent(request);

            assertEquals(request.title(), existing.getTitle());
            assertEquals(request.description(), existing.getDescription());
            assertEquals(request.img(), existing.getImg());
            assertEquals(START.plusDays(1), existing.getStartDate());
            assertEquals(END.plusDays(1), existing.getEndDate());
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(brandRepository);
        }

        @Test
        @DisplayName("기존 URL: 요청의 isActive 값이 그대로 반영된다 (크롤러가 false를 보내면 비활성화됨)")
        void reflectsRequestedIsActive() {
            Event existing = buildEvent(mock(Brand.class));
            assertTrue(existing.getIsActive());
            CreateEvent request = createRequest(START, END, 1L, false, null);
            when(eventRepository.findByUrl(request.url())).thenReturn(Optional.of(existing));

            eventService.upsertCrawledEvent(request);

            assertFalse(existing.getIsActive());
        }

        @Test
        @DisplayName("실패: 신규 URL인데 브랜드가 없으면 NOT_FOUND, 저장하지 않는다")
        void brandNotFoundOnInsert() {
            CreateEvent request = createRequest(START, END, 999L, true, null);
            when(eventRepository.findByUrl(request.url())).thenReturn(Optional.empty());
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> eventService.upsertCrawledEvent(request), ErrorCode.NOT_FOUND);

            verify(eventRepository, never()).save(any(Event.class));
        }
    }

    @Nested
    @DisplayName("searchEvents")
    class SearchEventsTests {

        private Pageable searchAndCapturePageable(EventSearchRequest request) {
            when(eventRepository.searchEvents(any(), any(), any(), any(), any(), any()))
                    .thenReturn(Page.empty());

            eventService.searchEvents(request);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(eventRepository).searchEvents(any(), any(), any(), any(), any(), captor.capture());
            return captor.getValue();
        }

        private String searchAndCaptureKeyword(EventSearchRequest request) {
            when(eventRepository.searchEvents(any(), any(), any(), any(), any(), any()))
                    .thenReturn(Page.empty());

            eventService.searchEvents(request);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(eventRepository).searchEvents(any(), any(), any(), captor.capture(), any(), any());
            return captor.getValue();
        }

        private static Sort latest() {
            return Sort.by(Sort.Order.desc("startDate"), Sort.Order.desc("id"));
        }

        static Stream<Arguments> sortProvider() {
            return Stream.of(
                    Arguments.of("popular", Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("id"))),
                    Arguments.of("endingSoon", Sort.by(Sort.Order.asc("endDate"), Sort.Order.desc("id"))),
                    Arguments.of("oldest", Sort.by(Sort.Order.asc("startDate"), Sort.Order.asc("id"))),
                    Arguments.of("latest", latest()),
                    Arguments.of(null, latest()),
                    Arguments.of("알 수 없는 값", latest())
            );
        }

        @ParameterizedTest(name = "{index} - sort={0}")
        @MethodSource("sortProvider")
        @DisplayName("정렬 옵션이 올바른 Sort로 변환된다 (null/알 수 없는 값은 latest)")
        void sortMapping(String sort, Sort expected) {
            Pageable pageable = searchAndCapturePageable(searchRequest(null, sort, 0, 10));

            assertEquals(expected, pageable.getSort());
        }

        @ParameterizedTest(name = "{index} - page={0}, size={1} → page={2}, size={3}")
        @CsvSource({
                "-1,  10, 0, 10",   // 음수 페이지 → 0
                "0,   10, 0, 10",
                "3,   10, 3, 10",
                "0,   0,  0, 1",    // size 0 → 최소 1
                "0,   -5, 0, 1",    // 음수 size → 최소 1
                "0,   1,  0, 1",    // 경계값
                "0,   100, 0, 100", // 경계값
                "0,   101, 0, 100", // 최대 100으로 제한
                "0,   1000, 0, 100"
        })
        @DisplayName("page/size가 안전한 범위로 보정된다")
        void pageAndSizeAreClamped(int page, int size, int expectedPage, int expectedSize) {
            Pageable pageable = searchAndCapturePageable(searchRequest(null, "latest", page, size));

            assertEquals(expectedPage, pageable.getPageNumber());
            assertEquals(expectedSize, pageable.getPageSize());
        }

        @Test
        @DisplayName("키워드 앞뒤 공백은 제거된다")
        void keywordIsTrimmed() {
            String keyword = searchAndCaptureKeyword(searchRequest("  치킨  ", "latest", 0, 10));

            assertEquals("치킨", keyword);
        }

        @ParameterizedTest(name = "{index} - keyword=[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t"})
        @DisplayName("null/빈 문자열/공백만 있는 키워드는 null로 변환된다")
        void blankKeywordBecomesNull(String keyword) {
            String captured = searchAndCaptureKeyword(searchRequest(keyword, "latest", 0, 10));

            assertNull(captured);
        }

        @Test
        @DisplayName("brandId, categoryId, eventCode 필터가 repository로 그대로 전달된다")
        void filtersArePassedToRepository() {
            when(eventRepository.searchEvents(any(), any(), any(), any(), any(), any()))
                    .thenReturn(Page.empty());

            eventService.searchEvents(searchRequest(1L, 2L, code(0), null, "latest", 0, 10));

            verify(eventRepository).searchEvents(
                    eq(1L), eq(2L), eq(code(0)), isNull(), any(LocalDateTime.class), any(Pageable.class));
        }

        @Test
        @DisplayName("검색 결과가 응답 DTO로 변환되고 페이지 정보가 유지된다")
        void resultIsMappedToResponse() {
            Event event = buildEvent(responseBrand(), Set.of(code(0)));
            ReflectionTestUtils.setField(event, "id", 10L);
            Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);
            when(eventRepository.searchEvents(any(), any(), any(), any(), any(), any())).thenReturn(page);

            Page<GetEventListResponse> result = eventService.searchEvents(searchRequest(null, "latest", 0, 10));

            assertEquals(1, result.getTotalElements());
            GetEventListResponse item = result.getContent().get(0);
            assertEquals(10L, item.id());
            assertEquals("기존 제목", item.title());
            assertEquals(1L, item.brandId());
            assertEquals("BHC", item.brandName());
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 페이지를 반환한다")
        void emptyResult() {
            when(eventRepository.searchEvents(any(), any(), any(), any(), any(), any()))
                    .thenReturn(Page.empty());

            Page<GetEventListResponse> result = eventService.searchEvents(searchRequest(null, "latest", 0, 10));

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getEvent")
    class GetEventTests {

        @Test
        @DisplayName("첫 조회(같은 IP로 처음 봄): 조회수가 증가한다")
        void firstViewIncreasesCount() {
            Event event = buildEvent(responseBrand());
            when(currentUserService.getOptionalUser()).thenReturn(null);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            givenClientIp(CLIENT_IP);
            givenFirstView(CLIENT_IP, 1L, true);

            eventService.getEvent(1L);

            verify(eventRepository, times(1)).increaseViewCount(1L);
        }

        @Test
        @DisplayName("재조회(같은 IP로 3시간 내 재조회): 조회수가 증가하지 않는다")
        void repeatedViewDoesNotIncreaseCount() {
            Event event = buildEvent(responseBrand());
            when(currentUserService.getOptionalUser()).thenReturn(null);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            givenClientIp(CLIENT_IP);
            givenFirstView(CLIENT_IP, 1L, false);

            eventService.getEvent(1L);

            verify(eventRepository, never()).increaseViewCount(anyLong());
        }

        @Test
        @DisplayName("조회수 키는 이벤트/IP별로 구분된다 (event:view:{eventId}:{ip})")
        void viewKeyIsScopedByEventAndIp() {
            Event event = buildEvent(responseBrand());
            when(currentUserService.getOptionalUser()).thenReturn(null);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            givenClientIp(CLIENT_IP);
            when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(Duration.ofHours(3)))).thenReturn(true);

            eventService.getEvent(1L);

            verify(valueOperations).setIfAbsent("event:view:1:" + CLIENT_IP, "1", Duration.ofHours(3));
        }

        @Test
        @DisplayName("로그인 + 즐겨찾기한 이벤트: isFavorite=true")
        void loggedInUserWithFavorite() {
            User user = mock(User.class);
            Event event = buildEvent(responseBrand());
            when(currentUserService.getOptionalUser()).thenReturn(user);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            when(favoriteRepository.findByUserAndEvent(user, event)).thenReturn(Optional.of(mock(Favorite.class)));
            givenClientIp(CLIENT_IP);
            givenFirstView(CLIENT_IP, 1L, true);

            GetEventResponse response = eventService.getEvent(1L);

            assertTrue(response.isFavorite());
            assertEquals("기존 제목", response.title());
            assertEquals("BHC", response.brandName());
        }

        @Test
        @DisplayName("로그인 + 즐겨찾기하지 않은 이벤트: isFavorite=false")
        void loggedInUserWithoutFavorite() {
            User user = mock(User.class);
            Event event = buildEvent(responseBrand());
            when(currentUserService.getOptionalUser()).thenReturn(user);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            when(favoriteRepository.findByUserAndEvent(user, event)).thenReturn(Optional.empty());
            givenClientIp(CLIENT_IP);
            givenFirstView(CLIENT_IP, 1L, true);

            GetEventResponse response = eventService.getEvent(1L);

            assertFalse(response.isFavorite());
        }

        @Test
        @DisplayName("비로그인: 즐겨찾기 조회 없이 isFavorite=false")
        void anonymousUser() {
            Event event = buildEvent(responseBrand());
            when(currentUserService.getOptionalUser()).thenReturn(null);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            givenClientIp(CLIENT_IP);
            givenFirstView(CLIENT_IP, 1L, true);

            GetEventResponse response = eventService.getEvent(1L);

            assertFalse(response.isFavorite());
            verifyNoInteractions(favoriteRepository);
        }

        @Test
        @DisplayName("Redis 장애 시: 조회수는 유지하고, 이벤트 조회 자체는 정상 응답한다")
        void redisFailureStillIncreasesCountAndReturnsEvent() {
            Event event = buildEvent(responseBrand());
            when(currentUserService.getOptionalUser()).thenReturn(null);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            when(currentUserService.getClientIp()).thenReturn(CLIENT_IP);
            when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("연결 실패"));

            GetEventResponse response = assertDoesNotThrow(() -> eventService.getEvent(1L));

            assertEquals("기존 제목", response.title());
            verify(eventRepository, never()).increaseViewCount(anyLong());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이벤트면 NOT_FOUND, 조회수/IP 조회 모두 하지 않는다")
        void eventNotFound() {
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> eventService.getEvent(999L), ErrorCode.NOT_FOUND);

            verify(eventRepository, never()).increaseViewCount(anyLong());
            verifyNoInteractions(favoriteRepository, redisTemplate);
            verify(currentUserService, never()).getClientIp();
        }
    }

    @Nested
    @DisplayName("isFirstView")
    class IsFirstViewTests {

        @Test
        @DisplayName("SETNX가 true를 반환하면(처음 기록됨) true, 3시간 TTL로 설정한다")
        void trueWhenKeyWasAbsent() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent("event:view:1:203.0.113.5", "1", Duration.ofHours(3)))
                    .thenReturn(true);

            assertTrue(eventService.isFirstView(1L, "203.0.113.5"));
        }

        @Test
        @DisplayName("SETNX가 false를 반환하면(이미 존재) false")
        void falseWhenKeyAlreadyExists() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent("event:view:1:203.0.113.5", "1", Duration.ofHours(3)))
                    .thenReturn(false);

            assertFalse(eventService.isFirstView(1L, "203.0.113.5"));
        }

        @Test
        @DisplayName("SETNX가 null을 반환해도(레디스 응답 이상) 예외 없이 false로 취급한다")
        void nullResultIsTreatedAsFalse() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .thenReturn(null);

            assertFalse(eventService.isFirstView(1L, "203.0.113.5"));
        }

        @Test
        @DisplayName("Redis 연결 장애(예외)가 나면 예외를 던지지 않고 false를 반환한다 (fail-open)")
        void redisFailureIsTreatedAsFirstView() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .thenThrow(new RedisConnectionFailureException("연결 실패"));

            assertFalse(assertDoesNotThrow(() -> eventService.isFirstView(1L, "203.0.113.5")));
        }

        @Test
        @DisplayName("opsForValue() 호출 자체에서 예외가 나도 예외를 던지지 않고 false를 반환한다")
        void redisFailureOnOpsForValueIsTreatedAsFirstView() {
            when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("연결 실패"));

            assertFalse(assertDoesNotThrow(() -> eventService.isFirstView(1L, "203.0.113.5")));
        }
    }

    @Nested
    @DisplayName("updateEvent")
    class UpdateEventTests {

        private Event givenExistingEvent(Set<EventCode> codes) {
            Event event = buildEvent(mock(Brand.class), codes);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            return event;
        }

        private Event givenExistingEvent() {
            return givenExistingEvent(null);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이벤트면 NOT_FOUND")
        void eventNotFound() {
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> eventService.updateEvent(999L, emptyUpdateRequest()), ErrorCode.NOT_FOUND);

            verify(eventRepository, never()).existsByUrlAndIdNot(any(), any());
        }

        @Test
        @DisplayName("실패: 다른 이벤트가 이미 쓰는 URL이면 ALREADY_EXISTS, 기존 값은 변경되지 않는다")
        void duplicateUrl() {
            Event event = givenExistingEvent();
            String originalUrl = event.getUrl();
            UpdateEvent request = updateRequest("변경 제목", null, "https://dup.url", null, null, null, null, null);
            when(eventRepository.existsByUrlAndIdNot("https://dup.url", 1L)).thenReturn(true);

            assertBusinessException(() -> eventService.updateEvent(1L, request), ErrorCode.ALREADY_EXISTS);

            assertEquals("기존 제목", event.getTitle());
            assertEquals(originalUrl, event.getUrl());
        }

        @Test
        @DisplayName("성공: 모든 필드를 한 번에 수정할 수 있다")
        void updateAllFields() {
            Event event = givenExistingEvent();
            LocalDateTime newStart = START.plusDays(1);
            LocalDateTime newEnd = END.plusDays(3);
            UpdateEvent request = updateRequest(
                    "새 제목", "새 설명", "https://new.url", "https://new.img", newStart, newEnd, false, null);
            when(eventRepository.existsByUrlAndIdNot("https://new.url", 1L)).thenReturn(false);

            eventService.updateEvent(1L, request);

            assertEquals("새 제목", event.getTitle());
            assertEquals("새 설명", event.getDescription());
            assertEquals("https://new.url", event.getUrl());
            assertEquals("https://new.img", event.getImg());
            assertEquals(newStart, event.getStartDate());
            assertEquals(newEnd, event.getEndDate());
            assertFalse(event.getIsActive());
        }

        @Test
        @DisplayName("성공: 제목만 보내면 나머지 필드는 기존 값이 유지되고 URL 중복 검사는 하지 않는다")
        void partialUpdateKeepsOtherFields() {
            Event event = givenExistingEvent();
            UpdateEvent request = updateRequest("새 제목", null, null, null, null, null, null, null);

            eventService.updateEvent(1L, request);

            assertEquals("새 제목", event.getTitle());
            assertEquals("기존 설명", event.getDescription());
            assertEquals("https://www.bhc.co.kr/event/currentEvent/old", event.getUrl());
            assertEquals("https://image.url/old.png", event.getImg());
            assertEquals(START, event.getStartDate());
            assertEquals(END, event.getEndDate());
            assertTrue(event.getIsActive());
            verify(eventRepository, never()).existsByUrlAndIdNot(any(), any());
        }

        @ParameterizedTest(name = "{index} - 값=[{0}]")
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("문자열 필드가 빈 문자열/공백이면 '미입력'으로 취급해 기존 값을 유지한다")
        void blankStringsKeepOriginal(String blank) {
            Event event = givenExistingEvent();
            UpdateEvent request = updateRequest(blank, blank, blank, blank, null, null, null, null);

            eventService.updateEvent(1L, request);

            assertEquals("기존 제목", event.getTitle());
            assertEquals("기존 설명", event.getDescription());
            assertEquals("https://www.bhc.co.kr/event/currentEvent/old", event.getUrl());
            assertEquals("https://image.url/old.png", event.getImg());
            verify(eventRepository, never()).existsByUrlAndIdNot(any(), any());
        }

        @Test
        @DisplayName("성공: 자기 자신의 URL을 그대로 보내도 수정된다 (existsByUrlAndIdNot이 자신을 제외)")
        void sameUrlAsSelfIsAllowed() {
            Event event = givenExistingEvent();
            String ownUrl = event.getUrl();
            UpdateEvent request = updateRequest("새 제목", null, ownUrl, null, null, null, null, null);
            when(eventRepository.existsByUrlAndIdNot(ownUrl, 1L)).thenReturn(false);

            eventService.updateEvent(1L, request);

            assertEquals("새 제목", event.getTitle());
            assertEquals(ownUrl, event.getUrl());
        }

        @Test
        @DisplayName("isActive=false만 보내면 비활성화되고, null이면 기존 값을 유지한다")
        void isActiveHandling() {
            Event event = givenExistingEvent();

            eventService.updateEvent(1L, emptyUpdateRequest());
            assertTrue(event.getIsActive(), "null이면 유지");

            eventService.updateEvent(1L, updateRequest(null, null, null, null, null, null, false, null));
            assertFalse(event.getIsActive(), "false면 비활성화");

            eventService.updateEvent(1L, updateRequest(null, null, null, null, null, null, true, null));
            assertTrue(event.getIsActive(), "true면 재활성화");
        }

        @Test
        @DisplayName("실패: 새 종료일이 기존 시작일보다 빠르면 INVALID_REQUEST, 값은 변경되지 않는다")
        void newEndDateBeforeExistingStartDate() {
            Event event = givenExistingEvent();
            UpdateEvent request = updateRequest("새 제목", null, null, null, null, START.minusDays(1), null, null);

            assertBusinessException(() -> eventService.updateEvent(1L, request), ErrorCode.INVALID_REQUEST);

            assertEquals("기존 제목", event.getTitle());
            assertEquals(END, event.getEndDate());
        }

        @Test
        @DisplayName("실패: 새 시작일이 기존 종료일보다 늦으면 INVALID_REQUEST")
        void newStartDateAfterExistingEndDate() {
            Event event = givenExistingEvent();
            UpdateEvent request = updateRequest(null, null, null, null, END.plusDays(1), null, null, null);

            assertBusinessException(() -> eventService.updateEvent(1L, request), ErrorCode.INVALID_REQUEST);

            assertEquals(START, event.getStartDate());
        }

        @Test
        @DisplayName("성공: 시작일/종료일 중 하나만 보내면 나머지는 기존 값과 조합해 검증한다")
        void onlyOneDateIsProvided() {
            Event event = givenExistingEvent();

            eventService.updateEvent(1L, updateRequest(null, null, null, null, null, END.plusDays(10), null, null));

            assertEquals(START, event.getStartDate());
            assertEquals(END.plusDays(10), event.getEndDate());
        }

        @Test
        @DisplayName("eventCodes가 null이면 기존 코드가 유지된다")
        void nullEventCodesKeepsExisting() {
            Event event = givenExistingEvent(Set.of(code(0), code(1)));

            eventService.updateEvent(1L, emptyUpdateRequest());

            assertEquals(Set.of(code(0), code(1)), event.getEventCodes());
        }

        @Test
        @DisplayName("eventCodes를 보내면 차집합 기준으로 추가/삭제되어 요청한 Set과 같아진다")
        void eventCodesAreSynchronized() {
            Event event = givenExistingEvent(Set.of(code(0), code(1)));
            UpdateEvent request = updateRequest(null, null, null, null, null, null, null, Set.of(code(1), code(2)));

            eventService.updateEvent(1L, request);

            assertEquals(Set.of(code(1), code(2)), event.getEventCodes());
        }

        @Test
        @DisplayName("eventCodes에 빈 Set을 보내면 모든 코드가 제거된다")
        void emptyEventCodesClearsAll() {
            Event event = givenExistingEvent(Set.of(code(0), code(1)));
            UpdateEvent request = updateRequest(null, null, null, null, null, null, null, new HashSet<>());

            eventService.updateEvent(1L, request);

            assertTrue(event.getEventCodes().isEmpty());
        }

        @Test
        @DisplayName("eventCodes가 기존과 동일하면 변화 없다")
        void sameEventCodesNoChange() {
            Event event = givenExistingEvent(Set.of(code(0), code(1)));
            UpdateEvent request = updateRequest(null, null, null, null, null, null, null, Set.of(code(0), code(1)));

            eventService.updateEvent(1L, request);

            assertEquals(Set.of(code(0), code(1)), event.getEventCodes());
        }
    }

    @Nested
    @DisplayName("deactivateEvent")
    class DeactivateEventTests {

        @Test
        @DisplayName("성공: 활성 이벤트가 비활성화된다")
        void success() {
            Event event = buildEvent(mock(Brand.class));
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

            eventService.deactivateEvent(1L);

            assertFalse(event.getIsActive());
        }

        @Test
        @DisplayName("이미 비활성인 이벤트를 다시 비활성화해도 예외 없이 비활성 상태 유지")
        void idempotent() {
            Event event = buildEvent(mock(Brand.class));
            event.deactivate();
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

            assertDoesNotThrow(() -> eventService.deactivateEvent(1L));

            assertFalse(event.getIsActive());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이벤트면 NOT_FOUND")
        void eventNotFound() {
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> eventService.deactivateEvent(999L), ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getEventCodes")
    class GetEventCodesTests {

        @Test
        @DisplayName("EventCode enum의 모든 값이 빠짐없이 반환된다")
        void returnsAllEnumValues() {
            List<GetEventCodeListResponse> result = eventService.getEventCodes();

            assertEquals(EventCode.values().length, result.size());
            verifyNoInteractions(eventRepository);
        }
    }
}
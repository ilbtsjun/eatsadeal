package com.backend.brand;

import com.backend.brand.dto.CreateBrand;
import com.backend.brand.dto.GetBrandListResponse;
import com.backend.brand.dto.GetBrandResponse;
import com.backend.brand.dto.UpdateBrand;
import com.backend.brand.entity.Brand;
import com.backend.brand.entity.BrandCategory;
import com.backend.brand.repository.BrandCategoryRepository;
import com.backend.brand.repository.BrandRepository;
import com.backend.brand.service.BrandCategoryService;
import com.backend.brand.service.BrandService;
import com.backend.category.entity.Category;
import com.backend.category.repository.CategoryRepository;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceTests {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private BrandCategoryRepository brandCategoryRepository;

    @Mock
    private BrandCategoryService brandCategoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BrandService brandService;


    private Brand buildBrand(Long id, String name) {
        Brand brand = Brand.builder()
                .name(name)
                .url("https://www." + name.toLowerCase() + ".co.kr")
                .img("https://image.url/" + name.toLowerCase() + ".png")
                .build();
        ReflectionTestUtils.setField(brand, "id", id);
        return brand;
    }

    private Brand buildBrand() {
        return buildBrand(1L, "BHC");
    }

    private Category categoryWithId(Long id) {
        Category category = mock(Category.class);
        lenient().when(category.getId()).thenReturn(id);
        return category;
    }

    private BrandCategory link(Brand brand, Category category) {
        return BrandCategory.builder().brand(brand).category(category).build();
    }

    private CreateBrand createRequest(String name, String url, String img, List<Long> categoryIds) {
        return new CreateBrand(name, url, img, categoryIds);
    }

    private UpdateBrand updateRequest(String name, String url, String img, List<Long> categoryIds) {
        return new UpdateBrand(name, url, img, categoryIds);
    }

    private BusinessException assertBusinessException(Runnable runnable, ErrorCode expected) {
        BusinessException exception = assertThrows(BusinessException.class, runnable::run);
        assertEquals(expected, exception.getErrorCode());
        return exception;
    }

    @Nested
    @DisplayName("getBrandList")
    class GetBrandListTests {

        @Test
        @DisplayName("성공: 활성/비활성 브랜드를 모두 이름 오름차순으로 조회하고 isActive를 응답에 담는다")
        void success() {
            Brand bbq = buildBrand(1L, "BBQ");
            Brand bhc = buildBrand(2L, "BHC");
            bhc.deactive();
            when(brandRepository.findAll(any(Sort.class))).thenReturn(List.of(bbq, bhc));

            List<GetBrandListResponse> result = brandService.getBrandList();

            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).id());
            assertEquals("BBQ", result.get(0).name());
            assertEquals("https://image.url/bbq.png", result.get(0).img());
            assertTrue(result.get(0).isActive());
            assertEquals(2L, result.get(1).id());
            assertEquals("BHC", result.get(1).name());
            assertFalse(result.get(1).isActive(), "비활성 브랜드도 목록에 포함되고 isActive=false로 내려간다");
            verify(brandRepository).findAll(Sort.by(Sort.Direction.ASC, "name"));
        }

        @Test
        @DisplayName("성공: 브랜드가 없으면 빈 리스트")
        void empty() {
            when(brandRepository.findAll(any(Sort.class))).thenReturn(List.of());

            assertTrue(brandService.getBrandList().isEmpty());
        }
    }

    @Nested
    @DisplayName("createBrand")
    class CreateBrandTests {

        @Test
        @DisplayName("성공: 카테고리를 먼저 모두 검증한 뒤 브랜드를 저장하고 카테고리를 순서대로 연결한다")
        void success() {
            Category cat1 = categoryWithId(1L);
            Category cat2 = categoryWithId(2L);
            CreateBrand request = createRequest("BHC", "https://www.bhc.co.kr", "https://image.url/bhc.png", List.of(1L, 2L));
            when(brandRepository.existsByName("BHC")).thenReturn(false);
            when(brandRepository.existsByUrl("https://www.bhc.co.kr")).thenReturn(false);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat1));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(cat2));

            brandService.createBrand(request);

            ArgumentCaptor<Brand> captor = ArgumentCaptor.forClass(Brand.class);
            verify(brandRepository, times(1)).save(captor.capture());
            Brand saved = captor.getValue();
            assertEquals("BHC", saved.getName());
            assertEquals("https://www.bhc.co.kr", saved.getUrl());
            assertEquals("https://image.url/bhc.png", saved.getImg());
            assertTrue(saved.getIsActive(), "새 브랜드는 활성 상태로 생성된다");

            InOrder inOrder = inOrder(categoryRepository, brandRepository, brandCategoryService);
            inOrder.verify(categoryRepository).findById(1L);
            inOrder.verify(categoryRepository).findById(2L);
            inOrder.verify(brandRepository).save(saved);
            inOrder.verify(brandCategoryService).addCategory(saved, cat1);
            inOrder.verify(brandCategoryService).addCategory(saved, cat2);
        }

        @Test
        @DisplayName("성공: categoryIds에 중복이 있어도 카테고리당 한 번만 조회/연결한다 (distinct)")
        void duplicateCategoryIdsAreProcessedOnce() {
            Category cat1 = categoryWithId(1L);
            Category cat2 = categoryWithId(2L);
            CreateBrand request = createRequest("BHC", "https://www.bhc.co.kr", "https://image.url/bhc.png", List.of(1L, 1L, 2L));
            when(brandRepository.existsByName("BHC")).thenReturn(false);
            when(brandRepository.existsByUrl("https://www.bhc.co.kr")).thenReturn(false);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat1));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(cat2));

            brandService.createBrand(request);

            verify(categoryRepository, times(1)).findById(1L);
            verify(categoryRepository, times(1)).findById(2L);
            verify(brandCategoryService, times(1)).addCategory(any(Brand.class), eq(cat1));
            verify(brandCategoryService, times(1)).addCategory(any(Brand.class), eq(cat2));
            verifyNoMoreInteractions(brandCategoryService);
        }

        @Test
        @DisplayName("실패: categoryIds가 비어 있으면 INVALID_REQUEST 예외가 발생하고 브랜드는 저장되지 않는다")
        void failWhenCategoryIdsEmpty() {
            CreateBrand request = createRequest("BHC", "https://www.bhc.co.kr", "https://image.url/bhc.png", List.of());

            assertBusinessException(() -> brandService.createBrand(request), ErrorCode.INVALID_REQUEST);

            verify(brandRepository, never()).save(any(Brand.class));
            verifyNoInteractions(categoryRepository, brandCategoryService);
        }

        @Test
        @DisplayName("실패: 이미 있는 이름이면 ALREADY_EXISTS (URL 검사는 단락 평가로 생략), 아무것도 저장하지 않는다")
        void duplicateName() {
            CreateBrand request = createRequest("BHC", "https://www.bhc.co.kr", "https://image.url/bhc.png", List.of(1L));
            when(brandRepository.existsByName("BHC")).thenReturn(true);

            assertBusinessException(() -> brandService.createBrand(request), ErrorCode.ALREADY_EXISTS);

            verify(brandRepository, never()).save(any(Brand.class));
            verifyNoInteractions(categoryRepository, brandCategoryService);
        }

        @Test
        @DisplayName("실패: 이름은 새롭지만 URL이 중복이면 ALREADY_EXISTS")
        void duplicateUrl() {
            CreateBrand request = createRequest("BHC", "https://www.bhc.co.kr", "https://image.url/bhc.png", List.of(1L));
            when(brandRepository.existsByName("BHC")).thenReturn(false);
            when(brandRepository.existsByUrl("https://www.bhc.co.kr")).thenReturn(true);

            assertBusinessException(() -> brandService.createBrand(request), ErrorCode.ALREADY_EXISTS);

            verify(brandRepository, never()).save(any(Brand.class));
            verifyNoInteractions(categoryRepository, brandCategoryService);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 카테고리 ID면 NOT_FOUND. 브랜드는 저장되지 않고 어떤 카테고리도 연결되지 않는다")
        void categoryNotFound() {
            Category cat1 = categoryWithId(1L);
            CreateBrand request = createRequest("BHC", "https://www.bhc.co.kr", "https://image.url/bhc.png", List.of(1L, 999L));
            when(brandRepository.existsByName("BHC")).thenReturn(false);
            when(brandRepository.existsByUrl("https://www.bhc.co.kr")).thenReturn(false);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat1));
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> brandService.createBrand(request), ErrorCode.NOT_FOUND);

            verify(brandRepository, never()).save(any(Brand.class));
            verifyNoInteractions(brandCategoryService);
        }

        @Test
        @DisplayName("실패: BrandCategoryService.addCategory에서 난 예외는 그대로 전파된다")
        void addCategoryExceptionPropagates() {
            Category cat1 = categoryWithId(1L);
            CreateBrand request = createRequest("BHC", "https://www.bhc.co.kr", "https://image.url/bhc.png", List.of(1L));
            when(brandRepository.existsByName("BHC")).thenReturn(false);
            when(brandRepository.existsByUrl("https://www.bhc.co.kr")).thenReturn(false);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat1));
            doThrow(new BusinessException(ErrorCode.ALREADY_EXISTS))
                    .when(brandCategoryService).addCategory(any(Brand.class), eq(cat1));

            assertBusinessException(() -> brandService.createBrand(request), ErrorCode.ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("getBrand")
    class GetBrandTests {

        @Test
        @DisplayName("성공: 브랜드 정보와 연결된 카테고리 ID 목록을 반환한다")
        void success() {
            Brand brand = buildBrand();
            Category cat1 = categoryWithId(10L);
            Category cat2 = categoryWithId(20L);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandCategoryRepository.findByBrand(brand)).thenReturn(List.of(
                    link(brand, cat1),
                    link(brand, cat2)
            ));

            GetBrandResponse response = brandService.getBrand(1L);

            assertEquals(1L, response.id());
            assertEquals("BHC", response.name());
            assertEquals("https://www.bhc.co.kr", response.url());
            assertEquals("https://image.url/bhc.png", response.img());
            assertEquals(List.of(10L, 20L), response.categoryIds());
        }

        @Test
        @DisplayName("성공: 연결된 카테고리가 없으면 categoryIds는 빈 리스트")
        void successWithoutCategories() {
            Brand brand = buildBrand();
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandCategoryRepository.findByBrand(brand)).thenReturn(List.of());

            GetBrandResponse response = brandService.getBrand(1L);

            assertTrue(response.categoryIds().isEmpty());
        }

        @Test
        @DisplayName("성공: 비활성 브랜드도 목록과 마찬가지로 ID로 조회된다")
        void inactiveBrandIsReturned() {
            Brand brand = buildBrand();
            brand.deactive();
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandCategoryRepository.findByBrand(brand)).thenReturn(List.of());

            GetBrandResponse response = assertDoesNotThrow(() -> brandService.getBrand(1L));

            assertEquals("BHC", response.name());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 브랜드면 NOT_FOUND, 카테고리 조회는 하지 않는다")
        void brandNotFound() {
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> brandService.getBrand(999L), ErrorCode.NOT_FOUND);

            verifyNoInteractions(brandCategoryRepository);
        }
    }

    @Nested
    @DisplayName("updateBrand")
    class UpdateBrandTests {

        private Brand givenBrandFound() {
            Brand brand = buildBrand();
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            return brand;
        }

        private Brand givenExistingBrand(BrandCategory... links) {
            Brand brand = givenBrandFound();
            when(brandCategoryRepository.findByBrand(brand)).thenReturn(List.of(links));
            return brand;
        }

        @Test
        @DisplayName("실패: 존재하지 않는 브랜드면 NOT_FOUND, 중복 검사/카테고리 처리는 하지 않는다")
        void brandNotFound() {
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> brandService.updateBrand(999L, updateRequest("새 이름", null, null, List.of(1L))),
                    ErrorCode.NOT_FOUND);

            verify(brandRepository, never()).existsByNameAndIdNot(any(), any());
            verifyNoInteractions(brandCategoryRepository, brandCategoryService, categoryRepository);
        }

        @Test
        @DisplayName("실패: 다른 브랜드가 쓰는 이름이면 ALREADY_EXISTS, 브랜드는 변경되지 않고 URL 검사/카테고리 처리도 하지 않는다")
        void duplicateName() {
            Brand brand = givenBrandFound();
            when(brandRepository.existsByNameAndIdNot("BBQ", 1L)).thenReturn(true);

            assertBusinessException(() -> brandService.updateBrand(1L, updateRequest("BBQ", "https://new.url", null, List.of(1L))),
                    ErrorCode.ALREADY_EXISTS);

            assertEquals("BHC", brand.getName());
            assertEquals("https://www.bhc.co.kr", brand.getUrl());
            verify(brandRepository, never()).existsByUrlAndIdNot(any(), any());
            verifyNoInteractions(brandCategoryRepository, brandCategoryService, categoryRepository);
        }

        @Test
        @DisplayName("실패: 다른 브랜드가 쓰는 URL이면 ALREADY_EXISTS, 브랜드는 변경되지 않는다")
        void duplicateUrl() {
            Brand brand = givenBrandFound();
            when(brandRepository.existsByNameAndIdNot("새 이름", 1L)).thenReturn(false);
            when(brandRepository.existsByUrlAndIdNot("https://dup.url", 1L)).thenReturn(true);

            assertBusinessException(() -> brandService.updateBrand(1L, updateRequest("새 이름", "https://dup.url", null, List.of(1L))),
                    ErrorCode.ALREADY_EXISTS);

            assertEquals("BHC", brand.getName());
            assertEquals("https://www.bhc.co.kr", brand.getUrl());
            verifyNoInteractions(brandCategoryRepository, brandCategoryService, categoryRepository);
        }

        @Test
        @DisplayName("성공: 자기 자신의 이름/URL을 그대로 보내도 수정된다 (AndIdNot 검사에 자신의 ID가 전달됨)")
        void sameNameAndUrlAsSelfIsAllowed() {
            Brand brand = givenBrandFound();
            when(brandRepository.existsByNameAndIdNot("BHC", 1L)).thenReturn(false);
            when(brandRepository.existsByUrlAndIdNot("https://www.bhc.co.kr", 1L)).thenReturn(false);

            brandService.updateBrand(1L, updateRequest("BHC", "https://www.bhc.co.kr", "https://new.img", null));

            assertEquals("BHC", brand.getName());
            assertEquals("https://new.img", brand.getImg());
            verify(brandRepository).existsByNameAndIdNot("BHC", 1L);
            verify(brandRepository).existsByUrlAndIdNot("https://www.bhc.co.kr", 1L);
        }

        @Test
        @DisplayName("성공: 이름/URL/이미지를 모두 수정할 수 있다")
        void updateAllFields() {
            Brand brand = givenBrandFound();
            when(brandRepository.existsByNameAndIdNot("새 이름", 1L)).thenReturn(false);
            when(brandRepository.existsByUrlAndIdNot("https://new.url", 1L)).thenReturn(false);

            brandService.updateBrand(1L, updateRequest("새 이름", "https://new.url", "https://new.img", null));

            assertEquals("새 이름", brand.getName());
            assertEquals("https://new.url", brand.getUrl());
            assertEquals("https://new.img", brand.getImg());
        }

        @ParameterizedTest(name = "{index} - categoryIds={0}")
        @NullAndEmptySource
        @DisplayName("성공: categoryIds가 null이거나 빈 리스트면 이름만 수정되고 카테고리는 건드리지 않는다 (URL/이미지는 유지)")
        void nullOrEmptyCategoryIdsKeepsExistingCategories(List<Long> categoryIds) {
            Brand brand = givenBrandFound();

            brandService.updateBrand(1L, updateRequest("새 이름", null, null, categoryIds));

            assertEquals("새 이름", brand.getName());
            assertEquals("https://www.bhc.co.kr", brand.getUrl());
            assertEquals("https://image.url/bhc.png", brand.getImg());
            verify(brandRepository, never()).existsByUrlAndIdNot(any(), any());
            verifyNoInteractions(brandCategoryRepository, brandCategoryService, categoryRepository);
        }

        @ParameterizedTest(name = "{index} - 값=[{0}]")
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("문자열 필드가 빈 문자열/공백이면 기존 값을 유지하고 중복 검사도 하지 않는다")
        void blankStringsKeepOriginal(String blank) {
            Brand brand = givenBrandFound();

            brandService.updateBrand(1L, updateRequest(blank, blank, blank, null));

            assertEquals("BHC", brand.getName());
            assertEquals("https://www.bhc.co.kr", brand.getUrl());
            assertEquals("https://image.url/bhc.png", brand.getImg());
            verify(brandRepository, never()).existsByNameAndIdNot(any(), any());
            verify(brandRepository, never()).existsByUrlAndIdNot(any(), any());
        }

        @Test
        @DisplayName("카테고리 동기화: 기존 {1,2} → 요청 {2,3}이면 1은 삭제, 3은 추가, 2는 그대로")
        void categoriesAreSynchronized() {
            Category cat1 = categoryWithId(1L);
            Category cat2 = categoryWithId(2L);
            Category cat3 = categoryWithId(3L);
            Brand brand = buildBrand();
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandCategoryRepository.findByBrand(brand))
                    .thenReturn(List.of(link(brand, cat1), link(brand, cat2)));
            when(categoryRepository.findAllById(List.of(3L))).thenReturn(List.of(cat3));

            brandService.updateBrand(1L, updateRequest(null, null, null, List.of(2L, 3L)));

            verify(brandCategoryService, times(1)).deleteCategory(brand, cat1);
            verify(brandCategoryService, times(1)).addCategory(brand, cat3);
            verifyNoMoreInteractions(brandCategoryService); // cat2는 삭제도 추가도 되지 않음
        }

        @Test
        @DisplayName("카테고리 동기화: 요청이 기존과 같으면 삭제/추가/카테고리 조회가 모두 없다")
        void sameCategoriesNoChange() {
            Brand brand = buildBrand();
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            Category cat1 = categoryWithId(1L);
            Category cat2 = categoryWithId(2L);
            when(brandCategoryRepository.findByBrand(brand))
                    .thenReturn(List.of(link(brand, cat1), link(brand, cat2)));

            brandService.updateBrand(1L, updateRequest(null, null, null, List.of(1L, 2L)));

            verifyNoInteractions(brandCategoryService, categoryRepository);
        }

        @Test
        @DisplayName("카테고리 동기화: 기존 {1,2} → 요청 {1}이면 2만 삭제된다 (남길 카테고리만 보내면 나머지는 해제)")
        void removeOnly() {
            Category cat1 = categoryWithId(1L);
            Category cat2 = categoryWithId(2L);
            Brand brand = buildBrand();
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandCategoryRepository.findByBrand(brand))
                    .thenReturn(List.of(link(brand, cat1), link(brand, cat2)));

            brandService.updateBrand(1L, updateRequest(null, null, null, List.of(1L)));

            verify(brandCategoryService).deleteCategory(brand, cat2);
            verifyNoMoreInteractions(brandCategoryService);
            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("카테고리 동기화: 기존 카테고리가 없으면 요청한 카테고리만 추가된다")
        void addOnly() {
            Category cat3 = categoryWithId(3L);
            Brand brand = givenExistingBrand();
            when(categoryRepository.findAllById(List.of(3L))).thenReturn(List.of(cat3));

            brandService.updateBrand(1L, updateRequest(null, null, null, List.of(3L)));

            verify(brandCategoryService).addCategory(brand, cat3);
            verifyNoMoreInteractions(brandCategoryService);
        }

        @Test
        @DisplayName("카테고리 동기화: 요청에 중복 ID가 있어도 한 번만 처리된다")
        void duplicateIdsInRequest() {
            Category cat3 = categoryWithId(3L);
            Brand brand = givenExistingBrand();
            when(categoryRepository.findAllById(List.of(3L))).thenReturn(List.of(cat3));

            brandService.updateBrand(1L, updateRequest(null, null, null, List.of(3L, 3L)));

            verify(brandCategoryService, times(1)).addCategory(brand, cat3);
        }

        @Test
        @DisplayName("실패: DB에 없는 카테고리 ID가 섞여 있으면 NOT_FOUND, 아무 카테고리도 추가되지 않고 브랜드 필드도 바뀌지 않는다")
        void unknownCategoryIdThrowsNotFound() {
            Category cat3 = categoryWithId(3L);
            Brand brand = givenExistingBrand();
            when(categoryRepository.findAllById(any())).thenReturn(List.of(cat3));

            assertBusinessException(
                    () -> brandService.updateBrand(1L, updateRequest("새 이름", null, null, List.of(3L, 999L))),
                    ErrorCode.NOT_FOUND);

            verify(brandCategoryService, never()).addCategory(any(), any());
            assertEquals("BHC", brand.getName(), "브랜드 필드 변경은 카테고리 검증 뒤에 일어난다");
        }
    }

    @Nested
    @DisplayName("deleteBrand")
    class DeleteBrandTests {

        @Test
        @DisplayName("성공: 브랜드는 실제로 삭제되지 않고 비활성화된다. 카테고리 연결도 그대로 둔다")
        void deactivatesBrand() {
            Brand brand = buildBrand();
            assertTrue(brand.getIsActive());
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            brandService.deleteBrand(1L);

            assertFalse(brand.getIsActive());
            verify(brandRepository, never()).delete(any(Brand.class));
            verifyNoInteractions(brandCategoryRepository, brandCategoryService);
        }

        @Test
        @DisplayName("이미 비활성인 브랜드를 다시 삭제해도 예외 없이 비활성 상태 유지 (멱등)")
        void idempotent() {
            Brand brand = buildBrand();
            brand.deactive();
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            assertDoesNotThrow(() -> brandService.deleteBrand(1L));

            assertFalse(brand.getIsActive());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 브랜드면 NOT_FOUND")
        void brandNotFound() {
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> brandService.deleteBrand(999L), ErrorCode.NOT_FOUND);

            verify(brandRepository, never()).delete(any(Brand.class));
        }
    }

    @Nested
    @DisplayName("activeBrand")
    class ActiveBrandTests {

        @Test
        @DisplayName("성공: 비활성 브랜드가 다시 활성화된다")
        void reactivatesBrand() {
            Brand brand = buildBrand();
            brand.deactive();
            assertFalse(brand.getIsActive());
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            brandService.activeBrand(1L);

            assertTrue(brand.getIsActive());
        }

        @Test
        @DisplayName("이미 활성인 브랜드를 활성화해도 예외 없이 활성 상태 유지 (멱등)")
        void idempotent() {
            Brand brand = buildBrand();
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            assertDoesNotThrow(() -> brandService.activeBrand(1L));

            assertTrue(brand.getIsActive());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 브랜드면 NOT_FOUND")
        void brandNotFound() {
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> brandService.activeBrand(999L), ErrorCode.NOT_FOUND);
        }
    }
}
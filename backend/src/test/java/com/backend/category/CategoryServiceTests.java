package com.backend.category;

import com.backend.brand.entity.Brand;
import com.backend.brand.entity.BrandCategory;
import com.backend.brand.repository.BrandCategoryRepository;
import com.backend.category.dto.CreateCategory;
import com.backend.category.dto.GetCategoryResponse;
import com.backend.category.dto.UpdateCategory;
import com.backend.category.entity.Category;
import com.backend.category.repository.CategoryRepository;
import com.backend.category.service.CategoryService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTests {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandCategoryRepository brandCategoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category buildCategory(Long id, String name, String img) {
        Category category = Category.builder()
                .name(name)
                .img(img)
                .build();
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Category buildCategory() {
        return buildCategory(1L, "치킨", "https://image.url/chicken.png");
    }

    private Brand buildBrand(String name) {
        return Brand.builder()
                .name(name)
                .url("https://www." + name.toLowerCase() + ".co.kr")
                .img("https://image.url/" + name.toLowerCase() + ".png")
                .build();
    }

    private BrandCategory link(Brand brand, Category category) {
        return BrandCategory.builder().brand(brand).category(category).build();
    }

    private BusinessException assertBusinessException(Runnable runnable, ErrorCode expected) {
        BusinessException exception = assertThrows(BusinessException.class, runnable::run);
        assertEquals(expected, exception.getErrorCode());
        return exception;
    }

    @Nested
    @DisplayName("getCategoryList")
    class GetCategoryListTests {

        @Test
        @DisplayName("성공: 모든 카테고리를 응답 DTO로 변환하고 repository 순서를 유지한다")
        void success() {
            Category chicken = buildCategory(1L, "치킨", "https://image.url/chicken.png");
            Category pizza = buildCategory(2L, "피자", "https://image.url/pizza.png");
            when(categoryRepository.findAll()).thenReturn(List.of(chicken, pizza));

            List<GetCategoryResponse> result = categoryService.getCategoryList();

            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).id());
            assertEquals("치킨", result.get(0).name());
            assertEquals("https://image.url/chicken.png", result.get(0).img());
            assertEquals(2L, result.get(1).id());
            assertEquals("피자", result.get(1).name());
            assertEquals("https://image.url/pizza.png", result.get(1).img());
        }

        @Test
        @DisplayName("성공: 카테고리가 없으면 빈 리스트 (null 아님)")
        void empty() {
            when(categoryRepository.findAll()).thenReturn(List.of());

            List<GetCategoryResponse> result = categoryService.getCategoryList();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategoryTests {

        @Test
        @DisplayName("성공: 이름이 중복되지 않으면 요청값 그대로 저장된다")
        void success() {
            CreateCategory request = new CreateCategory("치킨", "https://image.url/chicken.png");
            when(categoryRepository.existsByName("치킨")).thenReturn(false);

            categoryService.createCategory(request);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository, times(1)).save(captor.capture());
            assertEquals("치킨", captor.getValue().getName());
            assertEquals("https://image.url/chicken.png", captor.getValue().getImg());
        }

        @Test
        @DisplayName("실패: 이미 있는 이름이면 ALREADY_EXISTS, 저장하지 않는다")
        void duplicateName() {
            CreateCategory request = new CreateCategory("치킨", "https://image.url/chicken.png");
            when(categoryRepository.existsByName("치킨")).thenReturn(true);

            assertBusinessException(() -> categoryService.createCategory(request), ErrorCode.ALREADY_EXISTS);

            verify(categoryRepository, never()).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("getCategory")
    class GetCategoryTests {

        @Test
        @DisplayName("성공: ID로 조회한 카테고리를 응답 DTO로 변환한다")
        void success() {
            Category category = buildCategory();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            GetCategoryResponse response = categoryService.getCategory(1L);

            assertEquals(1L, response.id());
            assertEquals("치킨", response.name());
            assertEquals("https://image.url/chicken.png", response.img());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID면 NOT_FOUND")
        void notFound() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> categoryService.getCategory(999L), ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategoryTests {

        @Test
        @DisplayName("성공: 이미지가 변경되고 이름은 그대로다")
        void success() {
            Category category = buildCategory();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            categoryService.updateCategory(1L, new UpdateCategory("https://image.url/new.png"));

            assertEquals("https://image.url/new.png", category.getImg());
            assertEquals("치킨", category.getName());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID면 NOT_FOUND")
        void notFound() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(
                    () -> categoryService.updateCategory(999L, new UpdateCategory("https://image.url/new.png")),
                    ErrorCode.NOT_FOUND);
        }

        @ParameterizedTest(name = "{index} - img=[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t"})
        @DisplayName("실패: 이미지가 null/빈 문자열/공백이면 INVALID_REQUEST, 기존 이미지는 유지된다")
        void blankImg(String img) {
            Category category = buildCategory();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            assertBusinessException(
                    () -> categoryService.updateCategory(1L, new UpdateCategory(img)),
                    ErrorCode.INVALID_REQUEST);

            assertEquals("https://image.url/chicken.png", category.getImg());
        }
    }


    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTests {

        @Test
        @DisplayName("성공: 사용 중인 브랜드가 없으면 예외 없이 카테고리를 삭제한다")
        void deletesWhenNoBrandUsesCategory() {
            Category category = buildCategory();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(brandCategoryRepository.findByCategory(category)).thenReturn(List.of());

            assertDoesNotThrow(() -> categoryService.deleteCategory(1L));

            verify(categoryRepository, times(1)).delete(category);
        }

        @Test
        @DisplayName("순서: 삭제 전에 사용 중인 브랜드 여부를 먼저 조회한다")
        void checksUsageBeforeDelete() {
            Category category = buildCategory();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(brandCategoryRepository.findByCategory(category)).thenReturn(List.of());

            categoryService.deleteCategory(1L);

            InOrder inOrder = inOrder(brandCategoryRepository, categoryRepository);
            inOrder.verify(brandCategoryRepository).findByCategory(category);
            inOrder.verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("실패: 브랜드 1개가 사용 중이면 CATEGORY_CANNOT_DELETE, 메시지에 브랜드 이름, 삭제하지 않는다")
        void cannotDeleteWhenOneBrandUsesCategory() {
            Category category = buildCategory();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(brandCategoryRepository.findByCategory(category))
                    .thenReturn(List.of(link(buildBrand("BHC"), category)));

            BusinessException exception = assertBusinessException(
                    () -> categoryService.deleteCategory(1L), ErrorCode.CATEGORY_CANNOT_DELETE);

            // BusinessException(ErrorCode, String)이 message를 getMessage()로 노출한다고 가정
            assertTrue(exception.getMessage().contains("BHC"));
            verify(categoryRepository, never()).delete(any(Category.class));
        }

        @Test
        @DisplayName("실패: 브랜드 여러 개가 사용 중이면 메시지에 모든 브랜드 이름이 담기고 삭제하지 않는다")
        void cannotDeleteWhenManyBrandsUseCategory() {
            Category category = buildCategory();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(brandCategoryRepository.findByCategory(category)).thenReturn(List.of(
                    link(buildBrand("BHC"), category),
                    link(buildBrand("BBQ"), category),
                    link(buildBrand("Kyochon"), category)
            ));

            BusinessException exception = assertBusinessException(
                    () -> categoryService.deleteCategory(1L), ErrorCode.CATEGORY_CANNOT_DELETE);

            assertAll(
                    () -> assertTrue(exception.getMessage().contains("BHC")),
                    () -> assertTrue(exception.getMessage().contains("BBQ")),
                    () -> assertTrue(exception.getMessage().contains("Kyochon"))
            );
            verify(categoryRepository, never()).delete(any(Category.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID면 NOT_FOUND, 브랜드 조회도 삭제도 하지 않는다")
        void notFound() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> categoryService.deleteCategory(999L), ErrorCode.NOT_FOUND);

            verifyNoInteractions(brandCategoryRepository);
            verify(categoryRepository, never()).delete(any(Category.class));
        }
    }
}
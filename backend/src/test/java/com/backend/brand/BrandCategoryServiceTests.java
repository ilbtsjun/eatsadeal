package com.backend.brand;

import com.backend.brand.entity.Brand;
import com.backend.brand.entity.BrandCategory;
import com.backend.brand.repository.BrandCategoryRepository;
import com.backend.brand.service.BrandCategoryService;
import com.backend.category.entity.Category;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandCategoryServiceTests {

    @Mock
    private BrandCategoryRepository brandCategoryRepository;

    @InjectMocks
    private BrandCategoryService brandCategoryService;

    private Brand buildBrand() {
        return Brand.builder()
                .name("BHC")
                .url("https://www.bhc.co.kr")
                .img("https://image.url/bhc.png")
                .build();
    }

    private Category mockCategory(String name) {
        Category category = mock(Category.class);
        lenient().when(category.getName()).thenReturn(name);
        return category;
    }

    private BusinessException assertBusinessException(Runnable runnable, ErrorCode expected) {
        BusinessException exception = assertThrows(BusinessException.class, runnable::run);
        assertEquals(expected, exception.getErrorCode());
        return exception;
    }

    @Nested
    @DisplayName("addCategory")
    class AddCategoryTests {

        @Test
        @DisplayName("성공: 아직 연결되지 않은 브랜드-카테고리면 BrandCategory가 저장된다")
        void success() {
            Brand brand = buildBrand();
            Category category = mockCategory("치킨");
            when(brandCategoryRepository.findByBrandAndCategory(brand, category)).thenReturn(null);

            brandCategoryService.addCategory(brand, category);

            ArgumentCaptor<BrandCategory> captor = ArgumentCaptor.forClass(BrandCategory.class);
            verify(brandCategoryRepository, times(1)).save(captor.capture());
            assertSame(brand, captor.getValue().getBrand());
            assertSame(category, captor.getValue().getCategory());
        }

        @ParameterizedTest(name = "{index} - brand null={0}, category null={1}")
        @CsvSource({"true,false", "false,true", "true,true"})
        @DisplayName("실패: brand 또는 category가 null이면 NOT_FOUND, repository는 호출하지 않는다")
        void nullArguments(boolean brandIsNull, boolean categoryIsNull) {
            Brand brand = brandIsNull ? null : buildBrand();
            Category category = categoryIsNull ? null : mockCategory("치킨");

            assertBusinessException(() -> brandCategoryService.addCategory(brand, category), ErrorCode.NOT_FOUND);

            verifyNoInteractions(brandCategoryRepository);
        }

        @Test
        @DisplayName("실패: 이미 연결되어 있으면 ALREADY_EXISTS + 안내 메시지, 저장하지 않는다")
        void alreadyExists() {
            Brand brand = buildBrand();
            Category category = mockCategory("치킨");
            when(brandCategoryRepository.findByBrandAndCategory(brand, category))
                    .thenReturn(BrandCategory.builder().brand(brand).category(category).build());

            BusinessException exception = assertBusinessException(
                    () -> brandCategoryService.addCategory(brand, category), ErrorCode.ALREADY_EXISTS);

            assertEquals("BHC에는 이미 치킨이(가) 있습니다.", exception.getMessage());
            verify(brandCategoryRepository, never()).save(any(BrandCategory.class));
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTests {

        @Test
        @DisplayName("성공: 연결이 존재하면 해당 BrandCategory가 삭제된다")
        void success() {
            Brand brand = buildBrand();
            Category category = mockCategory("치킨");
            BrandCategory link = BrandCategory.builder().brand(brand).category(category).build();
            when(brandCategoryRepository.findByBrandAndCategory(brand, category)).thenReturn(link);

            brandCategoryService.deleteCategory(brand, category);

            verify(brandCategoryRepository, times(1)).delete(link);
        }

        @ParameterizedTest(name = "{index} - brand null={0}, category null={1}")
        @CsvSource({"true,false", "false,true", "true,true"})
        @DisplayName("실패: brand 또는 category가 null이면 NOT_FOUND, repository는 호출하지 않는다")
        void nullArguments(boolean brandIsNull, boolean categoryIsNull) {
            Brand brand = brandIsNull ? null : buildBrand();
            Category category = categoryIsNull ? null : mockCategory("치킨");

            assertBusinessException(() -> brandCategoryService.deleteCategory(brand, category), ErrorCode.NOT_FOUND);

            verifyNoInteractions(brandCategoryRepository);
        }

        @Test
        @DisplayName("실패: 연결이 없으면 NOT_FOUND + 안내 메시지, 삭제하지 않는다")
        void linkNotFound() {
            Brand brand = buildBrand();
            Category category = mockCategory("치킨");
            when(brandCategoryRepository.findByBrandAndCategory(brand, category)).thenReturn(null);

            BusinessException exception = assertBusinessException(
                    () -> brandCategoryService.deleteCategory(brand, category), ErrorCode.NOT_FOUND);

            assertEquals("BHC에는 치킨이(가) 없습니다.", exception.getMessage());
            verify(brandCategoryRepository, never()).delete(any(BrandCategory.class));
        }
    }
}
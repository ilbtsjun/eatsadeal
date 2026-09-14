package com.backend;

import com.backend.brand.entity.Brand;
import com.backend.brand.entity.BrandCategory;
import com.backend.brand.repository.BrandCategoryRepository;
import com.backend.brand.repository.BrandRepository;
import com.backend.category.entity.Category;
import com.backend.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final BrandCategoryRepository brandCategoryRepository;

    @Override
    public void run(String... args) {
        Category category = categoryRepository.findById(1L).orElseGet(() -> {
            Category newCategory = Category.builder()
                    .name("치킨")
                    .img("https://www.magnific.com/kr/free-psd/crispy-fried-chicken-drumsticks-plate_409843237.htm...")
                    .build();
            return categoryRepository.save(newCategory);
        });

        chickenInit(category);
    }

    private void chickenInit(Category category){
        saveBrandIfNotExists(1L,
                "BHC", "https://www.bhc.co.kr",
                "https://www.bhc.co.kr/_next/static/media/ico_logo_footer.643042c8.svg",
                category);
        saveBrandIfNotExists(2L,
                "BBQ",
                "https://bbq.co.kr/",
                "https://bbq.co.kr/images/symbols/logo-blue.svg",
                category);
        saveBrandIfNotExists(3L,
                "KyoChonChicken",
                "https://www.kyochon.com/main/index.asp",
                "https://www.kyochon.com/images/common/h1_logo_new2023.png",
                category);
        saveBrandIfNotExists(4L,
                "Pelicana",
                "https://www.pelicana.co.kr/main",
                "https://www.pelicana.co.kr/_nuxt/img/logo.54d3328.png",
                category);
    }

    private void saveBrandIfNotExists(Long id, String name, String url, String img, Category category) {
        if (!brandRepository.existsById(id)) {
            Brand brand = Brand.builder()
                    .name(name)
                    .url(url)
                    .img(img)
                    .build();
            brandRepository.save(brand);

            BrandCategory brandCategory = BrandCategory.builder()
                    .brand(brand)
                    .category(category)
                    .build();
            brandCategoryRepository.save(brandCategory);
        }
    }
}
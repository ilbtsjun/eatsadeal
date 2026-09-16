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

        category = categoryRepository.findById(2L).orElseGet(() -> {
            Category newCategory = Category.builder()
                    .name("피자")
                    .img("https://www.magnific.com/kr/free-psd/delicious-pepperoni-pizza-with-mushrooms-olives_410556008.htm#fromView=search&page=1&position=2&uuid=c7d48e22-d25b-4bfb-9562-8514a9e23c39&track=ais_hybrid&query=%ED%94%BC%EC%9E%90")
                    .build();
            return categoryRepository.save(newCategory);
        });

        pizzaInit(category);

        category = categoryRepository.findById(3L).orElseGet(() -> {
            Category newCategory = Category.builder()
                    .name("햄버거")
                    .img("https://www.magnific.com/kr/free-psd/juicy-burger-with-crispy-fries-red-onions_409868910.htm#fromView=search&page=1&position=1&uuid=4dbc0571-956a-43b4-bfef-0c4859a1d8d8&track=ais_hybrid&query=%ED%96%84%EB%B2%84%EA%B1%B0")
                    .build();
            return categoryRepository.save(newCategory);
        });

        hamburgerInit(category);
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
        saveBrandIfNotExists(5L,
                "Goobne",
                "https://www.goobne.co.kr/main",
                "https://www.goobne.co.kr/pc/assets/img/header-logo2.svg",
                category);
    }

    private void pizzaInit(Category category){
        saveBrandIfNotExists(6L,
                "Dominos", "https://web.dominos.co.kr/main",
                "https://i.namu.wiki/i/tMdC1Tf4vAmadRUb0hJ2th--qZwaAI5ILkjBllBFxbDEoYylggFQsx4_mm0JNEMnlqYCwCxK1C9TOx-s8isoy8uopduT1hX1jI5IB-0yKDOWSslsR7ZYah_W-ZTSLjKFc6S2H7HBfr3FVJlAF3soSQ.svg",
                category);
        saveBrandIfNotExists(7L,
                "Papajohns",
                "https://pji.co.kr//",
                "https://imgcdn4.pji.co.kr/pc/next/images/logo_red.png",
                category);
        saveBrandIfNotExists(8L,
                "Pizzamaru",
                "https://www.pizzamaru.co.kr/",
                "https://www.pizzamaru.co.kr/img/logo.png",
                category);
        saveBrandIfNotExists(9L,
                "Pizzaetang",
                "https://pizzaetang.com/",
                "https://ecimg.cafe24img.com/pg2696b65764996061/etang3651/web/upload/pizza/logo_white.png",
                category);
        saveBrandIfNotExists(10L,
                "Pizzaschool",
                "http://pizzaschool.net/",
                "http://pschool.yyjaja.gethompy.com/wp-content/uploads/2015/08/logo.png",
                category);
    }

    private void hamburgerInit(Category category){
        saveBrandIfNotExists(11L,
                "Burgerking",
                "https://www.burgerking.co.kr/home",
                "https://i.namu.wiki/i/MlmEGrKa5ct7fhuL12pkUI5CVhNTjqDnyEdC_498PTh3Q2m3xKi-7lxc5VFjON5roY4wargvFQpA22km23XYKWAJKethSQvmRLGPzA1jPhRbKU05dza7x3Qu1EvwV1_h3dSSr68RNQzwPJhbJKMREA.svg",
                category);
        saveBrandIfNotExists(12L,
                "Lottelia",
                "https://www.lotteeatz.com/brand/ria",
                "https://i.namu.wiki/i/31assvbCDL3WwNq5AZGj4kXMwCARITBnHHXinUmWdi-Ncr9uUNBqQUQOBRuhm5aE2bOScoDTNNUnrBspgXJzerRqwNq9JZp4TfxwcFB0oUmeM4Dv5DixbqSuu5P-9j1qMplzxXnYKfa8cMzFy6o1Qw.svg",
                category);
        saveBrandIfNotExists(13L,
                "Mcdonalds",
                "https://www.mcdonalds.co.kr/kor/",
                "https://i.namu.wiki/i/KVO-ZHq9r3hF2nfUcIDhwFDrUqioqXL3-AWg9gqL5TjCVw1sAVVgr9Touj-ODoFwruJaYT0xvDWSKuN_JPz1mFd0BfzsbQp_rMfAIaGEU8RztHbKMVRTcRr6089SZwidQAr7XH90eGi_ZDno6Fe4rg.svg",
                category);
        saveBrandIfNotExists(14L,
                "Nobrandburger",
                "https://www.shinsegaefood.com/nobrandburger/index.sf",
                "https://i.namu.wiki/i/GUQfTgiZzf5ZGcP08TsRzzqiFTNP5-RXXAWzgRrYba3NVVsgDndeI6WS1n8PmgnIArHDIlUUruoTXgDi76foww.svg",
                category);
        saveBrandIfNotExists(15L,
                "Momstouch",
                "https://momstouch.co.kr/home.php",
                "https://i.namu.wiki/i/_OVViD0gVEpYz_DiSaTp1cP2-cplRfN_7I8T4pD2vYZS4ac94fAtF7Q0zV7bcqkJsyfpzHwT712wYfVYK3adcm9EI22WGbAv8CWxkDZyFmw90AOIcK0mAOy5AK-XPSNd28mzT_Ci_lfH4nWeMerN7g.svg",
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
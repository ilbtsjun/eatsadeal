INSERT INTO category (id, name, img) VALUES
                                         (1, '치킨', 'https://www.magnific.com/kr/free-psd/crispy-fried-chicken-drumsticks-plate_409843237.htm...'),
                                         (2, '피자', 'https://www.magnific.com/kr/free-psd/delicious-pepperoni-pizza-with-mushrooms-olives_410556008.htm#fromView=search&page=1&position=2&uuid=c7d48e22-d25b-4bfb-9562-8514a9e23c39&track=ais_hybrid&query=%ED%94%BC%EC%9E%90'),
                                         (3, '햄버거', 'https://www.magnific.com/kr/free-psd/juicy-burger-with-crispy-fries-red-onions_409868910.htm#fromView=search&page=1&position=1&uuid=4dbc0571-956a-43b4-bfef-0c4859a1d8d8&track=ais_hybrid&query=%ED%96%84%EB%B2%84%EA%B1%B0');

INSERT INTO brand (id, name, url, img, is_active) VALUES
                                                      (1, 'BHC', 'https://www.bhc.co.kr', 'https://www.bhc.co.kr/_next/static/media/ico_logo_footer.643042c8.svg', b'1'),
                                                      (2, 'BBQ', 'https://bbq.co.kr/', 'https://bbq.co.kr/images/symbols/logo-blue.svg', b'1'),
                                                      (3, 'KyoChonChicken', 'https://www.kyochon.com/main/index.asp', 'https://www.kyochon.com/images/common/h1_logo_new2023.png', b'1'),
                                                      (4, 'Pelicana', 'https://www.pelicana.co.kr/main', 'https://www.pelicana.co.kr/_nuxt/img/logo.54d3328.png', b'1'),
                                                      (5, 'Goobne', 'https://www.goobne.co.kr/main', 'https://www.goobne.co.kr/pc/assets/img/header-logo2.svg', b'1'),
                                                      (6, 'Dominos', 'https://web.dominos.co.kr/main', 'https://i.namu.wiki/i/tMdC1Tf4vAmadRUb0hJ2th--qZwaAI5ILkjBllBFxbDEoYylggFQsx4_mm0JNEMnlqYCwCxK1C9TOx-s8isoy8uopduT1hX1jI5IB-0yKDOWSslsR7ZYah_W-ZTSLjKFc6S2H7HBfr3FVJlAF3soSQ.svg', b'1'),
                                                      (7, 'Papajohns', 'https://pji.co.kr//', 'https://imgcdn4.pji.co.kr/pc/next/images/logo_red.png', b'1'),
                                                      (8, 'PizzaMaru', 'https://www.pizzamaru.co.kr/', 'https://www.pizzamaru.co.kr/img/logo.png', b'1'),
                                                      (9, 'Pizzaetang', 'https://pizzaetang.com/', 'https://ecimg.cafe24img.com/pg2696b65764996061/etang3651/web/upload/pizza/logo_white.png', b'1'),
                                                      (10, 'PizzaSchool', 'http://pizzaschool.net/', 'http://pschool.yyjaja.gethompy.com/wp-content/uploads/2015/08/logo.png', b'1'),
                                                      (11, 'Burgerking', 'https://www.burgerking.co.kr/home', 'https://i.namu.wiki/i/MlmEGrKa5ct7fhuL12pkUI5CVhNTjqDnyEdC_498PTh3Q2m3xKi-7lxc5VFjON5roY4wargvFQpA22km23XYKWAJKethSQvmRLGPzA1jPhRbKU05dza7x3Qu1EvwV1_h3dSSr68RNQzwPJhbJKMREA.svg', b'1'),
                                                      (12, 'Lotteria', 'https://www.lotteeatz.com/brand/ria', 'https://i.namu.wiki/i/31assvbCDL3WwNq5AZGj4kXMwCARITBnHHXinUmWdi-Ncr9uUNBqQUQOBRuhm5aE2bOScoDTNNUnrBspgXJzerRqwNq9JZp4TfxwcFB0oUmeM4Dv5DixbqSuu5P-9j1qMplzxXnYKfa8cMzFy6o1Qw.svg', b'1'),
                                                      (13, 'KFC', 'https://www.kfckorea.com/', 'https://i.namu.wiki/i/PpSks5K2IFxn3X3UTYdL4525HOoBTba1Crw1KWnCWGl9KcitSg1DNKB_ngqR6GO7FWVRVycEGcyYXHkHND9oM_NYZaPYrTNDvY4EmnGMU-935uBMzgaWFvHC2Mj2vNOdqHoSchuKfxC6ZVWMKWbyZA.svg', b'1'),
                                                      (14, 'Frankburger', 'https://frankburger.co.kr/index_brand.html', 'https://i.namu.wiki/i/op6IULFdxqTY7u1o7kUewjPf4omfXwt2qL7_7K0Qdn-kKqKH4bTmC073Bxv3WV0bH9a_tH4_s_E2IQnL889lGo50S3dfyWlbJDsdMWFT_8_JIQlNC2dQEQkAk_GyzOQM-xbygSHqsqzB4EoErJDv2A.svg', b'1'),
                                                      (15, 'Momstouch', 'https://momstouch.co.kr/home.php', 'https://i.namu.wiki/i/_OVViD0gVEpYz_DiSaTp1cP2-cplRfN_7I8T4pD2vYZS4ac94fAtF7Q0zV7bcqkJsyfpzHwT712wYfVYK3adcm9EI22WGbAv8CWxkDZyFmw90AOIcK0mAOy5AK-XPSNd28mzT_Ci_lfH4nWeMerN7g.svg', b'1');

INSERT INTO brand_category (brand_id, category_id) VALUES
                                                       (1, 1),
                                                       (2, 1),
                                                       (3, 1),
                                                       (4, 1),
                                                       (5, 1),
                                                       (6, 2),
                                                       (7, 2),
                                                       (8, 2),
                                                       (9, 2),
                                                       (10, 2),
                                                       (11, 3),
                                                       (12, 3),
                                                       (13, 3),
                                                       (14, 3),
                                                       (15, 3);
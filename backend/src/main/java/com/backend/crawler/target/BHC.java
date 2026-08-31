package com.backend.crawler.target;

import com.backend.crawler.common.Crawler;
import com.backend.dto.EventCreateDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@AllArgsConstructor
public class BHC implements Crawler {

    @Override
    public String getName() {
        return "BHC";
    }

    @Override
    public List<EventCreateDto> crawl() {
        List<EventCreateDto> eventList = new ArrayList<>();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get("https://www.bhc.co.kr/event/currentEvent");

            // Next.js 동적 렌더링 완료 대기
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            List<WebElement> eventElements = wait.until(
                    ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(".event_lists_wrap a"))
            );

            log.info("[BHC] 크롤링된 이벤트 개수: {}", eventElements.size());

            // 데이터 파싱
            Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
            for (WebElement element : eventElements) {
                try {
                    String text = element.getText().trim();
                    if(text.isBlank()){
                        throw new IllegalArgumentException("크롤링 오류");
                    }
                    String title = text.substring(0,text.indexOf('\n'));

                    String startDate = "";
                    String endDate = "";

                    Matcher matcher = datePattern.matcher(text);

                    if (matcher.find()) {
                        startDate = matcher.group();
                    }
                    if (matcher.find()) {
                        endDate = matcher.group();
                    }

                    String linkUrl = element.getAttribute("href");
                    String imgUrl = element.findElement(By.tagName("img")).getAttribute("src");

                    EventCreateDto eventDto = new EventCreateDto(
                            title,
                            null,
                            linkUrl,
                            imgUrl,
                            LocalDate.parse(startDate).atStartOfDay(),
                            LocalDate.parse(endDate).atTime(23, 59, 59),
                            getName(),
                            null);
                    eventList.add(eventDto);
                } catch (Exception e) {
                    log.error("[BHC] 개별 항목 파싱 오류: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[BHC] 크롤링 실패: {}", e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
        return eventList;
    }
}
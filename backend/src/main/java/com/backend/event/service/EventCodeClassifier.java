package com.backend.event.service;

import com.backend.event.dto.EventCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EventCodeClassifier {
    private final AnthropicEventCodeAi eventCodeAi;

    public Set<EventCode> classify(String title, String description, String img) {
        Set<EventCode> byRules = classifyByRules(title, description);
        if (!byRules.isEmpty()) {
            return byRules;
        }

        Set<EventCode> byText = eventCodeAi.classify(title, description, null);
        if (!byText.isEmpty() || !StringUtils.hasText(img)) {
            return byText;
        }
        return eventCodeAi.classify(title, description, img);
    }

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private static final Pattern BUY_ONE_GET_ONE = Pattern.compile("(?<![0-9])1\\s*\\+\\s*1(?![0-9])", FLAGS);
    private static final Pattern BUY_N_GET_N = Pattern.compile("(?<![0-9])[2-9]\\s*\\+\\s*[1-9](?![0-9])|\\bn\\s*\\+\\s*1\\b", FLAGS);
    private static final Pattern DISCOUNT_RATE = Pattern.compile("\\d+(\\.\\d+)?\\s*(%|퍼센트|프로)", FLAGS);
    private static final Pattern DISCOUNT_PRICE = Pattern.compile("\\d[\\d,]*\\s*원\\s*(할인|off|즉시|쿠폰)|[천만]\\s*원\\s*(할인|off|쿠폰)|원\\s*할인", FLAGS);
    private static final Pattern TAKE_OUT = Pattern.compile("포장|픽업|테이크\\s*아웃|take\\s*-?\\s*out", FLAGS);
    private static final Pattern DELIVERY_FREE = Pattern.compile("배달\\s*(비|팁)\\s*(무료|0\\s*원|할인|쿠폰|없)|무료\\s*배달|배달\\s*무료", FLAGS);
    private static final Pattern GIFT_PROMO = Pattern.compile("사은품|굿즈|경품|선물|기프트|gift", FLAGS);
    private static final Pattern GIVEAWAY = Pattern.compile("증정");
    private static final Pattern PAYMENT_PROMO = Pattern.compile("카드|페이(?!지)|\\bpay\\b|제휴|간편\\s*결제|통신사", FLAGS);
    private static final Pattern MEMBERSHIP = Pattern.compile("멤버십|회원|신규|가입|등급|첫\\s*주문|앱\\s*설치|생일|적립|포인트", FLAGS);
    private static final Pattern TIME_SALE = Pattern.compile("타임\\s*세일|요일|평일|주말|심야|런치|점심|\\d{1,2}\\s*시\\s*[~\\-]", FLAGS);

    public static Set<EventCode> classifyByRules(String... texts) {
        String text = joinTexts(texts);
        Set<EventCode> codes = EnumSet.noneOf(EventCode.class);
        if (text.isEmpty()) {
            return codes;
        }

        add(codes, EventCode.BUY_ONE_GET_ONE, BUY_ONE_GET_ONE, text);
        add(codes, EventCode.BUY_N_GET_N, BUY_N_GET_N, text);
        add(codes, EventCode.DISCOUNT_RATE, DISCOUNT_RATE, text);
        add(codes, EventCode.DISCOUNT_PRICE, DISCOUNT_PRICE, text);
        add(codes, EventCode.TAKE_OUT, TAKE_OUT, text);
        add(codes, EventCode.DELIVERY_FREE, DELIVERY_FREE, text);
        add(codes, EventCode.GIFT_PROMO, GIFT_PROMO, text);
        add(codes, EventCode.PAYMENT_PROMO, PAYMENT_PROMO, text);
        add(codes, EventCode.MEMBERSHIP, MEMBERSHIP, text);
        add(codes, EventCode.TIME_SALE, TIME_SALE, text);

        // "1+1 증정" 처럼 수량 행사에 붙은 '증정'은 사은품이 아니다.
        boolean quantityPromo = codes.contains(EventCode.BUY_ONE_GET_ONE) || codes.contains(EventCode.BUY_N_GET_N);
        if (!quantityPromo && GIVEAWAY.matcher(text).find()) {
            codes.add(EventCode.GIFT_PROMO);
        }
        return codes;
    }

    private static void add(Set<EventCode> codes, EventCode code, Pattern pattern, String text) {
        if (pattern.matcher(text).find()) {
            codes.add(code);
        }
    }

    private static String joinTexts(String... texts) {
        StringBuilder sb = new StringBuilder();
        if (texts != null) {
            for (String text : texts) {
                if (text != null && !text.isBlank()) {
                    sb.append(text).append(' ');
                }
            }
        }
        return sb.toString().trim();
    }
}

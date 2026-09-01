package com.backend.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventCode {
    DISCOUNT_PRICE("정액 할인", "특정 금액 즉시 할인"),
    DISCOUNT_RATE("정률 할인", "특정 비율(%) 할인"),
    BUY_ONE_GET_ONE("1+1", "1+1 증정 행사"),
    BUY_N_GET_N("N+1", "2+1, 3+1 등 수량 연계 행사"),
    TAKE_OUT("포장 할인", "방문 포장/픽업 할인"),
    DELIVERY_FREE("배달비 무료", "배달 팁 0원"),
    GIFT_PROMO("사은품 증정", "굿즈 및 사은품 증정"),
    PAYMENT_PROMO("제휴/결제 할인", "카드사/페이 제휴 할인"),
    MEMBERSHIP("멤버십/신규", "신규 가입 및 등급 혜택"),
    TIME_SALE("타임세일", "특정 시간/요일 한정 할인");

    private final String title;
    private final String description;
}
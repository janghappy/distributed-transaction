package com.example.event;

// 프로듀서와 @KafkaListener가 같은 토픽 이름을 쓰도록 한곳에서 관리한다.
// @KafkaListener(topics = ...)에 쓰려면 컴파일 타임 상수여야 하므로 static final String으로 둔다.
public final class Topics {

    public static final String ORDER_PLACED = "order-placed";
    public static final String QUANTITY_DECREASED = "quantity-decreased";
    public static final String QUANTITY_DECREASED_FAIL = "quantity-decreased-fail";
    public static final String POINT_USED = "point-used";
    public static final String POINT_USE_FAIL = "point-use-fail";

    private Topics() {
    }
}

package com.example.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

@Configuration
public class KafkaConfig {

    // 메시지는 문자열(StringDeserializer)로 받고, @KafkaListener 메서드의 파라미터 타입으로 JSON을 변환한다.
    // 받을 타입을 문자열(spring.json.value.default.type)로 지정하지 않아도 되어 오타·클래스 이동에 안전하다.
    // Boot가 이 빈을 기본 리스너 컨테이너 팩토리에 자동으로 적용한다.
    @Bean
    public RecordMessageConverter messageConverter() {
        return new JsonMessageConverter();
    }
}

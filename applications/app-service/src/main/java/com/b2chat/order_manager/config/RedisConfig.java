package com.b2chat.order_manager.config;

import com.b2chat.order_manager.cache.helper.ProductEntityRedisSerializer;
import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, ProductEntity> productReactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ProductEntityRedisSerializer valueSerializer = new ProductEntityRedisSerializer(objectMapper);

        RedisSerializationContext<String, ProductEntity> context =
                RedisSerializationContext.<String, ProductEntity>newSerializationContext(new StringRedisSerializer())
                        .value(valueSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}


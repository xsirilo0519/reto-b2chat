package com.b2chat.order_manager.config;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisConfig Tests")
class RedisConfigTest {

    @Mock
    private ReactiveRedisConnectionFactory connectionFactory;

    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() {
        redisConfig = new RedisConfig();
    }

    @Test
    @DisplayName("productReactiveRedisTemplate - debe retornar un bean no nulo")
    void productReactiveRedisTemplate_shouldReturnNonNullBean() {
        ReactiveRedisTemplate<String, ProductEntity> template =
                redisConfig.productReactiveRedisTemplate(connectionFactory);
        assertThat(template).isNotNull();
    }

    @Test
    @DisplayName("productReactiveRedisTemplate - debe retornar instancia de ReactiveRedisTemplate")
    void productReactiveRedisTemplate_shouldReturnCorrectType() {
        ReactiveRedisTemplate<String, ProductEntity> template =
                redisConfig.productReactiveRedisTemplate(connectionFactory);
        assertThat(template).isInstanceOf(ReactiveRedisTemplate.class);
    }

    @Test
    @DisplayName("productReactiveRedisTemplate - cada llamada debe retornar una nueva instancia")
    void productReactiveRedisTemplate_shouldReturnNewInstanceEachCall() {
        ReactiveRedisTemplate<String, ProductEntity> first =
                redisConfig.productReactiveRedisTemplate(connectionFactory);
        ReactiveRedisTemplate<String, ProductEntity> second =
                redisConfig.productReactiveRedisTemplate(connectionFactory);
        assertThat(first).isNotSameAs(second);
    }
}


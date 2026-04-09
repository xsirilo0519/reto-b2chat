package com.b2chat.order_manager.cache.helper;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;

public class ProductEntityRedisSerializer implements RedisSerializer<ProductEntity> {

    private final ObjectMapper objectMapper;

    public ProductEntityRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(ProductEntity value) throws SerializationException {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializando ProductEntity a JSON", e);
        }
    }

    @Override
    public ProductEntity deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return objectMapper.readValue(bytes, ProductEntity.class);
        } catch (IOException e) {
            throw new SerializationException("Error deserializando ProductEntity desde JSON", e);
        }
    }
}



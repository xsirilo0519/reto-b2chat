package com.b2chat.order_manager.reactive.web.product.mapper;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.b2chat.order_manager.reactive.web.product.dto.ProductDto;
import com.b2chat.order_manager.reactive.web.product.dto.ProductResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    ProductEntity toEntity(ProductDto productDto);

    ProductResponseDto toResponse(ProductEntity productEntity);
}


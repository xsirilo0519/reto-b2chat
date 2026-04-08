package com.b2chat.order_manager.repository.product.mapper;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.b2chat.order_manager.repository.product.data.ProductData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ProductDataMapper {
    ProductDataMapper INSTANCE = Mappers.getMapper(ProductDataMapper.class);

    ProductData toData(ProductEntity productEntity);

    ProductEntity toDomain(ProductData productData);
}


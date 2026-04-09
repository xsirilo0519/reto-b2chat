package com.b2chat.order_manager.repository.order.mapper;

import com.b2chat.order_manager.domain.order.OrderItemEntity;
import com.b2chat.order_manager.repository.order.data.OrderItemData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface OrderItemDataMapper {
    OrderItemDataMapper INSTANCE = Mappers.getMapper(OrderItemDataMapper.class);

    OrderItemData toData(OrderItemEntity orderItemEntity);

    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "productDescription", ignore = true)
    OrderItemEntity toDomain(OrderItemData orderItemData);
}


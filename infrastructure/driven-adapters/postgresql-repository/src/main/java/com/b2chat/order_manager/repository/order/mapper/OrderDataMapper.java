package com.b2chat.order_manager.repository.order.mapper;

import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderStatus;
import com.b2chat.order_manager.repository.order.data.OrderData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface OrderDataMapper {
    OrderDataMapper INSTANCE = Mappers.getMapper(OrderDataMapper.class);

    @Mapping(target = "status", expression = "java(orderEntity.getStatus().name())")
    OrderData toData(OrderEntity orderEntity);

    @Mapping(target = "status", expression = "java(com.b2chat.order_manager.domain.order.OrderStatus.valueOf(orderData.getStatus()))")
    @Mapping(target = "items", ignore = true)
    OrderEntity toDomain(OrderData orderData);
}


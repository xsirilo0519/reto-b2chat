package com.b2chat.order_manager.reactive.web.order.mapper;

import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderItemEntity;
import com.b2chat.order_manager.reactive.web.order.dto.OrderDto;
import com.b2chat.order_manager.reactive.web.order.dto.OrderItemDto;
import com.b2chat.order_manager.reactive.web.order.dto.OrderItemResponseDto;
import com.b2chat.order_manager.reactive.web.order.dto.OrderResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "completed", ignore = true)
    OrderEntity toEntity(OrderDto orderDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "productDescription", ignore = true)
    OrderItemEntity toItemEntity(OrderItemDto orderItemDto);

    OrderResponseDto toResponse(OrderEntity orderEntity);

    OrderItemResponseDto toItemResponse(OrderItemEntity item);
}


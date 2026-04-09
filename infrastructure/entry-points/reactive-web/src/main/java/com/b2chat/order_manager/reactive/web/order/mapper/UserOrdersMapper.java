package com.b2chat.order_manager.reactive.web.order.mapper;

import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderItemEntity;
import com.b2chat.order_manager.reactive.web.order.dto.OrderItemSummaryDto;
import com.b2chat.order_manager.reactive.web.order.dto.OrderSummaryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserOrdersMapper {

    UserOrdersMapper INSTANCE = Mappers.getMapper(UserOrdersMapper.class);

    /** OrderItemEntity.total  ──▶  OrderItemSummaryDto.subtotal */
    @Mapping(target = "subtotal", source = "total")
    OrderItemSummaryDto toItemSummary(OrderItemEntity item);

    /** OrderEntity.id  ──▶  OrderSummaryDto.orderId
     *  items se mapean automáticamente usando toItemSummary() */
    @Mapping(target = "orderId", source = "id")
    OrderSummaryDto toOrderSummary(OrderEntity order);
}


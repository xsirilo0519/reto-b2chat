package com.b2chat.order_manager.reactive.web.order.dto;

import com.b2chat.order_manager.domain.order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class UpdateOrderStatusDto {

    @NotNull(message = "El estado es obligatorio")
    private OrderStatus status;
}


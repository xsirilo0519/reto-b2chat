package com.b2chat.order_manager.config;

import com.b2chat.order_manager.domain.order.OrderGateway;
import com.b2chat.order_manager.domain.products.gateway.ProductGateway;
import com.b2chat.order_manager.domain.users.gateway.UserGateway;
import com.b2chat.order_manager.usecase.OrdersUseCase;
import com.b2chat.order_manager.usecase.ProductsUseCase;
import com.b2chat.order_manager.usecase.UsersUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public UsersUseCase usersUseCase(UserGateway userGateway) {
        return new UsersUseCase(userGateway);
    }

    @Bean
    public ProductsUseCase productsUseCase(ProductGateway productGateway) {
        return new ProductsUseCase(productGateway);
    }

    @Bean
    public OrdersUseCase ordersUseCase(OrderGateway orderGateway,
                                       ProductGateway productGateway,
                                       UserGateway userGateway) {
        return new OrdersUseCase(orderGateway, productGateway, userGateway);
    }
}

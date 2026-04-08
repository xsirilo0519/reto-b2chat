package com.b2chat.order_manager.config;

import com.b2chat.order_manager.domain.notification.OrderNotificationGateway;
import com.b2chat.order_manager.domain.order.OrderGateway;
import com.b2chat.order_manager.domain.order.OrderPublishGateway;
import com.b2chat.order_manager.domain.products.gateway.ProductGateway;
import com.b2chat.order_manager.domain.users.gateway.UserGateway;
import com.b2chat.order_manager.usecase.OrdersUseCase;
import com.b2chat.order_manager.usecase.ProductsUseCase;
import com.b2chat.order_manager.usecase.UsersUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("UseCaseConfig Tests")
class UseCaseConfigTest {

    @Mock private UserGateway userGateway;
    @Mock private ProductGateway productGateway;
    @Mock private OrderGateway orderGateway;
    @Mock private OrderNotificationGateway notificationGateway;
    @Mock private OrderPublishGateway publishGateway;

    private UseCaseConfig useCaseConfig;

    @BeforeEach
    void setUp() {
        useCaseConfig = new UseCaseConfig();
    }

    @Test
    @DisplayName("usersUseCase - debe crear un bean UsersUseCase no nulo")
    void usersUseCase_shouldReturnNonNullBean() {
        UsersUseCase result = useCaseConfig.usersUseCase(userGateway);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("usersUseCase - debe crear una instancia de tipo UsersUseCase")
    void usersUseCase_shouldReturnCorrectType() {
        UsersUseCase result = useCaseConfig.usersUseCase(userGateway);
        assertThat(result).isInstanceOf(UsersUseCase.class);
    }

    @Test
    @DisplayName("usersUseCase - cada llamada debe retornar una nueva instancia")
    void usersUseCase_shouldReturnNewInstanceEachCall() {
        UsersUseCase first  = useCaseConfig.usersUseCase(userGateway);
        UsersUseCase second = useCaseConfig.usersUseCase(userGateway);
        assertThat(first).isNotSameAs(second);
    }

    @Test
    @DisplayName("productsUseCase - debe crear un bean ProductsUseCase no nulo")
    void productsUseCase_shouldReturnNonNullBean() {
        ProductsUseCase result = useCaseConfig.productsUseCase(productGateway);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("productsUseCase - debe crear una instancia de tipo ProductsUseCase")
    void productsUseCase_shouldReturnCorrectType() {
        ProductsUseCase result = useCaseConfig.productsUseCase(productGateway);
        assertThat(result).isInstanceOf(ProductsUseCase.class);
    }

    @Test
    @DisplayName("productsUseCase - cada llamada debe retornar una nueva instancia")
    void productsUseCase_shouldReturnNewInstanceEachCall() {
        ProductsUseCase first  = useCaseConfig.productsUseCase(productGateway);
        ProductsUseCase second = useCaseConfig.productsUseCase(productGateway);
        assertThat(first).isNotSameAs(second);
    }

    @Test
    @DisplayName("ordersUseCase - debe crear un bean OrdersUseCase no nulo")
    void ordersUseCase_shouldReturnNonNullBean() {
        OrdersUseCase result = useCaseConfig.ordersUseCase(
                orderGateway, productGateway, userGateway, notificationGateway, publishGateway);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("ordersUseCase - debe crear una instancia de tipo OrdersUseCase")
    void ordersUseCase_shouldReturnCorrectType() {
        OrdersUseCase result = useCaseConfig.ordersUseCase(
                orderGateway, productGateway, userGateway, notificationGateway, publishGateway);
        assertThat(result).isInstanceOf(OrdersUseCase.class);
    }

    @Test
    @DisplayName("ordersUseCase - cada llamada debe retornar una nueva instancia")
    void ordersUseCase_shouldReturnNewInstanceEachCall() {
        OrdersUseCase first = useCaseConfig.ordersUseCase(
                orderGateway, productGateway, userGateway, notificationGateway, publishGateway);
        OrdersUseCase second = useCaseConfig.ordersUseCase(
                orderGateway, productGateway, userGateway, notificationGateway, publishGateway);
        assertThat(first).isNotSameAs(second);
    }
}

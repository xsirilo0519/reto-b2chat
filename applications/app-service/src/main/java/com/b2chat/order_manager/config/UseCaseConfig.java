package com.b2chat.order_manager.config;

import com.b2chat.order_manager.usecase.UsersUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public UsersUseCase usersUseCase() {
        return new UsersUseCase();
    }
}

package com.b2chat.order_manager.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DataBaseConfig {

    @Value("${custom.db.hostUrl}")
    private String  hostUrl;
    @Value("${custom.db.username}")
    private String username;
    @Value("${custom.db.password}")
    private String password;
    @Value("${custom.db.schema}")
    private String schema;

    @Bean
    @Primary
    public ConnectionFactory connectionFactory(){

        ConnectionFactory factory = ConnectionFactories.get(buildConnectionUrl());
        ConnectionPoolConfiguration configuration = ConnectionPoolConfiguration.builder(factory)
                .maxIdleTime(java.time.Duration.ofMinutes(30))
                .maxLifeTime(java.time.Duration.ofHours(1))
                .maxAcquireTime(java.time.Duration.ofSeconds(30))
                .maxCreateConnectionTime(java.time.Duration.ofSeconds(30))
                .initialSize(5)
                .maxSize(20)
                .postAllocate(connection -> Mono.from(connection.createStatement("SET search_path TO "+ schema).execute()).then())
                .build();
        return new ConnectionPool(configuration);
    }

    private String buildConnectionUrl() {
        String encodedUser = URLEncoder.encode(username, StandardCharsets.UTF_8);
        String encodedPass = URLEncoder.encode(password, StandardCharsets.UTF_8);
        return String.format("r2dbc:postgresql://%s:%s@%s", encodedUser, encodedPass, hostUrl);
    }
}

package com.b2chat.order_manager.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataBaseConfig Tests")
class DataBaseConfigTest {

    private DataBaseConfig config;
    private ConnectionPool openPool;

    @BeforeEach
    void setUp() {
        config = new DataBaseConfig();
    }

    @AfterEach
    void tearDown() {
        if (openPool != null) {
            openPool.dispose();
            openPool = null;
        }
    }

    private void configureFields(String hostUrl, String username, String password, String schema) {
        ReflectionTestUtils.setField(config, "hostUrl",  hostUrl);
        ReflectionTestUtils.setField(config, "username", username);
        ReflectionTestUtils.setField(config, "password", password);
        ReflectionTestUtils.setField(config, "schema",   schema);
    }

    private String invokeUrl() {
        return ReflectionTestUtils.invokeMethod(config, "buildConnectionUrl");
    }

    private ConnectionPool createPool(String schema) {
        configureFields("localhost:5432/test_db", "testuser", "testpass", schema);
        openPool = (ConnectionPool) config.connectionFactory();
        return openPool;
    }

    private Object getPoolConfig(ConnectionPool pool) {
        Object innerPool = ReflectionTestUtils.getField(pool, "connectionPool");
        return ReflectionTestUtils.getField(innerPool, "poolConfig");
    }

    private Object getAllocationStrategy(ConnectionPool pool) {
        return ReflectionTestUtils.invokeMethod(getPoolConfig(pool), "allocationStrategy");
    }

    @Test
    @DisplayName("buildConnectionUrl - debe usar el protocolo r2dbc:postgresql://")
    void buildConnectionUrl_shouldUseR2dbcPostgresqlProtocol() {
        configureFields("localhost:5432/mydb", "user", "pass", "public");
        assertThat(invokeUrl()).startsWith("r2dbc:postgresql://");
    }

    @Test
    @DisplayName("buildConnectionUrl - debe incluir el hostUrl al final de la URL")
    void buildConnectionUrl_shouldIncludeHostUrl() {
        configureFields("localhost:5432/mydb", "user", "pass", "public");
        assertThat(invokeUrl()).endsWith("@localhost:5432/mydb");
    }

    @Test
    @DisplayName("buildConnectionUrl - debe construir la URL con formato correcto para credenciales simples")
    void buildConnectionUrl_shouldBuildCorrectUrlForSimpleCredentials() {
        configureFields("localhost:5432/b2chat", "postgres", "postgres", "public");
        assertThat(invokeUrl()).isEqualTo("r2dbc:postgresql://postgres:postgres@localhost:5432/b2chat");
    }

    @Test
    @DisplayName("buildConnectionUrl - debe codificar los caracteres especiales del password")
    void buildConnectionUrl_shouldUrlEncodeSpecialCharsInPassword() {
        configureFields("localhost:5432/mydb", "user", "P@ss#123!", "public");
        String encodedPassword = URLEncoder.encode("P@ss#123!", StandardCharsets.UTF_8);
        assertThat(invokeUrl()).contains(":" + encodedPassword + "@");
    }

    @Test
    @DisplayName("buildConnectionUrl - el asterisco (*) no es especial para URLEncoder y queda tal cual (Sofka2025*)")
    void buildConnectionUrl_shouldKeepAsteriskUnchanged() {
        configureFields("localhost:5432/b2chat_manager", "postgres", "Sofka2025*", "public");
        assertThat(invokeUrl()).isEqualTo("r2dbc:postgresql://postgres:Sofka2025*@localhost:5432/b2chat_manager");
    }

    @Test
    @DisplayName("buildConnectionUrl - debe codificar los caracteres especiales del username")
    void buildConnectionUrl_shouldUrlEncodeSpecialCharsInUsername() {
        configureFields("localhost:5432/mydb", "user@domain", "pass", "public");
        String encodedUser = URLEncoder.encode("user@domain", StandardCharsets.UTF_8);
        assertThat(invokeUrl()).contains(encodedUser + ":");
    }

    @Test
    @DisplayName("buildConnectionUrl - usuario y password sin caracteres especiales no deben modificarse")
    void buildConnectionUrl_shouldNotModifyPlainCredentials() {
        configureFields("db.host:5432/prod", "admin", "secret", "public");
        assertThat(invokeUrl()).isEqualTo("r2dbc:postgresql://admin:secret@db.host:5432/prod");
    }

    @Test
    @DisplayName("buildConnectionUrl - debe manejar espacios en el password codificándolos como +")
    void buildConnectionUrl_shouldEncodeSpacesInPassword() {
        configureFields("localhost:5432/mydb", "user", "my password", "public");
        String encodedPassword = URLEncoder.encode("my password", StandardCharsets.UTF_8);
        assertThat(invokeUrl()).contains(encodedPassword);
    }

    @Test
    @DisplayName("connectionFactory - debe retornar una instancia de ConnectionPool no nula")
    void connectionFactory_shouldReturnNonNullConnectionPool() {
        configureFields("localhost:5432/test_db", "user", "pass", "public");
        ConnectionFactory result = config.connectionFactory();
        assertThat(result).isNotNull().isInstanceOf(ConnectionPool.class);
        openPool = (ConnectionPool) result;
    }

    @Test
    @DisplayName("connectionFactory - las métricas del pool deben estar disponibles")
    void connectionFactory_shouldExposeMetrics() {
        assertThat(createPool("public").getMetrics()).isPresent();
    }

    @Test
    @DisplayName("connectionFactory - debe configurar maxAcquireTime=30 segundos")
    void connectionFactory_shouldConfigureMaxAcquireTimeAs30Seconds() {
        assertThat(ReflectionTestUtils.getField(createPool("public"), "maxAcquireTime"))
                .isEqualTo(Duration.ofSeconds(30));
    }

}

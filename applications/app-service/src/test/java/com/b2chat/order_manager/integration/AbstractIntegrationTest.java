package com.b2chat.order_manager.integration;

import com.b2chat.order_manager.repository.order.data.OrderData;
import com.b2chat.order_manager.repository.order.data.OrderItemData;
import com.b2chat.order_manager.repository.order.repository.OrderDataRepository;
import com.b2chat.order_manager.repository.order.repository.OrderItemDataRepository;
import com.b2chat.order_manager.repository.product.data.ProductData;
import com.b2chat.order_manager.repository.product.repository.ProductDataRepository;
import com.b2chat.order_manager.repository.user.data.UserData;
import com.b2chat.order_manager.repository.user.repository.UserDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String ORDER_PROCESS_QUEUE = "order.process.queue";
    protected static final String ORDER_PROCESSING_QUEUE = "order.processing.queue";
    protected static final String ORDER_RECEIVED_QUEUE = "order.notification.received.queue";
    protected static final String ORDER_COMPLETED_QUEUE = "order.notification.completed.queue";
    protected static final String ORDER_CANCELLED_QUEUE = "order.notification.cancelled.queue";

    protected static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("order_manager_it")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("sql/init.sql");

    protected static final RabbitMQContainer RABBIT_MQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

    protected static final GenericContainer<?> MAILHOG = new GenericContainer<>(DockerImageName.parse("mailhog/mailhog:v1.0.1"))
            .withExposedPorts(1025, 8025)
            .waitingFor(Wait.forListeningPort());

    static {
        Startables.deepStart(Stream.of(POSTGRESQL, RABBIT_MQ, MAILHOG)).join();
    }

    @LocalServerPort
    protected int port;

    protected WebTestClient webTestClient;

    @Autowired
    protected AmqpAdmin amqpAdmin;

    @Autowired
    protected UserDataRepository userDataRepository;

    @Autowired
    protected ProductDataRepository productDataRepository;

    @Autowired
    protected OrderDataRepository orderDataRepository;

    @Autowired
    protected OrderItemDataRepository orderItemDataRepository;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("custom.db.hostUrl", () -> POSTGRESQL.getHost() + ":" + POSTGRESQL.getMappedPort(5432) + "/" + POSTGRESQL.getDatabaseName());
        registry.add("custom.db.username", POSTGRESQL::getUsername);
        registry.add("custom.db.password", POSTGRESQL::getPassword);
        registry.add("custom.db.schema", () -> "public");

        registry.add("spring.rabbitmq.host", RABBIT_MQ::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBIT_MQ.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        registry.add("spring.mail.host", MAILHOG::getHost);
        registry.add("spring.mail.port", () -> MAILHOG.getMappedPort(1025));
        registry.add("spring.mail.username", () -> "integration@test.com");
        registry.add("spring.mail.password", () -> "integration");
        registry.add("spring.mail.properties.mail.smtp.auth", () -> false);
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> false);
    }

    @BeforeEach
    void resetInfrastructureState() {
        String token = obtainJwtToken();
        webTestClient = WebTestClient.bindToServer()
                .responseTimeout(Duration.ofSeconds(20))
                .baseUrl("http://localhost:" + port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        purgeQueue(ORDER_PROCESS_QUEUE);
        purgeQueue(ORDER_PROCESSING_QUEUE);
        purgeQueue(ORDER_RECEIVED_QUEUE);
        purgeQueue(ORDER_COMPLETED_QUEUE);
        purgeQueue(ORDER_CANCELLED_QUEUE);
        resetDatabase();
    }

    protected String obtainJwtToken() {
        Map<?, ?> response = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "admin", "password", "admin"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        return response != null ? (String) response.get("token") : "";
    }

    protected void resetDatabase() {
        orderItemDataRepository.deleteAll().block(Duration.ofSeconds(5));
        orderDataRepository.deleteAll().block(Duration.ofSeconds(5));
        productDataRepository.deleteAll().block(Duration.ofSeconds(5));
        userDataRepository.deleteAll().block(Duration.ofSeconds(5));
    }

    protected void purgeQueue(String queueName) {
        amqpAdmin.purgeQueue(queueName, true);
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "+" + System.nanoTime() + "@test.com";
    }

    protected void awaitEmail(String recipient, String subject) {
        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(readMailhogMessages())
                        .contains(recipient)
                        .contains(subject));
    }

    protected String readMailhogMessages() {
        return WebClient.builder()
                .baseUrl("http://" + MAILHOG.getHost() + ":" + MAILHOG.getMappedPort(8025))
                .build()
                .get()
                .uri("/api/v2/messages")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(5));
    }

    protected UserData persistUser(String name, String email, String address) {
        UserData user = new UserData();
        user.setName(name);
        user.setEmail(email);
        user.setAddress(address);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userDataRepository.save(user).block(Duration.ofSeconds(5));
    }

    protected ProductData persistProduct(String name, String description, BigDecimal price, int stock) {
        ProductData product = new ProductData();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return productDataRepository.save(product).block(Duration.ofSeconds(5));
    }

    protected OrderData persistOrder(Long userId, String status, BigDecimal totalAmount, boolean completed) {
        OrderData order = new OrderData();
        order.setUserId(userId);
        order.setStatus(status);
        order.setTotalAmount(totalAmount);
        order.setCompleted(completed);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return orderDataRepository.save(order).block(Duration.ofSeconds(5));
    }

    protected OrderItemData persistOrderItem(Long orderId, Long productId, int quantity, BigDecimal unitPrice, BigDecimal total) {
        OrderItemData item = new OrderItemData();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setTotal(total);
        return orderItemDataRepository.save(item).block(Duration.ofSeconds(5));
    }
}


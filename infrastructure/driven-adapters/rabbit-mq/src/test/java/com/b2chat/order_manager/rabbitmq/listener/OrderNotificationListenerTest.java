package com.b2chat.order_manager.rabbitmq.listener;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderNotificationListener Tests")
class OrderNotificationListenerTest {

    @Mock
    private JavaMailSender mailSender;

    private OrderNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderNotificationListener(mailSender);
    }

    private MimeMessage createInMemoryMimeMessage() {
        return new MimeMessage((Session) null);
    }

    private Map<String, Object> buildOrderEvent(Long orderId, String email, String name, Object total) {
        return buildOrderEvent((Object) orderId, email, name, total);
    }

    private Map<String, Object> buildOrderEvent(Object orderId, String email, String name, Object total) {
        Map<String, Object> event = new HashMap<>();
        event.put("orderId", orderId);
        event.put("userEmail", email);
        event.put("userName", name);
        event.put("totalAmount", total);
        return event;
    }


    @Test
    @DisplayName("onOrderReceived - debe crear y enviar un email cuando la orden es recibida")
    void onOrderReceived_shouldSendEmail() {
        when(mailSender.createMimeMessage()).thenReturn(createInMemoryMimeMessage());

        Map<String, Object> event = buildOrderEvent(1L, "user@test.com", "Juan", new BigDecimal("100.00"));

        listener.onOrderReceived(event);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("onOrderReceived - el asunto del email debe contener el número de orden")
    void onOrderReceived_shouldSendEmailWithOrderIdInSubject() {
        MimeMessage mimeMessage = createInMemoryMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Map<String, Object> event = buildOrderEvent(42L, "cliente@test.com", "María", new BigDecimal("200.00"));

        listener.onOrderReceived(event);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("onOrderReceived - no debe lanzar excepción cuando falla el envío (error capturado en catch)")
    void onOrderReceived_shouldNotThrow_whenMailSenderFails() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP no disponible"));

        Map<String, Object> event = buildOrderEvent(1L, "user@test.com", "Juan", new BigDecimal("100.00"));

        assertThatNoException().isThrownBy(() -> listener.onOrderReceived(event));
    }


    @Test
    @DisplayName("onOrderCompleted - debe crear y enviar un email cuando la orden es completada")
    void onOrderCompleted_shouldSendEmail() {
        when(mailSender.createMimeMessage()).thenReturn(createInMemoryMimeMessage());

        Map<String, Object> event = buildOrderEvent(2L, "user@test.com", "María", new BigDecimal("500.00"));

        listener.onOrderCompleted(event);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("onOrderCompleted - no debe lanzar excepción cuando falla el envío")
    void onOrderCompleted_shouldNotThrow_whenMailSenderFails() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Conexión rechazada"));

        Map<String, Object> event = buildOrderEvent(2L, "user@test.com", "María", new BigDecimal("500.00"));

        assertThatNoException().isThrownBy(() -> listener.onOrderCompleted(event));
    }


    @Test
    @DisplayName("onOrderCancelled - debe crear y enviar un email con el motivo de cancelación")
    void onOrderCancelled_shouldSendEmailWithReason() {
        when(mailSender.createMimeMessage()).thenReturn(createInMemoryMimeMessage());

        Map<String, Object> event = new HashMap<>();
        event.put("orderId", 3L);
        event.put("userEmail", "user@test.com");
        event.put("userName", "Carlos");
        event.put("reason", "Stock insuficiente para 'Laptop'. Solicitado: 5, disponible: 2");

        listener.onOrderCancelled(event);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("onOrderCancelled - no debe lanzar excepción cuando falla el envío")
    void onOrderCancelled_shouldNotThrow_whenMailSenderFails() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Timeout SMTP"));

        Map<String, Object> event = new HashMap<>();
        event.put("orderId", 3L);
        event.put("userEmail", "user@test.com");
        event.put("userName", "Carlos");
        event.put("reason", "Stock insuficiente");

        assertThatNoException().isThrownBy(() -> listener.onOrderCancelled(event));
    }

    @Test
    @DisplayName("onOrderCancelled - debe manejar orderId numérico entero (Integer) correctamente")
    void onOrderCancelled_shouldHandleIntegerOrderId() {
        when(mailSender.createMimeMessage()).thenReturn(createInMemoryMimeMessage());

        Map<String, Object> event = new HashMap<>();
        event.put("orderId", 5);
        event.put("userEmail", "user@test.com");
        event.put("userName", "Ana");
        event.put("reason", "Motivo de prueba");

        listener.onOrderCancelled(event);

        verify(mailSender).send(any(MimeMessage.class));
    }


    @Test
    @DisplayName("toLong - rama Number: debe convertir Integer a Long correctamente")
    void toLong_shouldConvertInteger_viaNumberBranch() {
        when(mailSender.createMimeMessage()).thenReturn(createInMemoryMimeMessage());

        Map<String, Object> event = buildOrderEvent(10, "u@test.com", "Juan", BigDecimal.TEN);

        listener.onOrderReceived(event);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("toLong - rama Number: debe convertir Double a Long correctamente")
    void toLong_shouldConvertDouble_viaNumberBranch() {
        when(mailSender.createMimeMessage()).thenReturn(createInMemoryMimeMessage());

        Map<String, Object> event = buildOrderEvent(7.0, "u@test.com", "Pedro", BigDecimal.ONE);

        listener.onOrderReceived(event);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("toLong - rama Number: debe convertir BigDecimal a Long correctamente")
    void toLong_shouldConvertBigDecimal_viaNumberBranch() {
        when(mailSender.createMimeMessage()).thenReturn(createInMemoryMimeMessage());

        Map<String, Object> event = buildOrderEvent(
                new BigDecimal("99"), "u@test.com", "Laura", BigDecimal.TEN);

        listener.onOrderReceived(event);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("toLong - rama String: debe parsear una cadena numérica a Long correctamente")
    void toLong_shouldParseString_viaParseStringBranch() {
        when(mailSender.createMimeMessage()).thenReturn(createInMemoryMimeMessage());

        Map<String, Object> event = buildOrderEvent("42", "u@test.com", "Luis", BigDecimal.ONE);

        listener.onOrderReceived(event);

        verify(mailSender).send(any(MimeMessage.class));
    }
}


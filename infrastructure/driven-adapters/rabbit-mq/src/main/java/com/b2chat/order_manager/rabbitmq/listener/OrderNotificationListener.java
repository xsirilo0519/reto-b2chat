package com.b2chat.order_manager.rabbitmq.listener;

import com.b2chat.order_manager.rabbitmq.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final JavaMailSender mailSender;

    @RabbitListener(queues = RabbitMQConfig.RECEIVED_QUEUE)
    public void onOrderReceived(Map<String, Object> event) {
        String to        = (String) event.get("userEmail");
        String userName  = (String) event.get("userName");
        Long   orderId   = toLong(event.get("orderId"));
        Object total     = event.get("totalAmount");

        String subject = "Pedido #" + orderId + " recibido";
        String body    = buildReceivedEmail(userName, orderId, total);

        sendEmail(to, subject, body);
        log.info("Email enviado a {} por orden recibida [id={}]", to, orderId);
    }

    @RabbitListener(queues = RabbitMQConfig.COMPLETED_QUEUE)
    public void onOrderCompleted(Map<String, Object> event) {
        String to       = (String) event.get("userEmail");
        String userName = (String) event.get("userName");
        Long   orderId  = toLong(event.get("orderId"));
        Object total    = event.get("totalAmount");

        String subject = "Pedido #" + orderId + " completado";
        String body    = buildCompletedEmail(userName, orderId, total);

        sendEmail(to, subject, body);
        log.info("Email enviado a {} por orden completada [id={}]", to, orderId);
    }

    @RabbitListener(queues = RabbitMQConfig.CANCELLED_QUEUE)
    public void onOrderCancelled(Map<String, Object> event) {
        String to       = (String) event.get("userEmail");
        String userName = (String) event.get("userName");
        Long   orderId  = toLong(event.get("orderId"));
        String reason   = (String) event.get("reason");

        String subject = "Pedido #" + orderId + " cancelado";
        sendEmail(to, subject, buildCancelledEmail(userName, orderId, reason));
        log.info("Email enviado a {} por orden cancelada [id={}]", to, orderId);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            var message = mailSender.createMimeMessage();
            var helper  = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", to, e.getMessage());
        }
    }

    private String buildReceivedEmail(String userName, Long orderId, Object total) {
        return """
                <html><body style="font-family:Arial,sans-serif;color:#333">
                  <h2>¡Hola, %s!</h2>
                  <p>Tu pedido ha sido <strong>recibido correctamente</strong>.</p>
                  <table style="border-collapse:collapse">
                    <tr><td style="padding:4px 12px"><strong>Pedido #:</strong></td><td>%d</td></tr>
                    <tr><td style="padding:4px 12px"><strong>Total:</strong></td><td>$%s</td></tr>
                    <tr><td style="padding:4px 12px"><strong>Estado:</strong></td><td>PENDING</td></tr>
                  </table>
                  <p style="margin-top:20px">Te notificaremos cuando tu pedido sea completado.</p>
                </body></html>
                """.formatted(userName, orderId, total);
    }

    private String buildCompletedEmail(String userName, Long orderId, Object total) {
        return """
                <html><body style="font-family:Arial,sans-serif;color:#333">
                  <h2>¡Hola, %s!</h2>
                  <p>Tu pedido ha sido <strong style="color:#27ae60">completado exitosamente</strong>. 🎉</p>
                  <table style="border-collapse:collapse">
                    <tr><td style="padding:4px 12px"><strong>Pedido #:</strong></td><td>%d</td></tr>
                    <tr><td style="padding:4px 12px"><strong>Total:</strong></td><td>$%s</td></tr>
                    <tr><td style="padding:4px 12px"><strong>Estado:</strong></td><td>COMPLETED</td></tr>
                  </table>
                  <p style="margin-top:20px">¡Gracias por tu compra!</p>
                </body></html>
                """.formatted(userName, orderId, total);
    }

    private String buildCancelledEmail(String userName, Long orderId, String reason) {
        return """
                <html><body style="font-family:Arial,sans-serif;color:#333">
                  <h2>¡Hola, %s!</h2>
                  <p>Lamentamos informarte que tu pedido ha sido <strong style="color:#e74c3c">cancelado</strong>.</p>
                  <table style="border-collapse:collapse">
                    <tr><td style="padding:4px 12px"><strong>Pedido #:</strong></td><td>%d</td></tr>
                    <tr><td style="padding:4px 12px"><strong>Estado:</strong></td><td>CANCELLED</td></tr>
                    <tr><td style="padding:4px 12px"><strong>Motivo:</strong></td><td>%s</td></tr>
                  </table>
                  <p style="margin-top:20px">Si tienes dudas, contáctanos.</p>
                </body></html>
                """.formatted(userName, orderId, reason);
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }
}



package com.iluwatar.hexagonal;

import com.iluwatar.hexagonal.architecture.config.AppConfig;
import com.iluwatar.hexagonal.architecture.domain.model.Payment;
import com.iluwatar.hexagonal.architecture.domain.model.PaymentMethod;
import com.iluwatar.hexagonal.architecture.domain.model.PaymentStatus;
import com.iluwatar.hexagonal.architecture.domain.model.Refund;
import com.iluwatar.hexagonal.architecture.service.PaymentService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class PaymentApp {

  public static void main(String[] args) {
    // Initialize Spring context
    ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
    PaymentService paymentService = context.getBean(PaymentService.class);

    System.out.println("=== Payment Processing Demo ===");

    // 1. Create payment
    Payment payment = paymentService.createPayment("ORDER-001", new BigDecimal(100.00), PaymentMethod.CREDIT_CARD);
    System.out.println("Created payment: " + payment);

    // 2. Process payment
    Payment processedPayment = paymentService.processPayment(payment.getId());
    System.out.println("Processed payment: " + processedPayment);

    // 3. Request refund
    if (processedPayment.getPaymentStatus() == PaymentStatus.SUCCESS) {
      Refund refund = paymentService.requestRefund(processedPayment.getId(), new BigDecimal(50.00), "Partial refund");
      System.out.println("Requested refund: " + refund);

      // 4. Process refund
      Refund processedRefund = paymentService.processRefund(refund.getId());
      System.out.println("Processed refund: " + processedRefund);
    }

    // 5. Get payment by order ID
    Payment retrievedPayment = paymentService.getPaymentByOrderId("ORDER-001");
    System.out.println("Retrieved payment by order ID: " + retrievedPayment);

    // 6. Payment statistics by method
    Map<PaymentMethod, Map<String, Object>> statsByMethod = paymentService.getPaymentStatisticsByMethod();
    System.out.println("Payment statistics by method: " + statsByMethod);

    // 7. Payment and refund statistics by date range
    LocalDateTime startDate = LocalDateTime.now().minus(1, ChronoUnit.DAYS);
    LocalDateTime endDate = LocalDateTime.now().plus(1, ChronoUnit.DAYS);
    Map<String, BigDecimal> statsByDate = paymentService.getPaymentAndRefundStatisticsByDateRange(startDate, endDate);
    System.out.println("Payment and refund statistics by date range: " + statsByDate);

    // 8. Payment success rate
    double successRate = paymentService.getPaymentSuccessRate();
    System.out.printf("Payment success rate: %.2f%%\n", successRate * 100);

    System.out.println("=== Demo Complete ===");
  }
}
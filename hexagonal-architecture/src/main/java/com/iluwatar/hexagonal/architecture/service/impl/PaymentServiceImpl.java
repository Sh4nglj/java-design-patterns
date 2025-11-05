package com.iluwatar.hexagonal.architecture.service.impl;

import com.iluwatar.hexagonal.architecture.domain.model.Payment;
import com.iluwatar.hexagonal.architecture.domain.model.PaymentMethod;
import com.iluwatar.hexagonal.architecture.domain.model.PaymentStatus;
import com.iluwatar.hexagonal.architecture.domain.model.Refund;
import com.iluwatar.hexagonal.architecture.domain.model.RefundStatus;
import com.iluwatar.hexagonal.architecture.domain.repository.PaymentRepository;
import com.iluwatar.hexagonal.architecture.domain.repository.RefundRepository;
import com.iluwatar.hexagonal.architecture.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final RefundRepository refundRepository;
  private final Random random = new Random();

  @Override
  @Transactional
  public Payment createPayment(String orderId, BigDecimal amount, PaymentMethod paymentMethod) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Payment amount must be greater than 0");
    }

    Payment payment = Payment.builder()
        .orderId(orderId)
        .amount(amount)
        .paymentMethod(paymentMethod)
        .paymentStatus(PaymentStatus.PENDING)
        .createdAt(LocalDateTime.now())
        .build();

    Payment savedPayment = paymentRepository.save(payment);
    log.info("Created payment with id: {}, orderId: {}, amount: {}, status: {}",
        savedPayment.getId(), savedPayment.getOrderId(), savedPayment.getAmount(), savedPayment.getPaymentStatus());
    return savedPayment;
  }

  @Override
  @Transactional
  public Payment processPayment(Long paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + paymentId));

    if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
      throw new IllegalArgumentException("Payment is already processed");
    }

    // Update status to PROCESSING
    payment.setPaymentStatus(PaymentStatus.PROCESSING);
    paymentRepository.save(payment);
    log.info("Payment processing started: paymentId={}", paymentId);

    // Simulate network delay (100-500ms)
    try {
      Thread.sleep(100 + random.nextInt(400));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      payment.setPaymentStatus(PaymentStatus.FAILED);
      payment.setFailureReason("Processing interrupted");
      paymentRepository.save(payment);
      log.error("Payment processing interrupted: paymentId={}", paymentId, e);
      return payment;
    }

    // 90% success rate
    if (random.nextInt(10) < 9) {
      // Payment success
      payment.setPaymentStatus(PaymentStatus.SUCCESS);
      payment.setTransactionId(UUID.randomUUID().toString());
      payment.setProcessedAt(LocalDateTime.now());
      paymentRepository.save(payment);
      log.info("Payment succeeded: paymentId={}, transactionId={}", paymentId, payment.getTransactionId());
    } else {
      // Payment failed
      payment.setPaymentStatus(PaymentStatus.FAILED);
      payment.setFailureReason("Payment processing failed");
      payment.setProcessedAt(LocalDateTime.now());
      paymentRepository.save(payment);
      log.info("Payment failed: paymentId={}, reason={}", paymentId, payment.getFailureReason());
    }

    return payment;
  }

  @Override
  @Transactional
  public Refund requestRefund(Long paymentId, BigDecimal refundAmount, String refundReason) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + paymentId));

    if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
      throw new IllegalArgumentException("Only successful payments can be refunded");
    }

    if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Refund amount must be greater than 0");
    }

    if (refundAmount.compareTo(payment.getAmount()) > 0) {
      throw new IllegalArgumentException("Refund amount cannot exceed payment amount");
    }

    // Check if total refunds exceed payment amount
    List<Refund> existingRefunds = refundRepository.findByPaymentId(paymentId);
    BigDecimal totalRefunded = existingRefunds.stream()
        .map(Refund::getRefundAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalRefunded.add(refundAmount).compareTo(payment.getAmount()) > 0) {
      throw new IllegalArgumentException("Total refund amount cannot exceed payment amount");
    }

    Refund refund = Refund.builder()
        .payment(payment)
        .refundAmount(refundAmount)
        .refundReason(refundReason)
        .refundStatus(RefundStatus.PENDING)
        .createdAt(LocalDateTime.now())
        .build();

    Refund savedRefund = refundRepository.save(refund);
    log.info("Refund requested: refundId={}, paymentId={}, amount={}",
        savedRefund.getId(), savedRefund.getPayment().getId(), savedRefund.getRefundAmount());
    return savedRefund;
  }

  @Override
  @Transactional
  public Refund processRefund(Long refundId) {
    Refund refund = refundRepository.findById(refundId)
        .orElseThrow(() -> new IllegalArgumentException("Refund not found with id: " + refundId));

    if (refund.getRefundStatus() != RefundStatus.PENDING) {
      throw new IllegalArgumentException("Refund is already processed");
    }

    // Update refund status to PROCESSED
    refund.setRefundStatus(RefundStatus.PROCESSED);
    refund.setRefundTransactionId(UUID.randomUUID().toString());
    Refund savedRefund = refundRepository.save(refund);

    // Check if full refund
    Payment payment = refund.getPayment();
    List<Refund> allRefunds = refundRepository.findByPaymentId(payment.getId());
    BigDecimal totalRefunded = allRefunds.stream()
        .map(Refund::getRefundAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalRefunded.compareTo(payment.getAmount()) == 0) {
      payment.setPaymentStatus(PaymentStatus.REFUNDED);
      paymentRepository.save(payment);
      log.info("Payment fully refunded: paymentId={}", payment.getId());
    }

    log.info("Refund processed: refundId={}, transactionId={}", savedRefund.getId(), savedRefund.getRefundTransactionId());
    return savedRefund;
  }

  @Override
  public Payment getPaymentByOrderId(String orderId) {
    return paymentRepository.findByOrderId(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Payment not found with orderId: " + orderId));
  }

  @Override
  public Payment getPaymentByTransactionId(String transactionId) {
    return paymentRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new IllegalArgumentException("Payment not found with transactionId: " + transactionId));
  }

  @Override
  public Page<Payment> getPaymentsByStatus(PaymentStatus paymentStatus, Pageable pageable) {
    return paymentRepository.findByPaymentStatus(paymentStatus, pageable);
  }

  @Override
  public Page<Refund> getRefundsByPaymentId(Long paymentId, Pageable pageable) {
    return refundRepository.findByPaymentId(paymentId, pageable);
  }

  @Override
  public Page<Refund> getRefundsByStatus(RefundStatus refundStatus, Pageable pageable) {
    return refundRepository.findByRefundStatus(refundStatus, pageable);
  }

  @Override
  public Map<PaymentMethod, Map<String, Object>> getPaymentStatisticsByMethod() {
    List<Object[]> results = paymentRepository.findTotalAmountAndTransactionCountByPaymentMethod();
    Map<PaymentMethod, Map<String, Object>> statistics = new HashMap<>();

    for (Object[] result : results) {
      PaymentMethod paymentMethod = (PaymentMethod) result[0];
      BigDecimal totalAmount = (BigDecimal) result[1];
      Long transactionCount = (Long) result[2];

      Map<String, Object> methodStats = new HashMap<>();
      methodStats.put("totalAmount", totalAmount);
      methodStats.put("transactionCount", transactionCount);
      statistics.put(paymentMethod, methodStats);
    }

    return statistics;
  }

  @Override
  public Map<String, BigDecimal> getPaymentAndRefundStatisticsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
    Object[] result = paymentRepository.findTotalPaymentAndRefundAmountByDateRange(startDate, endDate);
    BigDecimal totalPaymentAmount = (BigDecimal) result[0];
    BigDecimal totalRefundAmount = (BigDecimal) result[1];

    Map<String, BigDecimal> statistics = new HashMap<>();
    statistics.put("totalPaymentAmount", totalPaymentAmount != null ? totalPaymentAmount : BigDecimal.ZERO);
    statistics.put("totalRefundAmount", totalRefundAmount != null ? totalRefundAmount : BigDecimal.ZERO);
    return statistics;
  }

  @Override
  public double getPaymentSuccessRate() {
    Long successCount = paymentRepository.countSuccessPayments();
    Long totalCount = paymentRepository.countTotalPayments();

    if (totalCount == 0) {
      return 0.0;
    }

    return (double) successCount / totalCount;
  }
}
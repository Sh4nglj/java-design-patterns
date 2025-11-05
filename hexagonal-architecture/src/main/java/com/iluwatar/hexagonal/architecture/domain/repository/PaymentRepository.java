package com.iluwatar.hexagonal.architecture.domain.repository;

import com.iluwatar.hexagonal.architecture.domain.model.Payment;
import com.iluwatar.hexagonal.architecture.domain.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByOrderId(String orderId);

  Optional<Payment> findByTransactionId(String transactionId);

  Page<Payment> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

  @Query("SELECT p.paymentMethod, SUM(p.amount) AS totalAmount, COUNT(p) AS transactionCount " +
      "FROM Payment p " +
      "GROUP BY p.paymentMethod")
  List<Object[]> findTotalAmountAndTransactionCountByPaymentMethod();

  @Query("SELECT SUM(p.amount) AS totalPaymentAmount, SUM(r.refundAmount) AS totalRefundAmount " +
      "FROM Payment p " +
      "LEFT JOIN Refund r ON p.id = r.payment.id " +
      "WHERE p.createdAt BETWEEN :startDate AND :endDate")
  Object[] findTotalPaymentAndRefundAmountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentStatus = 'SUCCESS'")
  Long countSuccessPayments();

  @Query("SELECT COUNT(p) FROM Payment p")
  Long countTotalPayments();
}
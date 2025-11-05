package com.iluwatar.hexagonal.architecture.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "payment_id", nullable = false)
  private Payment payment;

  @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal refundAmount;

  @Column(name = "refund_reason")
  private String refundReason;

  @Enumerated(EnumType.STRING)
  @Column(name = "refund_status", nullable = false)
  private RefundStatus refundStatus;

  @Column(name = "refund_transaction_id", unique = true)
  private String refundTransactionId;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
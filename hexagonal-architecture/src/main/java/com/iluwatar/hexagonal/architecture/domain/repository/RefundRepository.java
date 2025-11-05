package com.iluwatar.hexagonal.architecture.domain.repository;

import com.iluwatar.hexagonal.architecture.domain.model.Refund;
import com.iluwatar.hexagonal.architecture.domain.model.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

  Page<Refund> findByPaymentId(Long paymentId, Pageable pageable);

  Page<Refund> findByRefundStatus(RefundStatus refundStatus, Pageable pageable);

  List<Refund> findByPaymentId(Long paymentId);
}
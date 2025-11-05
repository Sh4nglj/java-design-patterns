package com.iluwatar.hexagonal.architecture.service;

import com.iluwatar.hexagonal.architecture.domain.model.Payment;
import com.iluwatar.hexagonal.architecture.domain.model.PaymentMethod;
import com.iluwatar.hexagonal.architecture.domain.model.PaymentStatus;
import com.iluwatar.hexagonal.architecture.domain.model.Refund;
import com.iluwatar.hexagonal.architecture.domain.model.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public interface PaymentService {

  /**
   * 创建支付记录
   * @param orderId 订单ID
   * @param amount 支付金额
   * @param paymentMethod 支付方式
   * @return 创建的支付记录
   */
  Payment createPayment(String orderId, BigDecimal amount, PaymentMethod paymentMethod);

  /**
   * 处理支付
   * @param paymentId 支付ID
   * @return 处理后的支付记录
   */
  Payment processPayment(Long paymentId);

  /**
   * 申请退款
   * @param paymentId 支付ID
   * @param refundAmount 退款金额
   * @param refundReason 退款原因
   * @return 创建的退款记录
   */
  Refund requestRefund(Long paymentId, BigDecimal refundAmount, String refundReason);

  /**
   * 处理退款
   * @param refundId 退款ID
   * @return 处理后的退款记录
   */
  Refund processRefund(Long refundId);

  /**
   * 根据订单ID查询支付记录
   * @param orderId 订单ID
   * @return 支付记录
   */
  Payment getPaymentByOrderId(String orderId);

  /**
   * 根据交易ID查询支付记录
   * @param transactionId 交易ID
   * @return 支付记录
   */
  Payment getPaymentByTransactionId(String transactionId);

  /**
   * 根据支付状态查询支付记录
   * @param paymentStatus 支付状态
   * @param pageable 分页参数
   * @return 支付记录分页结果
   */
  Page<Payment> getPaymentsByStatus(PaymentStatus paymentStatus, Pageable pageable);

  /**
   * 根据支付ID查询退款记录
   * @param paymentId 支付ID
   * @param pageable 分页参数
   * @return 退款记录分页结果
   */
  Page<Refund> getRefundsByPaymentId(Long paymentId, Pageable pageable);

  /**
   * 根据退款状态查询退款记录
   * @param refundStatus 退款状态
   * @param pageable 分页参数
   * @return 退款记录分页结果
   */
  Page<Refund> getRefundsByStatus(RefundStatus refundStatus, Pageable pageable);

  /**
   * 按支付方式统计总金额和交易笔数
   * @return 统计结果
   */
  Map<PaymentMethod, Map<String, Object>> getPaymentStatisticsByMethod();

  /**
   * 按日期范围统计支付金额和退款金额
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 统计结果
   */
  Map<String, BigDecimal> getPaymentAndRefundStatisticsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * 计算支付成功率
   * @return 支付成功率
   */
  double getPaymentSuccessRate();
}
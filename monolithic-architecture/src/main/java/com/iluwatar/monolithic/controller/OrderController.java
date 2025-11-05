/*
 * This project is licensed under the MIT license. Module model-view-viewmodel is using ZK framework licensed under LGPL (see lgpl-3.0.txt).
 *
 * The MIT License
 * Copyright © 2014-2022 Ilkka Seppälä
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.iluwatar.monolithic.controller;

import com.iluwatar.monolithic.exceptions.InsufficientStockException;
import com.iluwatar.monolithic.exceptions.IllegalStateTransitionException;
import com.iluwatar.monolithic.exceptions.NonExistentProductException;
import com.iluwatar.monolithic.exceptions.NonExistentUserException;
import com.iluwatar.monolithic.model.Order;
import com.iluwatar.monolithic.model.OrderStatus;
import com.iluwatar.monolithic.model.OrderStatusHistory;
import com.iluwatar.monolithic.model.Product;
import com.iluwatar.monolithic.model.User;
import com.iluwatar.monolithic.repository.OrderRepository;
import com.iluwatar.monolithic.repository.OrderStatusHistoryRepository;
import com.iluwatar.monolithic.repository.ProductRepository;
import com.iluwatar.monolithic.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** OrderController is a controller class for managing Order operations. */
@Service
public class OrderController {
  private final OrderRepository orderRepository;
  private final UserRepository userRepository;
  private final ProductRepository productRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;

  /** This function handles the initializing of the controller. */
  public OrderController(
      OrderRepository orderRepository,
      UserRepository userRepository,
      ProductRepository productRepository,
      OrderStatusHistoryRepository orderStatusHistoryRepository) {
    this.orderRepository = orderRepository;
    this.userRepository = userRepository;
    this.productRepository = productRepository;
    this.orderStatusHistoryRepository = orderStatusHistoryRepository;
  }

  /** This function handles placing orders with all of its cases. */
  public Order placeOrder(Long userId, Long productId, Integer quantity) {
    final User user = userRepository.findById(userId)
        .orElseThrow(() -> new NonExistentUserException("User with ID " + userId + " not found"));

    final Product product = productRepository.findById(productId)
        .orElseThrow(() -> new NonExistentProductException("Product with ID " + productId + " not found"));

    if (product.getStockQuantity() < quantity) {
      throw new InsufficientStockException("Not enough stock for product " + productId);
    }

    product.setStockQuantity(product.getStockQuantity() - quantity);
    productRepository.save(product);

    final Order order = new Order(null, user, product, quantity, product.getPrice() * quantity, OrderStatus.PENDING);
    Order savedOrder = orderRepository.save(order);

    // Record initial status
    recordStatusChange(savedOrder, OrderStatus.PENDING, OrderStatus.PENDING, "System", "Order created");
    return savedOrder;
  }

  /**
   * Update order status with validation and stock management.
   * @param orderId the order id
   * @param targetStatus the target status
   * @param changedBy who changed the status
   * @param changeReason reason for the change
   * @return the updated order
   */
  @Transactional
  public Order updateOrderStatus(Long orderId, OrderStatus targetStatus, String changedBy, String changeReason) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Order with ID " + orderId + " not found"));

    OrderStatus currentStatus = order.getOrderStatus();

    // Validate status transition
    validateStatusTransition(currentStatus, targetStatus);

    // Handle stock management
    if (currentStatus == OrderStatus.PENDING && targetStatus == OrderStatus.PAID) {
      // Already checked in placeOrder, but just in case
      int updatedRows = productRepository.updateStock(order.getProduct().getId(), -order.getQuantity());
      if (updatedRows == 0) {
        throw new InsufficientStockException("Not enough stock for product " + order.getProduct().getId());
      }
    } else if ((targetStatus == OrderStatus.CANCELLED && currentStatus != OrderStatus.CANCELLED) ||
        (targetStatus == OrderStatus.REFUNDED && currentStatus != OrderStatus.REFUNDED)) {
      // Restore stock
      productRepository.updateStock(order.getProduct().getId(), order.getQuantity());
    }

    // Update status
    order.setOrderStatus(targetStatus);
    Order updatedOrder = orderRepository.save(order);

    // Record status change
    recordStatusChange(updatedOrder, currentStatus, targetStatus, changedBy, changeReason);

    return updatedOrder;
  }

  /**
   * Get orders by status with pagination.
   * @param status the order status
   * @param pageable the pageable information
   * @return a page of orders with the given status
   */
  public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
    return orderRepository.findByOrderStatus(status, pageable);
  }

  /**
   * Get order status history by order id.
   * @param orderId the order id
   * @return list of status changes in descending order of time
   */
  public List<OrderStatusHistory> getOrderStatusHistory(Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Order with ID " + orderId + " not found"));
    return orderStatusHistoryRepository.findByOrder(order, Sort.by(Sort.Direction.DESC, "changedAt"));
  }

  /**
   * Validate status transition according to business rules.
   * @param currentStatus current status
   * @param targetStatus target status
   */
  private void validateStatusTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
    if (currentStatus == targetStatus) {
      return; // No change needed
    }

    switch (currentStatus) {
      case PENDING:
        if (targetStatus != OrderStatus.PAID && targetStatus != OrderStatus.CANCELLED) {
          throw new IllegalStateTransitionException("PENDING can only transition to PAID or CANCELLED");
        }
        break;
      case PAID:
        if (targetStatus != OrderStatus.SHIPPED && targetStatus != OrderStatus.REFUNDED) {
          throw new IllegalStateTransitionException("PAID can only transition to SHIPPED or REFUNDED");
        }
        break;
      case SHIPPED:
        if (targetStatus != OrderStatus.DELIVERED) {
          throw new IllegalStateTransitionException("SHIPPED can only transition to DELIVERED");
        }
        break;
      case CANCELLED:
      case REFUNDED:
        throw new IllegalStateTransitionException("CANCELLED and REFUNDED are final states");
      case DELIVERED:
        // DELIVERED is also a final state in this business rule
        throw new IllegalStateTransitionException("DELIVERED is a final state");
      default:
        throw new IllegalStateTransitionException("Unknown status transition");
    }
  }

  /**
   * Record status change history.
   * @param order the order
   * @param fromStatus from status
   * @param toStatus to status
   * @param changedBy who changed the status
   * @param changeReason reason for the change
   */
  private void recordStatusChange(Order order, OrderStatus fromStatus, OrderStatus toStatus, String changedBy, String changeReason) {
    OrderStatusHistory history = new OrderStatusHistory();
    history.setOrder(order);
    history.setFromStatus(fromStatus);
    history.setToStatus(toStatus);
    history.setChangedBy(changedBy);
    history.setChangeReason(changeReason);
    orderStatusHistoryRepository.save(history);
  }
}

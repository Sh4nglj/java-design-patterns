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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderControllerTest {

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private OrderStatusHistoryRepository orderStatusHistoryRepository;

  @InjectMocks
  private OrderController orderController;

  private User testUser;
  private Product testProduct;
  private Order testOrder;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Initialize test data
    testUser = new User(1L, "John Doe", "john@example.com", "password123");
    testProduct = new Product(1L, "Test Product", "Description", 10.0, 100);
    testOrder = new Order(1L, testUser, testProduct, 5, 50.0, OrderStatus.PENDING);
  }

  @Test
  void testPlaceOrderSuccess() {
    // Mock repository responses
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    // Test placeOrder method
    Order result = orderController.placeOrder(1L, 1L, 5);

    // Verify results
    assertNotNull(result);
    assertEquals(1L, result.getId());
    assertEquals(OrderStatus.PENDING, result.getOrderStatus());
    verify(productRepository, times(1)).save(testProduct);
    verify(orderRepository, times(1)).save(any(Order.class));
    verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
    assertEquals(95, testProduct.getStockQuantity()); // Stock should be reduced by 5
  }

  @Test
  void testPlaceOrderUserNotFound() {
    // Mock repository response
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    // Test exception
    Exception exception = assertThrows(NonExistentUserException.class, () -> {
      orderController.placeOrder(1L, 1L, 5);
    });

    assertEquals("User with ID 1 not found", exception.getMessage());
  }

  @Test
  void testPlaceOrderProductNotFound() {
    // Mock repository responses
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(productRepository.findById(1L)).thenReturn(Optional.empty());

    // Test exception
    Exception exception = assertThrows(NonExistentProductException.class, () -> {
      orderController.placeOrder(1L, 1L, 5);
    });

    assertEquals("Product with ID 1 not found", exception.getMessage());
  }

  @Test
  void testPlaceOrderInsufficientStock() {
    // Mock repository responses
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    Product lowStockProduct = new Product(1L, "Test Product", "Description", 10.0, 3);
    when(productRepository.findById(1L)).thenReturn(Optional.of(lowStockProduct));

    // Test exception
    Exception exception = assertThrows(InsufficientStockException.class, () -> {
      orderController.placeOrder(1L, 1L, 5);
    });

    assertEquals("Not enough stock for product 1", exception.getMessage());
  }

  @Test
  void testUpdateOrderStatusPendingToPaidSuccess() {
    // Mock repository responses
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
    when(productRepository.updateStock(1L, -5)).thenReturn(1);

    // Test status update
    Order result = orderController.updateOrderStatus(1L, OrderStatus.PAID, "System", "Payment received");

    // Verify results
    assertNotNull(result);
    assertEquals(OrderStatus.PAID, result.getOrderStatus());
    verify(productRepository, times(1)).updateStock(1L, -5);
    verify(orderRepository, times(1)).save(testOrder);
    verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
  }

  @Test
  void testUpdateOrderStatusPendingToCancelledSuccess() {
    // Mock repository responses
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    // Test status update
    Order result = orderController.updateOrderStatus(1L, OrderStatus.CANCELLED, "User", "Order cancelled");

    // Verify results
    assertNotNull(result);
    assertEquals(OrderStatus.CANCELLED, result.getOrderStatus());
    verify(productRepository, times(1)).updateStock(1L, 5); // Stock should be restored
    verify(orderRepository, times(1)).save(testOrder);
    verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
  }

  @Test
  void testUpdateOrderStatusPaidToShippedSuccess() {
    // Set order status to PAID
    testOrder.setOrderStatus(OrderStatus.PAID);

    // Mock repository responses
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    // Test status update
    Order result = orderController.updateOrderStatus(1L, OrderStatus.SHIPPED, "System", "Order shipped");

    // Verify results
    assertNotNull(result);
    assertEquals(OrderStatus.SHIPPED, result.getOrderStatus());
    verify(orderRepository, times(1)).save(testOrder);
    verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
  }

  @Test
  void testUpdateOrderStatusShippedToDeliveredSuccess() {
    // Set order status to SHIPPED
    testOrder.setOrderStatus(OrderStatus.SHIPPED);

    // Mock repository responses
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    // Test status update
    Order result = orderController.updateOrderStatus(1L, OrderStatus.DELIVERED, "System", "Order delivered");

    // Verify results
    assertNotNull(result);
    assertEquals(OrderStatus.DELIVERED, result.getOrderStatus());
    verify(orderRepository, times(1)).save(testOrder);
    verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
  }

  @Test
  void testUpdateOrderStatusPaidToRefundedSuccess() {
    // Set order status to PAID
    testOrder.setOrderStatus(OrderStatus.PAID);

    // Mock repository responses
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    // Test status update
    Order result = orderController.updateOrderStatus(1L, OrderStatus.REFUNDED, "System", "Order refunded");

    // Verify results
    assertNotNull(result);
    assertEquals(OrderStatus.REFUNDED, result.getOrderStatus());
    verify(productRepository, times(1)).updateStock(1L, 5); // Stock should be restored
    verify(orderRepository, times(1)).save(testOrder);
    verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
  }

  @Test
  void testUpdateOrderStatusInvalidTransition() {
    // Set order status to PENDING
    testOrder.setOrderStatus(OrderStatus.PENDING);

    // Mock repository response
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

    // Test invalid transition (PENDING -> SHIPPED)
    Exception exception = assertThrows(IllegalStateTransitionException.class, () -> {
      orderController.updateOrderStatus(1L, OrderStatus.SHIPPED, "System", "Invalid transition");
    });

    assertEquals("PENDING can only transition to PAID or CANCELLED", exception.getMessage());
  }

  @Test
  void testUpdateOrderStatusFinalState() {
    // Set order status to CANCELLED
    testOrder.setOrderStatus(OrderStatus.CANCELLED);

    // Mock repository response
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

    // Test transition from final state
    Exception exception = assertThrows(IllegalStateTransitionException.class, () -> {
      orderController.updateOrderStatus(1L, OrderStatus.PAID, "System", "Invalid transition");
    });

    assertEquals("CANCELLED and REFUNDED are final states", exception.getMessage());
  }

  @Test
  void testGetOrdersByStatus() {
    // Create test orders
    List<Order> pendingOrders = new ArrayList<>();
    pendingOrders.add(testOrder);
    Page<Order> page = new PageImpl<>(pendingOrders, PageRequest.of(0, 10), 1);

    // Mock repository response
    when(orderRepository.findByOrderStatus(OrderStatus.PENDING, PageRequest.of(0, 10))).thenReturn(page);

    // Test getOrdersByStatus method
    Page<Order> result = orderController.getOrdersByStatus(OrderStatus.PENDING, PageRequest.of(0, 10));

    // Verify results
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(OrderStatus.PENDING, result.getContent().get(0).getOrderStatus());
  }

  @Test
  void testGetOrderStatusHistory() {
    // Create test history
    List<OrderStatusHistory> historyList = new ArrayList<>();
    OrderStatusHistory history = new OrderStatusHistory(1L, testOrder, OrderStatus.PENDING, OrderStatus.PAID, "System", "Payment received", null);
    historyList.add(history);

    // Mock repository response
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderStatusHistoryRepository.findByOrder(testOrder, Sort.by(Sort.Direction.DESC, "changedAt"))).thenReturn(historyList);

    // Test getOrderStatusHistory method
    List<OrderStatusHistory> result = orderController.getOrderStatusHistory(1L);

    // Verify results
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(OrderStatus.PENDING, result.get(0).getFromStatus());
    assertEquals(OrderStatus.PAID, result.get(0).getToStatus());
  }

  @Test
  void testUpdateOrderStatusInsufficientStock() {
    // Mock repository responses
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(productRepository.updateStock(1L, -5)).thenReturn(0); // Update fails (insufficient stock)

    // Test exception
    Exception exception = assertThrows(InsufficientStockException.class, () -> {
      orderController.updateOrderStatus(1L, OrderStatus.PAID, "System", "Payment received");
    });

    assertEquals("Not enough stock for product 1", exception.getMessage());
  }
}
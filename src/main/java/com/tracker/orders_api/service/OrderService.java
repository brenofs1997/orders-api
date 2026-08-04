package com.tracker.orders_api.service;

import com.tracker.orders_api.controller.dto.CreateOrderRequest;
import com.tracker.orders_api.entities.Order;
import com.tracker.orders_api.entities.OrderItem;
import com.tracker.orders_api.entities.OrderStatus;
import com.tracker.orders_api.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createOrder(CreateOrderRequest request) {

        List<OrderItem> items = request.items().stream()
                .map(itemReq -> OrderItem.builder()
                        .name(itemReq.name())
                        .quantity(itemReq.quantity())
                        .unitPrice(itemReq.unitPrice())
                        .build())
                .toList();

        Order order = Order.builder()
                .customerName(request.customerName())
                .deliveryAddress(request.deliveryAddress())
                .items(items)
                .status(OrderStatus.RECEIVED)
                .build();

        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
    }

    public Order updateOrderStatus(UUID id, OrderStatus newStatus) {
        Order order = getOrderById(id);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }


}

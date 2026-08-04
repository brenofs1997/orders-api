package com.tracker.orders_api.controller.dto;

import java.util.List;

public record CreateOrderRequest(
        String customerName,
        String deliveryAddress,
        List<OrderItemRequest> items
) {}
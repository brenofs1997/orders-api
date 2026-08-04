package com.tracker.orders_api.controller.dto;

import com.tracker.orders_api.entities.OrderStatus;

public record UpdateOrderStatusRequest(OrderStatus status) {}
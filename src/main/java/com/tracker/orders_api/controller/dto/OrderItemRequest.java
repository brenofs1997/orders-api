package com.tracker.orders_api.controller.dto;

import java.math.BigDecimal;

public record OrderItemRequest(String name, Integer quantity, BigDecimal unitPrice) {}
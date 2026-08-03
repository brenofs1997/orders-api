package com.tracker.orders_api.repository;

import com.tracker.orders_api.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
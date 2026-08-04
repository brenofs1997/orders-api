package com.tracker.orders_api.controller.dto;


public record LoginResponse(String accessToken, String refreshToken, Long expiresIn) {

}

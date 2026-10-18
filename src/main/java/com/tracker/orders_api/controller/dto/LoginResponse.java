package com.tracker.orders_api.controller.dto;


import com.tracker.orders_api.entities.User;

public record LoginResponse(String accessToken, String refreshToken, Long expiresIn, UserDto userDto) {

}

package com.petway.favorite.dto;
public record FavoriteRequest(String targetType, String targetId, String title, String address, String thumbnailUrl) {}

/* 즐겨찾기 대상의 종류와 ID, 목록에 표시할 제목·주소·이미지를 전달한다. */
package com.petway.favorite.dto;
public record FavoriteRequest(String targetType, String targetId, String title, String address, String thumbnailUrl) {}

/* 동물병원 제공자 조회 실패를 병원 API의 오류 응답으로 전달한다. */
package com.petway.hospital.client;

public class AnimalHospitalApiException extends RuntimeException {

    public AnimalHospitalApiException(String message) {
        super(message);
    }
}

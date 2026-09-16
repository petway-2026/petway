/* Spring Boot를 시작하고 com.petway 하위의 API, 서비스, 설정 빈을 등록하는 실행 진입점이다. */
package com.petway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PetwayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetwayApplication.class, args);
    }
}

/* AnimalRegistrationApiProperties의 동작을 검증하는 테스트다. 검증 대상: 동물등록 API 주소, 인증키와 연결·응답 제한 시간을 설정값으로 관리한다. */
package com.petway.external.animalregistration.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnimalRegistrationApiPropertiesTest {

    @Test
    void bindsDedicatedApiSettings() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "animal-registration-api.base-url", "https://animal.test/api",
                "animal-registration-api.service-key", "test-key",
                "animal-registration-api.connect-timeout", "2s",
                "animal-registration-api.read-timeout", "4s"
        ));

        AnimalRegistrationApiProperties properties = new Binder(source)
                .bind(
                        "animal-registration-api",
                        Bindable.of(AnimalRegistrationApiProperties.class)
                )
                .orElseThrow(() ->
                        new IllegalStateException("동물등록정보 API 설정을 바인딩할 수 없습니다."));

        assertThat(properties.getBaseUrl()).isEqualTo("https://animal.test/api");
        assertThat(properties.getServiceKey()).isEqualTo("test-key");
        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(4));
    }
}

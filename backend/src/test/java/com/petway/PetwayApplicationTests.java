package com.petway;

import com.petway.user.repository.UserAuthRepository;
import com.petway.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class PetwayApplicationTests {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserAuthRepository userAuthRepository;

    @Test
    void contextLoads() {
    }
}

package com.petway;

import com.petway.pet.repository.PetRepository;
import com.petway.user.repository.UserAuthRepository;
import com.petway.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class PetwayApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserAuthRepository userAuthRepository;

    @MockitoBean
    private PetRepository petRepository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("Swagger UI는 인증 없이 접근할 수 있다")
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("OpenAPI 문서는 PetWay 기본 정보를 반환한다")
    void apiDocsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title")
                        .value("PetWay Backend API"))
                .andExpect(jsonPath("$.info.version").value("v1"));
    }

    @Test
    @DisplayName("기존 보호 API는 인증 없이 접근할 수 없다")
    void protectedApiStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isForbidden());
    }
}

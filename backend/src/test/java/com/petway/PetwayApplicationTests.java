package com.petway;

import com.petway.pet.repository.PetRepository;
import com.petway.user.repository.UserAuthRepository;
import com.petway.user.repository.UserRepository;
import com.petway.user.entity.User;
import com.petway.user.entity.UserAuth;
import com.petway.user.enums.AuthType;
import com.petway.user.dto.LoginRequest;
import com.petway.user.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration"
})
class PetwayApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserAuthRepository userAuthRepository;

    @MockitoBean
    private PetRepository petRepository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void contextLoadsWithoutOAuthClientRegistration() {
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

    @Test
    void localActiveLoginSucceedsAndSessionAuthenticationRemains() throws Exception {
        User user = User.create("active@petway.test", "active");
        UserAuth localAuth = UserAuth.createLocal(
                user,
                passwordEncoder.encode("password123")
        );
        when(userRepository.findByEmail("active@petway.test"))
                .thenReturn(Optional.of(user));
        when(userAuthRepository.findByUserAndAuthType(user, AuthType.LOCAL))
                .thenReturn(Optional.of(localAuth));

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"active@petway.test","password":"password123"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("active@petway.test"));
    }

    @Test
    void localInactiveLoginFails() {
        User user = User.create("inactive-local@petway.test", "inactive");
        user.withdraw();
        when(userRepository.findByEmail("inactive-local@petway.test"))
                .thenReturn(Optional.of(user));

        assertThrows(
                AuthenticationException.class,
                () -> authService.login(
                        new LoginRequest(
                                "inactive-local@petway.test",
                                "password123"
                        )
                )
        );
        verify(userAuthRepository, never())
                .findByUserAndAuthType(user, AuthType.LOCAL);
    }
}

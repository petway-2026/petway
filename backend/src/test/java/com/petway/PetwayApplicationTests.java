/* PetwayApplication의 동작을 검증하는 테스트다. 검증 대상: Spring Boot를 시작하고 com.petway 하위의 API, 서비스, 설정 빈을 등록하는 실행 진입점이다. */
package com.petway;

import com.petway.external.animalregistration.exception.AnimalRegistrationErrorType;
import com.petway.external.animalregistration.exception.AnimalRegistrationException;
import com.petway.pet.repository.PetRepository;
import com.petway.pet.entity.Pet;
import com.petway.pet.enums.DogBreed;
import com.petway.pet.enums.PetSizeType;
import com.petway.pet.model.AnimalRegistrationLookupCondition;
import com.petway.pet.model.RegisteredPetProfile;
import com.petway.pet.port.AnimalRegistrationGateway;
import com.petway.place.model.PlacePetDetail;
import com.petway.place.port.TourPlaceGateway;
import com.petway.trip.repository.SavedTripRepository;
import com.petway.favorite.repository.FavoriteRepository;
import com.petway.review.repository.ReviewRepository;
import com.petway.user.repository.UserAuthRepository;
import com.petway.user.repository.UserRepository;
import com.petway.user.entity.User;
import com.petway.user.entity.UserAuth;
import com.petway.user.enums.AuthType;
import com.petway.user.dto.LoginRequest;
import com.petway.user.service.AuthService;
import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.WeatherRegion;
import com.petway.weather.dto.WeatherDisplay;
import com.petway.weather.dto.WeatherForecastResponse;
import com.petway.weather.service.WeatherQueryService;
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
import org.springframework.test.util.ReflectionTestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;

@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
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
    private TourPlaceGateway tourPlaceGateway;

    @MockitoBean
    private AnimalRegistrationGateway animalRegistrationGateway;

    @MockitoBean
    private WeatherQueryService weatherQueryService;

    @MockitoBean
    private SavedTripRepository savedTripRepository;

    @MockitoBean
    private FavoriteRepository favoriteRepository;

    @MockitoBean
    private ReviewRepository reviewRepository;

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
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.parameters[0].name")
                        .value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.parameters[0].in")
                        .value("header"))
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.parameters[0].required")
                        .value(true));
    }

    @Test
    @DisplayName("기존 보호 API는 인증 없이 접근할 수 없다")
    void protectedApiStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousMeReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedNonAdminStillReceivesForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/test").with(user("member").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsAllowsConfiguredOriginWithCredentials() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/auth/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void corsRejectsUnconfiguredOrigin() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/auth/me")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("동물등록정보 조회는 로그인 사용자만 사용할 수 있다")
    void animalRegistrationLookupRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/pets/registration-lookup").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationLookupRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인 사용자는 동물등록정보를 조회할 수 있다")
    void authenticatedUserCanLookUpAnimalRegistration() throws Exception {
        AnimalRegistrationLookupCondition condition =
                new AnimalRegistrationLookupCondition(
                        "410123456789012",
                        null,
                        "홍길동",
                        null
                );
        when(animalRegistrationGateway.lookup(condition)).thenReturn(
                new RegisteredPetProfile(
                        "보리",
                        LocalDate.of(2021, 5, 10),
                        DogBreed.MALTESE,
                        "말티즈",
                        true
                )
        );

        mockMvc.perform(post("/api/pets/registration-lookup").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .with(user("owner@petway.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationLookupRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petName").value("보리"))
                .andExpect(jsonPath("$.birthDate").value("2021-05-10"))
                .andExpect(jsonPath("$.breed").value("MALTESE"))
                .andExpect(jsonPath("$.breedDetail").value("말티즈"))
                .andExpect(jsonPath("$.neutered").value(true))
                .andExpect(jsonPath("$.animalRegistered").value(true))
                .andExpect(jsonPath("$.animalRegistrationNumber").doesNotExist())
                .andExpect(jsonPath("$.ownerName").doesNotExist());

        verify(animalRegistrationGateway).lookup(condition);
        verifyNoInteractions(petRepository);
    }

    @Test
    @DisplayName("동물등록정보 조회 조건이 잘못되면 외부 API를 호출하지 않는다")
    void invalidAnimalRegistrationLookupIsRejectedBeforeProviderCall() throws Exception {
        mockMvc.perform(post("/api/pets/registration-lookup").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .with(user("owner@petway.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "animalRegistrationNumber": "410123456789012",
                                  "rfidCode": "900123456789012",
                                  "ownerName": "홍길동"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("동물등록번호와 RFID 중 하나만 입력해주세요."));

        verifyNoInteractions(animalRegistrationGateway);
    }

    @Test
    @DisplayName("동물등록정보가 없으면 404를 반환한다")
    void missingAnimalRegistrationReturnsNotFound() throws Exception {
        when(animalRegistrationGateway.lookup(any()))
                .thenThrow(new AnimalRegistrationException(
                        AnimalRegistrationErrorType.NOT_FOUND,
                        "provider detail"
                ));

        mockMvc.perform(post("/api/pets/registration-lookup").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .with(user("owner@petway.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationLookupRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("일치하는 동물등록정보를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("동물등록정보 제공기관 오류는 502를 반환한다")
    void animalRegistrationProviderFailureReturnsBadGateway() throws Exception {
        when(animalRegistrationGateway.lookup(any()))
                .thenThrow(new AnimalRegistrationException(
                        AnimalRegistrationErrorType.API_ERROR,
                        "provider detail"
                ));

        mockMvc.perform(post("/api/pets/registration-lookup").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .with(user("owner@petway.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationLookupRequest()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message")
                        .value("동물등록정보 제공기관 호출에 실패했습니다."));
    }

    @Test
    @DisplayName("동물등록정보 제공기관 응답 시간 초과는 504를 반환한다")
    void animalRegistrationProviderTimeoutReturnsGatewayTimeout() throws Exception {
        when(animalRegistrationGateway.lookup(any()))
                .thenThrow(new AnimalRegistrationException(
                        AnimalRegistrationErrorType.TIMEOUT,
                        "provider detail"
                ));

        mockMvc.perform(post("/api/pets/registration-lookup").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .with(user("owner@petway.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationLookupRequest()))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.message")
                        .value("동물등록정보 제공기관의 응답 시간이 초과되었습니다."));
    }

    @Test
    @DisplayName("날씨 API는 인증 없이 접근할 수 있다")
    void weatherApiIsPublic() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 9, 5);
        WeatherForecastResponse response = new WeatherForecastResponse(
                targetDate,
                ForecastType.MID_TERM,
                "서울ㆍ인천ㆍ경기도",
                21.0,
                28.0,
                40,
                null,
                new WeatherDisplay(
                        "21~28℃",
                        "강수확률 40%",
                        "흐림",
                        "흐린 날씨가 예상되며, 강수확률 40%입니다."
                ),
                null,
                null,
                null,
                null,
                null,
                "중기예보의 지역 대표 지점 기온과 권역 날씨입니다.",
                "기상청"
        );
        when(weatherQueryService.getForecast(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        )).thenReturn(response);

        mockMvc.perform(get("/api/weather")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780")
                        .param("targetDate", targetDate.toString())
                        .param("region", "SEOUL_INCHEON_GYEONGGI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecastType").value("MID_TERM"))
                .andExpect(jsonPath("$.source").value("기상청"));

        verify(weatherQueryService).getForecast(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        );
    }

    private String registrationLookupRequest() {
        return """
                {
                  "animalRegistrationNumber": "410123456789012",
                  "ownerName": "홍길동"
                }
                """;
    }

    @Test
    @DisplayName("날씨 API의 GET 이외 요청은 인증 없이 접근할 수 없다")
    void weatherNonGetRequestRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/weather").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("사용자별 장소 동반 가능 판정 API는 인증 없이 접근할 수 없다")
    void placeCompatibilityRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/places/125266/compatibility"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("WalkFit 산책 추천 API는 인증 없이 접근할 수 없다")
    void walkFitRecommendationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/walk/recommendations")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780")
                        .param("targetDate", "2026-09-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("기존 관광공사 반려동물 상세 API는 인증 없이 접근할 수 있다")
    void publicPlacePetDetailPolicyRemainsUnchanged() throws Exception {
        mockMvc.perform(get("/api/places/not-a-number/pet-detail"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증 사용자는 자신의 대표견으로 장소 동반 가능 판정을 받는다")
    void authenticatedUserReceivesCompatibilityForOwnPrimaryPet() throws Exception {
        User userEntity = User.create("owner@petway.test", "보호자");
        Pet primaryPet = Pet.create(
                userEntity,
                "보리",
                "믹스견",
                null,
                null,
                null,
                new BigDecimal("7.8"),
                PetSizeType.SMALL,
                null,
                null,
                null,
                null,
                null,
                null,
                true
        );
        ReflectionTestUtils.setField(primaryPet, "id", 11L);

        when(userRepository.findByEmail("owner@petway.test"))
                .thenReturn(Optional.of(userEntity));
        when(petRepository.findByUserAndIsPrimaryTrue(userEntity))
                .thenReturn(Optional.of(primaryPet));
        when(tourPlaceGateway.getPetDetail("125266"))
                .thenReturn(new PlacePetDetail(
                        "전구역 동반가능",
                        "10kg 이하 동반 가능",
                        "목줄 필수",
                        null,
                        null
                ));

        mockMvc.perform(get("/api/places/125266/compatibility")
                        .with(user("owner@petway.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petId").value(11))
                .andExpect(jsonPath("$.petName").value("보리"))
                .andExpect(jsonPath("$.petwayAssessment.status")
                        .value("CONDITIONAL"))
                .andExpect(jsonPath("$.petwayAssessment.requirements[0].label")
                        .value("목줄"));
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
        String originalSessionId = session.getId();
        mockMvc.perform(post("/api/auth/login").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"active@petway.test","password":"password123"}
                                """))
                .andExpect(status().isOk());

        assertNotEquals(originalSessionId, session.getId());
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("active@petway.test"));

        // Real CSRF bootstrap, rather than the test helper, must work after login rotation.
        var tokenResponse = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        var token = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(tokenResponse.getResponse().getContentAsString());
        mockMvc.perform(post("/api/auth/logout").session(session)
                        .header(token.path("headerName").asText(), token.path("token").asText()))
                .andExpect(status().isOk());
        assertTrue(session.isInvalid());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void csrfBlocksUnauthenticatedLoginAndAuthenticatedMutationWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mockMvc.perform(post("/api/favorites").with(user("member")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void csrfBootstrapTokenIsSessionBoundAndCannotBeReusedByAnotherSession() throws Exception {
        var tokenResponse = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Cache-Control", "no-store"))
                .andReturn();
        var session = (MockHttpSession) tokenResponse.getRequest().getSession(false);
        assertNotNull(session);
        var token = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(tokenResponse.getResponse().getContentAsString());
        // Invalid credentials return 401 only after the actual CSRF header was accepted.
        mockMvc.perform(post("/api/auth/login").session(session)
                        .header(token.path("headerName").asText(), token.path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").session(new MockHttpSession())
                        .header(token.path("headerName").asText(), token.path("token").asText()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void oauthAuthorizationCallbackAndSessionFlowWithControlledProvider() throws Exception {
        var provider = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        var tokenCalls = new java.util.concurrent.atomic.AtomicInteger();
        provider.createContext("/token", exchange -> {
            tokenCalls.incrementAndGet();
            byte[] body = "{\"access_token\":\"test-only-access-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}"
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        provider.createContext("/userinfo", exchange -> {
            byte[] body = "{\"id\":\"kakao-test-101\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        provider.start();
        try {
            String base = "http://127.0.0.1:" + provider.getAddress().getPort();
            var registration = org.springframework.security.oauth2.client.registration.ClientRegistration
                    .withRegistrationId("kakao").clientId("test-client").clientSecret("test-secret")
                    .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("http://localhost/login/oauth2/code/kakao")
                    .authorizationUri(base + "/authorize").tokenUri(base + "/token")
                    .userInfoUri(base + "/userinfo").userNameAttributeName("id").build();
            when(clientRegistrationRepository.findByRegistrationId("kakao")).thenReturn(registration);
            User oauthUser = User.createOAuthUser("oauth-session@petway.test", "카카오 테스트");
            when(userAuthRepository.findByProviderAndProviderUserId("kakao", "kakao-test-101"))
                    .thenReturn(Optional.of(UserAuth.createKakao(oauthUser, "kakao-test-101")));
            when(userRepository.findByEmail(oauthUser.getEmail())).thenReturn(Optional.of(oauthUser));

            var start = mockMvc.perform(get("/oauth2/authorization/kakao"))
                    .andExpect(status().is3xxRedirection()).andReturn();
            var session = (MockHttpSession) start.getRequest().getSession(false);
            assertNotNull(session);
            String oldId = session.getId();
            var params = org.springframework.web.util.UriComponentsBuilder
                    .fromUriString(start.getResponse().getRedirectedUrl()).build().getQueryParams();
            assertEquals("login", params.getFirst("prompt"));
            String state = java.net.URLDecoder.decode(params.getFirst("state"), java.nio.charset.StandardCharsets.UTF_8);
            mockMvc.perform(get("/login/oauth2/code/kakao").session(session)
                            .param("code", "test-code").param("state", state))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .redirectedUrl("http://localhost:5173/"));
            assertNotEquals(oldId, session.getId());
            mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(oauthUser.getEmail()));
            assertEquals(1, tokenCalls.get());

            // An unbound callback must not exchange its code or create an authenticated session.
            mockMvc.perform(get("/login/oauth2/code/kakao").param("code", "test-code").param("state", state))
                    .andExpect(status().is3xxRedirection());
            assertEquals(1, tokenCalls.get());
        } finally {
            provider.stop(0);
        }
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

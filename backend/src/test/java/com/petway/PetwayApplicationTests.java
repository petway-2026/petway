package com.petway;

import com.petway.pet.repository.PetRepository;
import com.petway.pet.entity.Pet;
import com.petway.pet.enums.PetSizeType;
import com.petway.place.model.PlacePetDetail;
import com.petway.place.port.TourPlaceGateway;
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
    private TourPlaceGateway tourPlaceGateway;

    @MockitoBean
    private WeatherQueryService weatherQueryService;

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

    @Test
    @DisplayName("날씨 API의 GET 이외 요청은 인증 없이 접근할 수 없다")
    void weatherNonGetRequestRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/weather"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("사용자별 장소 동반 가능 판정 API는 인증 없이 접근할 수 없다")
    void placeCompatibilityRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/places/125266/compatibility"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WalkFit 산책 추천 API는 인증 없이 접근할 수 없다")
    void walkFitRecommendationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/walk/recommendations")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780")
                        .param("targetDate", "2026-09-01"))
                .andExpect(status().isForbidden());
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

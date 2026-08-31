package com.petway.external.weather.client;

import com.petway.external.weather.config.WeatherApiProperties;
import com.petway.external.weather.dto.KmaApiResponse;
import com.petway.external.weather.dto.KmaMidLandItem;
import com.petway.external.weather.dto.KmaMidTemperatureItem;
import com.petway.external.weather.dto.KmaShortForecastItem;
import com.petway.external.weather.mapper.KmaGridConverter;
import com.petway.external.weather.mapper.KmaSeasonalOutlookParser;
import com.petway.external.weather.mapper.KmaWeatherMapper;
import com.petway.weather.domain.WeatherForecast;
import com.petway.weather.domain.WeatherRegion;
import com.petway.weather.port.WeatherForecastGateway;
import com.petway.weather.service.WeatherForecastUnavailableException;
import com.petway.weather.service.WeatherProviderException;
import com.petway.weather.service.WeatherProviderTimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KmaWeatherClient implements WeatherForecastGateway {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm");
    private static final List<LocalTime> SHORT_ISSUE_TIMES = List.of(
            LocalTime.of(2, 0), LocalTime.of(5, 0), LocalTime.of(8, 0), LocalTime.of(11, 0),
            LocalTime.of(14, 0), LocalTime.of(17, 0), LocalTime.of(20, 0), LocalTime.of(23, 0)
    );
    private static final Pattern SEASONAL_RSS_URL = Pattern.compile(
            "https?://www\\.kma\\.go\\.kr/repositary/xml/fct/mon/img/fct_mon3rss_108_\\d{8}\\.xml"
    );

    private final RestClient restClient;
    private final WeatherApiProperties properties;
    private final KmaGridConverter gridConverter;
    private final KmaWeatherMapper mapper;
    private final KmaSeasonalOutlookParser seasonalParser;
    private final Clock clock;

    public KmaWeatherClient(
            @Qualifier("weatherRestClient") RestClient restClient,
            WeatherApiProperties properties,
            KmaGridConverter gridConverter,
            KmaWeatherMapper mapper,
            KmaSeasonalOutlookParser seasonalParser,
            Clock clock
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.gridConverter = gridConverter;
        this.mapper = mapper;
        this.seasonalParser = seasonalParser;
        this.clock = clock;
    }

    @Override
    public WeatherForecast getShortTerm(
            double latitude,
            double longitude,
            LocalDate targetDate,
            WeatherRegion region
    ) {
        String apiKey = properties.getShort().getApiKey();
        ensureApiKey(apiKey, "단기예보");
        KmaGridConverter.GridPoint grid = gridConverter.convert(latitude, longitude);
        LocalDateTime base = latestShortBaseTime();
        URI uri = UriComponentsBuilder.fromUriString(properties.getShort().getBaseUrl())
                .path("/getVilageFcst")
                .queryParam("serviceKey", "{serviceKey}")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1000)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", base.format(DATE))
                .queryParam("base_time", base.format(TIME))
                .queryParam("nx", grid.nx())
                .queryParam("ny", grid.ny())
                .encode()
                .buildAndExpand(decodedApiKey(apiKey))
                .toUri();

        KmaApiResponse<KmaShortForecastItem> response = get(
                uri,
                new ParameterizedTypeReference<>() {
                }
        );
        List<KmaShortForecastItem> items = items(response);
        try {
            return mapper.mapShortTerm(items, targetDate, base, region);
        } catch (IllegalStateException exception) {
            throw new WeatherForecastUnavailableException(
                    "요청한 여행일(" + targetDate + ")의 단기예보가 아직 제공되지 않았습니다."
            );
        }
    }

    @Override
    public WeatherForecast getMidTerm(WeatherRegion region, LocalDate targetDate) {
        String apiKey = properties.getMid().getApiKey();
        ensureApiKey(apiKey, "중기예보");
        LocalDateTime base = latestMidBaseTime();
        int forecastDay = Math.toIntExact(ChronoUnit.DAYS.between(base.toLocalDate(), targetDate));
        if (forecastDay < 4 || forecastDay > 10) {
            throw new WeatherForecastUnavailableException(
                    "요청한 여행일(" + targetDate + ")의 중기예보가 아직 제공되지 않았습니다."
            );
        }

        KmaMidLandItem land = first(getMidLand(region, base));
        KmaMidTemperatureItem temperature = first(getMidTemperature(region, base));
        return mapper.mapMidTerm(land, temperature, region, targetDate, forecastDay, base);
    }

    @Override
    public WeatherForecast getSeasonalOutlook(WeatherRegion region, LocalDate targetDate) {
        try {
            String guide = restClient.get()
                    .uri(properties.getSeasonalGuideUrl())
                    .retrieve()
                    .body(String.class);
            Matcher matcher = SEASONAL_RSS_URL.matcher(guide == null ? "" : guide);
            if (!matcher.find()) {
                throw new WeatherProviderException("기상청 3개월 전망 주소를 확인할 수 없습니다.");
            }
            byte[] xml = restClient.get()
                    .uri(httpsUri(matcher.group()))
                    .retrieve()
                    .body(byte[].class);
            if (xml == null || xml.length == 0) {
                throw new WeatherProviderException("기상청 3개월 전망 응답이 비어 있습니다.");
            }
            return seasonalParser.parse(xml, region, targetDate);
        } catch (WeatherProviderException exception) {
            throw exception;
        } catch (IllegalStateException exception) {
            throw new WeatherForecastUnavailableException(exception.getMessage());
        } catch (ResourceAccessException exception) {
            throw providerAccessException("기상청 3개월 전망 호출", exception);
        } catch (RestClientException exception) {
            throw new WeatherProviderException("기상청 3개월 전망 호출에 실패했습니다.", exception);
        }
    }

    private KmaApiResponse<KmaMidLandItem> getMidLand(WeatherRegion region, LocalDateTime base) {
        URI uri = midUri("/getMidLandFcst", region.getLandForecastCode(), base);
        return get(uri, new ParameterizedTypeReference<>() {
        });
    }

    private KmaApiResponse<KmaMidTemperatureItem> getMidTemperature(WeatherRegion region, LocalDateTime base) {
        URI uri = midUri("/getMidTa", region.getTemperatureForecastCode(), base);
        return get(uri, new ParameterizedTypeReference<>() {
        });
    }

    private URI midUri(String path, String regionCode, LocalDateTime base) {
        String apiKey = properties.getMid().getApiKey();
        return UriComponentsBuilder.fromUriString(properties.getMid().getBaseUrl())
                .path(path)
                .queryParam("serviceKey", "{serviceKey}")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 10)
                .queryParam("dataType", "JSON")
                .queryParam("regId", regionCode)
                .queryParam("tmFc", base.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")))
                .encode()
                .buildAndExpand(decodedApiKey(apiKey))
                .toUri();
    }

    private <T> KmaApiResponse<T> get(URI uri, ParameterizedTypeReference<KmaApiResponse<T>> type) {
        try {
            KmaApiResponse<T> response = restClient.get().uri(uri).retrieve().body(type);
            validate(response);
            return response;
        } catch (WeatherProviderException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw providerAccessException("기상청 날씨 정보 호출", exception);
        } catch (RestClientException exception) {
            throw new WeatherProviderException("기상청 날씨 정보 호출에 실패했습니다.", exception);
        }
    }

    private <T> void validate(KmaApiResponse<T> response) {
        if (response == null || response.response() == null || response.response().header() == null) {
            throw new WeatherProviderException("기상청 날씨 응답 형식이 올바르지 않습니다.");
        }
        if (!"00".equals(response.response().header().resultCode())) {
            throw new WeatherProviderException("기상청에서 날씨 정보를 정상적으로 제공하지 못했습니다.");
        }
    }

    private <T> List<T> items(KmaApiResponse<T> response) {
        if (response.response().body() == null
                || response.response().body().items() == null
                || response.response().body().items().item() == null
                || response.response().body().items().item().isEmpty()) {
            throw new WeatherForecastUnavailableException("조회 가능한 날씨 정보가 없습니다.");
        }
        return response.response().body().items().item();
    }

    private <T> T first(KmaApiResponse<T> response) {
        return items(response).get(0);
    }

    private LocalDateTime latestShortBaseTime() {
        ZonedDateTime availableAt = ZonedDateTime.now(clock).withZoneSameInstant(KOREA).minusMinutes(10);
        LocalDate date = availableAt.toLocalDate();
        LocalTime latest = SHORT_ISSUE_TIMES.stream()
                .filter(time -> !time.isAfter(availableAt.toLocalTime()))
                .reduce((first, second) -> second)
                .orElse(null);
        if (latest == null) {
            date = date.minusDays(1);
            latest = LocalTime.of(23, 0);
        }
        return LocalDateTime.of(date, latest);
    }

    private LocalDateTime latestMidBaseTime() {
        ZonedDateTime availableAt = ZonedDateTime.now(clock).withZoneSameInstant(KOREA).minusMinutes(10);
        LocalDate date = availableAt.toLocalDate();
        LocalTime time;
        if (!availableAt.toLocalTime().isBefore(LocalTime.of(18, 0))) {
            time = LocalTime.of(18, 0);
        } else if (!availableAt.toLocalTime().isBefore(LocalTime.of(6, 0))) {
            time = LocalTime.of(6, 0);
        } else {
            date = date.minusDays(1);
            time = LocalTime.of(18, 0);
        }
        return LocalDateTime.of(date, time);
    }

    private void ensureApiKey(String apiKey, String forecastName) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new WeatherProviderException("기상청 " + forecastName + " API 인증키가 설정되지 않았습니다.");
        }
    }

    private String decodedApiKey(String apiKey) {
        String key = apiKey.trim();
        return key.contains("%") ? URLDecoder.decode(key, StandardCharsets.UTF_8) : key;
    }

    private URI httpsUri(String url) {
        return URI.create(url.replaceFirst("^http://", "https://"));
    }

    private WeatherProviderException providerAccessException(
            String operation,
            ResourceAccessException exception
    ) {
        if (isTimeout(exception)) {
            return new WeatherProviderTimeoutException(operation + " 시간이 초과되었습니다.", exception);
        }
        return new WeatherProviderException(operation + "에 실패했습니다.", exception);
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

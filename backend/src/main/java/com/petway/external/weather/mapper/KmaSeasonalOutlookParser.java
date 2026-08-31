package com.petway.external.weather.mapper;

import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.OutlookProbabilities;
import com.petway.weather.domain.WeatherForecast;
import com.petway.weather.domain.WeatherRegion;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KmaSeasonalOutlookParser {

    private static final Pattern ANNOUNCED_AT = Pattern.compile(
            "(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일\\s*(\\d{1,2})시"
    );

    public WeatherForecast parse(byte[] xml, WeatherRegion region, LocalDate targetDate) {
        try {
            Document document = document(xml);
            LocalDateTime announcedAt = parseAnnouncedAt(text(document.getDocumentElement(), "ydate"));
            int month = Math.toIntExact(ChronoUnit.MONTHS.between(
                    YearMonth.from(announcedAt),
                    YearMonth.from(targetDate)
            ));
            if (month < 1 || month > 3) {
                throw new IllegalStateException("요청한 달의 3개월 전망이 아직 제공되지 않았습니다.");
            }

            Element temperatureRegion = regionElement(
                    document,
                    "local_ta",
                    "local_ta_name",
                    region
            );
            Element precipitationRegion = regionElement(
                    document,
                    "local_rn",
                    "local_rn_name",
                    region
            );
            OutlookProbabilities temperatures = probabilities(temperatureRegion, month, "ta");
            OutlookProbabilities precipitation = probabilities(precipitationRegion, month, "rn");

            return new WeatherForecast(
                    targetDate,
                    ForecastType.SEASONAL_OUTLOOK,
                    region.getDisplayName(),
                    null,
                    null,
                    null,
                    null,
                    temperatures.dominantLevel(),
                    precipitation.dominantLevel(),
                    temperatures,
                    precipitation,
                    announcedAt,
                    "특정 날짜의 날씨가 아닌 해당 월의 기온·강수량 확률 전망입니다."
            );
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("3개월 전망 응답 형식을 해석할 수 없습니다.", exception);
        }
    }

    private Document document(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private Element regionElement(
            Document document,
            String tag,
            String nameTag,
            WeatherRegion region
    ) {
        NodeList nodes = document.getElementsByTagName(tag);
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            String actualName = normalize(text(element, nameTag));
            boolean matches = region.getSeasonalRegionNames().stream()
                    .map(this::normalize)
                    .anyMatch(actualName::equals);
            if (matches) {
                return element;
            }
        }
        throw new IllegalStateException("요청한 지역의 3개월 전망을 찾을 수 없습니다.");
    }

    private OutlookProbabilities probabilities(Element region, int month, String category) {
        String prefix = "month" + month + "_local_" + category;
        return new OutlookProbabilities(
                integer(region, prefix + "_minVal"),
                integer(region, prefix + "_similarVal"),
                integer(region, prefix + "_maxVal")
        );
    }

    private int integer(Element element, String tagName) {
        return Integer.parseInt(text(element, tagName).trim());
    }

    private String text(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            throw new IllegalStateException("필수 전망 항목이 없습니다: " + tagName);
        }
        Node node = nodes.item(0);
        return node.getTextContent();
    }

    private LocalDateTime parseAnnouncedAt(String value) {
        Matcher matcher = ANNOUNCED_AT.matcher(value);
        if (!matcher.find()) {
            throw new IllegalStateException("3개월 전망 발표 시각을 해석할 수 없습니다.");
        }
        return LocalDateTime.of(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                Integer.parseInt(matcher.group(4)),
                0
        );
    }

    private String normalize(String value) {
        return value.replace("·", "ㆍ").replaceAll("\\s", "");
    }
}

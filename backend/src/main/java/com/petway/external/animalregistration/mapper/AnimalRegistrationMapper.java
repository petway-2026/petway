package com.petway.external.animalregistration.mapper;

import com.petway.external.animalregistration.dto.AnimalRegistrationApiResponse;
import com.petway.external.animalregistration.exception.AnimalRegistrationErrorType;
import com.petway.external.animalregistration.exception.AnimalRegistrationException;
import com.petway.pet.enums.DogBreed;
import com.petway.pet.model.RegisteredPetProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

@Component
public class AnimalRegistrationMapper {

    private static final Set<String> SUCCESS_CODES = Set.of("00", "0000");
    private static final Map<String, DogBreed> BREEDS = Map.ofEntries(
            entry("말티즈", DogBreed.MALTESE),
            entry("푸들", DogBreed.POODLE),
            entry("포메라니안", DogBreed.POMERANIAN),
            entry("시추", DogBreed.SHIH_TZU),
            entry("치와와", DogBreed.CHIHUAHUA),
            entry("비숑프리제", DogBreed.BICHON_FRISE),
            entry("요크셔테리어", DogBreed.YORKSHIRE_TERRIER),
            entry("골든리트리버", DogBreed.GOLDEN_RETRIEVER),
            entry("래브라도리트리버", DogBreed.LABRADOR_RETRIEVER),
            entry("진돗개", DogBreed.JINDO),
            entry("진도견", DogBreed.JINDO),
            entry("시바견", DogBreed.SHIBA_INU),
            entry("시바이누", DogBreed.SHIBA_INU),
            entry("웰시코기", DogBreed.WELSH_CORGI),
            entry("프렌치불도그", DogBreed.FRENCH_BULLDOG),
            entry("프렌치불독", DogBreed.FRENCH_BULLDOG),
            entry("비글", DogBreed.BEAGLE),
            entry("닥스훈트", DogBreed.DACHSHUND),
            entry("슈나우저", DogBreed.SCHNAUZER),
            entry("보더콜리", DogBreed.BORDER_COLLIE),
            entry("저먼셰퍼드", DogBreed.GERMAN_SHEPHERD),
            entry("독일셰퍼드", DogBreed.GERMAN_SHEPHERD),
            entry("사모예드", DogBreed.SAMOYED),
            entry("시베리안허스키", DogBreed.SIBERIAN_HUSKY),
            entry("도베르만", DogBreed.DOBERMAN),
            entry("그레이트데인", DogBreed.GREAT_DANE),
            entry("아키타", DogBreed.AKITA),
            entry("알래스칸말라뮤트", DogBreed.ALASKAN_MALAMUTE),
            entry("바센지", DogBreed.BASENJI),
            entry("보스턴테리어", DogBreed.BOSTON_TERRIER),
            entry("복서", DogBreed.BOXER),
            entry("불도그", DogBreed.BULLDOG),
            entry("불독", DogBreed.BULLDOG),
            entry("불테리어", DogBreed.BULL_TERRIER),
            entry("카네코르소", DogBreed.CANE_CORSO),
            entry("캐벌리어킹찰스스패니얼", DogBreed.CAVALIER_KING_CHARLES_SPANIEL),
            entry("차우차우", DogBreed.CHOW_CHOW),
            entry("코커스패니얼", DogBreed.COCKER_SPANIEL),
            entry("달마시안", DogBreed.DALMATIAN),
            entry("잉글리시스프링어스패니얼", DogBreed.ENGLISH_SPRINGER_SPANIEL),
            entry("이탈리안그레이하운드", DogBreed.ITALIAN_GREYHOUND),
            entry("잭러셀테리어", DogBreed.JACK_RUSSELL_TERRIER),
            entry("라사압소", DogBreed.LHASA_APSO),
            entry("미니어처핀셔", DogBreed.MINIATURE_PINSCHER),
            entry("파피용", DogBreed.PAPILLON),
            entry("페키니즈", DogBreed.PEKINGESE),
            entry("퍼그", DogBreed.PUG),
            entry("러프콜리", DogBreed.ROUGH_COLLIE),
            entry("세인트버나드", DogBreed.SAINT_BERNARD),
            entry("셔틀랜드쉽독", DogBreed.SHETLAND_SHEEPDOG),
            entry("휘핏", DogBreed.WHIPPET),
            entry("아프간하운드", DogBreed.AFGHAN_HOUND),
            entry("보르조이", DogBreed.BORZOI),
            entry("뉴펀들랜드", DogBreed.NEWFOUNDLAND),
            entry("버니즈마운틴독", DogBreed.BERNESE_MOUNTAIN_DOG),
            entry("그레이트피레니즈", DogBreed.GREAT_PYRENEES),
            entry("올드잉글리시쉽독", DogBreed.OLD_ENGLISH_SHEEPDOG),
            entry("웨스트하이랜드화이트테리어", DogBreed.WEST_HIGHLAND_WHITE_TERRIER),
            entry("에어데일테리어", DogBreed.AIREDALE_TERRIER),
            entry("믹스견", DogBreed.MIXED),
            entry("혼합견", DogBreed.MIXED),
            entry("도사견", DogBreed.TOSA),
            entry("핏불테리어", DogBreed.PIT_BULL_TERRIER),
            entry("아메리칸스태퍼드셔테리어", DogBreed.AMERICAN_STAFFORDSHIRE_TERRIER),
            entry("스태퍼드셔불테리어", DogBreed.STAFFORDSHIRE_BULL_TERRIER),
            entry("로트와일러", DogBreed.ROTTWEILER)
    );

    public RegisteredPetProfile toRegisteredPetProfile(
            AnimalRegistrationApiResponse response
    ) {
        if (response == null
                || response.response() == null
                || response.response().header() == null) {
            throw invalidResponse();
        }
        AnimalRegistrationApiResponse.Response providerResponse = response.response();
        if (!SUCCESS_CODES.contains(trim(providerResponse.header().resultCode()))) {
            throw new AnimalRegistrationException(
                    AnimalRegistrationErrorType.API_ERROR,
                    "동물등록정보 제공기관에서 요청을 처리하지 못했습니다."
            );
        }
        if (providerResponse.body() == null || providerResponse.body().item() == null) {
            throw new AnimalRegistrationException(
                    AnimalRegistrationErrorType.NOT_FOUND,
                    "일치하는 동물등록정보를 찾을 수 없습니다."
            );
        }

        AnimalRegistrationApiResponse.Item item = providerResponse.body().item();
        String petName = trimToNull(item.dogNm());
        if (petName == null) {
            throw invalidResponse();
        }

        String breedDetail = trimToNull(item.kindNm());
        return new RegisteredPetProfile(
                petName,
                parseBirthDate(item.birthDt()),
                mapBreed(breedDetail),
                breedDetail,
                mapNeutered(item.neuterYn())
        );
    }

    private DogBreed mapBreed(String breedName) {
        if (breedName == null) {
            return null;
        }
        try {
            return DogBreed.valueOf(
                    breedName.trim()
                            .toUpperCase(Locale.ROOT)
                            .replaceAll("[\\s-]+", "_")
            );
        } catch (IllegalArgumentException ignored) {
            // 공식 응답의 한글 품종명 매핑을 이어서 확인한다.
        }

        String normalized = normalizeBreed(breedName);
        DogBreed mapped = BREEDS.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        if (normalized.contains("믹스") || normalized.contains("혼합")) {
            return DogBreed.MIXED;
        }
        return null;
    }

    private String normalizeBreed(String value) {
        return value
                .replace("[개]", "")
                .replaceAll("[\\s._-]", "")
                .trim();
    }

    private LocalDate parseBirthDate(String value) {
        String date = trimToNull(value);
        if (date == null) {
            return null;
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{
                DateTimeFormatter.BASIC_ISO_DATE,
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy.MM.dd")
        }) {
            try {
                return LocalDate.parse(date, formatter);
            } catch (DateTimeParseException ignored) {
                // 다른 공식 응답 형식을 이어서 확인한다.
            }
        }
        return null;
    }

    private Boolean mapNeutered(String value) {
        String normalized = trim(value).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "Y", "YES", "예", "중성화", "완료" -> true;
            case "N", "NO", "아니오", "미중성화", "미완료" -> false;
            default -> null;
        };
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AnimalRegistrationException invalidResponse() {
        return new AnimalRegistrationException(
                AnimalRegistrationErrorType.INVALID_RESPONSE,
                "동물등록정보 응답 형식이 올바르지 않습니다."
        );
    }
}

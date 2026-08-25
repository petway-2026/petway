# PetWay Codex 개발 참조 문서

> 이 문서는 PetWay 개발 전 Codex가 반드시 읽어야 하는 **프로젝트 최상위 컨텍스트**다.  
> 기능 구현, 리팩터링, 테스트, 외부 API 연동, DB 변경 시 이 문서를 우선 기준으로 사용한다.

---

# 0. Codex 작업 원칙

## 최우선 목표

PetWay는 **2026 관광데이터 활용 공모전 웹·앱 구현 부문** 출품을 목적으로 한다.

이 프로젝트의 목표는 복잡한 기술을 많이 사용하는 것이 아니라 다음을 만족하는 **안정적으로 구동되는 완제품**을 빠르게 만드는 것이다.

1. 지정과제 6번을 명확하게 해결한다.
2. 한국관광공사 OpenAPI가 핵심 서비스 로직에 실제로 사용된다.
3. 반려동물 동반 규정을 구조화하고 사용자 반려견 기준으로 이용 가능 여부를 판정한다.
4. AI는 검증된 실제 관광 장소만 추천한다.
5. 핵심 사용자 흐름이 처음부터 끝까지 끊기지 않고 동작한다.
6. 불필요한 기술과 기능을 추가하지 않는다.

## 개발 방식

이 프로젝트는 **AI/Codex 중심으로 개발**한다.

사용자는 직접 손코딩하지 않는 것을 전제로 한다.

따라서 Codex는 작업 시 다음을 반드시 수행한다.

1. 작업 전에 기존 Repository 구조와 관련 파일을 먼저 읽는다.
2. 현재 구현을 최대한 유지하고 필요한 범위만 수정한다.
3. 한 번에 여러 독립 기능을 구현하지 않는다.
4. 구현 전 완료 조건과 테스트 조건을 확인한다.
5. 구현 후 직접 빌드와 테스트를 실행한다.
6. 실패 시 로그를 읽고 원인을 수정한다.
7. 테스트가 통과하지 않은 상태를 완료라고 보고하지 않는다.
8. 임의로 요구사항을 확대하지 않는다.
9. 새로운 프레임워크나 인프라를 함부로 추가하지 않는다.
10. 추측이 필요한 경우 코드로 임의 구현하지 말고 TODO 또는 확인 필요 사항으로 명시한다.

---

# 1. 공모전 컨텍스트

## 선택 지정과제

**과제번호 6번**

> 모호한 반려동물 출입 조건 및 규정 인지 오류로 현장 입장 거부 및 헛걸음 유발 문제

따라서 모든 핵심 기능은 다음 질문과 연결되어야 한다.

> "이 기능이 반려인이 방문 전에 자신의 반려견이 실제로 이용 가능한 장소인지 더 정확하게 판단하도록 돕는가?"

연결되지 않는 기능은 MVP 우선순위에서 낮춘다.

## 1차 심사 기준

- 서비스 구현성: 30
- 서비스 기획력: 30
- 데이터 활용 적절성: 20
- 서비스 발전성: 20

특히 **기능 완결성, 구동 안정성, 완성도**를 중요하게 본다.

## 한국관광공사 OpenAPI

한국관광공사 OpenAPI 활용은 필수다.

단순 화면 장식용 호출이 아니라 실제 서비스 기능과 연결해야 한다.

서비스 내 다음 흐름에서 OpenAPI를 사용한다.

- 지역별 장소 검색
- 위치 기반 장소 검색
- 카테고리 검색
- 장소 상세 조회
- 반려동물 동반정보 조회
- 코스 후보 검색
- AI 추천 후보 검색
- 저장한 코스의 출발 전 최신 정보 재검증

OpenAPI 호출 로그와 서비스 내 활용 내역을 확인할 수 있도록 한다.

---

# 2. 서비스 정의

## 서비스명

**PetWay**

## 슬로건

**오늘 산책부터 주말 여행까지, 우리 아이와 갈 수 있는 곳을 한 번에.**

## 한 줄 정의

한국관광공사 OpenAPI의 반려동물 동반 관광정보를 실시간으로 활용하여 장소별 동반 조건을 구조화하고, 반려견의 체중·크기·성향과 날씨·거리 등을 종합해 실제 이용 가능한 장소와 외출·여행 코스를 추천하는 서비스.

## 핵심 가치

PetWay의 핵심은 단순히

> "반려동물 동반 장소를 보여주는 것"

이 아니다.

핵심은

> **"내 반려견이 실제로 갈 수 있는지를 판단하고, 그 장소를 실제 외출과 여행으로 연결하는 것"**

이다.

---

# 3. 핵심 서비스 파이프라인

PetWay의 가장 중요한 데이터 흐름은 아래와 같다.

```text
사용자 / 마이펫
        ↓
한국관광공사 OpenAPI
        ↓
반려동물 동반 원문 정보
        ↓
PetRule Parser
        ↓
Compatibility Engine
        ↓
AVAILABLE / CONDITIONAL / NEED_CONFIRMATION / UNAVAILABLE
        ↓
PetFit / Weather / Distance
        ↓
장소 추천 또는 코스 구성
        ↓
필요 시 OpenAI Agent
```

AI는 이 흐름을 우회해서는 안 된다.

---

# 4. 개발 범위 우선순위

## P0 — 반드시 완성

다음 기능이 공모전 MVP의 핵심이다.

### 회원 / 마이펫

- 회원가입
- 로그인
- 로그아웃
- 로그인 사용자 정보 조회
- 반려견 등록
- 반려견 조회
- 반려견 수정
- 반려견 삭제
- 대표 반려견 설정

### 장소 탐색

- 지역 기반 장소 검색
- 카테고리 검색
- 한국관광공사 OpenAPI 실제 호출
- 장소 상세 조회
- 반려동물 동반정보 조회
- 지도 표시

### PetRule

- 반려동물 동반 텍스트 정규화
- 체중 제한 추출
- 크기 제한 추출
- 실내/실외 조건 추출
- 이동장 조건 추출
- 목줄 조건 추출
- 불명확 정보 처리

### Compatibility

- 체중 조건 판정
- 크기 조건 판정
- 이용 조건 판정
- 4단계 결과 생성
- 판정 근거 제공

### AI

- 자연어 요청 입력
- 관광공사 API에서 실제 후보 검색
- 서버에서 후보 검증
- 입장 불가 장소 제거
- 검증된 후보만 OpenAI에 전달
- 추천 이유 반환
- 사용자가 수락한 경우에만 코스 반영

### 날씨

- 기상 API 조회
- 비/눈/폭염/한파 등 단순 기상 위험 판단
- 장소/코스와 날씨 충돌 안내

### OpenAPI 로그

- 호출 API
- 호출 목적
- 성공/실패
- HTTP 상태
- 외부 결과 코드가 있다면 결과 코드
- 응답 시간
- 결과 건수
- 호출 시각

---

## P1 — P0 완료 후 구현

- PetFit Score
- 코스 생성
- 코스 장소 추가/삭제
- 코스 순서 변경
- 장소 간 거리 표시
- 코스 저장/조회
- 날씨 기반 코스 검증
- 출발 전 최신 관광정보 재검증
- 실내 대체 장소 추천

---

## P2 — 시간이 충분할 때만 구현

- 다견 동시 선택
- 장소 비교
- 즐겨찾기
- 빠른 외출 코스 고도화
- 방문 기록
- 현장 제보
- 장소 정보 신뢰도
- 복잡한 영업시간 검증
- 다일 여행
- 추천 모델 고도화

P2 기능 때문에 P0/P1 안정성을 해치지 않는다.

---

# 5. 권장 기술 구조

기존 Repository에 이미 기술 구성이 있다면 기존 구조를 우선 유지한다.

새 프로젝트라면 다음과 같은 단순 구조를 권장한다.

```text
Frontend
- React
- JavaScript 또는 TypeScript
- Kakao Map

Backend
- Spring Boot 3.x
- Java 17+
- REST API
- Spring Security
- JPA 또는 MyBatis 중 Repository에서 이미 사용하는 방식 하나만 사용

Database
- MySQL 8

External API
- 한국관광공사 OpenAPI
- 기상청 또는 선택한 Weather API
- OpenAI API
```

## 금지 또는 지양

공모전 MVP에서는 특별한 이유가 없는 한 다음을 추가하지 않는다.

- Microservice
- Kafka
- RabbitMQ
- Elasticsearch
- Kubernetes
- 여러 종류의 DB
- 복잡한 CQRS/Event Sourcing
- 불필요한 Redis
- 불필요한 Vector DB
- 멀티 Agent 아키텍처
- 자체 ML 모델 학습
- 과도한 디자인 패턴

기술 수가 많다고 가점이 생기는 프로젝트가 아니다.

---

# 6. 한국관광공사 OpenAPI 사용 정책

## 원칙

PetWay에서 관광 장소의 Source of Truth는 한국관광공사 OpenAPI다.

관광공사 전체 데이터를 PetWay DB에 대량 복제하지 않는다.

사용자 액션에 따라 실제 API를 호출한다.

```text
사용자 검색
↓
TourAPI 호출
↓
검색 결과 사용

장소 상세 클릭
↓
TourAPI 상세 호출
↓
반려동물 동반정보 파싱

AI 추천 요청
↓
TourAPI 후보 검색
↓
PetWay 검증
↓
OpenAI

코스 재검증
↓
저장된 tour_content_id
↓
TourAPI 최신 상세 재조회
```

## 기능별 활용

| PetWay 기능 | OpenAPI 역할 |
|---|---|
| 지역별 장소 검색 | 지역 기반 관광정보 조회 |
| 내 주변 장소 | 위치 기반 관광정보 조회 |
| 키워드 검색 | 통합/키워드 검색 |
| 장소 상세 | 공통/소개/상세정보 조회 |
| 반려동물 조건 | 반려동물 동반 상세정보 조회 |
| 이미지 | 이미지정보 조회 |
| 오늘의 추천 | 실제 장소 후보 조회 |
| 빠른 외출 코스 | 실제 장소 후보 조회 |
| 여행 코스 | 지역별 실제 장소 후보 조회 |
| AI 추천 | AI에 전달할 실제 장소 후보 조회 |
| 출발 전 재검증 | 저장된 contentId 기반 최신 정보 재조회 |

## contentId

`tour_content_id`는 PetWay와 한국관광공사 데이터를 연결하는 핵심 외부 식별자로 사용한다.

예:

```text
course_place.tour_content_id
        ↓
한국관광공사 OpenAPI
        ↓
최신 상세정보
```

## 관광 데이터 저장 정책

저장:

- 사용자가 생성한 코스
- 코스에 선택한 장소의 최소 스냅샷
- 관광공사의 `contentId`
- 사용자에게 마지막으로 제시한 Compatibility 판정 스냅샷

저장하지 않음:

- 관광공사 전체 장소 데이터 마스터
- 기상청 전체 예보 데이터
- AI 후보 전체 목록
- AI 응답 전체 히스토리
- 사용자 위치 이력

## 원천 데이터

한국관광공사 원문을 PetWay가 임의로 수정한 공식 데이터처럼 표시하지 않는다.

필요한 경우:

```text
관광공사 원문
        +
PetWay 해석 결과
```

를 구분한다.

서비스 화면에서는 관광 데이터 출처를 명확히 표시한다.

---

# 7. PetRule Parser

## 목적

관광공사에서 제공되는 반려동물 동반 관련 텍스트를 PetWay가 판정할 수 있는 구조화 규칙으로 변환한다.

예:

```text
10kg 미만 반려견 동반 가능.
실내 이용 시 이동장 사용 필수.
야외에서는 목줄 착용 시 이용 가능.
```

↓

```text
maxWeight = 10
weightOperator = LT
indoorAllowed = true
outdoorAllowed = true
carrierRequiredIndoor = true
leashRequiredOutdoor = true
```

## Parser 원칙

1. 최초 구현은 규칙 기반으로 한다.
2. 정규식과 명시적 키워드를 우선 사용한다.
3. 추측하지 않는다.
4. 해석되지 않는 조건은 `NEED_CONFIRMATION`으로 이어지게 한다.
5. Parser 실패가 전체 API 실패로 이어지지 않게 한다.
6. 원문을 확인할 수 있는 정보는 디버깅을 위해 남긴다.
7. 같은 의미의 표현 차이를 정규화한다.

## 우선 파싱 대상

- `Nkg 이하`
- `Nkg 미만`
- 소형견
- 중형견
- 대형견
- 실내 이용 가능/불가
- 야외 이용 가능/불가
- 테라스만 가능
- 이동장/캐리어 필수
- 목줄 필수
- 일부 구역만 가능

## 불명확 정보

아래와 같은 경우 임의로 AVAILABLE을 반환하지 않는다.

```text
반려동물 동반 가능
```

처럼 구체 조건이 부족하고 사용자의 실제 입장 가능 여부를 확정할 수 없다면:

```text
NEED_CONFIRMATION
```

으로 처리할 수 있어야 한다.

---

# 8. Compatibility Engine

Compatibility와 추천 점수는 반드시 분리한다.

## Compatibility

질문:

> 이 반려견이 이 장소를 실제로 이용할 수 있는가?

상태:

```text
AVAILABLE
CONDITIONAL
NEED_CONFIRMATION
UNAVAILABLE
```

### 의미

#### AVAILABLE

명확하게 입장 조건을 만족하고 추가 조건이 없다.

#### CONDITIONAL

입장은 가능하지만 지켜야 할 조건이 있다.

예:

- 이동장 필수
- 목줄 필수
- 야외만 가능
- 일부 구역만 가능

#### NEED_CONFIRMATION

공식 정보만으로 확정하기 어렵다.

추측하지 않는다.

#### UNAVAILABLE

명확한 제한 때문에 해당 반려견은 이용할 수 없다.

예:

```text
반려견 12kg
장소 최대 10kg 이하
→ UNAVAILABLE
```

## 판정 근거

모든 판정에는 사용자에게 설명 가능한 이유가 있어야 한다.

예:

```text
CONDITIONAL

- 반려견 체중 8kg
- 장소 체중 제한 10kg 이하 → 충족
- 실내 이용 시 이동장 필요
```

단순 상태 코드만 반환하지 않는다.

---

# 9. PetFit Score

PetFit은 Compatibility와 별개다.

```text
Compatibility
= 갈 수 있는가?

PetFit
= 갈 수 있는 장소 중 어디가 더 적합한가?
```

UNAVAILABLE 장소는 추천 후보에서 제외한다.

초기 PetFit은 복잡한 AI 모델 대신 설명 가능한 규칙 기반 점수로 구현한다.

후보 요소:

- Compatibility
- 거리
- 날씨
- 반려견 성향
- 실내/실외 선호

방문 기록 등의 데이터는 초기 MVP에서 필수 요소가 아니다.

---

# 10. AI Agent 규칙

## 역할

AI는 사실 판정기가 아니라 **추천 및 자연어 인터페이스**다.

AI가 다음을 직접 만들어내거나 결정해서는 안 된다.

- 존재하지 않는 관광 장소
- 관광공사에 없는 장소 정보
- 체중 제한의 임의 해석
- 반려견 입장 가능 여부의 사실 판정
- 날씨 수치 생성

## 필수 흐름

```text
사용자 자연어 요청
        ↓
요청 조건 해석
        ↓
한국관광공사 OpenAPI 후보 조회
        ↓
PetRule Parser
        ↓
Compatibility Engine
        ↓
UNAVAILABLE 제거
        ↓
날씨 / 거리 / PetFit
        ↓
검증된 후보만 OpenAI 전달
        ↓
추천 및 설명
```

## 예시

사용자:

> 우리 콩이 7kg 푸들인데 해운대에서 2시간 정도 놀고 싶어. 오후에 비 온다는데 실내 위주로 추천해줘.

AI 처리 시 필요한 도구의 예:

```text
getPrimaryPet()
searchTourPlaces()
getTourPlaceDetail()
getPetDetail()
getWeather()
calculateCompatibility()
calculatePetFit()
```

단, 초기 MVP에서 Tool Calling 구현이 불안정하다면 하나의 Agent Orchestrator Service가 명시적으로 위 기능을 순서대로 호출해도 된다.

**멀티 Agent 구조를 만들지 않는다.**

## AI 결과 반영

AI 추천이 반환되었다고 자동으로 사용자의 코스를 변경하지 않는다.

반드시:

```text
추천
↓
사용자 확인
↓
적용
```

순서로 처리한다.

---

# 11. 날씨 처리

날씨는 단순 정보 표시가 아니라 장소 또는 코스 검증에 사용한다.

예:

```text
장소: 야외 이용만 가능
방문 예정 시간: 비
↓
경고
```

사용자 표현 예:

> 해당 장소는 야외 이용만 가능한데 방문 예정 시간에 비가 예보되어 있습니다.

초기 위험 판단은 단순 규칙 기반으로 구현한다.

- 강수
- 눈
- 고온
- 저온
- 강풍

의료적 안전 판단이나 전문 수의학적 판단을 하지 않는다.

---

# 12. 위치정보 처리

사용자 위치는 주변 장소 검색에 사용할 수 있다.

기본 흐름:

```text
Browser Geolocation
        ↓
PetWay Backend
        ↓
한국관광공사 위치 기반 조회
```

원칙:

- 위치 이력을 DB에 영구 저장하지 않는다.
- API 요청 처리에 필요한 동안만 사용한다.
- 서비스키를 브라우저 코드에 노출하지 않는다.
- 위치 권한 거부 시 지역 직접 선택으로 대체 가능해야 한다.

---

# 13. 데이터베이스

MVP에서는 다음 5개 테이블을 기본으로 한다.

```text
app_user
pet
course
course_place
api_request_log
```

불필요하게 테이블을 추가하지 않는다.

---

## 13.1 app_user

```text
user_id BIGINT PK
email VARCHAR(150) NOT NULL UNIQUE
password_hash VARCHAR(255) NOT NULL
nickname VARCHAR(50) NOT NULL
user_status VARCHAR(20) NOT NULL
created_at DATETIME NOT NULL
updated_at DATETIME NOT NULL
```

`user_status`:

```text
ACTIVE
WITHDRAWN
```

비밀번호 원문 저장 금지.

---

## 13.2 pet

```text
pet_id BIGINT PK
user_id BIGINT FK NOT NULL
pet_name VARCHAR(50) NOT NULL
breed VARCHAR(100)
birth_date DATE
weight_kg DECIMAL(5,2) NOT NULL
size_type VARCHAR(20) NOT NULL
activity_level VARCHAR(20)
crowd_tolerance VARCHAR(20)
place_preference VARCHAR(20)
is_primary BOOLEAN NOT NULL
created_at DATETIME NOT NULL
updated_at DATETIME NOT NULL
```

관계:

```text
app_user 1 : N pet
```

대표 반려견은 사용자별 최대 1마리만 허용한다.

대표 반려견 변경은 트랜잭션으로 처리한다.

예:

```text
기존 대표 pet → false
새 대표 pet → true
```

`size_type` 기준은 구현 전에 PetWay 내부 규칙을 명시한다.

---

## 13.3 course

```text
course_id BIGINT PK
user_id BIGINT FK NOT NULL
pet_id BIGINT FK NOT NULL
course_name VARCHAR(100) NOT NULL
travel_date DATE
region_name VARCHAR(100)
course_status VARCHAR(20) NOT NULL
created_at DATETIME NOT NULL
updated_at DATETIME NOT NULL
```

상태:

```text
DRAFT
SAVED
```

코스 생성 시 반드시:

```text
pet.user_id == 로그인 사용자 user_id
```

를 검증한다.

MVP는 한 코스당 반려견 1마리를 기준으로 한다.

---

## 13.4 course_place

권장 구조:

```text
course_place_id BIGINT PK
course_id BIGINT FK NOT NULL

tour_content_id VARCHAR(50) NOT NULL
content_type_id VARCHAR(30)

place_name VARCHAR(200) NOT NULL
category_type VARCHAR(30) NOT NULL

latitude DECIMAL(10,7) NOT NULL
longitude DECIMAL(10,7) NOT NULL

sequence_no INT NOT NULL

planned_arrival_time TIME
expected_stay_minutes INT

compatibility_status VARCHAR(30)
compatibility_reason VARCHAR(500)
compatibility_checked_at DATETIME

created_at DATETIME NOT NULL
updated_at DATETIME NOT NULL
```

권장 제약:

```text
UNIQUE(course_id, sequence_no)
```

동일 장소의 중복 추가를 막기로 결정할 경우:

```text
UNIQUE(course_id, tour_content_id)
```

를 추가할 수 있다.

`compatibility_status`는 최신 영구 사실이 아니라 **마지막 검증 시점의 스냅샷**이다.

출발 전 재검증 시 다시 계산한다.

---

## 13.5 api_request_log

```text
api_request_log_id BIGINT PK
user_id BIGINT NULL FK

api_provider VARCHAR(30) NOT NULL
api_type VARCHAR(50) NOT NULL
request_purpose VARCHAR(50) NOT NULL

response_status INT
external_result_code VARCHAR(50)
response_time_ms BIGINT
result_count INT

success BOOLEAN NOT NULL
error_message VARCHAR(500)

requested_at DATETIME NOT NULL
```

Provider 예:

```text
TOUR_API
WEATHER_API
OPENAI
```

Purpose 예:

```text
PLACE_SEARCH
PLACE_DETAIL
PET_DETAIL
COURSE_WEATHER
AI_RECOMMENDATION
COURSE_REVALIDATION
```

보안상 API Key, Authorization Header, 전체 비밀 URL을 로그에 저장하지 않는다.

회원 삭제 시 API 로그의 `user_id`는 필요하면 `ON DELETE SET NULL` 전략을 사용한다.

---

# 14. DB에 저장하지 않는 데이터

| 데이터 | 저장 여부 |
|---|---|
| 회원 | 저장 |
| 반려견 | 저장 |
| 코스 | 저장 |
| 코스에 담은 장소 최소 스냅샷 | 저장 |
| API 호출 로그 | 저장 |
| 관광공사 전체 장소 마스터 | 저장하지 않음 |
| 기상 전체 데이터 | 저장하지 않음 |
| AI 후보 전체 목록 | 저장하지 않음 |
| AI 응답 전체 히스토리 | 저장하지 않음 |
| 사용자 GPS 이력 | 저장하지 않음 |

PetRule은 초기 MVP에서는 요청 시 파싱해도 된다.

성능 문제가 발생하면 다음과 같은 캐시 전략을 추가할 수 있다.

```text
원문
↓
hash
↓
동일 원문이면 기존 파싱 결과 사용
↓
원문 변경 시 재파싱
```

단, 초기 구현을 복잡하게 하기 위해 별도 영구 테이블을 선제적으로 추가하지 않는다.

---

# 15. 핵심 API 호출 로그 목적

공모전에서 한국관광공사 OpenAPI가 실제 핵심 기능에 활용됨을 설명하기 쉽도록 목적을 명확하게 기록한다.

예:

```text
PLACE_SEARCH
PLACE_DETAIL
PET_DETAIL
NEARBY_SEARCH
AI_RECOMMENDATION
COURSE_REVALIDATION
```

로그 시스템 때문에 사용자 기능이 실패해서는 안 된다.

가능하면 로깅 실패는 핵심 요청 실패와 분리한다.

---

# 16. 반드시 작성할 테스트

Codex는 아래 핵심 규칙을 자동 테스트로 작성해야 한다.

## Compatibility

### Case 1

```text
반려견: 7kg
장소: 10kg 이하
Expected: AVAILABLE
```

### Case 2

```text
반려견: 12kg
장소: 10kg 이하
Expected: UNAVAILABLE
```

### Case 3

```text
반려견: 7kg
장소: 10kg 이하 + 실내 이동장 필수
Expected: CONDITIONAL
```

### Case 4

```text
반려견: 7kg
장소: 동반 가능이라고만 기재되고 상세 조건 불명확
Expected: NEED_CONFIRMATION
```

### Case 5

```text
반려견: LARGE
장소: 소형견만 가능
Expected: UNAVAILABLE
```

## Parser

최소 다음 표현을 테스트한다.

```text
10kg 이하
10kg 미만
소형견만 가능
대형견 불가
실내 이동장 필수
야외 목줄 착용
테라스만 동반 가능
```

## 대표 반려견

```text
한 사용자에게 primary pet 두 마리가 동시에 존재하지 않도록 한다.
```

## Course

```text
다른 사용자의 pet_id로 course를 생성할 수 없어야 한다.
```

## AI

```text
UNAVAILABLE 후보는 OpenAI 입력 후보에 포함되지 않아야 한다.
```

## OpenAPI 장애

외부 API 실패 시:

- 사용자에게 이해 가능한 오류 반환
- 서버 stack trace 직접 노출 금지
- api_request_log 실패 기록
- 가능한 경우 전체 서비스가 crash 하지 않도록 처리

---

# 17. 예외 처리 원칙

## 외부 API

다음 상황을 구분한다.

- timeout
- HTTP 4xx
- HTTP 5xx
- HTTP 200이지만 외부 resultCode 실패
- 빈 결과
- JSON schema 예상 차이
- 반려동물 상세정보 없음

## 사용자 메시지

내부 예외 객체나 stack trace를 그대로 반환하지 않는다.

예:

```text
현재 관광정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
```

반려동물 정보가 부족한 경우:

```text
제공된 정보만으로 입장 가능 여부를 확정하기 어렵습니다.
```

---

# 18. 보안 기본 규칙

- 비밀번호는 BCrypt 등 안전한 단방향 해시 사용
- API Key Git commit 금지
- `.env`, `application-secret.yml` 등 비밀 파일 commit 금지
- 외부 API Key를 Frontend에 노출 금지
- 로그에 API Key 출력 금지
- 사용자 인증이 필요한 API에서 userId를 요청 body 값만 믿지 말고 인증 주체 기준으로 검증
- 다른 사용자의 pet/course 접근 금지
- CORS는 필요한 Origin만 허용
- 운영 환경 stack trace 노출 금지

---

# 19. 화면에서 반드시 드러나야 하는 핵심 가치

장소 카드 또는 상세 화면에서는 가능하면 다음 내용을 한 화면에서 이해할 수 있어야 한다.

예:

```text
○○카페

콩이 · 푸들 · 7kg

🟡 조건부 이용 가능

✓ 체중 10kg 이하 조건 충족
⚠ 실내 이용 시 이동장 필요
✓ 목줄 착용 시 야외 이용 가능

현재 위치에서 1.2km

PetFit 91

출처: 한국관광공사
```

PetWay의 핵심 차별성이 UI에 보이지 않으면 백엔드 구현만 잘 되어 있어도 공모전에서 전달력이 떨어진다.

---

# 20. 핵심 시연 시나리오

공모전 시연에서 최소 아래 흐름은 처음부터 끝까지 정상 동작해야 한다.

```text
1. 로그인

2. 반려견 등록
   콩이 / 푸들 / 7kg

3. 지역 검색
   예: 해운대

4. 한국관광공사 OpenAPI 실제 호출

5. 장소 선택

6. 반려동물 동반 상세정보 조회

7. PetRule Parser 실행

8. Compatibility 판정
   예:
   CONDITIONAL
   - 10kg 이하 조건 충족
   - 실내 이동장 필요

9. 날씨 확인

10. 자연어 AI 요청
   "오늘 오후 비 온다는데 2시간 정도 실내 위주 코스로 추천해줘."

11. TourAPI에서 실제 후보 조회

12. UNAVAILABLE 장소 제거

13. 검증된 후보만 AI 추천

14. 추천 근거 표시

15. 사용자가 선택해 코스 저장

16. 출발 전 재검증
    tour_content_id로 최신 상세정보 재조회
```

이 흐름이 깨지지 않는 것이 다른 부가기능 수보다 중요하다.

---

# 21. Codex 기능 구현 순서

기본적으로 아래 순서를 따른다.

```text
STEP 01
Repository 분석 / 프로젝트 실행 확인

STEP 02
DB schema 5개 테이블

STEP 03
회원가입 / 로그인

STEP 04
마이펫 CRUD / 대표 반려견

STEP 05
한국관광공사 API Client

STEP 06
지역/카테고리 장소 검색

STEP 07
장소 상세 / 반려동물 상세 조회

STEP 08
PetRule Parser

STEP 09
Compatibility Engine

STEP 10
검색 결과 / 상세 UI / 지도

STEP 11
Weather API

STEP 12
PetFit 기본 점수

STEP 13
Course CRUD 최소 기능

STEP 14
AI Agent / Recommendation

STEP 15
출발 전 재검증

STEP 16
API Request Log

STEP 17
통합 테스트

STEP 18
배포 및 실제 환경 smoke test
```

이미 구현된 단계가 있다면 중복 개발하지 않는다.

---

# 22. Codex 작업 단위 규칙

각 작업 요청을 받으면 아래 순서를 따른다.

## 1. Inspect

관련 파일, 기존 코드, DB schema, 테스트를 먼저 읽는다.

## 2. Plan

수정 대상과 구현 방법을 짧게 정리한다.

## 3. Implement

요청받은 기능 범위만 구현한다.

## 4. Test

가능한 자동 테스트를 작성하거나 기존 테스트를 실행한다.

최소:

- build
- unit test
- integration test 또는 API smoke test

중 해당 작업에 필요한 것을 실행한다.

## 5. Verify

실제 완료 조건을 확인한다.

## 6. Report

작업 종료 시 다음만 명확히 보고한다.

```text
변경한 내용
변경한 주요 파일
실행한 테스트
테스트 결과
남아 있는 확인사항
```

코드가 실제로 실패하는데 "완료"라고 보고하지 않는다.

---

# 23. Definition of Done

하나의 기능은 아래를 만족해야 완료다.

- 요구사항이 구현됨
- 빌드 성공
- 관련 테스트 성공
- 정상 시나리오 검증
- 주요 실패 시나리오 검증
- 외부 API 예외 처리
- 다른 기존 기능을 깨뜨리지 않음
- secret이 코드에 hard coding되지 않음
- 불필요한 라이브러리 추가 없음
- 문서 또는 API 명세가 필요한 경우 함께 갱신

---

# 24. 금지되는 구현 습관

Codex는 다음 행동을 하지 않는다.

## 범위 확대

요청하지 않은 대규모 기능을 같이 만들지 않는다.

예:

```text
"장소 검색 구현"
```

요청에

- 리뷰 시스템
- 알림 시스템
- 소셜 로그인
- 관리자 대시보드

를 임의 추가하지 않는다.

## 임의 데이터 생성

실제 한국관광공사 응답이 필요한 곳에 그럴듯한 가짜 장소를 운영 코드 fallback으로 반환하지 않는다.

Mock은 테스트 환경에서만 명확히 구분하여 사용한다.

## 무근거 AI 판정

OpenAI가 장소 입장 가능 여부를 최종 사실로 결정하지 않는다.

## 거대한 파일 생성

한 Service 또는 Component에 모든 기능을 무리하게 몰아넣지 않는다.

반대로 작은 클래스 수십 개로 과도하게 분리하지도 않는다.

현재 복잡도에 적절한 구조를 유지한다.

## 무분별한 리팩터링

기능 구현을 위해 필요하지 않은 기존 코드 전체 스타일 변경을 하지 않는다.

---

# 25. 모르는 데이터에 대한 처리

한국관광공사 실제 response field나 분류코드가 문서/실제 호출로 확인되지 않은 경우:

1. 추측해서 필드명을 만들지 않는다.
2. 공식 API 명세 또는 실제 응답을 먼저 확인한다.
3. 확인 전에는 Adapter/DTO의 TODO로 남길 수 있다.
4. 실제 응답 샘플이 확보되면 DTO와 Parser를 맞춘다.

특히 다음은 실제 데이터 확인 후 확정한다.

- 카페 분류 방식
- 반려동물 상세정보 실제 필드
- 체중/크기/이동장 조건의 실제 원문 형태
- 특정 API Operation 이름 및 파라미터
- 외부 resultCode 정상값

---

# 26. 프로젝트 의사결정 우선순위

구현 선택지가 여러 개라면 아래 순서로 판단한다.

```text
1. 지정과제 6 해결력
2. 핵심 기능 완결성
3. 구동 안정성
4. 한국관광공사 OpenAPI 실질 활용성
5. 사용자 이해 용이성
6. 구현 시간
7. 유지보수성
8. 기술적 화려함
```

기술적 화려함은 가장 낮은 우선순위다.

---

# 27. 최종 판단 기준

PetWay의 구현이 올바른 방향인지 판단할 때 항상 아래 질문을 사용한다.

### 질문 1

이 기능이 **내 반려견이 실제로 갈 수 있는 장소인지** 판단하는 데 기여하는가?

### 질문 2

한국관광공사 OpenAPI가 이 기능의 실제 데이터 원천으로 사용되는가?

### 질문 3

AI가 사실을 만들어내지 않고 서버에서 검증된 데이터만 사용하고 있는가?

### 질문 4

심사위원이 시연 화면만 보고도 PetWay의 차별점을 이해할 수 있는가?

### 질문 5

이 기능을 추가함으로써 핵심 시연의 안정성이 떨어지지 않는가?

하나라도 명확하지 않다면 구현 전 범위를 다시 검토한다.

---

# 28. Codex 시작 메시지

새로운 Codex 세션에서 개발을 시작할 때 다음 지시를 사용한다.

```text
먼저 이 저장소의 PETWAY_CODEX_CONTEXT.md를 전체 읽고 프로젝트의 목적,
MVP 범위, 데이터 정책, OpenAPI 정책, AI 사용 원칙, DB 구조를 이해해라.

그 다음 현재 저장소 구조와 구현 상태를 분석하라.

아직 코드를 수정하지 말고 다음을 먼저 보고하라.

1. 현재 기술 스택
2. 현재 구현 완료 기능
3. 미구현 핵심 기능
4. 이 문서와 현재 코드 사이의 충돌 사항
5. 다음으로 구현할 가장 작은 작업 단위
6. 해당 작업의 테스트/완료 조건

내 승인 또는 다음 작업 지시 후에만 코드를 수정해라.
```

---

# 29. 문서 우선순위

정보가 충돌할 경우 다음 순서를 따른다.

```text
1. 사용자의 최신 명시적 지시
2. 실제 공모전 공식 자료
3. 이 PETWAY_CODEX_CONTEXT.md
4. Repository의 기존 설계 문서
5. 기존 코드
6. Codex의 자체 추론
```

공모전 공식 조건이나 실제 외부 API 명세와 이 문서가 충돌한다면 공식 자료를 우선하고, 문서가 오래되었음을 사용자에게 보고한다.

---


## Git / 브랜치 작업 규칙

- `main` 브랜치에서 직접 기능을 개발하거나 직접 push하지 않는다.
- 모든 기능 개발은 별도의 작업 브랜치에서 수행한다.
- 신규 기능은 `feat/*` 브랜치를 사용한다.
- 버그 수정은 `fix/*` 브랜치를 사용한다.
- 문서 수정은 `docs/*` 브랜치를 사용한다.
- 리팩터링은 `refactor/*` 브랜치를 사용한다.
- 작업 전 반드시 최신 `main`을 pull한 뒤 새 브랜치를 생성한다.
- 작업 완료 후 build/test를 수행하고 성공 여부를 확인한다.
- 완료된 작업은 Pull Request를 통해 `main`에 병합하는 것을 전제로 한다.
- Codex는 사용자의 명시적 지시 없이 `main` 브랜치에 직접 commit 또는 push하지 않는다.
- Codex는 임의로 branch merge를 수행하지 않는다.
- 
# 30. 한 문장 요약

> **PetWay는 관광공사 OpenAPI에서 실제 반려동물 동반 장소를 조회하고, 장소의 동반 조건을 규칙으로 구조화하여 내 반려견의 실제 이용 가능 여부를 서버가 판정한 뒤, 검증된 장소만 날씨·거리·AI 추천과 연결하는 서비스다.**

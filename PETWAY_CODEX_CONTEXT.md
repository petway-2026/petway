PetWay Codex 개발 참조 문서

이 문서는 PetWay 개발 전 Codex가 반드시 읽어야 하는 프로젝트 최상위 컨텍스트이자 구현 Source of Truth다.
기능 구현, 리팩터링, 테스트, 외부 API 연동, DB 변경 시 이 문서를 우선 기준으로 사용한다.
최신 기획 문서의 제품 범위와 팀이 확정한 MVP 구현 우선순위를 함께 반영한다.

0. Codex 작업 원칙

최우선 목표

PetWay는 2026 관광데이터 활용 공모전 웹·앱 구현 부문 출품을 목적으로 한다.

이 프로젝트의 목표는 복잡한 기술을 많이 사용하는 것이 아니라 다음을 만족하는 안정적으로 구동되는 완제품을 빠르게 만드는 것이다.

지정과제 6번을 명확하게 해결한다.

한국관광공사 OpenAPI가 핵심 서비스 로직에 실제로 사용된다.

반려동물 동반 규정을 구조화하고 사용자 반려견 기준으로 실제 이용 가능 여부를 판정한다.

AI는 검증된 실제 관광 장소만 추천한다.

실제 현장과 공식 정보가 다를 경우 사용자 제보와 관리자 검토로 보완 가능한 구조를 갖는다.

일상 외출과 여행이 하나의 서비스 흐름으로 연결된다.

핵심 사용자 흐름이 처음부터 끝까지 끊기지 않고 동작한다.

불필요한 기술과 기능을 추가하지 않는다.

개발 방식

이 프로젝트는 AI/Codex 중심으로 개발한다.

사용자는 직접 손코딩하지 않는 것을 전제로 한다.

Codex는 작업 시 다음을 반드시 수행한다.

작업 전에 현재 branch와 Repository 구조, 관련 파일을 먼저 확인한다.

PETWAY_CODEX_CONTEXT.md를 먼저 읽는다.

현재 구현을 최대한 유지하고 필요한 범위만 수정한다.

한 번에 여러 독립 기능을 구현하지 않는다.

구현 전 완료 조건과 테스트 조건을 확인한다.

구현 후 직접 build/test를 실행한다.

실패 시 로그를 읽고 원인을 수정한다.

테스트가 통과하지 않은 상태를 완료라고 보고하지 않는다.

임의로 요구사항을 확대하지 않는다.

새로운 프레임워크나 인프라를 함부로 추가하지 않는다.

실제 외부 API 스펙이 확인되지 않은 경우 추측 구현하지 않는다.

1. 공모전 컨텍스트

선택 지정과제

과제번호 6번

모호한 반려동물 출입 조건 및 규정 인지 오류로 현장 입장 거부 및 헛걸음 유발 문제

따라서 모든 핵심 기능은 다음 질문과 연결되어야 한다.

이 기능이 반려인이 방문 전에 자신의 반려견이 실제로 이용 가능한 장소인지 더 정확하게 판단하도록 돕는가?

1차 심사 기준

서비스 구현성: 30

서비스 기획력: 30

데이터 활용 적절성: 20

서비스 발전성: 20

기능 수보다 완결성, 구동 안정성, 데이터 활용의 직접성을 우선한다.

한국관광공사 OpenAPI

한국관광공사 OpenAPI 활용은 필수다.

단순 화면 장식용 호출이 아니라 다음 핵심 흐름에서 실제로 사용한다.

지역별 장소 검색

카테고리별 검색

장소 상세 조회

반려동물 동반정보 조회

일상 외출 후보 검색

빠른 외출 코스 후보 검색

여행 코스 후보 검색

AI 추천 후보 검색

출발 전 최신 정보 재검증

2. 서비스 정의

서비스명

PetWay

슬로건

오늘 산책부터 주말 여행까지, 우리 아이와 갈 수 있는 곳을 한 번에.

한 줄 정의

한국관광공사 OpenAPI의 반려동물 동반 관광정보를 실시간으로 활용하여 장소별 비정형 동반 조건을 구조화하고, 반려견의 체중·크기·성향과 날씨·거리 등을 종합해 일상 외출부터 여행 코스까지 추천하는 반려생활 맞춤형 서비스.

핵심 가치

PetWay의 핵심은 단순히

"반려동물 동반 장소를 보여주는 것"

이 아니다.

핵심은

"내 반려견이 실제로 갈 수 있는지를 판단하고, 그 장소를 실제 외출과 여행으로 연결하는 것"

이다.

3. 서비스 핵심 구성

PetWay는 크게 일상 외출과 여행 두 흐름으로 구성한다.

                         PetWay
                            │
            ┌───────────────┴───────────────┐
            │                               │
        오늘 뭐하지?                     여행 가기
            │                               │
      ┌─────┼──────┐                ┌───────┼───────┐
      │     │      │                │       │       │
산책추천 내주변 빠른외출          지역검색 코스생성 AI보완
│     │      │                │       │       │
└─────┴──────┴────────┬───────┴───────┴───────┘
│
장소 적합성 분석
│
한국관광공사 OpenAPI

4. 핵심 서비스 파이프라인

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
필요 시 OpenAI 코스 어시스턴트

AI는 이 흐름을 우회해서는 안 된다.

5. 현재 확정 기술 스택

현재 Repository에 구성된 React + Spring Boot 구조를 유지한다.

Frontend

React

Vite

TypeScript 우선

Kakao Map JavaScript SDK

Backend

Java 17+

Spring Boot 3.x

Gradle

Spring MVC / REST API

Spring Security

Validation

Spring Data JPA

MySQL 8

외부 API Client는 Spring RestClient 우선

External API

한국관광공사 OpenAPI

기상청 API

OpenAI API

Kakao Map JavaScript SDK

임의 추가 금지

명시적 승인 없이 다음을 추가하지 않는다.

Microservice

Kafka

RabbitMQ

Elasticsearch

Kubernetes

별도 Vector DB

불필요한 Redis

WebFlux 전환

멀티 Agent 구조

자체 ML 모델 학습

과도한 디자인 패턴

6. 개발 범위 우선순위

기획 문서에는 전체 서비스 발전 기능까지 포함되어 있지만, 개발은 아래 순서를 따른다.

M1 — 핵심 문제 해결

반드시 먼저 완성한다.

회원가입 / 로그인 / 로그아웃

USER / ADMIN Role 기반 인증 구조

마이펫 등록 / 조회 / 수정 / 삭제

대표 반려견 설정

한국관광공사 지역/카테고리 장소 검색

장소 상세

반려동물 동반정보 조회

PetRule Parser

Compatibility Engine

판정 상태 + 판정 근거 UI

한국관광공사 데이터 출처 표기

M2 — 반복 사용 가능한 일상 외출

날씨 API

산책 적합 시간

오늘 뭐하지?

지역 후보 + 브라우저 거리 계산

PetFit

빠른 외출 코스

지도 표시

M3 — 여행

여행 코스 생성

코스 장소 추가/삭제

코스 순서 변경

방문 예정시간 / 체류시간

날씨 기반 코스 검증

AI 코스 어시스턴트

출발 전 최신 정보 재검증

M4 — 발전 기능

시간이 충분할 때만 구현한다.

장소 비교

즐겨찾기

방문 기록

사용자 현장 제보

관리자 제보 검토

AI 기반 제보 요약 / 유형 분류 / 공식정보 충돌 탐지

장소 정보 신뢰도

여러 반려견 동시 조건 고려

추천 고도화

M4 기능 때문에 M1~M3의 안정성을 해치지 않는다.

7. 마이펫 / 다견 정책

사용자 한 명은 여러 반려견을 등록할 수 있다.

app_user 1 : N pet

반려견 정보:

이름

견종

생년월일

체중

크기

활동 수준

혼잡 장소 적응도

실내/야외 선호

대표 반려견 여부

현재 MVP 구현 결정

여러 반려견 등록/관리는 지원한다.

대표 반려견을 설정할 수 있다.

현재 코스 판정은 한 번에 반려견 1마리 기준으로 한다.

여러 반려견을 동시에 선택해 하나의 코스를 판정하는 기능은 M4 확장이다.

course_pet N 테이블을 초기 단계에 임의로 추가하지 않는다.

보호자 수 기반 판정은 초기 MVP에서 제외한다.

size_type

size_type은 유지한다.

10kg 이하, 5kg 미만 등은 weight_kg로 판정한다.

원문에 소형견만, 대형견 불가 등 크기 제한이 명시된 경우 size_type을 사용한다.

체중만 보고 PetWay가 임의로 SMALL/MEDIUM/LARGE를 추론하지 않는다.



## 7-1. 사용자 / 관리자 권한 정책

PetWay는 일반 사용자와 관리자를 구분한다.

Role은 아래 두 가지를 사용한다.

USER

ADMIN

기본 원칙:

일반 회원가입 시 USER로만 생성한다.

클라이언트 요청으로 ADMIN 권한을 지정할 수 없도록 한다.

ADMIN 계정은 초기 데이터, 운영자 전용 생성 절차 등 통제된 방법으로만 생성한다.

관리자 기능은 Spring Security에서 Role 기반 권한 검증을 적용한다.

현재 로그인 구현 단계에서 user_role 구조까지 포함한다.

관리자 Dashboard와 제보 검토 기능 자체는 M4에서 구현해도 된다.

user_status 초기 값은 다음만 사용한다.

ACTIVE

WITHDRAWN

Codex는 필요하지 않은 Role 또는 상태값을 임의로 추가하지 않는다.

## 7-2. 공모전 심사용 테스트 계정 정책

PetWay가 별도 계정 로그인을 사용하는 경우 배포 환경에 공모전 지정 형식의 테스트 전용 USER 계정을 준비한다.

이메일 계정 예시:

ID: openapi@서비스도메인

PW: 2026openapi!

원칙:

개인 계정을 심사용 계정으로 제출하지 않는다.

테스트 계정은 일반 USER 권한으로 생성한다.

테스트 계정으로 로그인부터 핵심 시연 흐름 전체를 사용할 수 있어야 한다.

최종 제출 전에 실제 배포 URL에서 테스트 계정 로그인을 반드시 검증한다.


8. PetRule Parser

한국관광공사의 비정형 반려동물 동반정보를 PetWay 내부에서 계산 가능한 규칙으로 변환한다.

예:

10kg 미만 반려견 가능
실내 이동장 필수
야외 목줄 착용 필요

↓

maxWeight = 10
weightOperator = LT
indoorAllowed = true
carrierRequiredIndoor = true
leashRequiredOutdoor = true

1차 지원 규칙

Nkg 이하

Nkg 미만

소형견 / 중형견 / 대형견

실내 가능 / 불가

야외 가능 / 불가

테라스만 가능

이동장 / 캐리어 필수

목줄 필수

일부 구역만 가능

Parser 원칙

규칙 기반 정규식/키워드를 우선 사용한다.

추측하지 않는다.

해석되지 않는 조건은 원문을 유지한다.

자동 판정 근거가 부족하면 NEED_CONFIRMATION으로 처리한다.

Parser 실패가 전체 장소 조회 실패로 이어지지 않게 한다.

실제 TourAPI 응답을 확인한 뒤 표현 패턴을 추가한다.

다음 규정은 실제 데이터 빈도와 일정 확인 후 확장한다.

허용 마릿수

입마개

예방접종

기저귀 / 매너벨트

견종 제한

나이 / 중성화

추가요금

예약 / 전화문의

Codex는 위 확장 규칙을 임의로 P0에 추가하지 않는다.

9. Compatibility Engine

Compatibility는 다음 질문에 답한다.

이 반려견이 이 장소를 실제로 이용할 수 있는가?

상태는 정확히 네 가지를 사용한다.

AVAILABLE
CONDITIONAL
NEED_CONFIRMATION
UNAVAILABLE

AVAILABLE

명확한 입장 조건을 충족하며 추가 이용 조건이 없음.

CONDITIONAL

입장 가능하지만 이동장, 목줄, 야외 전용 등 추가 조건이 있음.

NEED_CONFIRMATION

공식 정보 또는 Parser 결과만으로 확정할 수 없음.

UNAVAILABLE

체중 또는 명확한 이용 제한을 위반함.

모든 판정에는 사용자에게 보여줄 수 있는 근거가 있어야 한다.

예:

CONDITIONAL

- 로켓 체중 8kg
- 장소 제한 10kg 이하 → 충족
- 실내 이용 시 이동장 필요

AI가 Compatibility의 최종 사실 판정을 대신하지 않는다.

10. PetFit Score

Compatibility와 PetFit은 분리한다.

Compatibility = 갈 수 있는가?
PetFit        = 갈 수 있는 장소 중 얼마나 잘 맞는가?

초기 PetFit은 설명 가능한 규칙 기반 점수로 구현한다.

우선 요소:

Compatibility

거리

날씨

반려견 성향

실내/야외 선호

기획상 사용자 방문 평가는 향후 PetFit 고도화 요소다.

방문 데이터가 없는 초기 구현에서 임의의 방문 기반 점수를 생성하지 않는다.

11. 오늘 뭐하지? / 산책 적합 시간

최신 기획에서 일상 반복 사용성을 위한 핵심 기능이다.

오늘 뭐하지?

대표 반려견과 현재/예정 날씨를 기반으로 당일 이용하기 좋은 활동과 장소를 제공한다.

예:

로켓과 오늘 뭐하지?

현재 23℃
오후부터 비 예보

08:00 ~ 10:00
산책 추천

오늘 추천
- 보라매공원
- 실내 동반 카페

산책 적합 시간

기상청 예보를 활용해 시간대별 외출 참고정보를 제공한다.

기준:

기온

강수

적설

풍속

의료적·수의학적 판단이 아니라 기상조건 기반 참고정보로 구현한다.

12. 위치정보 정책 — 중요

최신 기획 기준으로 사용자의 정확한 GPS는 가능한 한 브라우저 내부에서만 사용한다.

기본 흐름:

HTML5 Geolocation
↓
사용자 Browser
↓
현재 GPS
↓
후보 장소 좌표와 거리 계산
↓
화면 거리순 정렬

원칙:

사용자 GPS 이력을 DB에 저장하지 않는다.

기본 구현에서 정확한 GPS를 PetWay Backend로 전송하지 않는다.

지역 검색 등으로 장소 후보를 확보한 뒤 브라우저에서 거리를 계산하는 방식을 우선한다.

위치 권한 거부 시 지역 직접 선택이 가능해야 한다.

GPS 서버 전송이 필요한 신규 기능을 추가하려면 구현 전에 별도 검토한다.

Codex는 별도 승인 전 다음 구조를 구현하지 않는다.

Browser GPS → Backend → TourAPI 위치검색

13. 한국관광공사 OpenAPI 사용 정책

PetWay에서 관광 장소의 Source of Truth는 한국관광공사 OpenAPI다.

관광공사 전체 데이터를 PetWay DB에 대량 복제하지 않는다.

공모전 공식 안내에 따라 로컬 관광지 Master DB 중심이 아니라 실제 사용자 기능에서 OpenAPI를 실시간 호출하는 구조를 우선한다.

제출 인증키를 기준으로 개발 기간 내 실제 OpenAPI 호출 내역이 확인될 수 있으므로, 화면만 OpenAPI처럼 보이게 하고 실제로는 로컬 DB만 조회하는 구조를 만들지 않는다.

주요 호출 흐름

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

tour_content_id

PetWay 저장 데이터와 관광공사 데이터를 연결하는 핵심 외부 식별자로 사용한다.

course_place.tour_content_id
↓
TourAPI 재조회
↓
최신 상세정보

저장 정책

저장:

회원

반려견

여행 코스

코스에 선택한 장소의 최소 스냅샷

관광공사의 contentId

마지막 Compatibility 판정 스냅샷

외부 API 호출 로그

저장하지 않음:

관광공사 전체 장소 마스터

기상청 전체 예보

AI 후보 전체 목록

AI 응답 전체 히스토리

사용자 GPS 이력

14. 데이터 출처 정책

한국관광공사 관광데이터가 노출되는 화면에는 기관명 기준 출처를 표시한다.

예:

출처: ⓒ한국관광공사

또는 공식 안내에 맞는 기관명 표기를 사용한다.

TourAPI라는 API 서비스명만 출처처럼 단독 표기하지 않는다.

공사의 공식 CI/BI 로고 이미지는 임의로 사용하지 않고 텍스트 형태로 출처를 표기한다.

PetWay가 생성한 해석 결과와 공식 원문은 구분한다.

공식 관광정보
+
PetWay 판정/해석

15. 날씨 처리

기상청 API를 이용한다.

기본 활용:

기온

강수

적설

풍속

시간대별 외출 참고정보

코스 날씨 충돌 검증

예:

장소: 야외 이용만 가능
방문 예정 시간: 비
↓
주의/경고



기상청 날씨 조회는 개인 GPS를 서버에 전송하는 방식보다 한국관광공사에서 받은 장소 좌표/지역 정보를 기준으로 필요한 예보를 조회하는 방식을 우선한다.

## 15-1. Kakao Map 정책

지도는 Kakao Map JavaScript SDK를 사용한다.

기본 역할:

한국관광공사 장소 좌표 기반 Marker 표시

코스 장소 시각화

사용자의 브라우저 현재 위치 표시

브라우저 내부 거리 계산 결과와 연계

Kakao JavaScript Key는 브라우저 SDK 특성상 Frontend에서 사용될 수 있다.

단, Kakao Developers에서 허용 도메인을 제한하고 개발/운영 환경별 설정을 분리한다.

한국관광공사/기상청/OpenAI 등 서버용 Secret Key를 Frontend로 노출하지 않는다.


16. AI 코스 어시스턴트

AI는 사실 판정기가 아니라 추천 및 자연어 인터페이스다.

필수 흐름:

사용자 자연어 요청
↓
관광공사 OpenAPI 후보 조회
↓
PetRule Parser
↓
Compatibility
↓
UNAVAILABLE 제거
↓
PetFit + 날씨 + 현재 코스
↓
검증된 후보만 OpenAI 전달
↓
추천 결과

AI가 다음을 임의로 만들어내거나 사실로 결정하지 않는다.

존재하지 않는 장소

공식 동반 조건

체중 제한

Compatibility 결과

날씨 수치

AI 추천은 자동 반영하지 않는다.

AI 추천
↓
사용자 확인
↓
코스 적용

멀티 Agent 구조를 만들지 않는다.

17. 데이터베이스 — 현재 MVP

초기 핵심 테이블은 5개로 유지한다.

app_user
pet
course
course_place
api_request_log

확장 기능 때문에 초기부터 10개 테이블을 생성하지 않는다.

app_user

user_id BIGINT PK
email VARCHAR(150) NOT NULL UNIQUE
password_hash VARCHAR(255) NOT NULL
nickname VARCHAR(50) NOT NULL
user_status VARCHAR(20) NOT NULL
user_role VARCHAR(20) NOT NULL
created_at DATETIME NOT NULL
updated_at DATETIME NOT NULL

초기 값:

user_status = ACTIVE / WITHDRAWN

user_role = USER / ADMIN

일반 회원가입은 반드시 USER로 생성한다.

pet

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

대표 반려견은 사용자별 최대 1마리다.

course

course_id BIGINT PK
user_id BIGINT FK NOT NULL
pet_id BIGINT FK NOT NULL
course_name VARCHAR(100) NOT NULL
travel_date DATE
region_name VARCHAR(100)
course_status VARCHAR(20) NOT NULL
created_at DATETIME NOT NULL
updated_at DATETIME NOT NULL

현재 MVP 상태:

DRAFT
SAVED

MVP는 한 코스당 반려견 1마리를 기준으로 한다.

course_place

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

권장:

UNIQUE(course_id, sequence_no)

api_request_log

api_request_log_id BIGINT PK
user_id BIGINT NULL FK
api_provider VARCHAR(30) NOT NULL
api_type VARCHAR(50) NOT NULL
request_purpose VARCHAR(50) NOT NULL
tour_content_id VARCHAR(50)
response_status INT
external_result_code VARCHAR(50)
response_time_ms BIGINT
result_count INT
success BOOLEAN NOT NULL
error_message VARCHAR(500)
requested_at DATETIME NOT NULL

external_result_code와 tour_content_id는 목적이 다르므로 둘 다 유지한다.

API Key, Authorization Header, secret은 로그에 남기지 않는다.

공모전 제출을 위해 한국관광공사 OpenAPI 신청자의 인코딩키/디코딩키 정보와 실제 사용한 OpenAPI 목록을 별도로 관리하되, Repository에는 secret을 저장하지 않는다.

18. 기획상 후속 기능

최신 기획에는 아래 기능도 포함된다.

장소 비교

즐겨찾기

빠른 외출 코스 고도화

방문 기록

사용자 현장 제보

관리자 제보 검토

AI 기반 제보 요약 / 유형 분류 / 공식정보 충돌 탐지

장소 정보 신뢰도

여러 반려견 동시 조건 고려

향후 구현이 필요할 경우 다음과 같은 테이블을 추가할 수 있다.

favorite_place
place_visit
visit_pet
place_report
course_pet

place_report는 M4 구현 시 다음 개념을 기준으로 상세 컬럼을 확정한다.

report_id
user_id
tour_content_id
report_type
report_content
ai_summary
ai_detected_conflict
review_status
reviewed_by
reviewed_at
created_at

현재 단계에서 place_report 테이블을 미리 생성하지 않는다.

Codex는 현재 작업 요청 없이 위 테이블을 임의로 생성하지 않는다.

공식 관광정보와 사용자 제보 정보는 반드시 구분하여 표시한다.



## 18-1. 사용자 현장 제보 / 관리자 검토 / AI 분석 정책

이 기능은 지정과제의 핵심 문제인 “공식 정보와 실제 현장의 차이”를 보완하기 위한 발전 기능이다.

예시:

공식 관광정보

10kg 이하 동반 가능

사용자 현장 제보

8kg 반려견인데 현장에서는 5kg 이하만 가능하다고 안내받아 입장을 거절당함

처리 흐름:

사용자 제보
↓
place_report 저장
↓
AI 분석
- 제보 요약
- 유형 분류
- 공식정보와 충돌 여부 탐지
- 관리자 확인 포인트 정리
  ↓
  관리자 Dashboard
  ↓
  APPROVE / REJECT / NEED_VERIFICATION

AI는 관리자 보조 도구로만 사용한다.

AI가 할 수 있는 일:

비정형 제보 요약

제보 유형 분류

유사/중복 제보 후보 표시

한국관광공사 공식정보와 제보 내용의 차이 요약

관리자 확인 필요사항 생성

AI가 하면 안 되는 일:

한국관광공사 공식정보 자동 수정

PetRule 자동 수정

Compatibility 규칙 자동 변경

사용자 1인의 제보를 곧바로 사실로 확정

관리자 승인 없이 사용자 제보를 공식정보처럼 노출

서비스 화면에서는 다음 두 레이어를 반드시 구분한다.

[공식 관광정보]
+
[사용자 현장 제보]

예:

[공식 관광정보]
10kg 이하 동반 가능

[최근 현장 제보]
체중 제한이 다르다는 이용 경험이 제보되었습니다.
방문 전 시설 확인을 권장합니다.

장소 정보 신뢰도는 향후 설명 가능한 규칙 기반 HIGH / MEDIUM / LOW로 구현할 수 있다.
AI가 임의로 신뢰도 점수를 만들어내지 않는다.


19. 팀 개발 순서

STEP 1

공통 Domain / DB 5개 확정

STEP 2 — 병렬 가능

A. Auth + USER/ADMIN Role + Pet
B. TourAPI Client + 실제 응답 검증
C. PetRule + Compatibility

STEP 3

실제 TourAPI → PetRule → Compatibility 통합

STEP 4

장소 검색 / 상세 React UI

STEP 5

날씨 + 산책 적합 시간

STEP 6

브라우저 위치 기반 거리 계산 + PetFit + 오늘 뭐하지?

STEP 7

Kakao Map + Course

STEP 8

AI 코스 어시스턴트

STEP 9

출발 전 재검증

STEP 10

API 로그 / 통합 테스트 / 배포 안정화

STEP 11

시간이 남으면 M4 기능

M4 우선순위는 사용자 현장 제보 → 관리자 검토 → AI 제보 분석 → 정보 신뢰도 → 나머지 편의 기능 순으로 검토한다.

20. 필수 테스트

Parser

10kg 이하
10kg 미만
소형견만 가능
대형견 불가
실내 이동장 필수
야외 목줄 착용
테라스만 동반 가능

Compatibility

7kg + 10kg 이하
→ AVAILABLE

12kg + 10kg 이하
→ UNAVAILABLE

7kg + 실내 이동장 필수
→ CONDITIONAL

동반 가능이라고만 되어 있고 상세 조건 부족
→ NEED_CONFIRMATION

LARGE + 소형견만 가능
→ UNAVAILABLE

보안/소유권

다른 사용자의 pet 조회/수정 금지

다른 사용자의 course 접근 금지

일반 USER의 ADMIN API 접근 금지

일반 회원가입으로 ADMIN 생성 금지

비밀번호 원문 저장 금지

AI

UNAVAILABLE 후보는 OpenAI 입력에 포함되지 않아야 한다.

사용자 제보 AI 분석 결과가 공식정보/PetRule/Compatibility를 자동 수정하지 않아야 한다.

외부 API

다음 상황을 테스트한다.

timeout

HTTP 4xx

HTTP 5xx

HTTP 200 + 외부 resultCode 실패

빈 결과

예상하지 못한 JSON 필드/누락

반려동물 상세정보 없음

21. 보안 기본 규칙

비밀번호는 BCrypt 등 안전한 단방향 해시 사용

API Key Git commit 금지

.env, application-secret.yml 등 secret 파일 commit 금지

한국관광공사 / 기상청 / OpenAI 등 서버용 Secret Key를 Frontend 코드에 하드코딩 금지

Kakao Map JavaScript Key는 브라우저 SDK 특성에 맞게 사용하되 Kakao Developers에서 허용 도메인을 제한하고 환경별 설정을 분리

로그에 API Key 출력 금지

userId 요청값만 믿지 말고 인증 주체 기준으로 소유권 검증

다른 사용자의 pet/course 접근 금지

ADMIN 전용 API는 Role 검증 필수

일반 회원가입으로 ADMIN 생성 금지

CORS는 필요한 Origin만 허용

운영 환경 stack trace 노출 금지

22. Git / 브랜치 작업 규칙

main 브랜치에서 직접 기능을 개발하거나 직접 push하지 않는다.

모든 기능 개발은 별도의 작업 브랜치에서 수행한다.

신규 기능은 feat/*

버그 수정은 fix/*

문서 수정은 docs/*

리팩터링은 refactor/*

작업 전 최신 main을 pull한다.

작업 완료 후 build/test를 수행한다.

Pull Request를 통해 main에 병합한다.

Codex는 사용자의 명시적 지시 없이 main에 직접 commit/push하지 않는다.

Codex는 임의로 branch merge를 수행하지 않는다.

23. Codex 작업 단위 규칙

각 작업 요청은 아래 순서를 따른다.

Inspect
↓
Plan
↓
Implement
↓
Test
↓
Verify
↓
Report

작업 종료 시 다음을 보고한다.

변경한 내용
변경한 주요 파일
실행한 테스트
테스트 결과
남아 있는 확인사항

24. 문서 / 결정 우선순위

정보가 충돌할 경우 다음 순서를 따른다.

1. 사용자의 최신 명시적 팀 결정
2. 실제 공모전 공식 자료 / FAQ
3. 최신 PetWay 기획 문서
4. 이 PETWAY_CODEX_CONTEXT.md
5. Repository 기존 코드
6. Codex 자체 추론

단, 제품 범위와 구현 우선순위는 구분한다.

예:

기획상 방문 기록 존재
하지만 현재 구현 우선순위는 M4

이 경우 Codex가 임의로 방문 기록부터 구현하지 않는다.

25. 모르는 데이터에 대한 처리

한국관광공사 실제 response field나 분류코드가 확인되지 않은 경우:

추측해서 필드명을 만들지 않는다.

공식 API 명세 또는 실제 응답을 먼저 확인한다.

확인 전에는 TODO 또는 확인 필요 사항으로 남긴다.

실제 응답 샘플 확보 후 DTO/Parser를 맞춘다.

특히 다음은 실제 데이터 확인 후 확정한다.

카페 분류 방식

반려동물 상세정보 실제 필드

체중/크기/이동장 조건 실제 원문 형태

API Operation 이름 및 파라미터

외부 resultCode 정상값

26. 핵심 시연 시나리오

1. 로그인

2. 반려견 등록
   로켓 / 8kg

3. 지역 검색

4. 한국관광공사 OpenAPI 실제 호출

5. 장소 선택

6. 반려동물 동반 상세정보 조회

7. PetRule Parser

8. Compatibility 판정
    + 판정 근거

9. 날씨 확인

10. 오늘 뭐하지? 또는 코스 구성

11. AI 보완 요청

12. 실제 TourAPI 후보 조회

13. UNAVAILABLE 제거

14. 검증된 후보만 AI 추천

15. 사용자 확인 후 코스 저장

16. 출발 전 tour_content_id로 최신 정보 재검증

M4까지 구현된 경우 선택 시연:

17. 사용자가 현장 정보 불일치 제보

18. AI가 제보 요약 / 유형 분류 / 공식정보 충돌 탐지

19. 관리자가 Dashboard에서 검토

20. 공식 관광정보와 사용자 제보를 구분해 화면에 표시

이 흐름이 깨지지 않는 것이 부가기능 수보다 중요하다.

27. Codex 시작 메시지

PETWAY_CODEX_CONTEXT.md를 전체 읽어라.

현재 branch가 main이면 코드를 수정하지 말고 중단하라.

그 다음 현재 Repository 구조와 관련 구현을 확인하고,
이번 요청 범위에 필요한 최소 변경만 계획하라.

실제 코드를 수정하기 전에:
1. 현재 구현 상태
2. 이번 작업 범위
3. 수정 예상 파일
4. 테스트/완료 조건
   을 먼저 정리하라.

PetWay의 제품 범위와 현재 개발 우선순위를 혼동하지 말고,
요청하지 않은 M4 기능이나 신규 테이블/프레임워크를 임의로 추가하지 마라.

28. 한 문장 요약

PetWay는 한국관광공사 OpenAPI의 실제 반려동물 동반정보를 PetRule로 구조화하고, 마이펫 기준 Compatibility를 판정한 뒤, 기상청 날씨·브라우저 거리·PetFit·Kakao Map·일상 외출·여행 코스를 연결하며, AI에는 검증된 실제 장소만 전달하고 현장 제보는 AI 분석 후 관리자 검토로 보완하는 반려생활 맞춤형 관광 서비스다.
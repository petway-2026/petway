0. 문서의 역할과 우선순위

이 문서는 PetWay 개발 전 Codex가 반드시 읽어야 하는 프로젝트 최상위 구현 컨텍스트다.

기능 구현, 리팩터링, 테스트, 외부 API 연동, DB 변경, 배포 준비 시 이 문서를 우선 기준으로 사용한다.

정보가 충돌할 경우 아래 순서를 따른다.

사용자의 최신 명시적 팀 결정

공모전 공식 공고 / FAQ

실제 외부 API 공식 명세와 실응답

최신 PetWay 기획 / IA

이 PETWAY_CODEX_CONTEXT.md

Repository 기존 코드

Codex 자체 추론

실제 API와 문서가 다르면 공식 명세와 실응답을 우선한다.

Codex는 문서를 근거로 이미 구현된 기능을 다시 만들지 않는다.
반드시 현재 Repository 상태를 먼저 조사한다.

1. Codex 작업 원칙

1-1. 최우선 목표

PetWay는 2026 관광데이터 활용 공모전 웹·앱 구현 부문 출품을 목적으로 한다.

이 프로젝트의 목표는 복잡한 기술을 많이 사용하는 것이 아니다.

다음 조건을 만족하는 안정적으로 구동되는 완제품을 빠르게 만드는 것이 목표다.

지정과제 6번을 명확하게 해결한다.

한국관광공사 OpenAPI가 핵심 서비스 로직에 실제로 사용된다.

반려동물 동반 규정을 구조화한다.

사용자 반려견 기준 실제 이용 가능 여부를 판정한다.

공식 원문과 PetWay 해석 결과를 구분한다.

불확실한 규정은 임의로 가능/불가로 단정하지 않는다.

AI는 검증된 실제 관광 장소만 추천한다.

일상 외출과 여행이 하나의 서비스 흐름으로 연결된다.

핵심 사용자 흐름이 처음부터 끝까지 끊기지 않고 동작한다.

불필요한 프레임워크와 인프라를 추가하지 않는다.

기능 수보다 구현 완결성, 데이터 활용 직접성, 배포 안정성을 우선한다.

1-2. 개발 방식

이 프로젝트는 AI/Codex 중심으로 개발한다.

사용자는 직접 손코딩을 최소화한다.

Codex는 아래 순서를 따른다.

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

반드시 수행:

작업 전에 현재 branch와 working tree를 확인한다.

PETWAY_CODEX_CONTEXT.md를 먼저 전체 읽는다.

remote branch와 중복 구현 여부를 확인한다.

현재 구현을 최대한 유지하고 필요한 범위만 수정한다.

한 번에 여러 독립 기능을 구현하지 않는다.

구현 전 완료 조건과 테스트 조건을 정리한다.

구현 후 build/test를 직접 실행한다.

테스트가 통과하지 않은 상태를 완료라고 보고하지 않는다.

기존 사용자의 local 변경을 임의로 reset/revert하지 않는다.

실제 외부 API 스펙이 확인되지 않은 경우 추측 구현하지 않는다.

신규 dependency는 필요성이 명확하지 않으면 추가하지 않는다.

사용자의 명시적 지시 없이 다음을 하지 않는다.

main 직접 개발

main 직접 push

branch merge

rebase

cherry-pick

reset

unrelated refactor

대규모 package 변경

신규 framework 도입

2. 공모전 컨텍스트

2-1. 선택 지정과제

과제번호 6번:

모호한 반려동물 출입 조건 및 규정 인지 오류로 현장 입장 거부 및 헛걸음 유발 문제

따라서 모든 핵심 기능은 다음 질문과 연결되어야 한다.

이 기능이 반려인이 방문 전에 자신의 반려견이 실제로 이용 가능한 장소인지 더 정확하게 판단하도록 돕는가?

2-2. 1차 심사 기준

서비스 구현성: 30

서비스 기획력: 30

데이터 활용 적절성: 20

서비스 발전성: 20

기능 수보다 다음을 우선한다.

핵심 E2E 완결성

실제 배포 안정성

공공데이터 활용의 직접성

사용자가 문제 해결을 즉시 이해할 수 있는 UX

발전 기능의 현실성

2-3. 공모전 API / 브랜딩 원칙

한국관광공사 OpenAPI 활용은 필수다.

파일 데이터만으로 구현하지 않는다.

가능하면 실제 OpenAPI 실시간 호출 구조를 우선한다.

관광공사 전체 데이터를 로컬 Master DB로 대량 복제하지 않는다.

실제 사용 API와 인증키를 최종 제출 기준에 맞춰 관리한다.

API Key / Secret을 Git에 저장하지 않는다.

공사 공식 CI/BI 로고를 임의로 사용하지 않는다.

PetWay를 한국관광공사/KTO의 공식 서비스처럼 브랜딩하지 않는다.

관광공사 데이터가 노출되는 화면에는 공식 지침에 맞는 출처를 표기한다.

출처 예:

출처: ⓒ한국관광공사

3. 서비스 정의

3-1. 서비스명

PetWay

3-2. 슬로건

오늘 산책부터 주말 여행까지, 우리 아이와 갈 수 있는 곳을 한 번에.

3-3. 한 줄 정의

한국관광공사의 반려동물 동반여행 OpenAPI를 실시간 활용해 장소별 비정형 동반 조건을 구조화하고, 사용자의 반려견 정보와 비교해 실제 이용 가능 여부와 필요한 준비사항을 설명한 뒤 날씨·거리·일상 외출·여행 코스로 연결하는 반려생활 맞춤형 관광 서비스.

3-4. 핵심 가치

PetWay의 핵심은 단순히:

반려동물 동반 장소를 보여주는 것

이 아니다.

핵심은:

내 반려견이 실제로 갈 수 있는지를 판단하고, 왜 가능한지/왜 어려운지 설명한 뒤 실제 외출과 여행으로 연결하는 것

이다.

4. 핵심 사용자 E2E

회원가입 / 로그인
↓
반려견 등록
↓
대표 반려견 선택
↓
장소 검색
↓
KorPetTourService2 실시간 호출
↓
장소 상세 + 반려동물 상세
↓
필드별 Raw Adapter
↓
PetRule Parser
↓
Compatibility Engine
↓
AVAILABLE / CONDITIONAL / NEED_CONFIRMATION / UNAVAILABLE
↓
판정 근거 + 준비물 + 이용 범위
↓
공식 원문 / 출처 확인
↓
오늘 추천 또는 코스 추가
↓
출발 전 최신 규정 재검증

이 흐름이 끝까지 동작하는 것이 부가기능 수보다 중요하다.

5. 현재 확정 기술 스택

현재 Repository의 React + Spring Boot 구조를 유지한다.

Frontend

React

Vite

TypeScript

React Router

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

외부 HTTP Client: Spring RestClient 우선

External API

한국관광공사 KorPetTourService2

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

Vector DB

불필요한 Redis

WebFlux 전환

멀티 Agent 구조

자체 ML 모델

과도한 디자인 패턴

6. 2026-08-27 현재 main 구현 상태

현재 main 기준으로 이미 존재하는 기능을 중복 구현하지 않는다.

구현됨 / 상당부분 구현됨

Auth

일반 회원가입

세션 로그인

로그아웃

/api/auth/me

BCrypt

USER / ADMIN 구조

app_user + user_auth 인증 구조

React AuthContext / ProtectedRoute

다견 Pet CRUD

대표 반려견

Pet Frontend

PetRule / Parser 초안

Parser 테스트 25개

아직 main에 없음

KorPetTourService2 장소 API

Place Search

Place Detail

detailCommon2

detailPetTour2

PetTour Raw Adapter

Compatibility Engine

판정 reasons / requirements

Weather

Kakao Map

PetFit

오늘 뭐하지?

Course

OpenAI 추천 / Agent

출발 전 재검증

현재 테스트 상태

PetRuleParserTest: 25개 통과

전체 Backend 테스트: PetwayApplicationTests.contextLoads() 1개 실패

실패 원인: 최근 PetService 추가 후 PetRepository mock 누락

Frontend npm run build: 성공

현재 테스트 실패를 기능 실패로 과장하지 않는다.
다만 전체 테스트 green 상태는 다음 개발 전에 복구하는 것을 권장한다.

7. 한국관광공사 API 기준 — 매우 중요

7-1. PetWay의 기준 API

PetWay의 기본 관광 API는 다음으로 통일한다.

한국관광공사_반려동물_동반여행_서비스
Base URL:
https://apis.data.go.kr/B551011/KorPetTourService2

현재 공식 명세 및 실제 호출로 확인된 주요 operation:

areaBasedList2

locationBasedList2

searchKeyword2

detailPetTour2

detailCommon2

detailIntro2

detailInfo2

detailImage2

petTourSyncList2

areaCode2

categoryCode2

ldongCode2

lclsSystmCode2

7-2. 실제 호출 검증

실제 승인된 KorPetTourService2 키를 사용해 다음을 확인했다.

areaBasedList2
HTTP 200
resultCode = 0000
전체 장소 약 9,686건

detailPetTour2도 실제 호출 성공을 확인했다.

따라서 장소 검색 + 일반 상세 + 반려동물 상세 흐름은 KorPetTourService2 중심으로 구성한다.

7-3. 기존 KorService2 코드 처리

기존 팀 PR #7 / feature/place-search 일부는 KorService2/areaBasedList2 기준이다.

이 구현은 공모전 규정 위반이었던 것이 아니다.

그러나 PetWay의 현재 팀 결정은 KorPetTourService2 중심 통일이다.

기존 코드를 무조건 삭제하거나 새로 만들지 않는다.

우선 재사용 가능성을 확인한다.

재사용 후보:

RestClient 구조

ConfigurationProperties

connect/read timeout

external resultCode 처리

exception taxonomy

Gateway / Service / Controller 계층

pagination / filter DTO

mock 기반 Client/Mapper/Service/Controller 테스트 방식

수정/재검증 대상:

Base URL

operation 경로

query parameter

response DTO

category/content type 계약

detailPetTour2 필드

현재 main Auth/Pet 구조와 통합

실제 승인 key smoke test

별도 필요성이 입증되지 않는 한 신규 기능에 KorService2를 추가하지 않는다.

8. detailPetTour2 실제 응답 기준 — 매우 중요

8-1. 실제 JSON 구조

실제 응답 예:

{
"response": {
"header": {
"resultCode": "0000",
"resultMsg": "OK"
},
"body": {
"items": {
"item": [
{
"contentid": "2930927",
"relaAcdntRiskMtr": "",
"acmpyTypeCd": "전구역 동반가능",
"relaPosesFclty": "",
"relaFrnshPrdlst": "",
"etcAcmpyInfo": "",
"relaPurcPrdlst": "",
"acmpyPsblCpam": "",
"relaRntlPrdlst": "",
"acmpyNeedMtr": ""
}
]
},
"numOfRows": 1,
"pageNo": 1,
"totalCount": 1
}
}
}

실제 표본 기준:

items.item은 단건이어도 배열이었다.

값 없음은 null보다 "" 빈 문자열이 주로 사용됐다.

빈 문자열은 곧바로 unknown으로 처리하지 않는다.

빈 문자열은 우선 해당 필드에 정보 없음으로 처리한다.

8-2. 주요 필드

필드

의미

contentid

콘텐츠 ID

acmpyTypeCd

동반 유형

acmpyPsblCpam

동반 가능 동물

acmpyNeedMtr

동반 시 필요사항

etcAcmpyInfo

기타 동반 정보

relaAcdntRiskMtr

사고 대비사항

relaPosesFclty

구비 시설

relaFrnshPrdlst

비치 품목

relaPurcPrdlst

구매 품목

relaRntlPrdlst

렌탈 품목

8-3. 실제 데이터 특성

필드 이름과 실제 자연어 내용이 항상 완벽하게 일치하지 않는다.

같은 규정이 여러 필드에 중복되거나 예상과 다른 필드에 들어가는 사례가 존재한다.

따라서:

전체 문자열 단일 Parser

보다:

detailPetTour2
→ 필드별 Raw Adapter
→ source field + raw value 보존
→ PetRule Parser

구조를 사용한다.

9. 2026-08-27 실제 데이터 분석 결과

관광지·문화시설·레포츠·숙박·쇼핑·음식점 등을 층화해 약 120건의 실제 detailPetTour2 데이터를 분석했다.

패턴

등장 장소 수

목줄

60

일부구역

59

견종/맹견 조건

50

배변 처리

48

입마개

37

이동장/켄넬/케이지

19

체중

17

야외/외부

12

소형견

11

실내/내부

9

예약/문의

9

유모차

9

매너벨트

8

마릿수 제한

5

예방접종

3

요금

3

테라스

2

기저귀

0

실제 표현 예:

20㎏ 미만 동반 가능
9kg 이하
목줄 착용
일부구역 동반가능
전 견종 동반 가능
맹견의 경우 입마개 착용 필수
배변봉투 지참 및 배변처리 필수
이동장(켄넬)사용
야외 공간만 동반 가능
야외테라스만
중소형견
1인 당 반려견 최대 1마리
사전 문의 필수
사전예약 후 이용 가능

체중 조건의 이하, 미만 모두 실제 데이터에서 확인했다.

이 분석은 Parser MVP 우선순위의 근거로 사용한다.

10. PetRule Parser — 최신 설계

10-1. 역할

PetRule Parser는 API JSON을 단순 DTO 매핑하는 기능이 아니다.

API 필드 내부의 자연어/반정형 규정을 PetWay 내부에서 비교 가능한 규칙으로 구조화한다.

detailPetTour2
↓
PetTourRawAdapter
↓
PetRuleParser
↓
PetRule

Parser는 Compatibility 상태를 결정하지 않는다.

10-2. 단일 rawText 구조 금지

기존 초안은:

rawText 하나
→ Parser

형태였으나 실제 API 확인 결과 전체 시스템 기준으로는 부족하다.

최신 구조:

detailPetTour2 response
↓
PetTourRawAdapter
↓
source field + raw value 보존
↓
PetRuleParser
↓
PetRule

작은 문자열 Parser 함수 자체는 재사용할 수 있다.

다만 전체 상세를 하나의 문자열로 합쳐 source context를 잃지 않는다.

10-3. acmpyTypeCd

acmpyTypeCd는 실제 데이터에서 거의 enum 성격으로 관찰됐다.

예:

전구역 동반가능
일부구역 동반가능

이 필드는 정규식보다 exact mapping을 우선한다.

10-4. MVP Parser 우선순위

실데이터 빈도와 Compatibility 활용도를 함께 고려한다.

전구역 / 일부구역

목줄 / 리드줄

전 견종 허용 / 맹견 / 특정 견종 제한

배변봉투 / 배변처리

입마개

이동장 / 캐리어 / 켄넬 / 케이지

체중 kg / ㎏ + 이하 / 미만

실내 / 야외 / 외부 / 테라스 / 특정 시설 범위

소형견 / 중형견 / 대형견 / 중소형견

유모차 / 매너벨트

실데이터에서 확인했지만 후순위:

마릿수 제한

예방접종

예약 / 사전 문의

추가 요금

10-5. 체중

반드시 지원:

10kg 이하
10 kg 이하
20㎏ 미만

kg와 Unicode ㎏를 정규화한다.

이하와 미만은 별도 연산자다.

이하 → LESS_THAN_OR_EQUAL
미만 → LESS_THAN

10-6. 복수 WeightLimit

실제 데이터에는 같은 장소 안에서도 공간/시설별로 서로 다른 체중 제한이 존재할 수 있다.

따라서 최종 구조를 단일 WeightLimit 하나로 고정하지 않는다.

필요 시:

List<WeightLimit>

또는:

대상 + 공간 + 체중 제한

관계를 표현한다.

실제 fixture 기준으로 최소 구조를 설계한다.

10-7. 크기

체중과 크기는 별개다.

금지:

8kg → SMALL 자동 추론

원문에 실제로 아래가 명시될 때만 크기 조건을 생성한다.

소형견

중형견

대형견

중소형견

중소형견을 소형견 substring 하나로 잘못 파싱하지 않는다.

또한:

대형견 입마개 필수

를 단순 LARGE allowed로 처리하지 않는다.

대상과 requirement 관계를 보존할 수 있어야 한다.

10-8. Requirement

실제 중요 표현:

목줄 착용

입마개 착용

배변봉투 지참

배변처리

이동장 사용

이동장(켄넬)사용

케이지

유모차

매너벨트

acmpyNeedMtr는 자체적으로 “필요사항” 문맥이므로 그 안의:

목줄 착용
이동장 사용

은 필수/필요 단어가 없어도 requirement로 해석할 수 있다.

다른 source field에서는 같은 표현을 무조건 필수조건으로 단정하지 않는다.

10-9. SpaceScope

유지한다.

실제 데이터에는:

건물 내 이동 시 케이지
건물 외 이동 시 목줄
야외 공간만 동반 가능
일부구역 동반가능

등 범위가 다른 규정이 존재한다.

단, 다음을 추측하지 않는다.

실내 시설 이용은 개별 문의 필요

→ INDOOR_ALLOWED=true로 해석 금지

10-10. unknown 처리

단일 hasUnknownContent boolean만으로 최종 구조를 고정하지 않는다.

권장:

unparsedFragments

각 fragment는 최소 다음을 보존한다.

sourceField
rawText
reason(optional)

빈 문자열은 unknown fragment가 아니다.

10-11. Parser 안전 원칙

규칙 기반 정규식/키워드 우선

source field 문맥 활용

추측 금지

Parser 실패가 장소 상세 전체 실패로 전파되지 않음

일부만 해석 가능하면 해석된 규칙은 유지

미해석 부분은 원문/source와 함께 보존

AI를 Parser 필수 실행 경로로 사용하지 않음

Parser가 Compatibility 상태를 만들지 않음

11. 현재 Parser 초안 상태

현재 main에는 다음 초안이 존재한다.

PetRule.java

PetRuleParser.java

PetRuleParserTest.java

기존 초안의 좋은 원칙:

원문 보존

이하 / 미만 구분

체중→크기 추론 금지

Parser / Compatibility 책임 분리

불변 모델

null/blank 안전 처리

실데이터 기준 보완 필요:

단일 rawText 입력

단일 WeightLimit

㎏ 미지원

중소형견 미지원

켄넬 미지원

입마개 미지원

배변처리 미지원

견종/맹견 미지원

전구역 미지원

source field 없음

unparsed fragment 없음

실제 목줄 착용 표현 범위 제한

일부 allowedSizes 추론 위험

Codex는 기존 초안을 전부 폐기하지도, 그대로 완료 처리하지도 않는다.

실제 fixture를 추가한 뒤 ADJUST 중심으로 리팩터링한다.

12. Compatibility Engine

Compatibility는 다음 질문에 답한다.

이 반려견이 이 장소를 실제로 이용할 수 있는가?

상태는 정확히 네 가지다.

AVAILABLE

CONDITIONAL

NEED_CONFIRMATION

UNAVAILABLE

AVAILABLE

명확한 입장 조건을 충족하고 추가 행동 조건이 없음.

CONDITIONAL

입장은 가능하지만 추가 조건이 있음.

예:

목줄

입마개

이동장

일부 구역

야외 전용

배변봉투

NEED_CONFIRMATION

공식 정보만으로 확정 불가.

예:

사전 문의

개별 문의

규정 상충

핵심 미해석 규정

실내 여부 불명확

UNAVAILABLE

명확한 조건 위반.

예:

Pet 12kg
장소 10kg 이하
→ UNAVAILABLE

모든 판정은 사용자에게 보여줄 reason이 있어야 한다.

AI가 Compatibility 최종 판정을 대신하지 않는다.

13. 방문 준비 체크리스트

Parser/Compatibility 결과에서 실제 방문 전 준비해야 할 항목을 보여준다.

후보:

목줄

입마개

배변봉투

이동장 / 켄넬 / 케이지

유모차

매너벨트

예방접종 증빙

장소마다 실제 필요한 것만 표시한다.

14. 마이펫 / 다견 정책

사용자 1명은 여러 반려견을 등록할 수 있다.

app_user 1 : N pet

M1:

다견 등록

다견 조회/수정/삭제

대표 반려견 1마리 설정

장소 상세에서 판정 기준 반려견 변경

한 번에 한 마리 기준 Compatibility

코스도 한 마리 기준

M4:

두부 + 우주

처럼 여러 마리를 동시에 선택해 마릿수 제한과 조합 조건까지 종합 판정한다.

실제 API에는:

1인 당 반려견 최대 1마리

같은 규정이 존재한다.

초기부터 course_pet N 테이블을 추가하지 않는다.

14-1. size_type 정책

size_type은 유지한다.

이유:

소형견만 가능

대형견 불가

중소형견 가능

같은 실제 규정 판정에 필요하다.

체중으로 size를 자동 추론하지 않는다.

현재 main의 Pet Entity/DTO/Frontend에는 size_type이 없으므로 Compatibility 구현 전 보완이 필요하다.

15. 사용자 / 관리자 권한 정책

Role:

USER

ADMIN

원칙:

일반 회원가입은 USER

클라이언트 요청으로 ADMIN 생성 불가

ADMIN은 통제된 운영 방식으로만 생성

ADMIN API는 Role 검증

사용자 상태는 초기 ACTIVE, WITHDRAWN

관리자 Dashboard는 M4 가능.

16. 현재 Auth / DB 구조 — 매우 중요

현재 main의 실제 인증 구조를 기준으로 한다.

app_user
+
user_auth

비밀번호는 user_auth.password_hash에 저장한다.

Codex는 팀 결정 없이 다음을 하지 않는다.

user_auth 삭제

password_hash를 app_user로 이동

인증 schema 통합

현재 session 기반 인증을 JWT 등으로 전환

현재 실제 Entity:

User

UserAuth

Pet

향후:

Course

CoursePlace

ApiRequestLog

는 필요 시 추가한다.

17. 공모전 심사용 테스트 계정 정책

별도 로그인을 사용하는 경우 배포 환경에 공모전 지정 형식의 테스트 전용 USER 계정을 준비한다.

예:

ID: openapi@서비스도메인
PW: 2026openapi!

원칙:

개인 계정 제출 금지

일반 USER 권한

핵심 시연 흐름 전체 사용 가능

최종 제출 전 실제 배포 URL에서 검증

18. 장소 탐색 구조

장소 검색

KorPetTourService2 기준:

areaBasedList2

searchKeyword2

필요 시 locationBasedList2

장소 상세

detailCommon2

detailPetTour2

필요 시 intro/info/image API

기본 사용자 흐름

장소 검색
↓
장소 목록
↓
장소 상세
↓
반려동물 공식 정보
↓
내 반려견 Compatibility

React가 관광공사 API를 직접 호출하지 않는다.

React
↓
PetWay Spring Boot
↓
KorPetTourService2

서버용 관광공사 Key를 Frontend로 노출하지 않는다.

19. Kakao Map 정책 — 최신 확정

지도는 Kakao Map JavaScript SDK를 사용한다.

장소 Source of Truth는 KorPetTourService2다.

Kakao Map은 장소 검색 데이터 source가 아니라 지도 시각화 도구다.

기본 흐름:

KorPetTourService2
↓
장소명 / 주소 / 위도 / 경도
↓
PetWay Backend
↓
React
↓
Kakao Map Marker

현재 MVP에서는 다음을 하지 않는다.

관광공사 장소
↔
카카오 Local Search 장소
Entity Matching

카카오 Local Search API가 반드시 필요한 별도 기능이 확인되지 않는 한 사용하지 않는다.

Kakao Map 역할:

관광공사 좌표 기반 Marker

코스 장소 시각화

브라우저 현재 위치 표시

거리 계산 결과 연계

Kakao JavaScript Key는 브라우저 SDK 특성에 따라 Frontend에 사용할 수 있다.

단:

Kakao Developers 허용 도메인 제한

개발/운영 설정 분리

서버용 Secret Key Frontend 노출 금지

20. 날씨 처리

기상청 API를 사용한다.

기본 흐름:

KorPetTourService2
↓
장소 위도 / 경도
↓
기상청 격자 nx / ny 변환
↓
단기예보
↓
WeatherSummary

개인 GPS를 서버에 전송해 날씨를 조회하는 방식보다 장소 좌표 기반 조회를 우선한다.

MVP WeatherSummary 후보:

기온

강수확률

강수량

하늘상태

풍속

적설

날씨는 의료적/수의학적 판단이 아니라 외출 참고정보로 사용한다.

예:

야외만 동반 가능한 장소
+
방문 예정 시간 비
↓
주의 안내

21. 위치정보 정책

정확한 GPS는 기본적으로 Browser 내부에서만 사용한다.

HTML5 Geolocation
↓
Browser
↓
현재 GPS
↓
관광지 좌표와 거리 계산
↓
거리순 표시

금지:

GPS history DB 저장

기본 기능에서 정확한 GPS Backend 전송

승인 없이 Browser GPS → Backend → TourAPI

권한 거부 시 지역 직접 선택을 제공한다.

22. Compatibility와 PetFit

두 개를 분리한다.

Compatibility = 갈 수 있는가?
PetFit = 갈 수 있는 장소 중 얼마나 잘 맞는가?

초기 PetFit 후보:

Compatibility

거리

날씨

반려견 성향

실내/야외 선호

방문 데이터가 없는 초기 구현에서 임의의 방문 기반 점수를 만들지 않는다.

23. 오늘 뭐하지?

홈의 반복 사용 핵심 기능이다.

대표 반려견 + 날씨 + 거리 + Compatibility + PetFit을 이용한다.

예:

두부와 오늘 뭐하지?

현재 23℃
오후부터 비 예보

오늘 추천
- 실내 이용 가능 장소
- 가까운 야외 산책 장소

24. AI / Agent 정책 — 최신 확정

AI는 사실 source가 아니다.

Parser와 Compatibility는 AI를 사용하지 않는다.

Parser = Java 결정론적 규칙 엔진
Compatibility = Java 결정론적 판정 엔진

AI는 다음 역할만 한다.

사용자 자연어 의도 이해

검증된 후보 중 추천

추천 이유 설명

코스 구성 보조

기본 흐름:

사용자 자연어 요청
↓
KorPetTourService2 실제 후보
↓
PetRule
↓
Compatibility
↓
UNAVAILABLE 제거
↓
날씨 + 거리 + PetFit
↓
검증된 후보
↓
OpenAI
↓
추천 이유 / 코스 설명

AI가 생성/결정하면 안 되는 것:

존재하지 않는 장소

공식 규정

체중 제한

Compatibility

날씨 수치

Agent 서버 정책

별도 Python Agent Server를 만들지 않는다.

별도 Agent 서버를 추가하지 않는다.

현재 권장 구조:

React
↓
Spring Boot
↓
OpenAI API

Spring Boot가 OpenAI API를 호출한다.

AI 장애가 다음 기능을 막으면 안 된다.

Place Search

Place Detail

PetRule

Compatibility

M1에는 AI가 없어도 서비스 핵심 문제 해결이 완성되어야 한다.

25. Course / 출발 전 재검증

M3에서 구현한다.

기능:

코스 생성

장소 추가/삭제

순서

방문 시간

체류 시간

장소별 Compatibility

AI 코스 보완

지도

핵심:

[출발 전 다시 확인]

동작:

course_place.tour_content_id
↓
KorPetTourService2 최신 detail 재호출
↓
PetRule 재생성
↓
Compatibility 재판정
↓
규정 변경 / 주의사항 안내

이 기능은 지정과제의 헛걸음 방지와 직접 연결된다.

26. OpenAPI 저장 정책

장소 Source of Truth:

KorPetTourService2

저장:

회원

반려견

코스

코스 장소 최소 snapshot

tour_content_id

마지막 Compatibility snapshot

외부 API 호출 로그

저장하지 않음:

전체 관광지 Master

전체 기상예보

전체 AI 후보

전체 AI 응답 히스토리

사용자 GPS 이력

실제 API 응답 일부를 테스트 fixture로 저장할 수 있다.

fixture 목적:

DTO contract 테스트

Parser regression 테스트

실제 표현 회귀 테스트

Production source로 fixture를 사용하지 않는다.

27. 데이터 출처 정책

PetWay가 생성한 해석 결과와 공식 원문은 분리한다.

UI 예:

[PetWay 판정]
두부는 조건을 확인하면 이용할 수 있어요.

✓ 체중 조건 충족
! 목줄 필요
! 일부구역 이용

----------------

[공식 원문]
20㎏ 미만 동반 가능
목줄 착용
일부구역 동반가능

출처: ⓒ한국관광공사

금지:

PetWay를 한국관광공사 공식 서비스처럼 표현

공사 로고 임의 사용

KTO PetWay 같은 브랜딩

TourAPI만 출처처럼 단독 표기

28. 개발 우선순위

M1 — 핵심 문제 해결

Auth 안정화

다견 Pet CRUD

대표 반려견

Pet size_type 보완

KorPetTourService2 장소 검색

detailCommon2

detailPetTour2

실제 response DTO

PetTour Raw Adapter

실데이터 기반 PetRule Parser 리팩터링

Compatibility Engine

판정 reason

준비물

Place List / Detail Frontend

공식 원문 / 출처 UI

실제 핵심 E2E 통합 테스트

M1 완료 기준:

로그인
→ Pet 선택
→ 장소 검색
→ 실제 OpenAPI
→ 상세
→ Parser
→ Compatibility
→ reason / 준비물
→ 공식 원문

M2 — 반복 사용

기상청

Browser 거리

PetFit

오늘 뭐하지?

Kakao Map

M3 — 여행

Course

시간/순서

AI 코스

날씨 검증

출발 전 재검증

M4 — 발전 기능

사용자 현장 제보

관리자 검토

AI 제보 요약/분류/공식정보 충돌 탐지

정보 신뢰도

다견 동시 판정

즐겨찾기

방문기록

비교

추천 고도화

M4 때문에 M1~M3를 불안정하게 만들지 않는다.

29. 현재 팀 작업 분배 권장

기능을 Layer가 아니라 Vertical Slice로 나눈다.

Pet Slice

Pet Domain
→ Repository
→ Service
→ API
→ Pet UI

현재 상당부분 구현됨.

Place Slice

KorPetTourService2
→ Search
→ Common Detail
→ Pet Detail
→ Place API

Pet Intelligence Slice

Raw Adapter
→ PetRule
→ Parser
→ Compatibility
→ Reason

Core UX Slice

Home
→ Place List
→ Place Detail
→ Compatibility UI
→ Official Raw UI

M1 이후:

Context: Weather / Distance / PetFit

Journey: Course / AI / Revalidation

30. Contract First

AI/Codex 구현 전에 contract를 먼저 고정한다.

권장:

docs/contracts/
├ pet-api.md
├ place-api.md
├ pet-rule.md
└ compatibility.md

각 contract에는 최소 다음을 포함한다.

입력

출력

상태

에러

완료 조건

테스트 기준

31. AI Native 개발 흐름

Contract
↓
Codex 구현
↓
Codex 테스트
↓
Read-only Codex Review
↓
사람이 CRITICAL / MAJOR 확인
↓
PR
↓
main merge
↓
자동 배포

Review Agent는 파일 수정 없이 다음을 검사한다.

Contract 위반

다른 feature 침범

실제 API와 다른 추측

보안 문제

테스트 누락

과설계

merge blocker

등급:

CRITICAL

MAJOR

MINOR

32. 현재 PR / Branch 주의사항

PR #7 / feature/place-search

현재 main에 병합하지 않은 상태를 기준으로 한다.

기존 구현:

KorService2
areaBasedList2
GET /api/places
RestClient
timeout
Gateway
Service
Controller
mock/fixture tests

최신 기준:

구조 재사용 가능

KorPetTourService2로 API 계약 재검증 필요

실제 response DTO 재검증 필요

실제 승인 key smoke test 필요

현재 main Auth/Pet 구조와 충돌 여부 확인 필요

구 KorService2 상태 그대로 merge하지 않는다.

33. 배포 전략

목표:

최소 운영 복잡도 + 빠른 자동배포 + 심사 URL 안정성

현재 권장 구조:

GitHub main
↓
Cloud 자동배포
↓
React + Spring Boot
↓
MySQL
↓
제출 URL

세션 기반 인증을 사용하므로 운영에서는 가능한 한 single-origin 구조를 선호한다.

예:

Spring Boot가 React production build 서빙
/api/**
동일 Origin

장점:

운영 CORS 단순화

JSESSIONID cookie 처리 단순화

제출 URL 하나

프론트/백 버전 mismatch 감소

배포 플랫폼은 팀 최종 결정 전 특정 서비스에 종속시키지 않는다.

Railway, AWS, GCP 등 빠르고 안정적인 자동배포 수단을 사용할 수 있다.

공모전 안내의 AWS/GCP 프리티어 권장은 특정 클라우드 사용 강제가 아니다.

배포 Secret:

DB_URL
DB_USERNAME
DB_PASSWORD
TOUR_API_KEY
WEATHER_API_KEY
OPENAI_API_KEY

운영 secret을 Git에 저장하지 않는다.

34. Config / Secret 정책

실제 tracked application.yml은 현재 의도적으로 없다.

Codex는 이를 실수로 판단해 자동 복구하지 않는다.

현재 local 개발에서는 ignore된 local config / 환경설정을 사용할 수 있다.

주의:

일반 .env는 Spring Boot가 자동으로 읽는다고 가정하지 않는다.

필요 시 Run Configuration / OS env / Spring local profile로 전달한다.

신규 clone 시 필요한 datasource/profile/schema 절차를 문서화한다.

Codex가 .env를 실제 API 테스트에 사용할 때:

값 출력 금지

길이 출력 금지

일부 문자열 출력 금지

요청 URL에 key 포함 출력 금지

.env 수정 금지

Git add 금지

Frontend:

VITE_API_URL

을 사용한다.

기능별 API 파일에서 localhost 포트를 개별 하드코딩하지 않는 방향으로 통일한다.

35. 필수 테스트

TourAPI

실제 승인 Key smoke test

areaBasedList2

detailCommon2

detailPetTour2

HTTP 4xx

HTTP 5xx

timeout

HTTP 200 + external resultCode 실패

빈 결과

items.item array

"" 빈 문자열

예상 못한 field

반려동물 상세 없음

Parser — 실제 fixture 기반

최소:

전구역 동반가능
일부구역 동반가능
20㎏ 미만
9kg 이하
목줄 착용
맹견 입마개 필수
배변봉투 지참 및 배변처리
이동장(켄넬)사용
야외 공간만 동반 가능
중소형견
사전 문의
마릿수 제한

반드시:

kg / ㎏

이하 / 미만

체중→크기 추론 금지

중소형견 오인식 금지

source field 보존

빈 문자열 unknown 오판 금지

unknown fragment 보존

복수 WeightLimit

Compatibility

예:

7kg + 10kg 이하
→ AVAILABLE
(추가 조건이 있으면 CONDITIONAL)

12kg + 10kg 이하
→ UNAVAILABLE

7kg + 목줄 착용
→ CONDITIONAL

실내 이용 개별 문의
→ NEED_CONFIRMATION

Security

다른 사용자 Pet 접근 금지

다른 사용자 Course 접근 금지

USER ADMIN API 접근 금지

일반 회원가입 ADMIN 생성 금지

비밀번호 원문 저장 금지

AI

UNAVAILABLE 후보 OpenAI 전달 금지

AI가 공식 규정 생성 금지

AI가 Compatibility 변경 금지

36. Parser 검증

최종 Parser는 실제 detailPetTour2 데이터를 사용해 검증한다.

목적:

실 관광데이터 기반 규칙 엔진임을 입증

규칙별 추출 정확도 확인

unknown / NEED_CONFIRMATION 정책 검증

숫자는 실제 측정한 값만 사용한다.

임의 정확도 생성 금지.

37. M4 현장 제보

발전 기능.

사용자 현장 제보
↓
place_report
↓
AI
- 요약
- 유형 분류
- 공식정보와 충돌 탐지
  ↓
  관리자 검토
  ↓
  APPROVE / REJECT / NEED_VERIFICATION

AI는 공식 정보를 자동 수정하지 않는다.

화면:

[공식 관광정보]

[최근 현장 제보]

를 명확히 구분한다.

38. Git 규칙

main 직접 개발 금지

main 직접 push 금지

기능별 branch

PR 병합

작업 전 main/remote 최신 상태 확인

완료 전 전체 테스트

Codex 임의 merge 금지

Codex 임의 reset 금지

기존 사용자 수정사항 보호

branch naming은 팀 최신 규칙을 따른다.

이미 존재하는 feature/* branch를 문서 규칙만으로 강제 rename하지 않는다.

39. Codex 기본 시작 프롬프트

PETWAY_CODEX_CONTEXT.md를 전체 읽어라.

현재 branch가 main이면 코드를 수정하지 말고 중단하라.

현재 working tree, remote branch, Repository 구조,
이번 기능과 중복되는 구현을 먼저 조사하라.

실제 코드를 수정하기 전에:

1. 현재 구현 상태
2. 이번 작업 범위
3. 수정 예상 파일
4. 테스트/완료 조건

을 먼저 보고하라.

KorPetTourService2의 공식 명세와 실응답을 최우선 기준으로 사용하고
확인되지 않은 필드/규칙을 추측하지 마라.

현재 구현된 Auth/Pet/PetRule을 중복 구현하지 마라.

사용자가 요청하지 않은 M4, 신규 DB 테이블,
신규 framework/dependency를 임의로 추가하지 마라.

기존 사용자의 local 변경을 reset/revert하지 마라.

작업 후 실제 build/test를 실행하고 결과를 보고하라.

명시적 요청 없이는 commit/push/merge하지 마라.

40. 핵심 시연 시나리오

1. 로그인

2. 반려견 등록
   두부 / 8kg / 소형견

3. 대표 반려견 선택

4. 지역/키워드 검색

5. KorPetTourService2 실제 호출

6. 장소 선택

7. detailCommon2

8. detailPetTour2

9. PetTour Raw Adapter

10. PetRule Parser

11. Compatibility

12. "두부가 갈 수 있나요?"
    상태 + 판정 근거

13. 방문 준비물

14. 공식 원문

15. 출처

16. 날씨 / 거리

17. 오늘 뭐하지? 또는 코스

18. AI 사용 시 검증된 실제 후보만 전달

19. 사용자 확인 후 코스 저장

20. 출발 전 tour_content_id로 최신 규정 재검증

이 시나리오가 끊기지 않는 것이 가장 중요하다.

41. 개발 완료 판단 기준

기능은 코드가 존재한다고 완료가 아니다.

완료 조건:

실제 API 또는 fixture 기반 검증

관련 unit/integration test 통과

핵심 E2E에서 사용 가능

공식 원문/근거 추적 가능

다른 feature를 깨지 않음

배포 환경에서도 동작 가능

42. 한 문장 요약

PetWay는 한국관광공사 KorPetTourService2의 실제 반려동물 동반 데이터를 필드 문맥을 보존해 PetRule로 구조화하고, 마이펫 기준 Compatibility와 판정 근거·준비물을 제공한 뒤, 날씨·거리·오늘 외출·여행 코스·출발 전 재검증까지 연결하며, AI에는 검증된 실제 장소만 전달하는 반려생활 맞춤형 관광 서비스다.

43. 2026-09-04 최신 main 구현 상태 체크리스트

이 절은 앞선 설계 원칙과 과거 구현 상태를 삭제하거나 변경하지 않고 최신 `main`의 추가 구현 상태를 기록한다.

체크 기준:

- [x] 현재 production code에 구현되어 있고 관련 build 또는 test가 통과함
- [ ] 미구현이거나 runtime 연결, 실제 외부 API, 핵심 E2E 또는 배포 검증이 남음

43-1. 현재 검증 결과

- [x] Backend 전체 테스트 통과
- [x] Frontend production build 통과
- [x] 최신 `main` working tree clean 상태 확인
- [ ] 실제 제출 환경의 전체 외부 API smoke test
- [ ] 로그인부터 일정 저장까지 핵심 E2E 브라우저 반복 검증
- [ ] 실제 배포 URL 검증

43-2. Auth / User

- [x] 일반 회원가입
- [x] 세션 로그인 및 로그아웃
- [x] `/api/auth/me`
- [x] BCrypt
- [x] Kakao OAuth
- [x] `USER`, `ADMIN`
- [x] 일반 회원가입을 통한 `ADMIN` 직접 생성 차단 구조
- [ ] 운영 CSRF 전략
- [ ] 운영 Cookie 및 SameSite/Secure 검증
- [ ] 공모전 테스트 전용 `USER` 계정

43-3. Pet

- [x] 다견 CRUD
- [x] 대표 반려견
- [x] `size_type`
- [x] 동물등록번호 또는 RFID 기반 외부 등록정보 조회
- [x] 외부 조회 결과를 Pet 등록 폼에 자동 입력
- [x] 자동 입력 이후 사용자 확인을 거친 Pet 저장
- [x] 조회조건과 원본 외부 응답을 Pet DB에 저장하지 않음
- [ ] 체중 소수점 최대 2자리 허용 여부 팀 결정
- [ ] 체중 소수 허용 시 `9.99`, `10.00`, `10.01` 경계 테스트

43-4. Place / Map

- [x] KorPetTourService2 `areaBasedList2`
- [x] KorPetTourService2 `locationBasedList2`
- [x] KorPetTourService2 `detailPetTour2`
- [x] 지역 기반 장소 조회
- [x] 위치 기반 주변 장소 조회
- [x] Kakao Map marker
- [x] Browser Geolocation
- [x] 문화정보원 CSV 보조 데이터 Catalog 코드
- [x] 관광공사와 문화정보원 출처 필드 응답
- [ ] KorPetTourService2 `detailCommon2`
- [ ] 문화정보원 `csv-*` contentId의 상세 및 Compatibility 조회
- [ ] 출처별 contentId 계약
- [ ] 혼합 출처 중복 제거
- [ ] 혼합 결과 `page`, `size`, `totalCount` 계약
- [ ] 문화정보원 CSV 배포 공급 방식
- [ ] Frontend `/places` 비로그인 Route
- [ ] 공개 장소·날씨와 인증 필요 Compatibility·WalkFit UI 분리

43-5. PetTour Raw / PetRule Parser

- [x] `acmpyTypeCd` source 보존
- [x] `acmpyPsblCpam` source 보존
- [x] `acmpyNeedMtr` source 보존
- [x] `etcAcmpyInfo` source 보존
- [x] `relaAcdntRiskMtr` source 보존
- [x] kg와 Unicode ㎏ 처리
- [x] 이하와 미만 연산자 분리
- [x] 복수 WeightLimit
- [x] 소형·중형·대형·중소형견
- [x] 전구역·일부구역·실내·야외·테라스 범위
- [x] 목줄·입마개·배변봉투·배변처리·이동장·유모차·매너벨트
- [x] source field와 unparsed fragment 보존
- [x] `relaPosesFclty` source 보존
- [x] `relaFrnshPrdlst` source 보존
- [x] `relaPurcPrdlst` source 보존
- [x] `relaRntlPrdlst` source 보존
- [ ] 보호자 1인당 반려견 마릿수 제한
- [x] 예방접종
- [x] 사전 예약
- [x] 사전 문의
- [ ] 추가 요금
- [ ] 연령 조건

43-6. Compatibility

- [x] `AVAILABLE`
- [x] `CONDITIONAL`
- [x] `NEED_CONFIRMATION`
- [x] `UNAVAILABLE`
- [x] 체중 조건 판정
- [x] 크기 조건 판정
- [x] 공간 범위 판정
- [x] 준비사항과 reason 응답
- [x] 명확하지 않은 규정의 `NEED_CONFIRMATION` 처리
- [x] 공식정보와 PetWay 판정 분리 UI
- [ ] 최소 체중 미달 reason의 깨진 한글 수정
- [ ] 실제 등록 견종의 위험견 여부와 대상 requirement 비교
- [x] 예약·문의 확인 조건 판정
- [x] 예방접종 조건 판정
- [ ] 마릿수·요금 조건 판정
- [ ] 판정 기준 반려견을 장소 화면에서 변경하는 UI
- [ ] 여러 반려견 동시 판정

43-7. Weather / WalkFit

- [x] D0~D3 단기예보
- [x] D4~D10 중기예보
- [x] D11~D90 3개월 전망
- [x] 전북자치도 지역명 처리
- [x] 예보 발표 경계 처리
- [x] 날씨 전용 HTTP Client
- [x] 비로그인 `GET /api/weather`
- [x] 대표 반려견과 Compatibility 기반 WalkFit
- [x] 거리·장소 유형·날씨 반영
- [x] 최대 8개 후보 평가 및 반환
- [x] provider 전체 429와 일부 실패 분리
- [ ] 실제 기상청 Key 기반 배포 smoke test
- [ ] 비로그인 장소 화면과 로그인 WalkFit 표시 정책 적용

43-8. KTO quota 보호

- [x] `locationBasedList2` 2분 bounded cache
- [x] cache 최대 256 entries
- [x] 좌표 소수점 3자리 정규화
- [x] 실패·429·timeout 응답 cache 제외
- [x] `detailPetTour2` cache
- [x] 동일 contentId cold request single-flight
- [x] TravelCourse nearby 최대 5 page
- [x] unique 후보 60개 확보 시 early-stop
- [x] Compatibility 최대 45개 시도
- [x] eligible 후보 30개 확보 시 early-stop
- [x] TravelCourse cold worst case 최대 약 50 KTO 호출 구조
- [ ] 실제 API 호출량 logging 및 측정
- [ ] 제출 환경 429 회귀 smoke test
- [ ] 현재 미사용 `searchAllPlaces` 최대 50 page 경로의 재사용 방지 또는 상한 재검토

43-9. Course / Route / AI

- [x] 여행 코스 Builder
- [x] 여행일별 장소 구성
- [x] 날짜별 날씨
- [x] Kakao Mobility 자동차 경로
- [x] ODsay 대중교통 경로
- [x] OpenAI structured recommendation client
- [x] 입력 후보와 AI 응답 validator
- [x] `UNAVAILABLE` AI 결과 차단
- [x] AI 장애 시 heuristic fallback
- [x] `saved_trip` 및 itinerary JSON 저장 Backend API
- [x] 저장 일정 목록·상세·삭제 화면
- [ ] 여행 코스 화면의 `saveCurrentTrip` 실행 버튼
- [ ] AI 결과와 fallback 결과의 명시적 상태 필드 및 배지
- [ ] 클라이언트가 아닌 서버에서 실제 TourAPI 후보 검증
- [ ] 정식 `course`, `course_place` 구조 또는 현재 JSON snapshot 정책 확정
- [ ] 장소별 Compatibility snapshot
- [ ] 저장 일정 수정
- [ ] 출발 전 `tour_content_id` 최신 규정 재호출
- [ ] 재호출 후 PetRule 재생성 및 Compatibility 재판정
- [ ] 규정 변경 전후 안내

43-10. Favorite / Review / Hospital / M4

- [x] 즐겨찾기 Backend와 Frontend
- [x] 리뷰 Backend와 Frontend
- [x] 동물병원 및 동물약국 Backend와 Frontend
- [x] 행정안전부 동물병원 API Client
- [x] 문화정보원 병원·약국 Catalog 코드
- [ ] Frontend가 전달하는 `facilityType`과 Backend source 분기 수정
- [ ] CSV가 없는 환경의 병원 fallback
- [ ] 문화정보원 CSV가 없는 환경의 동물약국 처리
- [ ] 사용자 현장 제보
- [ ] 관리자 제보 검토
- [ ] AI 제보 요약·분류·공식정보 충돌 탐지
- [ ] 정보 신뢰도
- [ ] 방문 기록
- [ ] 장소 비교

43-11. Docs / Test / Deployment

- [x] `README.md` 존재
- [x] `docs/README.md` 존재
- [x] Backend 단위·Controller·Security 테스트 실행 가능
- [x] Frontend TypeScript production build 실행 가능
- [ ] `README.md` 기존 초기 골격 설명의 최신화 여부 결정
- [ ] 실제 환경변수 목록 최신화
- [ ] `docs/contracts/pet-api.md`
- [ ] `docs/contracts/place-api.md`
- [ ] `docs/contracts/weather-api.md`
- [ ] `docs/contracts/pet-rule.md`
- [ ] `docs/contracts/compatibility.md`
- [ ] `docs/contracts/course-api.md`
- [ ] `docs/deployment.md`
- [ ] Favorite 전용 테스트
- [ ] Review 전용 테스트
- [ ] SavedTrip 전용 테스트
- [ ] Kakao Directions Client 테스트
- [ ] ODsay Transit Client 테스트
- [ ] 문화정보원 CSV loading 테스트
- [ ] 혼합 출처 pagination 테스트
- [ ] Frontend component 및 사용자 흐름 자동 테스트
- [ ] 핵심 E2E 브라우저 smoke test
- [ ] 외부 API 실제 호출 smoke test
- [ ] 운영 Secret 공급 절차
- [ ] CSRF, Cookie, CORS 운영 정책
- [ ] `api_request_log`
- [ ] 공모전 테스트 계정
- [ ] 자동배포와 제출 URL 반복 검증

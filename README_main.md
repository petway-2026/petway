# PetWay

> 오늘 산책부터 주말 여행까지, 우리 아이와 갈 수 있는 곳을 한 번에.

PetWay는 한국관광공사의 반려동물 동반여행 OpenAPI를 실시간으로 활용해 장소별 비정형 동반 조건을 구조화하고, 사용자의 반려견 정보와 비교해 실제 이용 가능 여부와 필요한 준비사항을 설명한 뒤 날씨·거리·일상 외출·여행 코스로 연결하는 반려생활 맞춤형 관광 서비스입니다.

PetWay의 핵심은 단순히 반려동물 동반 장소를 보여주는 것이 아닙니다.

> 내 반려견이 실제로 갈 수 있는지를 방문 전에 판단하고, 그 이유와 필요한 준비물을 설명한 뒤 실제 외출과 여행으로 연결합니다.

## 이 문서의 범위와 작성 기준

이 문서는 다음 네 자료를 하나의 메인 README로 통합한 문서입니다.

1. Notion 메인 페이지와 그 안의 기획서·기능 요구사항·비기능 요구사항·ERD·패키지 구조·관광공사 API 활용 방식·공모전 중요자료
2. `README_before.md`
3. `docs/README.md`
4. `PETWAY_CODEX_CONTEXT.md`

추가로 현재 Repository 코드를 읽어 문서의 구현 체크 여부를 검증했습니다.

확인 한계도 숨기지 않습니다.

- Notion의 `패키지구조` 하위 페이지는 제목 외 본문이 비어 있었습니다.
- Notion 기획서와 ERD의 일부 코드 블록은 열린 화면에서 `Plain Text 코드를 불러오는 중…` 상태여서 블록 원문을 확인할 수 없었습니다. 주변 설명으로 내용을 만들어 채우지 않았습니다.
- 공모전 중요자료의 접힌 `전체 추진 일정`, `1차 심사 안내`, `최종 심사 안내`, `시상 내역`의 세부 본문은 열린 상태에서 노출되지 않아 이 문서에 임의로 작성하지 않았습니다.
- 팀원 소개에는 이름과 개인 연락처가 있었지만, 공개 Repository의 메인 README 용도를 고려해 전화번호와 이메일은 옮기지 않았습니다.

문서끼리 충돌하는 경우에는 해당 절에서 각 출처의 내용을 분리하고 바로 다음에 최선의 방안을 작성합니다. 최선의 방안은 구현 완료 사실이 아니라 현재 기준의 권장안이며, 팀 결정이 필요한 항목은 그대로 미확정으로 표시합니다.

---

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [공모전 및 데이터 활용 원칙](#공모전-및-데이터-활용-원칙)
- [핵심 사용자 흐름](#핵심-사용자-흐름)
- [핵심 도메인](#핵심-도메인)
- [현재 구현 상태](#현재-구현-상태)
- [목표 요구사항과 현재 코드의 차이](#목표-요구사항과-현재-코드의-차이)
- [데이터 및 ERD](#데이터-및-erd)
- [기술 스택과 구조](#기술-스택과-구조)
- [로컬 실행](#로컬-실행)
- [환경변수](#환경변수)
- [테스트](#테스트)
- [개발 우선순위](#개발-우선순위)
- [개발 원칙](#개발-원칙)
- [관련 문서](#관련-문서)

---

## 프로젝트 개요

### 해결하려는 문제

관광정보에 표시되는 `반려동물 동반 가능`은 모든 반려견이 모든 공간을 이용할 수 있다는 의미가 아닙니다.

실제 장소에는 다음과 같은 서로 다른 조건이 존재합니다.

- 10kg 이하 또는 15kg 미만 등 체중 제한
- 소형견만 가능, 대형견은 야외만 가능 등 크기·공간 조건
- 실내 이용 시 이동장 필요
- 테라스 또는 일부 구역만 동반 가능
- 목줄, 입마개, 배변봉투, 매너벨트 등 준비물
- 보호자 1인당 허용 마릿수 또는 팀당 최대 마릿수
- 예방접종, 사전 예약, 문의, 추가 요금 등 부가 조건

이 정보는 주로 자연어로 제공되므로 사용자가 원문을 직접 읽고 자신의 반려견이 실제로 이용할 수 있는지 다시 판단해야 합니다. PetWay는 이 비정형 정보를 규칙으로 구조화하고, 반려견과 방문 조건을 비교하여 설명 가능한 결과를 제공합니다.

### 주요 사용자

- 일상적으로 반려견과 산책하거나 가까운 동반 장소를 찾는 사용자
- 반려견과 당일치기 또는 주말여행을 계획하는 사용자
- 체중, 공간, 이동장 등 출입 조건 확인이 중요한 사용자
- 여러 반려견을 등록하고 함께 방문할 장소를 찾는 사용자

### 핵심 가치

- 관광공사 데이터를 단순 노출하지 않고 실제 방문 판단에 사용합니다.
- 장소 중심이 아니라 반려견과 방문 조건 중심으로 판정합니다.
- 입장 가능 여부인 `Compatibility`와 추천 순위인 `PetFit/WalkFit`을 분리합니다.
- 공식 원문과 PetWay의 해석을 구분하여 보여줍니다.
- 명확하지 않은 조건은 임의로 단정하지 않습니다.
- 일상 외출과 여행 계획을 하나의 흐름으로 연결합니다.

---

## 공모전 및 데이터 활용 원칙

PetWay는 2026 관광데이터 활용 공모전 웹·앱 구현 부문의 지정과제 6번을 기준으로 합니다.

> 모호한 반려동물 출입 조건 및 규정 인지 오류로 발생하는 현장 입장 거부와 헛걸음을 줄입니다.

### 심사 관점

| 항목 | 배점 | PetWay 대응 |
| --- | ---: | --- |
| 서비스 구현성 | 30 | 핵심 E2E 완결성, 안정적인 외부 API 연동, 배포 품질 |
| 서비스 기획력 | 30 | 마이펫 기준의 실제 이용 가능성 판단 |
| 데이터 활용 적절성 | 20 | KorPetTourService2를 검색·판정·추천·코스에 직접 활용 |
| 서비스 발전성 | 20 | 날씨, WalkFit, 여행 코스, 방문 기록, 현장 제보 |

### 한국관광공사 데이터 원칙

- 한국관광공사 OpenAPI 활용은 필수입니다.
- 파일 데이터만으로 관광공사 OpenAPI를 대체하지 않습니다.
- 장소 검색, 상세, 동반 조건, 추천 후보, 코스 후보, 출발 전 재검증에 실제 OpenAPI를 사용합니다.
- 관광공사 전체 장소를 PetWay의 로컬 Master DB로 복제하지 않습니다.
- 저장이 필요한 경우 `contentId`와 최소 Snapshot만 보관합니다.
- 인증키와 전체 요청 URL을 클라이언트, 로그, 응답, Git에 노출하지 않습니다.
- 화면에는 `출처: ⓒ한국관광공사` 또는 공식 지침에 맞는 기관명 텍스트를 표시합니다.
- 한국관광공사의 공식 CI/BI 로고를 임의로 사용하거나 공식 서비스처럼 브랜딩하지 않습니다.
- 문화정보원 CSV 등 다른 데이터는 보조 자료이며 KTO OpenAPI의 역할을 대체하지 않습니다.

### 외부 API 활용 지점

| 기능 | 데이터/API |
| --- | --- |
| 지역별 장소 검색 | KorPetTourService2 `areaBasedList2` |
| 현재 위치 주변 검색 | KorPetTourService2 `locationBasedList2` |
| 반려동물 동반 조건 | KorPetTourService2 `detailPetTour2` |
| 장소 공통 상세 | KorPetTourService2 `detailCommon2` 예정 |
| 여행일 날씨 | 기상청 단기·중기·3개월 전망 |
| 지도 | Kakao Map JavaScript SDK |
| 자동차 경로 | Kakao Mobility |
| 대중교통 경로 | ODsay |
| 코스 보완 | OpenAI API |
| 동물등록정보 불러오기 | 농림축산검역본부 동물등록 정보조회 서비스 |
| 동물병원 | 행정안전부 공공데이터 API 및 문화정보원 보조 데이터 |

### `PETWAY_CODEX_CONTEXT.md`의 KorPetTourService2 operation 범위

| Operation | 문서상 용도 | 현재 코드 확인 |
| --- | --- | --- |
| `areaBasedList2` | 지역 기반 장소 목록 | [x] 구현 |
| `locationBasedList2` | 위치 기반 주변 장소 | [x] 구현 |
| `searchKeyword2` | 키워드 검색 | [ ] 현재 Client 호출 없음 |
| `detailPetTour2` | 반려동물 동반 상세 | [x] 구현 |
| `detailCommon2` | 장소 공통 상세 | [ ] 미구현 |
| `detailIntro2` | 콘텐츠 유형별 소개 | [ ] 필요 시 사용, 미구현 |
| `detailInfo2` | 반복 상세정보 | [ ] 필요 시 사용, 미구현 |
| `detailImage2` | 장소 이미지 | [ ] 필요 시 사용, 미구현 |
| `petTourSyncList2` | 반려동물 관광정보 동기화 목록 | [ ] 현재 서비스 경로 없음 |
| `areaCode2` | 지역 코드 | [ ] 현재 서비스 경로 없음 |
| `categoryCode2` | 카테고리 코드 | [ ] 현재 서비스 경로 없음 |
| `ldongCode2` | 법정동 코드 | [ ] 현재 서비스 경로 없음 |
| `lclsSystmCode2` | 분류체계 코드 | [ ] 현재 서비스 경로 없음 |

기존 `KorService2` 기반 구현의 HTTP Client, timeout, 예외, DTO 분리, Gateway·Service·Controller 구조는 재사용할 수 있지만 신규 장소 기능의 기준 API는 `KorPetTourService2`입니다.

### 공모전 중요자료에서 확인한 유의사항

- 동일 서비스로 한국관광공사 관광데이터 활용 공모전 수상 또는 공사 주관 개발·구축 지원을 받은 경우 심사 제외 대상입니다.
- 동일 서비스로 웹·앱 구현 부문과 생성형 AI 활용 관광 프롬프톤 부문 등에 중복 지원할 수 없습니다.
- 지정과제는 제출 시 하나를 선택하며, 선택한 지정과제의 핵심 해결과제와 한국관광공사 OpenAPI 활용이 드러나야 합니다.
- 파일 형태 관광데이터만 활용하면 인정되지 않으며, 운영계정 신청정보와 실제 API 호출 내역 및 서비스 활용 내역을 확인합니다.
- 생성형 AI와 AI 코딩 도구 사용 자체에는 제한이 없지만 평가의 핵심은 안정적으로 구동되는 완제품입니다.
- 웹은 최종 서비스 URL을 제출하고, 앱은 제출 기한 내 앱스토어 등록과 승인이 필요합니다.
- 두 종류 이상의 서비스 형태를 개발할 경우 동일 내용의 서비스여야 합니다.
- 제출서류 누락, 작성 가이드 미준수, 마감기한 미준수, 연락 불가에 따른 불이익은 참가자 책임입니다.
- 서비스와 제출물의 소유권·초상권·저작권·표절 등 법적 책임은 참가자에게 있습니다.
- 허위 또는 결격사유가 확인되면 평가 제외, 수상 취소, 상금 회수가 가능합니다.
- 외부 전문 심사위원의 세부 평가 결과는 공개되지 않으며 적격대상이 없으면 선정 규모가 바뀔 수 있습니다.
- 개발비, 도메인, 장비, 서버비에 대한 별도 지원은 없으며 공식 자료는 AWS·GCP 등의 프리티어 활용을 권장합니다.
- 서비스는 한국관광공사가 운영하지 않으며 공사 명칭·KTO·공식 로고로 공식 서비스처럼 보이게 해서는 안 됩니다.
- 개인위치정보를 서버로 전송하면 저장 여부와 관계없이 신고 대상이 될 수 있다는 안내가 있으며, 정확한 판단은 위치정보지원센터의 공식 검토가 필요합니다.
- 공식 출처 기관명 텍스트 표기는 필수이고 공식 CI/BI 이미지의 무단 사용은 허용되지 않습니다.

공모전 중요자료에 표시된 문의처:

- 공모전 운영: `gongmo@stunning.kr`, `070-4060-4046` — 평일 10:00~17:00, 점심 12:30~13:30 제외
- OpenAPI: `tourapi@knto.or.kr`, `070-4287-3219` — 평일 09:00~18:00, 점심 12:00~13:00 제외
- 위치정보지원센터: `www.lbsc.kr`, `02-588-0185`

최신 일정은 변동 가능하므로 [Notion 원문](https://app.notion.com/p/3b239ad79c5e804095c0c9ffcc871116)에서 다시 확인합니다. 열린 화면에서 접힌 일정·심사·시상 세부 내용은 확인할 수 없었으며 추정하지 않았습니다.

---

## 핵심 사용자 흐름

```text
회원가입 / 로그인
        ↓
반려견 등록 또는 동물등록정보 불러오기
        ↓
대표 반려견 선택
        ↓
지역 또는 현재 위치 기반 장소 검색
        ↓
KorPetTourService2 실제 호출
        ↓
장소 상세 + 반려동물 동반 상세
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
공식 원문 + 출처 확인
        ↓
날씨 / 거리 / WalkFit / 여행 코스
        ↓
사용자 확인 후 일정 저장
        ↓
출발 전 최신 규정 재검증
```

비로그인 사용자는 장소·지도·날씨를 확인할 수 있고, 반려견 등록·맞춤 Compatibility·WalkFit·일정 저장 등 사용자 데이터가 필요한 기능은 로그인이 필요합니다.

---

## 핵심 도메인

### Pet

사용자는 여러 반려견을 등록하고 대표 반려견을 선택할 수 있습니다.

주요 정보:

- 이름, 견종, 생년월일, 체중
- 체형, 활동 수준, 실내·야외 선호
- 혼잡 환경 적응도, 다른 반려견과의 사회성
- 예방접종 및 중성화 여부 확장 가능

입장 판정에서는 실제 체중을 우선 사용합니다. `sizeType`이 존재하더라도 장소 규정을 임의의 소형·중형·대형 기준으로 바꾸거나 체중을 크기로 추론해서는 안 됩니다.

### PetRule Parser

`detailPetTour2`의 자연어 정보를 다음 규칙으로 구조화합니다.

- 최소·최대 체중과 `미만 / 이하 / 초과 / 이상`
- 전구역·일부 구역·실내·야외·테라스
- 목줄·입마개·이동장·유모차·배변봉투·매너벨트
- 보호자 수 대비 반려견 수와 팀당 최대 마릿수
- 예방접종·견종·연령·예약·문의·추가 요금
- 파싱하지 못한 원문 조각과 원본 필드

PetRule은 관광공사 원문을 수정한 데이터가 아니라 PetWay가 계산에 사용하는 도메인 객체입니다. 모호한 문장은 버리지 않고 원문과 함께 유지합니다.

#### `detailPetTour2` 실응답 기준

`PETWAY_CODEX_CONTEXT.md`에 기록된 2026-08-27 실제 호출 결과는 다음과 같습니다.

- 승인된 KorPetTourService2 Key로 `areaBasedList2` HTTP 200, `resultCode=0000`을 확인했습니다.
- 당시 전체 장소 수는 약 9,686건으로 기록되어 있습니다.
- `detailPetTour2` 실제 호출 성공을 확인했습니다.
- `items.item`은 단건이어도 배열이었습니다.
- 값 없음은 `null`보다 빈 문자열 `""`이 주로 사용됐습니다.
- 빈 문자열은 해당 필드에 정보가 없다는 뜻이며 그 자체를 unknown fragment로 만들지 않습니다.
- 같은 규정이 여러 필드에 중복되거나 필드명과 다른 문맥으로 들어올 수 있으므로 필드별 Raw Adapter와 source field를 보존합니다.

| 원본 필드 | 의미 | 현재 내부 모델 |
| --- | --- | --- |
| `contentid` | 콘텐츠 ID | [x] 보존 |
| `acmpyTypeCd` | 동반 유형 | [x] `accompanimentType` |
| `acmpyPsblCpam` | 동반 가능 동물·용량 | [x] `allowedCapacity` |
| `acmpyNeedMtr` | 동반 시 필요사항 | [x] `requiredItems` |
| `etcAcmpyInfo` | 기타 동반 정보 | [x] `additionalInformation` |
| `relaAcdntRiskMtr` | 사고 대비사항 | [x] `safetyNotice` |
| `relaPosesFclty` | 구비 시설 | [ ] 내부 모델 누락 |
| `relaFrnshPrdlst` | 비치 품목 | [ ] 내부 모델 누락 |
| `relaPurcPrdlst` | 구매 품목 | [ ] 내부 모델 누락 |
| `relaRntlPrdlst` | 렌탈 품목 | [ ] 내부 모델 누락 |

#### 실제 표본 분석

`PETWAY_CODEX_CONTEXT.md`에는 관광지·문화시설·레포츠·숙박·쇼핑·음식점 등을 층화해 약 120건의 실제 상세 데이터를 분석한 결과가 기록되어 있습니다.

| 패턴 | 등장 장소 수 |
| --- | ---: |
| 목줄 | 60 |
| 일부 구역 | 59 |
| 견종·맹견 조건 | 50 |
| 배변 처리 | 48 |
| 입마개 | 37 |
| 이동장·켄넬·케이지 | 19 |
| 체중 | 17 |
| 야외·외부 | 12 |
| 소형견 | 11 |
| 실내·내부 | 9 |
| 예약·문의 | 9 |
| 유모차 | 9 |
| 매너벨트 | 8 |
| 마릿수 제한 | 5 |
| 예방접종 | 3 |
| 요금 | 3 |
| 테라스 | 2 |
| 기저귀 | 0 |

실제 표현에는 `20㎏ 미만`, `9kg 이하`, 목줄, 일부 구역, 전 견종, 맹견 입마개, 배변봉투와 배변처리, 이동장·켄넬, 야외·테라스, 중소형견, 1인당 최대 1마리, 사전 문의·예약이 포함됐습니다. 이 수치는 당시 표본 분석 결과이며 전체 데이터의 통계로 확대 해석하지 않습니다.

### Compatibility

Compatibility는 “이 장소가 반려동물 동반 장소인가?”가 아니라 “현재 반려견과 방문 조건으로 실제 이용 가능한가?”를 판단합니다.

| 상태 | 의미 |
| --- | --- |
| `AVAILABLE` | 현재 입력 조건으로 이용 가능 |
| `CONDITIONAL` | 준비물 또는 추가 조건을 충족하면 이용 가능 |
| `NEED_CONFIRMATION` | 정보가 없거나 모호하여 확인 필요 |
| `UNAVAILABLE` | 명확한 장소 규정과 방문 조건이 충돌 |

판정 결과에는 상태만이 아니라 조건별 근거, 필요한 준비물, 이용 가능한 공간, 공식 원문을 함께 제공합니다.

### PetFit / WalkFit

Compatibility는 입장 가능 여부를 판단하고, PetFit/WalkFit은 이용 가능한 후보 중 현재 반려견에게 더 적합한 장소를 정렬합니다.

현재 고려 요소:

- 대표 반려견과 Compatibility
- 거리와 장소 유형
- 여행일 또는 당일 날씨
- 활동성 등 반려견 프로필

의료적 또는 수의학적 판단이 아니라 기상조건과 장소조건에 따른 외출 참고정보를 제공합니다.

### 여행 코스와 AI

- 지역과 날짜를 기준으로 실제 관광공사 장소 후보를 조회합니다.
- 자동차 또는 대중교통 경로를 확인합니다.
- 날짜별 날씨와 Compatibility를 함께 봅니다.
- AI는 검증된 실제 후보만 재배치하거나 설명해야 합니다.
- AI 장애가 장소 검색, PetRule, Compatibility를 막아서는 안 됩니다.
- AI 실패 시 규칙 기반 fallback을 사용할 수 있지만 실제 AI 결과와 구분해야 합니다.

### 위치정보

- Browser Geolocation으로 사용자의 현재 위치를 가져옵니다.
- 사용자 GPS 이력은 PetWay DB에 영구 저장하지 않습니다.
- 서버로 개인 위치정보를 전송하는 구조가 추가될 경우 위치정보 관련 법적 요건을 별도로 검토합니다.
- 장소 자체의 좌표를 검색·날씨·경로 조회에 사용하는 것과 사용자의 위치 이력을 저장하는 것은 구분합니다.

---

## 현재 구현 상태

기준일: **2026-09-04 최신 `main`**

- `[x]`: production code에 구현되어 있고 관련 테스트 또는 빌드가 통과한 항목
- `[ ]`: 미구현이거나 실제 외부 API, E2E 또는 배포 검증이 남은 항목

### 인증 및 사용자

- [x] 이메일 회원가입
- [x] 세션 로그인·로그아웃
- [x] 로그인 사용자 조회 `/api/auth/me`
- [x] BCrypt 비밀번호 암호화
- [x] Kakao OAuth 로그인과 기존 계정 연결
- [x] `USER`, `ADMIN` 권한 구조
- [x] 일반 회원가입을 통한 `ADMIN` 직접 생성 차단 구조
- [ ] 회원정보 수정
- [ ] 운영 CSRF 전략 확정
- [ ] 운영 Cookie `SameSite`·`Secure` 검증
- [ ] 공모전 심사용 테스트 전용 `USER` 계정 준비

### 반려견

- [x] 다견 등록·목록·상세·수정·삭제
- [x] 대표 반려견 설정
- [x] 체중, 체형, 활동성, 성향 정보 관리
- [x] 동물등록번호 또는 RFID 기반 등록정보 조회
- [x] 조회 결과를 등록 폼에 자동 입력
- [x] 사용자 확인 후 기존 Pet API로 최종 저장
- [x] 조회 조건과 외부 원본 응답을 Pet DB에 저장하지 않음
- [ ] 예방접종·중성화 여부의 실제 판정 연결
- [ ] 체중 소수점 최대 2자리 허용 여부 팀 결정 및 경계 테스트

현재 정수 체중 입력은 의도된 정책입니다. 장소 원문이 정수 kg 기준인 경우 불필요한 정밀도를 받지 않기 위한 선택이며, 팀이 필요하다고 결정하면 최대 소수점 둘째 자리까지만 확장합니다.

### 장소와 지도

- [x] `areaBasedList2` 지역 기반 장소 조회
- [x] `locationBasedList2` 현재 위치 주변 장소 조회
- [x] `detailPetTour2` 반려동물 동반 상세 조회
- [x] 지역 및 콘텐츠 유형 필터
- [x] Kakao 지도와 관광공사 좌표 기반 마커
- [x] Browser Geolocation
- [x] 문화정보원 CSV 보조 Catalog 코드
- [x] 관광공사·문화정보원 출처 필드 응답
- [ ] `detailCommon2` 장소 공통 상세 조회
- [x] `csv-*` 장소 ID의 상세 및 Compatibility 계약 수정
- [ ] 관광공사·문화정보원 혼합 결과의 중복 제거
- [ ] 혼합 결과의 `page`, `size`, `totalCount` 계약 정리
- [ ] 문화정보원 CSV의 로컬·CI·배포 공급 방식 확정
- [x] Frontend `/places` 비로그인 Route 분리
- [x] 공개 장소·날씨와 로그인 필요 맞춤 기능의 UI 분리

### PetRule Parser

- [x] `acmpyTypeCd`, `acmpyPsblCpam`, `acmpyNeedMtr`, `etcAcmpyInfo`, `relaAcdntRiskMtr` 원문 보존
- [x] `kg`와 Unicode `㎏` 처리
- [x] `이하`와 `미만` 연산자 분리
- [x] 복수 체중 제한 처리
- [x] 소형·중형·대형·중소형견 문구 처리
- [x] 전구역·일부 구역·실내·야외·테라스 범위
- [x] 목줄·입마개·배변봉투·배변처리·이동장·유모차·매너벨트
- [x] 파싱하지 못한 원문 조각 보존
- [ ] `relaPosesFclty`, `relaFrnshPrdlst`, `relaPurcPrdlst`, `relaRntlPrdlst` 보존
- [ ] 보호자 수 대비 반려견 마릿수와 팀당 최대 마릿수 파싱
- [ ] 예방접종·사전 예약·문의·추가 요금·연령 조건 파싱

### Compatibility

- [x] 4개 최종 상태 판정
- [x] 체중·크기·공간 범위 판정
- [x] 준비사항과 판정 근거 응답
- [x] 모호한 규정의 `NEED_CONFIRMATION` 처리
- [x] 명확한 판정 근거가 있을 때 미해석 부가 문구의 `NEED_CONFIRMATION` 오탐 방지
- [x] 공식정보와 PetWay 판정 분리 UI
- [ ] 최소 체중 미달 안내 문구 인코딩 수정
- [ ] 실제 등록 견종과 위험견 제한 비교
- [ ] 방문 보호자 수 입력
- [ ] 함께 방문할 여러 반려견 선택
- [ ] 보호자 수와 반려견 수 조합 판정
- [ ] 반려견별 판정 이유와 가장 엄격한 결과 반영
- [ ] 장소 화면에서 판정 기준 반려견 변경

### 날씨와 일상 추천

- [x] D0~D3 단기예보
- [x] D4~D10 중기예보
- [x] D11~D90 3개월 전망
- [x] 전북자치도 지역명과 발표 시각 경계 처리
- [x] 날씨 전용 HTTP Client와 timeout
- [x] 비로그인 `GET /api/weather`
- [x] 대표 반려견·Compatibility·거리·장소 유형·날씨 기반 WalkFit
- [x] WalkFit 최대 8개 후보 평가 및 반환
- [x] 가까운 후보 유지와 날짜별 순환 후보 다양화
- [x] 일부 외부 실패와 전체 429 구분
- [ ] 실제 기상청 운영 Key 기반 배포 smoke test
- [ ] 위치 권한 거부 시 지역 직접 선택 흐름 검증

### 여행 코스·경로·AI

- [x] 여행 코스 Builder
- [x] 여행일별 장소 구성과 날짜별 날씨
- [x] Kakao Mobility 자동차 경로
- [x] ODsay 대중교통 경로
- [x] OpenAI 구조화 추천 Client와 응답 검증
- [x] `UNAVAILABLE` AI 결과 차단
- [x] AI 장애 시 규칙 기반 fallback
- [x] 저장 일정 Backend API와 목록·상세·삭제 화면
- [x] 여행 코스 화면의 저장 버튼과 `saveCurrentTrip` 연결
- [x] 실제 AI 결과와 fallback 결과의 상태 구분
- [ ] 서버가 실제 TourAPI 후보와 Compatibility를 재검증한 뒤 AI에 전달
- [ ] 장소별 Compatibility Snapshot 저장
- [ ] 저장 일정 수정
- [ ] 출발 전 최신 동반정보 재호출·재파싱·재판정
- [ ] 저장 당시와 현재 규정 변경 안내

### 즐겨찾기·리뷰·생활시설·발전 기능

- [x] 즐겨찾기 Backend와 Frontend
- [x] 리뷰 Backend와 Frontend
- [x] 동물병원·동물약국 화면과 API
- [x] 행정안전부 동물병원 API Client
- [x] 문화정보원 병원·약국 Catalog 코드
- [x] Frontend `facilityType`과 Backend 데이터 Source 분기 수정
- [x] CSV가 없는 환경의 동물병원 공공 API fallback
- [ ] CSV가 없는 환경의 동물약국 처리
- [ ] 장소 2~3개 규정 비교
- [ ] 실제 방문 기록과 방문 반려견 Snapshot
- [ ] 사용자 현장 제보
- [ ] 관리자 제보 검토
- [ ] AI 제보 요약·분류·공식정보 충돌 탐지
- [ ] 장소 정보 신뢰도

### 품질·성능·배포

- [x] Backend 전체 테스트 통과
- [x] Frontend production build 통과
- [x] 주변 장소 2분 bounded cache
- [x] 주변 장소 cache 최대 256 entries와 좌표 정규화
- [x] 반려동물 상세 cache와 동일 `contentId` single-flight
- [x] 실패·429·timeout 응답 cache 제외
- [x] TravelCourse KTO cold worst case를 기존 약 170회에서 최대 약 50회로 축소
- [ ] 실제 KTO 호출량 기록 및 429 회귀 검증
- [ ] 로그인부터 일정 저장까지 핵심 브라우저 E2E 반복 검증
- [ ] 모든 외부 API의 운영 환경 smoke test
- [ ] 자동배포와 제출 URL 반복 검증
- [ ] 운영 Secret 공급 절차 문서화
- [ ] 공모전용 API 호출 로그 구현
- [x] Git 추적 설정에 포함된 Secret 형태의 기본값 제거
- [ ] 과거 노출 가능성이 있는 외부 API Key 교체 확인

---

## 목표 요구사항과 현재 코드의 차이

노션은 제품 목표와 상세 요구사항을 설명하고, `PETWAY_CODEX_CONTEXT.md`는 이후 합의된 구현 기준과 단계 구분을 포함하며, Repository는 현재 실제 구현을 보여줍니다. 아래에서는 서로 다른 내용을 합쳐서 확정하지 않고 출처별로 그대로 구분합니다.

### 충돌 1. 체중 소수점 지원

- **노션 기능·비기능 요구사항:** 실제 체중을 사용하며 `7.8kg`, `7.85kg` 같은 소수 입력을 지원합니다. ERD는 `weight_kg DECIMAL(5,2)`를 제안합니다.
- **`PETWAY_CODEX_CONTEXT.md`:** 최신 상태 체크리스트에서 소수점 최대 2자리 허용 여부를 팀 결정 항목으로 둡니다.
- **`README.md`:** 소수점 최대 2자리 허용 여부와 경계값 검증을 미완료로 표시합니다.
- **현재 코드:** Backend는 `BigDecimal`을 사용하지만 Request Validation이 `fraction = 0`이며, Frontend도 숫자가 아닌 문자를 제거하고 정수 여부를 검증합니다.
- **사용자의 최신 결정:** 장소 규정이 정수 kg 중심이므로 현재 정수 입력은 의도된 정책입니다. 확장한다면 불필요한 긴 소수가 아니라 최대 소수 둘째 자리까지만 고려합니다.

**최선의 방안:** 현재 정수 입력을 오류로 분류하지 않습니다. 다만 노션과 구현의 차이를 유지해서 표시하고, 팀이 소수 지원을 확정할 때 Backend `fraction = 2`, Frontend 입력·검증, DB scale, `9.99 / 10.00 / 10.01` 경계 테스트를 한 이슈에서 함께 변경합니다.

### 충돌 2. `sizeType` 보존 여부

- **노션 기능 요구사항:** `SMALL / MEDIUM / LARGE` 같은 PetWay 자체 크기 분류를 입장 판정 기준으로 사용하지 않습니다.
- **노션 ERD:** `size_type` 컬럼을 제거하고 실제 체중만 저장하는 방향을 제안합니다.
- **`PETWAY_CODEX_CONTEXT.md` 14-1:** 실제 데이터의 `소형견만 가능`, `대형견 불가`, `중소형견 가능`을 처리하기 위해 `size_type`을 유지하되 체중으로 자동 추론하지 않는다고 명시합니다.
- **현재 코드:** Pet Entity와 DTO에 `sizeType`이 있고, 등록 폼에서는 선택 정보로 받습니다. 여행 코스 화면에서도 표시·요약 정보로 사용합니다.

**최선의 방안:** `sizeType`을 즉시 제거하지 않습니다. 입장 판정의 기본은 실제 kg로 유지하고, 관광공사 원문이 명시적으로 크기 분류를 요구할 때만 사용합니다. 체중에서 크기를 자동 추론하지 않으며, 단순 추천·표시용인지 Compatibility 입력인지 사용 범위를 Contract에 명시합니다.

### 충돌 3. 다견 동시 판정의 구현 단계

- **노션 기능 요구사항:** 보호자 수, 함께 방문할 여러 반려견, 마릿수 규칙, 개별 체중을 P0 핵심으로 둡니다.
- **노션 ERD:** `course.guardian_count`, `course_pet`, `place_visit.guardian_count`, `visit_pet`을 제안합니다.
- **`PETWAY_CODEX_CONTEXT.md` 14:** M1은 한 마리 기준 Compatibility와 코스, 여러 마리 동시 판정은 M4로 둡니다. 초기부터 `course_pet` 테이블을 추가하지 않는다고 명시합니다.
- **현재 코드:** 한 사용자의 다견 CRUD와 대표견은 구현되어 있지만, 한 요청에서 여러 Pet과 보호자 수를 함께 판정하는 기능은 없습니다.

**최선의 방안:** 현재 구현을 M1 기준의 단일 Pet Compatibility로 명확히 유지합니다. 동시에 노션의 다견 요구를 삭제하지 않고 미구현 핵심 차별화 항목으로 추적합니다. 다견을 앞당길지는 팀이 우선순위를 정해야 하며, 결정 전 `course_pet`과 방문 테이블을 임의로 추가하지 않습니다.

### 충돌 4. 목표 ERD와 현재 저장 구조

- **노션 ERD:** `app_user`, `pet`, `course`, `course_pet`, `course_place`, `favorite_place`, `place_visit`, `visit_pet`, `place_report`, `api_request_log`의 10개 테이블을 제안합니다.
- **`PETWAY_CODEX_CONTEXT.md` 16:** 현재 인증 구조인 `app_user + user_auth`를 유지하고, `Course`, `CoursePlace`, `ApiRequestLog`는 필요 시 추가한다고 명시합니다.
- **현재 코드:** `User`, `UserAuth`, `Pet`, `SavedTrip`, `Favorite`, `Review` Entity가 있습니다. 여행 일정은 정규화된 `course/course_place`가 아니라 `saved_trip.itinerary_json`으로 저장합니다.

**최선의 방안:** 현재 `user_auth`, `saved_trip`, `review`를 문서만 보고 삭제하거나 이동하지 않습니다. 공모전 시연에 필요한 저장·조회가 현재 구조로 충분한지 먼저 검증하고, 출발 전 장소별 재검증과 다견 관계가 JSON 구조로 불가능하거나 불안정하다는 근거가 생길 때 `course/course_pet/course_place` 전환을 별도 설계 이슈로 결정합니다.

### 충돌 5. 장소 식별자와 문화정보원 CSV

- **노션과 `PETWAY_CODEX_CONTEXT.md`:** 관광공사 `contentId`를 장소 식별자로 유지하고 관광공사 OpenAPI를 Source of Truth로 사용합니다.
- **현재 코드:** 문화정보원 보조 데이터에는 `csv-*` 형식의 ID를 생성합니다. Controller와 Service가 해당 ID의 상세·Compatibility 조회를 지원하고, 응답에서 KTO 장소와 CSV 보조 데이터를 구분합니다.

**현재 반영:** KTO 장소에서는 원래 `contentId`를 유지하고 안전한 `csv-*` ID를 별도로 허용합니다. CSV 장소는 Catalog에서 상세를 조회하고 기존 Parser·Compatibility 흐름을 재사용합니다.

### 충돌 6. 장소·날씨의 비로그인 접근

- **최근 사용자 결정과 Backend Security:** 장소 조회와 `GET /api/weather/**`는 비로그인 접근을 허용합니다. Compatibility와 WalkFit 등 사용자 Pet이 필요한 기능은 인증을 요구합니다.
- **현재 Frontend:** `/places`는 공개 Route이며, 장소 지도와 날씨는 비로그인 상태에서도 조회할 수 있습니다. WalkFit 등 마이펫 기반 기능은 로그인 사용자에게만 노출합니다.

**현재 반영:** `/places`의 지도·장소·날씨 화면은 공개 Route로 분리하고, Compatibility·WalkFit·즐겨찾기·일정 저장 등 사용자 데이터가 필요한 기능은 인증 상태에 따라 안내 또는 보호합니다.

### 충돌 7. 여행 일정 저장 UI 상태

- **노션과 `PETWAY_CODEX_CONTEXT.md`:** 사용자가 확인한 코스를 저장하고 나중에 출발 전 재검증하는 흐름을 요구합니다.
- **`README.md`와 `docs/README.md`:** Backend 저장 API는 구현됐지만 여행 코스 화면의 저장 버튼 연결은 미구현으로 표시합니다.
- **현재 코드:** `saveCurrentTrip`과 저장 버튼이 연결되어 있으며 빈 코스·중복 요청을 차단합니다. 성공 후 저장 일정 상세로 이동하는 링크와 실패 안내를 제공하고, 목록·상세·삭제 화면도 존재합니다.

**현재 반영:** 기존 저장 구조와 API를 유지한 채 저장 버튼과 후속 확인 동선을 연결했습니다. 실제 저장·조회·삭제 브라우저 E2E와 저장 구조 변경은 별도 작업으로 분리합니다.

### 충돌 8. 문서의 과거 구현 상태와 최신 구현 상태

- **`README.md` 기존 문장:** 저장소에 초기 실행 골격만 있고 외부 API Key 사용 코드가 없다고 적혀 있습니다.
- **`docs/README.md` 기존 문장:** 초기 골격 단계이므로 별도 비즈니스 명세를 만들지 않는다고 적혀 있습니다.
- **`PETWAY_CODEX_CONTEXT.md` 6:** 2026-08-27 당시 장소·날씨·지도·코스 등이 main에 없었다고 기록합니다.
- **세 문서에 추가된 2026-09-04 체크리스트와 현재 코드:** 장소, 날씨, 지도, Compatibility, WalkFit, 코스, AI, 경로, 저장 일정 등 다수 기능이 구현되어 있습니다.

**최선의 방안:** 과거 문장은 이력으로 보존하되 메인 README의 현재 상태로 사용하지 않습니다. 이 문서의 `2026-09-04 최신 main` 체크리스트를 현재 상태의 기준으로 사용하고, 향후 상태 갱신 시 날짜와 검증 근거를 함께 변경합니다.

### 충돌 9. Secret 관리 원칙과 현재 설정

- **노션, `PETWAY_CODEX_CONTEXT.md`, `README.md`:** API Key와 DB 비밀번호를 Git에 저장하지 않고 환경변수 또는 외부 설정으로 관리해야 합니다.
- **현재 코드:** `backend/src/main/resources/application.yml`은 Git에서 제외되며, 추적 중인 `application.properties`는 `TOUR_API_KEY`의 빈 환경변수 기본값만 사용합니다.

**남은 방안:** 값을 문서에 복사하지 않습니다. 추적 설정 정리는 완료됐으며, 과거 노출 가능성이 있는 Key의 제공기관별 교체 여부를 확인하고 팀 실행값은 OS 환경변수, IDE Run Configuration, 배포 Secret으로 공급합니다.

### 충돌 10. 병원·약국 데이터 Source

- **기획서:** 동물병원·약국 등 생활 인프라 연계를 발전 방향으로 제시합니다.
- **현재 코드:** 병원은 행정안전부 API를 기본 Source로 사용하고 문화정보원 CSV를 보강·fallback으로 사용합니다. 약국은 문화정보원 CSV를 사용합니다.
- **현재 문서 체크리스트:** 병원의 CSV 미존재·공공 API 실패 경로는 처리됐지만, CSV가 없는 환경의 약국 처리 방식은 미완료입니다.

**남은 방안:** 약국의 기본 Source와 CSV가 없는 배포 환경의 빈 결과 처리 계약을 별도 이슈로 확정합니다.

### 충돌 11. 기능 우선순위

- **노션 기능 요구사항 P0:** 회원·반려견·관광공사 장소·Parser·Compatibility뿐 아니라 보호자 수, 다견 동시 판정, Kakao Map, 출발 전 최신 규정 재검증까지 반드시 구현하는 범위로 둡니다.
- **노션 P1:** 예방접종·견종·장비·요금 규칙, 장소 비교, 다견 동시 검색, 오늘 뭐하지, 날씨, PetFit, 코스, AI 추천을 둡니다.
- **노션 P2:** 방문 기록, 현장 제보, 정보 신뢰도, 즐겨찾기를 둡니다.
- **`PETWAY_CODEX_CONTEXT.md` M1:** 한 마리 기준 Compatibility를 먼저 완성하고 다견 동시 판정은 M4로 둡니다.
- **`PETWAY_CODEX_CONTEXT.md` M2~M4:** 날씨·거리·PetFit은 M2, 코스·AI·재검증은 M3, 다견·제보·방문·비교 등은 M4입니다.
- **현재 코드:** 날씨·WalkFit·코스·AI·즐겨찾기·리뷰는 이미 구현됐지만 `detailCommon2`, 다견·보호자 수 판정, 출발 전 재검증은 남아 있습니다.

**최선의 방안:** 이미 구현된 기능을 단계가 다르다는 이유로 제거하지 않습니다. 신규 기능 추가보다 현재 핵심 E2E의 runtime blocker와 Secret·배포 문제를 먼저 해결한 뒤, 지정과제와 직접 연결되는 `detailCommon2 → 다견·보호자 수 규칙 → 출발 전 재검증` 순으로 빈 구간을 닫습니다. 다견을 M1으로 앞당길지는 팀 결정으로 남깁니다.

### 충돌 12. GPS 처리 위치

- **노션 기획·비기능 요구사항:** 정확한 개인 GPS는 가능하면 단말기 안에서 사용하고 서버로 전송하지 않으며, 서버 전송이 필요해지면 위치정보 관련 요건을 별도 검토합니다.
- **`PETWAY_CODEX_CONTEXT.md` 21:** 기본 기능에서 정확한 `Browser GPS → Backend → TourAPI` 흐름을 승인 없이 사용하지 않는다고 명시합니다.
- **현재 장소·WalkFit 코드:** `navigator.geolocation`으로 받은 위도·경도를 `/api/places/nearby`와 `/api/walk/recommendations`의 Query Parameter로 Backend에 전달합니다.
- **현재 병원 코드:** GPS를 지도 중심과 클라이언트 거리 계산에만 사용하고 병원 API 요청에는 포함하지 않습니다.

**최선의 방안:** 현재 장소·WalkFit 동작이 문서 원칙과 다른 상태임을 즉시 문서화합니다. 기능을 유지하려면 서버 전송의 법적·운영 요건과 사용자 고지를 팀이 먼저 확인해야 합니다. 서버 전송을 피하려면 사용자가 선택한 지역·지도 중심 좌표를 사용하거나 개인을 특정할 수 없도록 처리한 좌표로 조회하는 별도 설계를 검토하되, 법적 적합성을 코드만 보고 추정하지 않습니다.

---

## 노션 기능 요구사항 전체 추적

아래 목록은 노션의 기능 요구사항을 기능군별로 빠짐없이 옮겨 현재 상태와 연결한 것입니다. 체크 여부는 노션의 우선순위가 아니라 현재 코드 구현 여부를 뜻합니다.

### 기획서의 19개 기능

| 번호 | 기능 | 현재 상태 |
| ---: | --- | --- |
| 9.1 | 마이펫 프로필 | [x] 구현 |
| 9.2 | 대표 반려견과 현재 날씨 기반 `오늘 뭐하지?` | [x] WalkFit으로 구현 |
| 9.3 | 기온·강수·적설·풍속 기반 시간대별 산책 적합 시간 | [ ] 현재 기능이 노션의 시간대별 요구를 모두 충족하는지는 확인되지 않음 |
| 9.4 | Browser Geolocation 기반 내 주변 장소 | [x] 구현, 단 GPS Backend 전송 충돌 존재 |
| 9.5 | 지역·카테고리·방문조건 기반 장소 검색 | [x] 지역·콘텐츠 유형 / [ ] 세부 방문조건 필터 |
| 9.6 | PetRule Parser | [x] 일부 규칙 구현, 요구 전체는 미완료 |
| 9.7 | 반려견 이용 가능 여부와 이유 | [x] 단일 Pet 기준 구현 |
| 9.8 | Compatibility와 분리된 PetFit Score | [x] WalkFit 형태로 구현 |
| 9.9 | 장소 2~3개 비교 | [ ] 미구현 |
| 9.10 | 30분·1시간·2시간 단위 빠른 외출 코스 | [ ] 해당 시간 선택형 기능은 미구현 |
| 9.11 | 장소 추가·삭제·순서·여행일·시간·체류시간의 여행 코스 Builder | [x] 상당부분 구현, Drag & Drop 여부는 현재 조사에서 확정하지 않음 |
| 9.12 | 조건 기반 코스 자동 생성 | [x] 구현 |
| 9.13 | 방문 예정 날씨 기반 코스 검증과 실내 대안 | [x] 날짜별 날씨 / [ ] 자동 실내 대체의 완결성은 확인되지 않음 |
| 9.14 | 출발 전 최신 규정·날씨 재검증 | [ ] 미구현 |
| 9.15 | 검증된 실제 후보 기반 AI 코스 어시스턴트 | [x] Client·Validator 존재 / [ ] 서버 원천 검증 구조 미완료 |
| 9.16 | 즐겨찾기와 최신정보 재조회 | [x] 저장·해제 / [ ] 즐겨찾기 재조회 시 최신 상세 보장 여부는 확인되지 않음 |
| 9.17 | 실제 방문과 현장 경험 기록 | [ ] 미구현 |
| 9.18 | 공식정보와 분리된 사용자 현장 제보 | [ ] 미구현 |
| 9.19 | 공식정보와 방문 데이터 기반 장소 정보 신뢰도 | [ ] 미구현 |

### 회원

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-USER-01 | 이메일·비밀번호·닉네임 회원가입 | 필수 | [x] 구현 |
| FR-USER-02 | 로그인 | 필수 | [x] 구현 |
| FR-USER-03 | 로그아웃 | 필수 | [x] 구현 |
| FR-USER-04 | 회원 기본정보 조회 | 필수 | [x] `/api/auth/me` |
| FR-USER-05 | 닉네임 등 회원정보 수정 | 중요 | [ ] 미구현 |

### 마이펫

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-PET-01 | 반려견 기본정보와 실제 체중 등록 | 필수 | [x] 구현 |
| FR-PET-02 | 반려견 목록 조회 | 필수 | [x] 구현 |
| FR-PET-03 | 반려견 상세 조회 | 필수 | [x] 구현 |
| FR-PET-04 | 반려견 수정 | 필수 | [x] 구현 |
| FR-PET-05 | 반려견 삭제 | 필수 | [x] 구현 |
| FR-PET-06 | 대표 반려견 설정 | 필수 | [x] 구현 |
| FR-PET-07 | 한 사용자의 다견 등록 | 필수 | [x] 구현 |

노션의 필수 정보는 이름·견종·생년월일·체중이며, 선택 정보는 활동 수준·실내외 선호·혼잡 적응도·사회성·중성화·예방접종입니다. 현재 코드는 이외에 동물등록정보 불러오기와 `sizeType`도 지원합니다.

### 장소 검색과 최신 조회

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-PLACE-01 | 지역별 장소 검색 | 필수 | [x] 구현 |
| FR-PLACE-02 | 관광지·음식점·카페 등 카테고리 검색 | 필수 | [x] 콘텐츠 유형 필터 구현 |
| FR-PLACE-03 | 장소 공통 상세 조회 | 필수 | [ ] `detailCommon2` 미구현 |
| FR-PLACE-04 | 반려동물 동반정보 조회 | 필수 | [x] `detailPetTour2` |
| FR-PLACE-05 | Pet과 구조화 규정 기준 결과 필터 | 필수 | [x] Compatibility·WalkFit에 부분 구현, 전체 검색 필터 계약은 미완료 |
| FR-PLACE-06 | 저장 장소의 최신 정보 재조회 | 필수 | [ ] 출발 전 재검증 미구현 |

### 체중 규칙

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-RULE-01 | 최대 체중 추출 | 필수 | [x] 구현 |
| FR-RULE-02 | 최소 체중 추출 | 중요 | [x] 구조 존재, 안내 문구 인코딩 오류 수정 필요 |
| FR-RULE-03 | 미만·이하·초과·이상 구분 | 필수 | [x] 미만·이하 구현, 초과·이상 지원 범위는 별도 검증 필요 |
| FR-RULE-04 | 체중 제한 없음 판별 | 필수 | [ ] 전 표현 범위 구현 여부를 현재 조사에서 확정하지 않음 |
| FR-RULE-05 | 불명확한 체중 조건을 임의 계산하지 않음 | 필수 | [x] `NEED_CONFIRMATION` 원칙 적용 |

### 보호자 수와 반려견 수

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-PETCOUNT-01 | 보호자 1인당 허용 마릿수 추출 | 필수 | [ ] 미구현 |
| FR-PETCOUNT-02 | 팀당 최대 마릿수 추출 | 중요 | [ ] 미구현 |
| FR-PETCOUNT-03 | 초과 시 추가요금·문의 조건 추출 | 중요 | [ ] 미구현 |
| FR-PETCOUNT-04 | 방문 보호자 수 입력 | 필수 | [ ] 미구현 |
| FR-PETCOUNT-05 | 함께 방문할 여러 Pet 선택 | 필수 | [ ] 미구현 |
| FR-PETCOUNT-06 | 보호자 수와 Pet 수 판정 | 필수 | [ ] 미구현 |
| FR-PETCOUNT-07 | 초과 시 불가 또는 확인 필요 안내 | 필수 | [ ] 미구현 |

### 공간 규칙

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-SPACE-01 | 실내 이용조건 추출 | 필수 | [x] 구현 |
| FR-SPACE-02 | 실외 이용조건 추출 | 필수 | [x] 구현 |
| FR-SPACE-03 | 테라스 전용 조건 | 필수 | [x] 구현 |
| FR-SPACE-04 | 특정 층·일부 공간 허용 | 중요 | [x] 일부 구역 범위 구현 |
| FR-SPACE-05 | 반려동물 전용공간 | 중요 | [ ] 별도 구조화 여부를 현재 조사에서 확정하지 않음 |

### 이동·보호장비 규칙

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-EQUIP-01 | 이동장·캐리어 조건 | 필수 | [x] 구현 |
| FR-EQUIP-02 | 반려동물 유모차 조건 | 중요 | [x] 구현 |
| FR-EQUIP-03 | 목줄·하네스 조건 | 필수 | [x] 구현 |
| FR-EQUIP-04 | 입마개 조건 | 중요 | [x] 문구 파싱, 실제 견종 비교는 미구현 |
| FR-EQUIP-05 | 기저귀·매너벨트 조건 | 중요 | [x] 매너벨트 구현, 기저귀 표현 범위는 별도 검증 필요 |

### 추가 입장조건

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-ENTRY-01 | 예방접종 조건 | 중요 | [ ] 미구현 |
| FR-ENTRY-02 | 특정 견종 제한 | 중요 | [ ] 실제 Pet 견종 비교 미구현 |
| FR-ENTRY-03 | 공격성 반려견 제한 | 중요 | [ ] 미구현 |
| FR-ENTRY-04 | 최소·최대 연령 | 확장 | [ ] 미구현 |
| FR-ENTRY-05 | 중성화 조건 | 확장 | [ ] 미구현 |
| FR-ENTRY-06 | 반려견당 추가요금 | 중요 | [ ] 미구현 |
| FR-ENTRY-07 | 사전예약·문의 | 중요 | [ ] 미구현 |

### Parser

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-PARSER-01 | 공백·특수문자·kg 표현 정규화 | 필수 | [x] 구현 |
| FR-PARSER-02 | 실제 kg 체중조건 파싱 | 필수 | [x] 구현 |
| FR-PARSER-03 | 1인 1견·팀당 최대 수 파싱 | 필수 | [ ] 미구현 |
| FR-PARSER-04 | 실내·실외·테라스 조건 파싱 | 필수 | [x] 구현 |
| FR-PARSER-05 | 이동장·목줄·입마개 등 장비 파싱 | 필수 | [x] 구현 |
| FR-PARSER-06 | 예방접종·견종·요금 등 기타 조건 | 중요 | [ ] 미구현 또는 부분 구현 |
| FR-PARSER-07 | 구조화 결과와 함께 공식 원문 보존 | 필수 | [x] 5개 필드 보존, 4개 source field 추가 필요 |
| FR-PARSER-08 | 해석 불가 규정을 확인 필요로 처리 | 필수 | [x] 구현 |

### Compatibility

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-COMP-01 | 선택한 모든 Pet의 체중조건 판정 | 필수 | [x] 단일 Pet 구현 / [ ] 다견 미구현 |
| FR-COMP-02 | 반려견 수 제한 판정 | 필수 | [ ] 미구현 |
| FR-COMP-03 | 보호자 수 대비 허용 Pet 수 계산 | 필수 | [ ] 미구현 |
| FR-COMP-04 | 실내·실외 조건 판정 | 필수 | [x] 구현 |
| FR-COMP-05 | 이동장 조건 반영 | 필수 | [x] 구현 |
| FR-COMP-06 | 목줄 등 장비 조건 반영 | 필수 | [x] 구현 |
| FR-COMP-07 | 예방접종 조건 비교 | 중요 | [ ] 미구현 |
| FR-COMP-08 | 견종 제한 비교 | 중요 | [ ] 미구현 |
| FR-COMP-09 | 모든 세부 규칙의 최종 상태 결합 | 필수 | [x] 현재 지원 규칙 기준 구현 |
| FR-COMP-10 | 조건별 판정 근거 제공 | 필수 | [x] 구현 |

최종 상태는 `AVAILABLE`, `CONDITIONAL`, `NEED_CONFIRMATION`, `UNAVAILABLE` 네 가지이며 세 문서와 현재 코드가 일치합니다.

### 방문조건 기반 필터

| ID | 요구사항 | 현재 상태 |
| --- | --- | --- |
| FR-FILTER-01 | 선택 Pet의 실제 kg 기준 | [x] Compatibility·WalkFit에 부분 적용 |
| FR-FILTER-02 | 선택한 모든 Pet을 수용하는 장소 | [ ] 미구현 |
| FR-FILTER-03 | 보호자 1인의 다견 가능 장소 | [ ] 미구현 |
| FR-FILTER-04 | 실내 이용 가능 | [ ] 검색 UI 필터로는 미구현 |
| FR-FILTER-05 | 야외 이용 가능 | [ ] 검색 UI 필터로는 미구현 |
| FR-FILTER-06 | 이동장 불필요 | [ ] 검색 UI 필터로는 미구현 |
| FR-FILTER-07 | 목줄 조건 | [ ] 검색 UI 필터로는 미구현 |
| FR-FILTER-08 | 입마개 조건 | [ ] 검색 UI 필터로는 미구현 |
| FR-FILTER-09 | 예방접종 확인 | [ ] 미구현 |
| FR-FILTER-10 | 추가요금 없음 | [ ] 미구현 |
| FR-FILTER-11 | 사전문의 불필요 | [ ] 미구현 |

### 다견 동시 검색

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-MULTIPET-01 | 여러 Pet 선택 | 필수 | [ ] 미구현 |
| FR-MULTIPET-02 | 방문 인원 선택 | 필수 | [ ] 미구현 |
| FR-MULTIPET-03 | 모든 Pet의 개별 체중 검증 | 필수 | [ ] 미구현 |
| FR-MULTIPET-04 | Pet 수와 사람 수 규정 비교 | 필수 | [ ] 미구현 |
| FR-MULTIPET-05 | 가장 엄격한 결과 적용 | 필수 | [ ] 미구현 |
| FR-MULTIPET-06 | Pet별 결과 제공 | 필수 | [ ] 미구현 |

### 장소 비교

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-COMPARE-01 | 장소 2~3개 선택 | 중요 | [ ] 미구현 |
| FR-COMPARE-02 | 체중 제한 비교 | 중요 | [ ] 미구현 |
| FR-COMPARE-03 | 동반 마릿수 비교 | 중요 | [ ] 미구현 |
| FR-COMPARE-04 | 실내·야외·테라스 비교 | 중요 | [ ] 미구현 |
| FR-COMPARE-05 | 이동장·목줄·입마개 비교 | 중요 | [ ] 미구현 |
| FR-COMPARE-06 | 접종·요금·문의조건 비교 | 중요 | [ ] 미구현 |

### 원문과 검증정보

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-VERIFY-01 | 관광공사 원문 표시 | 필수 | [x] 구현 |
| FR-VERIFY-02 | PetWay 자동판정 별도 표시 | 필수 | [x] 구현 |
| FR-VERIFY-03 | 정보 없음·모호함 강조 | 필수 | [x] 구현 |
| FR-VERIFY-04 | 최신 관광정보 조회 시각 | 중요 | [ ] 미구현 |

### 출발 전 재검증

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-REVALID-01 | 저장 장소의 최신 OpenAPI 조회 | 필수 | [ ] 미구현 |
| FR-REVALID-02 | 최신 규정 재파싱 | 필수 | [ ] 미구현 |
| FR-REVALID-03 | 현재 방문조건으로 재판정 | 필수 | [ ] 미구현 |
| FR-REVALID-04 | 저장 당시와 현재 규정의 변경 안내 | 중요 | [ ] 미구현 |

### 현장 정보 제보

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-REPORT-01 | 체중규정 변경 제보 | 중요 | [ ] 미구현 |
| FR-REPORT-02 | 동반 마릿수 제보 | 중요 | [ ] 미구현 |
| FR-REPORT-03 | 실내외 규정 제보 | 중요 | [ ] 미구현 |
| FR-REPORT-04 | 이동장·목줄 제보 | 중요 | [ ] 미구현 |
| FR-REPORT-05 | 입장 거절과 사유 제보 | 중요 | [ ] 미구현 |
| FR-REPORT-06 | 접종·요금·예약 등 기타 규정 제보 | 중요 | [ ] 미구현 |

### 방문 기록

| ID | 요구사항 | 노션 우선순위 | 현재 상태 |
| --- | --- | --- | --- |
| FR-VISIT-01 | 실제 방문 등록 | 중요 | [ ] 미구현 |
| FR-VISIT-02 | 함께 방문한 Pet 기록 | 중요 | [ ] 미구현 |
| FR-VISIT-03 | 방문 보호자 수 기록 | 중요 | [ ] 미구현 |
| FR-VISIT-04 | 입장 성공·거절 결과 | 중요 | [ ] 미구현 |
| FR-VISIT-05 | 공식 안내와 현장 규정 일치 여부 | 중요 | [ ] 미구현 |

---

## 노션 비기능 요구사항 전체 추적

### 성능

- [ ] `NFR-PERF-01` 관광공사 호출을 포함한 일반 장소 검색의 3초 이내 응답 목표를 실제 환경에서 측정
- [x] `NFR-PERF-02` 주요 외부 API 호출 중 로딩 상태 표시
- [x] `NFR-PERF-03` 동일 `contentId` 상세 요청의 cache와 single-flight 적용
- [x] `NFR-PERF-04` 장소 검색 페이지 단위 조회
- [x] `NFR-PERF-05` AI 호출을 일반 장소 검색·상세와 분리
- [x] `NFR-PERF-06` 입장 판정을 AI가 아닌 Java 규칙으로 처리

### 관광공사 OpenAPI

- [x] `NFR-TOUR-01` 검색·동반정보 등 핵심 기능에서 OpenAPI 사용
- [x] `NFR-TOUR-02` 전체 장소 Master DB로 API 대체 금지
- [x] `NFR-TOUR-03` KTO `contentId` 유지
- [ ] `NFR-TOUR-04` 저장 장소의 최신정보 재조회
- [ ] `NFR-TOUR-05` 기능별 API 활용 추적 로그
- [x] `NFR-TOUR-06` 현재 추적 설정에 포함된 Secret 형태 기본값 제거 / [ ] 과거 노출 Key 교체 확인
- [x] `NFR-TOUR-07` 관광공사 출처 표시 UI 존재

### 판정 정확성

- [x] `NFR-RULE-01` 실제 kg 기준 판정
- [x] `NFR-RULE-02` 미만과 이하 경계 구분
- [ ] `NFR-RULE-03` 보호자 수와 마릿수 관계 판정
- [x] `NFR-RULE-04` 체중·공간·장비 규칙의 독립 모델
- [x] `NFR-RULE-05` 현재 지원되는 복수 규칙의 결합
- [ ] `NFR-RULE-06` 다견 개별 판정
- [ ] `NFR-RULE-07` 다견의 가장 엄격한 결과 반영
- [x] `NFR-RULE-08` 결정론적 Parser·Compatibility 구조

### 불명확한 정보

- [x] `NFR-UNKNOWN-01` 원문에 없는 규정 생성 금지
- [x] `NFR-UNKNOWN-02` 정보 없음과 명시적 허용 구분
- [x] `NFR-UNKNOWN-03` 모호한 문장의 `NEED_CONFIRMATION`
- [x] `NFR-UNKNOWN-04` 공식 원문 접근
- [x] `NFR-UNKNOWN-05` 조건별 판정 근거
- [x] `NFR-UNKNOWN-06` 부분 파싱과 미해석 원문 보존

### 원본과 해석의 분리

- [x] `NFR-DATA-01` 관광공사 원문과 PetWay 분석 분리
- [ ] `NFR-DATA-02` 공식정보와 향후 사용자 제보 데이터 분리 구현
- [ ] `NFR-DATA-03` 최신 조회 시각 제공
- [x] `NFR-DATA-04` 공식 원문 임의 수정 금지
- [x] `NFR-DATA-05` KTO 장소의 외부 `contentId` 보존

### 다견과 보호자 수

- [x] `NFR-MULTI-01` 다견 등록
- [ ] `NFR-MULTI-01` 여러 Pet 동시 선택
- [ ] `NFR-MULTI-02` 회원 수와 구분된 방문 보호자 수
- [ ] `NFR-MULTI-03` 방문마다 다른 보호자·Pet 조합
- [ ] `NFR-MULTI-04` Pet별 판정 이유
- [ ] `NFR-MULTI-05` 다양한 마릿수 규칙 확장

### 안정성과 장애 대응

- [x] `NFR-STAB-01` 외부 API 예외를 공통 오류로 변환
- [x] `NFR-STAB-02` 관광공사·기상청·OpenAI timeout 설정
- [x] `NFR-STAB-03` 주요 외부 API 실패 안내와 재시도 UI
- [x] `NFR-STAB-04` 일부 장소 상세 실패를 전체 후보 실패와 분리
- [x] `NFR-STAB-05` AI 실패 시 규칙 기반 fallback
- [x] `NFR-STAB-06` 애플리케이션 로그 존재 / [ ] `api_request_log` 미구현

### 개인정보와 위치정보

- [x] `NFR-PRIV-01` GPS 영구 저장 없음
- [x] `NFR-PRIV-02` 위치 이력 생성 없음
- [x] `NFR-PRIV-03` Browser Geolocation 우선
- [x] `NFR-PRIV-04` 서비스에 불필요한 개인정보 미수집 원칙
- [ ] `NFR-PRIV-05` 향후 서버 위치 전송 기능 추가 시 법적 검토

### 보안

- [x] `NFR-SEC-01` BCrypt 비밀번호 저장
- [x] `NFR-SEC-02` 사용자 Pet·일정 API 인증 및 소유자 검증 구조
- [x] `NFR-SEC-03` 추적 설정의 Secret 형태 기본값 제거 / [ ] 과거 노출 Key 교체 확인
- [ ] `NFR-SEC-04` local·운영 Secret 공급 절차 확정
- [x] `NFR-SEC-05` 주요 Request의 서버 Validation
- [ ] `NFR-SEC-06` 운영 HTTPS 검증

### 입력값 검증

- [x] `NFR-VALID-01` 체중 0 이하와 범위 검증
- [ ] `NFR-VALID-02` 소수 체중은 노션 요구와 현재 정수 정책이 충돌하며 팀 결정 필요
- [ ] `NFR-VALID-03` 방문 보호자 수 검증은 기능 미구현
- [ ] `NFR-VALID-04` 방문 Pet 수 검증은 기능 미구현
- [x] `NFR-VALID-05` Pet 등록 필수값 검증

### 설명 가능성

- [x] `NFR-EXP-01` 최종 판정 이유 제공
- [x] `NFR-EXP-02` 현재 지원 규칙의 조건별 결과 표시
- [ ] `NFR-EXP-03` 다견 개별 결과
- [x] `NFR-EXP-04` 확인 필요 이유
- [x] `NFR-EXP-05` AI 성공과 heuristic fallback의 명시적 UI 구분

### 유지보수성

- [x] `NFR-MAIN-01` 외부 API와 도메인 분리
- [x] `NFR-MAIN-02` Parser 독립 패키지
- [x] `NFR-MAIN-03` Compatibility Engine 독립 패키지
- [x] `NFR-MAIN-04` 규칙별 모델과 Parser 확장 구조
- [x] `NFR-MAIN-05` 외부 DTO와 내부 DTO 분리
- [x] `NFR-MAIN-06` 상태·규칙 Enum 관리
- [x] `NFR-MAIN-07` 기능 도메인 중심 패키지

### 테스트 가능성

- [x] `NFR-TEST-01` Parser 단위 테스트
- [x] `NFR-TEST-02` 체중 연산자 경계 테스트
- [ ] `NFR-TEST-03` 보호자·Pet 마릿수 조합 테스트
- [x] `NFR-TEST-04` 현재 지원되는 복합조건 테스트
- [x] `NFR-TEST-05` `NEED_CONFIRMATION` 테스트
- [x] `NFR-TEST-06` 외부 API를 호출하지 않는 Parser·Compatibility 테스트

### 사용자 경험

- [x] `NFR-UX-01` 장소 상세에서 Compatibility 우선 표시
- [x] `NFR-UX-02` 원문을 체중·공간·장비 등으로 요약
- [x] `NFR-UX-03` 네 가지 상태 구분
- [x] `NFR-UX-04` 공식 원문 표시
- [ ] `NFR-UX-05` 한 화면의 다견 선택
- [ ] `NFR-UX-06` 장소·코스에서 방문 보호자 수 변경
- [ ] `NFR-UX-07` 모바일 핵심 흐름의 실제 반응형 브라우저 검증

### 배포와 운영

- [ ] `NFR-DEP-01` local·prod 설정과 Secret 공급 절차 확정
- [ ] `NFR-DEP-02` 재배포 후 MySQL 데이터 영속성 검증
- [ ] `NFR-DEP-03` 제출 URL의 실제 KTO 호출 검증
- [ ] `NFR-DEP-04` 배포 도메인의 Kakao 허용 도메인 등록
- [ ] `NFR-DEP-05` 별도 개발환경 없는 심사 URL 접속
- [ ] `NFR-DEP-06` 공모전 규격 테스트 계정
- [ ] `NFR-DEP-07` 운영 로그 확인 절차

---

## 데이터 및 ERD

### 저장 원칙

저장하는 데이터:

- 회원과 인증정보
- 반려견 프로필
- 사용자가 저장한 일정과 최소 장소 Snapshot
- 즐겨찾기와 리뷰
- 향후 방문 기록, 현장 제보, 외부 API 호출 로그

기본적으로 저장하지 않는 데이터:

- 관광공사 전체 장소와 전체 상세정보
- 관광공사 전체 반려동물 동반정보
- 모든 PetRule과 Compatibility 결과
- 전체 날씨 예보
- 사용자 GPS 이력
- 전체 AI 후보와 전체 AI 응답 이력

### 노션 목표 ERD와 현재 Entity

| 데이터 | 목표 구조 | 현재 상태 |
| --- | --- | --- |
| 회원 | `app_user` | [x] `User` |
| 인증 수단 | 확장 구조 | [x] `UserAuth` |
| 반려견 | `pet` | [x] `Pet` |
| 코스 | `course` | [ ] 현재 `SavedTrip`으로 대체 |
| 코스 참여 반려견 | `course_pet` | [ ] 미구현 |
| 코스 장소 | `course_place` | [ ] itinerary JSON에 포함 |
| 즐겨찾기 | `favorite_place` | [x] `Favorite` |
| 리뷰 | 별도 확장 | [x] `Review` |
| 방문 기록 | `place_visit` | [ ] 미구현 |
| 방문 반려견 | `visit_pet` | [ ] 미구현 |
| 현장 제보 | `place_report` | [ ] 미구현 |
| 외부 API 로그 | `api_request_log` | [ ] 미구현 |

관광 장소는 내부 `place_id` FK 대신 원본 `contentId`를 유지합니다. 저장 당시 Compatibility를 Snapshot으로 남기더라도 최신 판정은 출발 전에 다시 계산하는 것이 원칙입니다.

### 노션 목표 ERD 상세

아래는 노션 ERD의 제안 내용을 보존한 것입니다. 현재 코드의 확정 스키마가 아니며, 바로 위의 현재 Entity 표와 충돌 2·3·4를 함께 확인해야 합니다.

#### `app_user`

| 컬럼 | 타입 | NULL | KEY | 설명 |
| --- | --- | --- | --- | --- |
| `user_id` | BIGINT | N | PK | 회원 식별자 |
| `email` | VARCHAR(150) | N | UK | 로그인 이메일 |
| `password_hash` | VARCHAR(255) | N |  | 암호화 비밀번호 |
| `nickname` | VARCHAR(50) | N |  | 닉네임 |
| `user_status` | VARCHAR(20) | N |  | `ACTIVE / WITHDRAWN` |
| `created_at` | DATETIME | N |  | 가입일시 |
| `updated_at` | DATETIME | N |  | 수정일시 |

`PETWAY_CODEX_CONTEXT.md`와 현재 코드는 비밀번호 인증정보를 별도 `user_auth`에 둡니다. 권한 구조인 `USER / ADMIN`도 현재 코드에 있으므로 노션의 위 표만으로 현재 스키마를 덮어쓰지 않습니다.

#### `pet`

| 컬럼 | 타입 | NULL | KEY | 설명 |
| --- | --- | --- | --- | --- |
| `pet_id` | BIGINT | N | PK | 반려견 식별자 |
| `user_id` | BIGINT | N | FK | 소유 회원 |
| `pet_name` | VARCHAR(50) | N |  | 이름 |
| `breed` | VARCHAR(100) | Y |  | 견종 |
| `birth_date` | DATE | Y |  | 생년월일 |
| `weight_kg` | DECIMAL(5,2) | N |  | 실제 체중 |
| `activity_level` | VARCHAR(20) | Y |  | `LOW / NORMAL / HIGH` |
| `crowd_tolerance` | VARCHAR(20) | Y |  | 혼잡 환경 적응도 |
| `place_preference` | VARCHAR(20) | Y |  | `INDOOR / OUTDOOR / BOTH` |
| `sociality_level` | VARCHAR(20) | Y |  | 사회성 |
| `vaccinated` | BOOLEAN | Y |  | 예방접종 여부 |
| `neutered` | BOOLEAN | Y |  | 중성화 여부 |
| `is_primary` | BOOLEAN | N |  | 대표 반려견 여부 |
| `created_at` | DATETIME | N |  | 등록일시 |
| `updated_at` | DATETIME | N |  | 수정일시 |

노션은 `size_type` 제거를 제안하지만 `PETWAY_CODEX_CONTEXT.md`와 현재 코드는 이를 유지합니다. 체중 소수점도 현재는 정수 입력 정책입니다.

#### `course`

| 컬럼 | 타입 | NULL | KEY | 설명 |
| --- | --- | --- | --- | --- |
| `course_id` | BIGINT | N | PK | 코스 식별자 |
| `user_id` | BIGINT | N | FK | 코스 생성 회원 |
| `course_name` | VARCHAR(100) | N |  | 코스명 |
| `course_type` | VARCHAR(20) | N |  | `DAILY / TRIP` |
| `guardian_count` | INT | N |  | 실제 방문 예정 보호자 수 |
| `travel_date` | DATE | Y |  | 방문 예정일 |
| `start_time` | TIME | Y |  | 출발 예정시간 |
| `region_code` | VARCHAR(20) | Y |  | 관광공사 지역 코드 |
| `region_name` | VARCHAR(100) | Y |  | 지역 표시명 |
| `course_status` | VARCHAR(20) | N |  | `DRAFT / SAVED / COMPLETED` |
| `created_at` | DATETIME | N |  | 생성일시 |
| `updated_at` | DATETIME | N |  | 수정일시 |

`guardian_count`는 회원 수와 실제 방문 보호자 수를 구분하기 위한 방문 단위 값입니다. 현재 코드는 `course` 대신 `saved_trip`을 사용합니다.

#### `course_pet`

| 컬럼 | 타입 | NULL | KEY | 설명 |
| --- | --- | --- | --- | --- |
| `course_pet_id` | BIGINT | N | PK | 식별자 |
| `course_id` | BIGINT | N | FK | 코스 |
| `pet_id` | BIGINT | N | FK | 참여 반려견 |
| `created_at` | DATETIME | N |  | 등록일 |

노션은 `(course_id, pet_id)` UNIQUE를 제안합니다. `PETWAY_CODEX_CONTEXT.md`는 초기 단계에 이 테이블을 추가하지 않는다고 명시합니다.

#### `course_place`

| 컬럼 | 타입 | NULL | KEY | 설명 |
| --- | --- | --- | --- | --- |
| `course_place_id` | BIGINT | N | PK | 코스 장소 식별자 |
| `course_id` | BIGINT | N | FK | 소속 코스 |
| `tour_content_id` | VARCHAR(50) | N |  | 관광공사 `contentId` |
| `content_type_id` | VARCHAR(20) | Y |  | 콘텐츠 유형 |
| `place_name` | VARCHAR(200) | N |  | 장소명 Snapshot |
| `category_type` | VARCHAR(30) | Y |  | `TOUR / RESTAURANT / CAFE / PARK` |
| `address` | VARCHAR(300) | Y |  | 저장 당시 주소 |
| `latitude` | DECIMAL(10,7) | Y |  | 위도 |
| `longitude` | DECIMAL(10,7) | Y |  | 경도 |
| `sequence_no` | INT | N |  | 방문 순서 |
| `planned_arrival_time` | TIME | Y |  | 예상 도착시간 |
| `expected_stay_minutes` | INT | Y |  | 예상 체류시간 |
| `compatibility_status` | VARCHAR(30) | Y |  | 저장 당시 판정 Snapshot |
| `compatibility_reason` | VARCHAR(1000) | Y |  | 저장 당시 판정 이유 |
| `petfit_score` | DECIMAL(5,2) | Y |  | 저장 당시 적합도 |
| `created_at` | DATETIME | N |  | 코스 추가일 |

저장 당시 판정은 변화 비교용 Snapshot이며 최신 규정 자체가 아닙니다. 노션은 코스 내 `sequence_no` UNIQUE도 검토하도록 제안합니다.

#### `favorite_place`

| 컬럼 | 타입 | NULL | KEY | 설명 |
| --- | --- | --- | --- | --- |
| `favorite_place_id` | BIGINT | N | PK | 즐겨찾기 식별자 |
| `user_id` | BIGINT | N | FK | 회원 |
| `tour_content_id` | VARCHAR(50) | N |  | 관광공사 `contentId` |
| `content_type_id` | VARCHAR(20) | Y |  | 콘텐츠 유형 |
| `place_name` | VARCHAR(200) | N |  | 장소명 Snapshot |
| `latitude` | DECIMAL(10,7) | Y |  | 위도 |
| `longitude` | DECIMAL(10,7) | Y |  | 경도 |
| `created_at` | DATETIME | N |  | 저장일 |

노션은 `(user_id, tour_content_id)` UNIQUE를 제안하고, 상세정보는 다시 OpenAPI로 조회하는 원칙을 둡니다.

#### `place_visit`

| 컬럼 | 타입 | NULL | KEY | 설명 |
| --- | --- | --- | --- | --- |
| `visit_id` | BIGINT | N | PK | 방문 식별자 |
| `user_id` | BIGINT | N | FK | 작성 회원 |
| `tour_content_id` | VARCHAR(50) | N |  | 관광공사 `contentId` |
| `place_name` | VARCHAR(200) | N |  | 장소명 |
| `guardian_count` | INT | N |  | 실제 방문 보호자 수 |
| `visited_at` | DATETIME | N |  | 방문일시 |
| `entry_success` | BOOLEAN | Y |  | 실제 입장 성공 여부 |
| `indoor_allowed_actual` | BOOLEAN | Y |  | 실제 실내 이용 여부 |
| `carrier_required_actual` | BOOLEAN | Y |  | 실제 이동장 요구 여부 |
| `leash_required_actual` | BOOLEAN | Y |  | 실제 목줄 요구 여부 |
| `crowd_level` | VARCHAR(20) | Y |  | `LOW / NORMAL / HIGH` |
| `pet_friendly_score` | INT | Y |  | 친화도 1~5 |
| `revisit_intent` | BOOLEAN | Y |  | 재방문 의사 |
| `rule_match_status` | VARCHAR(20) | Y |  | 공식정보와 현장 규정 일치 여부 |
| `memo` | VARCHAR(500) | Y |  | 방문 메모 |
| `created_at` | DATETIME | N |  | 작성일 |

#### `visit_pet`

| 컬럼 | 타입 | NULL | KEY | 설명 |
| --- | --- | --- | --- | --- |
| `visit_pet_id` | BIGINT | N | PK | 식별자 |
| `visit_id` | BIGINT | N | FK | 방문 기록 |
| `pet_id` | BIGINT | N | FK | 방문한 반려견 |
| `weight_kg_snapshot` | DECIMAL(5,2) | N |  | 방문 당시 체중 |
| `created_at` | DATETIME | N |  | 등록일 |

노션은 `(visit_id, pet_id)` UNIQUE를 제안합니다. 체중 Snapshot은 Pet의 현재 체중이 바뀌어도 과거 판정을 재현하기 위한 값입니다.

#### `place_report`

| 컬럼 | 타입 | NULL | KEY | 설명 |
| --- | --- | --- | --- | --- |
| `report_id` | BIGINT | N | PK | 제보 식별자 |
| `user_id` | BIGINT | N | FK | 제보 회원 |
| `tour_content_id` | VARCHAR(50) | N |  | 관광공사 `contentId` |
| `place_name` | VARCHAR(200) | N |  | 장소명 |
| `report_type` | VARCHAR(50) | N |  | 제보 유형 |
| `report_content` | VARCHAR(1000) | Y |  | 상세내용 |
| `reported_value` | VARCHAR(500) | Y |  | 사용자가 확인한 값 |
| `report_status` | VARCHAR(20) | N |  | 노션 기준 `PENDING / CONFIRMED / REJECTED` |
| `visited_at` | DATE | Y |  | 실제 확인일 |
| `created_at` | DATETIME | N |  | 제보일 |
| `updated_at` | DATETIME | N |  | 처리일 |

`PETWAY_CODEX_CONTEXT.md`의 M4 흐름은 관리자 결과를 `APPROVE / REJECT / NEED_VERIFICATION`으로 표현하므로 상태명도 충돌합니다.

**최선의 방안:** 구현 전에 사용자 제보의 접수 상태와 관리자 검토 결과를 하나의 Enum으로 쓸지 분리할지 Contract에서 정합니다. AI는 요약·분류·충돌 탐지만 하고 공식정보를 자동 수정하지 않습니다.

#### `api_request_log`

| 컬럼 | 타입 | NULL | KEY | 설명 |
| --- | --- | --- | --- | --- |
| `api_request_log_id` | BIGINT | N | PK | 로그 ID |
| `user_id` | BIGINT | Y | FK | 요청 회원 |
| `api_provider` | VARCHAR(30) | N |  | `TOUR_API / WEATHER_API / OPENAI` |
| `api_type` | VARCHAR(50) | N |  | 호출 API 종류 |
| `request_purpose` | VARCHAR(50) | N |  | 서비스 내 호출 목적 |
| `tour_content_id` | VARCHAR(50) | Y |  | 특정 관광장소의 `contentId` |
| `response_status` | INT | Y |  | HTTP 상태 |
| `response_time_ms` | BIGINT | Y |  | 응답시간 |
| `result_count` | INT | Y |  | 결과 수 |
| `success` | BOOLEAN | N |  | 성공 여부 |
| `error_message` | VARCHAR(500) | Y |  | 실패내용 |
| `requested_at` | DATETIME | N |  | 요청일시 |

이 테이블은 공모전 주최 측의 인증키 기반 호출 검증을 대신하지 않고 내부 추적과 장애 분석에 사용합니다. API Key와 전체 인증 URL은 저장하지 않습니다.

### 노션 ERD 제약과 Validation

- `pet.weight_kg > 0`
- `course.guardian_count >= 1`
- `course_place.sequence_no >= 1`
- `course_place.expected_stay_minutes > 0`
- `place_visit.pet_friendly_score BETWEEN 1 AND 5`
- 같은 Pet의 동일 코스·동일 방문 중복 연결 방지

PetRule은 기본적으로 DB Entity가 아니라 요청 시 생성하는 도메인 객체입니다. 반복 파싱 비용이 실제 문제가 된 후에만 cache 성격의 저장 구조를 검토합니다.

---

## 기술 스택과 구조

### Frontend

- React 19.2.8
- TypeScript 7.0.2
- Vite 8.2.2
- React Router 7
- Kakao Map JavaScript SDK

### Backend

- Java 17
- Spring Boot 3.5.16
- Gradle 8.14.3 Wrapper
- Spring MVC / REST API
- Spring Security / Session / OAuth2 Client
- Spring Validation
- Spring Data JPA
- MySQL 8
- Spring RestClient
- springdoc OpenAPI
- OpenAI Java SDK

### 프로젝트 구조

```text
petway/
├─ frontend/                   # React + Vite + TypeScript
│  └─ src/
│     ├─ features/             # auth, pet, place, weather, walk, course, hospital
│     └─ shared/               # 공통 API와 UI
├─ backend/                    # Java + Spring Boot + Gradle
│  └─ src/
│     ├─ main/java/com/petway/
│     │  ├─ user/              # 회원·인증
│     │  ├─ pet/               # 반려견
│     │  ├─ place/             # 장소 조회
│     │  ├─ petrule/           # 비정형 규정 구조화
│     │  ├─ compatibility/     # 이용 가능 여부 판정
│     │  ├─ weather/           # 날씨 도메인
│     │  ├─ walk/              # WalkFit
│     │  ├─ trip/              # 저장 일정
│     │  ├─ route/             # 자동차·대중교통 경로
│     │  ├─ ai/                # AI 코스 추천
│     │  ├─ favorite/          # 즐겨찾기
│     │  ├─ review/            # 리뷰
│     │  ├─ hospital/          # 동물병원·약국
│     │  ├─ facility/          # 보조 시설 데이터
│     │  ├─ external/          # 외부 API Adapter
│     │  └─ global/            # 공통 설정·예외·보안
│     └─ test/
├─ docs/                       # 계약·배포 문서 위치
├─ PETWAY_CODEX_CONTEXT.md     # 구현 최상위 컨텍스트
└─ README.md
```

외부 API DTO와 PetWay 내부 DTO를 분리하고, 외부 API 호출·Parser·Compatibility Engine을 각각 독립된 역할로 유지합니다.

---

## 로컬 실행

### 사전 요구사항

- JDK 17 이상
- Node.js 20.19 이상 또는 22.12 이상
- MySQL 8
- 필요한 외부 API Key

### MySQL 예시

로컬 MySQL을 Docker로 실행하는 예시입니다. 비밀번호는 팀의 로컬 환경에 맞게 변경합니다.

```bash
docker run --name petway-mysql \
  -e MYSQL_ROOT_PASSWORD=<LOCAL_PASSWORD> \
  -e MYSQL_DATABASE=petway \
  -p 3307:3306 \
  -d mysql:8.0
```

### Backend

PowerShell 기준:

```powershell
cd backend
.\gradlew.bat bootRun
```

- 기본 Backend 주소: `http://localhost:10000`
- Swagger UI: `http://localhost:10000/swagger-ui/index.html`

### Frontend

```powershell
cd frontend
npm ci
npm run dev
```

- 기본 Frontend 주소: `http://localhost:5173`
- API 주소는 `VITE_API_URL`로 변경합니다.

> `npm install`은 의존성을 다시 해석하면서 `package-lock.json`을 변경할 수 있습니다. 기존 lockfile을 그대로 재현할 때는 `npm ci`를 사용합니다.

---

## 환경변수

실제 Key와 비밀번호는 Git에 커밋하지 않습니다. 아래는 변수명만 나타냅니다.

### Backend

| 변수 | 용도 |
| --- | --- |
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` | MySQL 사용자명 |
| `DB_PASSWORD` | MySQL 비밀번호 |
| `TOUR_API_KEY` | 한국관광공사 OpenAPI 인증키 |
| `WEATHER_SHORT_TERM_API_KEY` | 기상청 단기예보 인증키 |
| `WEATHER_MID_TERM_API_KEY` | 기상청 중기예보 인증키 |
| `OPENAI_API_KEY` | OpenAI API Key |
| `OPENAI_MODEL` | OpenAI 모델 |
| `OPENAI_TIMEOUT_SECONDS` | OpenAI timeout |
| `KAKAO_REST_API_KEY` | Kakao OAuth 및 Mobility REST API Key |
| `KAKAO_CLIENT_SECRET` | Kakao OAuth Client Secret |
| `ODSAY_API_KEY` | ODsay API Key |
| `ODSAY_CACHE_MINUTES` | ODsay cache 유지시간 |
| `ODSAY_CACHE_MAX_ENTRIES` | ODsay cache 최대 항목 수 |
| `ANIMAL_HOSPITAL_API_KEY` | 동물병원 API 인증키 |
| `ANIMAL_REGISTRATION_API_BASE_URL` | 동물등록정보 API Base URL |
| `ANIMAL_REGISTRATION_API_KEY` | 동물등록정보 API 인증키 |
| `FRONTEND_ORIGINS` | CORS 허용 Frontend Origin 목록 |

`backend/.env.example`은 변수명 참고용입니다. Spring Boot가 일반 `.env` 파일을 자동으로 읽는다고 가정하지 말고 IDE Run Configuration, OS 환경변수 또는 별도 local profile로 전달합니다.

### Frontend

| 변수 | 용도 |
| --- | --- |
| `VITE_API_URL` | Backend Base URL |
| `VITE_KAKAO_MAP_KEY` | Kakao Map JavaScript Key |

Frontend 로컬 설정은 `frontend/.env.local`처럼 Git에 포함되지 않는 파일을 사용합니다.

---

## 테스트

### Backend

```powershell
cd backend
.\gradlew.bat test
```

필수 검증 범위:

- TourAPI 성공·4xx·5xx·timeout·외부 `resultCode` 실패·빈 결과
- 실제 fixture 기반 PetRule Parser
- `kg/㎏`, `미만/이하`, 복수 체중조건, 공간·장비·모호한 문장
- Compatibility 경계값과 `NEED_CONFIRMATION`
- 다른 사용자의 Pet·일정 접근 차단
- 일반 회원가입의 `ADMIN` 생성 차단
- AI 후보와 결과 검증

### Frontend

```powershell
cd frontend
npm run build
```

브라우저 테스트는 다음 핵심 흐름을 사용자가 직접 확인합니다.

1. 회원가입 또는 로그인
2. 반려견 등록 또는 동물등록정보 불러오기
3. 대표 반려견 선택
4. 지도와 장소 검색
5. 장소 상세·Compatibility·공식 원문 확인
6. 여행일 날씨 확인
7. WalkFit 또는 여행 코스 생성
8. 자동차·대중교통 경로 확인
9. AI 추천과 fallback 표시 구분
10. 일정 저장·조회·삭제

코드가 존재하거나 단위 테스트가 통과한 것만으로 배포 완료라고 판단하지 않습니다. 실제 Key를 사용하는 smoke test와 제출 URL의 반복 시연까지 확인해야 합니다.

---

## 개발 우선순위

### 문서별 원래 우선순위

노션과 `PETWAY_CODEX_CONTEXT.md`의 단계는 서로 다르므로 하나로 섞지 않고 그대로 보존합니다.

#### 노션 기능 요구사항

**P0 — PetWay 핵심**

- 회원·로그인
- 반려견 등록, 실제 체중, 다견 등록
- 관광공사 장소 검색·상세·동반정보
- 체중·1인당 반려견 수·실내외·이동장·목줄 Parser
- 방문 인원과 방문 반려견 선택
- 체중·마릿수·인원 대비 Pet 수 Compatibility
- 네 가지 상태와 판정 근거
- Kakao Map
- 출발 전 최신 규정 재검증

**P1 — 핵심 차별화 강화**

- 예방접종·견종 제한·입마개·유모차·기저귀·매너벨트·추가요금
- 장소 규정 비교와 다견 동시 검색
- 오늘 뭐하지, 날씨, PetFit, 코스, AI 추천

**P2 — 사용자 데이터 축적**

- 방문 기록
- 현장 규정 제보
- 정보 신뢰도
- 즐겨찾기

#### `PETWAY_CODEX_CONTEXT.md`

**M1 — 핵심 문제 해결**

- Auth 안정화, 다견 Pet CRUD, 대표견, `size_type`
- KorPetTourService2 장소 검색, `detailCommon2`, `detailPetTour2`
- Raw Adapter, 실데이터 기반 Parser, 단일 Pet Compatibility
- 판정 이유·준비물·장소 목록·상세·공식 원문·출처 UI
- 핵심 E2E 통합 테스트

**M2 — 반복 사용**

- 기상청, Browser 거리, PetFit, 오늘 뭐하지, Kakao Map

**M3 — 여행**

- Course, 시간·순서, AI 코스, 날씨 검증, 출발 전 재검증

**M4 — 발전 기능**

- 사용자 현장 제보, 관리자 검토, AI 제보 보조, 정보 신뢰도
- 다견 동시 판정, 즐겨찾기, 방문 기록, 비교, 추천 고도화

두 우선순위의 충돌과 현재 권장 진행 순서는 [충돌 11](#충돌-11-기능-우선순위)에 기록합니다.

### P0 — 심사 시연 흐름 안정화

- [x] Git 추적 설정에서 Secret 형태의 기본값 제거
- [ ] 과거 노출 가능성이 있는 외부 API Key 교체 확인
- [x] `csv-*` 장소 상세·Compatibility 400 문제 해결
- [x] 동물병원 공공 API·CSV Source 분기와 fallback 해결
- [ ] CSV가 없는 환경의 동물약국 Source와 처리 방식 확정
- [x] 여행 코스 저장 버튼 연결
- [ ] 외부 API Key와 팀 실행 환경 통일
- [ ] 로그인부터 일정 저장까지 핵심 E2E 반복 검증
- [ ] 실제 KTO 호출량과 429 회귀 검증
- [ ] 제출 URL 자동배포와 운영 smoke test

### P1 — 지정과제 핵심 완결

- [ ] `detailCommon2` 장소 공통 상세
- [ ] 누락된 `detailPetTour2` 원본 필드 보존
- [ ] 보호자 수·반려견 수 규칙 파싱과 다견 동시 Compatibility
- [ ] 예방접종·견종·예약·문의·추가 요금 규칙
- [ ] 서버 검증 후보만 AI에 전달
- [ ] 저장 당시 Compatibility Snapshot
- [ ] 출발 전 최신 규정 재검증과 변경 안내
- [x] 공개 장소·날씨와 인증 필요 맞춤 기능의 Frontend Route 분리

### P2 — 반복 사용과 발전 기능

- [ ] 장소 규정 비교
- [ ] 방문 기록과 방문 반려견 Snapshot
- [ ] 사용자 현장 제보와 관리자 검토
- [ ] AI 제보 요약·유형 분류·공식정보 충돌 탐지
- [ ] 장소 정보 신뢰도
- [ ] 추천 고도화

기능 수를 더 늘리기보다 현재 핵심 흐름이 실제 배포 환경에서 끊기지 않도록 완성하는 것을 우선합니다.

---

## 개발 원칙

### 문서와 판단 우선순위

정보가 충돌하면 다음 순서를 사용합니다.

1. 사용자의 최신 명시적 팀 결정
2. 공모전 공식 공고와 FAQ
3. 외부 API 공식 명세와 실응답
4. 최신 PetWay 기획과 IA
5. `PETWAY_CODEX_CONTEXT.md`
6. Repository의 현재 코드
7. 추론

노션은 제품 목표와 요구사항의 원문이고, Repository는 현재 구현 상태입니다. 둘이 다르면 차이를 표시하고 팀 결정을 받기 전 임의로 데이터 모델이나 정책을 확정하지 않습니다.

### 작업 흐름

```text
Inspect → Plan → Implement → Test → Verify → Report
```

- 작업 전에 branch와 working tree를 확인합니다.
- 현재 코드를 조사해 이미 구현된 기능을 중복 개발하지 않습니다.
- 한 번에 여러 독립 기능을 섞지 않습니다.
- 완료 조건과 테스트 조건을 먼저 정합니다.
- 기존 변경사항을 임의로 reset하거나 revert하지 않습니다.
- 확인되지 않은 외부 API 필드와 규칙을 추측해 구현하지 않습니다.
- 신규 dependency와 infrastructure는 필요성이 분명할 때만 추가합니다.
- 명시적 요청 없이 main 직접 개발·push·merge·rebase·cherry-pick을 하지 않습니다.
- 테스트가 통과하지 않은 상태를 완료라고 보고하지 않습니다.

### Vertical Slice와 Contract First

기능은 Layer별로 따로 끝내기보다 다음과 같은 Vertical Slice로 연결합니다.

- Pet: Domain → Repository → Service → API → UI
- Place: KorPetTourService2 → Search → Common Detail → Pet Detail → Place API → UI
- Pet Intelligence: Raw Adapter → PetRule → Parser → Compatibility → Reason
- Core UX: Home → Place List → Place Detail → Compatibility → Official Raw
- 이후 흐름: Weather·Distance·WalkFit → Course·AI·Revalidation

구현 전에 `docs/contracts/`에서 입력, 출력, 상태, 오류, 완료 조건, 테스트 기준을 먼저 고정합니다. 검토 단계에서는 파일을 수정하지 않고 Contract 위반, 다른 기능 침범, 외부 API 추측, 보안, 테스트 누락, 과설계, merge blocker를 확인하고 `CRITICAL / MAJOR / MINOR`로 분류합니다.

### 기술 범위 제한

현재 React + Spring Boot 모놀리식 구조를 유지합니다. 명시적 승인 없이 다음을 추가하거나 전환하지 않습니다.

- Microservice
- Kafka, RabbitMQ
- Elasticsearch
- Kubernetes
- Vector DB
- 불필요한 Redis
- WebFlux 전환
- 별도 Python Agent Server 또는 멀티 Agent 서버
- 자체 ML 모델
- 과도한 디자인 패턴

### 배포 방향

- 목표는 최소 운영 복잡도, 빠른 자동배포, 안정적인 하나의 제출 URL입니다.
- 세션 인증을 사용하므로 가능하면 React production build와 `/api/**`를 같은 Origin에서 제공하는 구성을 우선 검토합니다.
- single-origin은 CORS와 `JSESSIONID` Cookie 처리, Frontend·Backend 버전 불일치를 줄이기 위한 권장안이며 특정 배포 플랫폼은 아직 확정하지 않습니다.
- MySQL 데이터는 재배포 후에도 유지되어야 합니다.
- 운영에서는 HTTPS, Kakao 허용 도메인, Cookie, CSRF, CORS, Secret을 검증합니다.
- Railway, AWS, GCP 등 구체 플랫폼은 팀 결정 전 이 문서에서 확정하지 않습니다.

### AI 원칙

- 서버가 실제 관광공사 후보와 Compatibility를 검증합니다.
- `UNAVAILABLE` 장소를 AI 후보로 전달하지 않습니다.
- AI가 장소, 공식 규정, Compatibility 결과를 새로 만들거나 수정하지 못하게 합니다.
- AI는 일정 조정, 설명, 정렬을 보조합니다.
- AI 실패가 일반 검색과 입장 판정을 막지 않게 합니다.

### 사용자 제보 원칙

```text
사용자 현장 제보
        ↓
AI 요약·유형 분류·공식정보 충돌 탐지
        ↓
관리자 검토
        ↓
승인 / 반려 / 확인 필요
```

사용자 제보와 AI 결과는 관광공사 공식정보나 Compatibility 규칙을 자동 수정하지 않습니다. 화면에서도 공식 관광정보와 현장 제보를 명확히 구분합니다.

---

## 팀

- 주연경
- 송병국
- 정수필

공개 Repository의 메인 README에는 개인 전화번호와 이메일을 기재하지 않습니다. 내부 연락처는 팀의 비공개 협업 문서에서 관리합니다.

---

## 관련 문서

- [PetWay 기획 Notion](https://app.notion.com/p/3b239ad79c5e804095c0c9ffcc871116)
- [`PETWAY_CODEX_CONTEXT.md`](./PETWAY_CODEX_CONTEXT.md): 구현 시 반드시 확인하는 최상위 컨텍스트
- [`README.md`](./README.md): 기존 실행 안내와 구현 상태 기록
- [`docs/README.md`](./docs/README.md): Contract·배포 문서 현황

추가할 Contract 문서:

- [ ] `docs/contracts/pet-api.md`
- [ ] `docs/contracts/place-api.md`
- [ ] `docs/contracts/weather-api.md`
- [ ] `docs/contracts/pet-rule.md`
- [ ] `docs/contracts/compatibility.md`
- [ ] `docs/contracts/course-api.md`
- [ ] `docs/deployment.md`

---

## 최종 정의

PetWay는 한국관광공사 KorPetTourService2의 실제 반려동물 동반 데이터를 필드 문맥과 공식 원문을 보존한 채 PetRule로 구조화하고, 마이펫과 방문 조건을 기준으로 Compatibility와 판정 근거·준비물을 제공한 뒤 날씨·거리·오늘 외출·여행 코스·출발 전 재검증까지 연결하는 반려생활 맞춤형 관광 서비스입니다.

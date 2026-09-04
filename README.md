# PetWay

반려견과 함께 이용할 수 있는 관광 장소와 코스를 안내하기 위한 PetWay 프로젝트입니다.
현재 저장소에는 실제 비즈니스 기능이 아닌 frontend/backend 초기 실행 골격만 포함되어 있습니다.

## 프로젝트 구조

```text
petway/
├─ frontend/                  # React + Vite + TypeScript
├─ backend/                   # Java + Spring Boot + Gradle
├─ docs/                      # 프로젝트 문서
├─ PETWAY_CODEX_CONTEXT.md    # 프로젝트 개발 기준
├─ README.md
└─ .gitignore
```

## 기술 버전

- Java 17
- Spring Boot 3.5.16
- Gradle 8.14.3 (Gradle Wrapper)
- React 19.2.8
- Vite 8.2.2
- TypeScript 7.0.2
- MySQL Connector/J 9.7.0 (MySQL 8 연결 드라이버)

## 현재 구현 상태 체크리스트

아래 체크리스트는 기존 문장을 변경하지 않고 최신 `main`의 구현 상태를 추가로 기록한 것이다.

- `[x]`: 현재 코드에 구현되어 있고 관련 빌드 또는 테스트가 통과한 항목
- `[ ]`: 미구현이거나 런타임 연결, 실제 외부 API 또는 배포 검증이 남은 항목

### 인증 및 사용자

- [x] 일반 회원가입
- [x] 세션 로그인 및 로그아웃
- [x] `/api/auth/me`
- [x] BCrypt 비밀번호 암호화
- [x] Kakao OAuth 로그인 및 기존 계정 연결
- [x] `USER`, `ADMIN` 권한 구조
- [ ] 운영 환경 CSRF, Cookie, CORS 정책 확정 및 검증
- [ ] 공모전 심사용 테스트 전용 `USER` 계정 준비

### 반려견

- [x] 다견 등록·조회·수정·삭제
- [x] 대표 반려견 설정
- [x] 품종, 체중, 체형, 활동성 및 성향 정보 관리
- [x] 동물등록번호 또는 RFID 기반 등록정보 조회
- [x] 동물등록정보 조회 결과의 등록 폼 자동 입력
- [x] 사용자 확인 후 기존 Pet 등록 API를 통한 최종 저장
- [ ] 체중 소수점 최대 2자리 허용 여부 확정 및 경계값 검증

### 장소 및 지도

- [x] KorPetTourService2 `areaBasedList2` 연동
- [x] KorPetTourService2 `locationBasedList2` 연동
- [x] KorPetTourService2 `detailPetTour2` 연동
- [x] 지역 및 현재 위치 기반 장소 조회
- [x] Kakao 지도와 관광공사 좌표 기반 마커
- [x] 문화정보원 CSV 보조 장소 데이터 연동 코드
- [ ] KorPetTourService2 `detailCommon2` 연동
- [ ] `csv-*` 장소 ID의 상세정보 및 Compatibility API 계약 수정
- [ ] 관광공사·문화정보원 혼합 결과의 중복 제거 및 페이지네이션 수정
- [ ] 문화정보원 CSV의 배포 공급 방식 확정
- [ ] 비로그인 장소·지도·날씨와 로그인 필요 맞춤 기능의 Frontend Route 분리

### PetRule 및 Compatibility

- [x] detailPetTour2 필드별 Raw Adapter
- [x] 체중, 크기, 공간 범위 Parser
- [x] 목줄, 입마개, 이동장, 배변봉투 등 준비사항 Parser
- [x] `AVAILABLE`, `CONDITIONAL`, `NEED_CONFIRMATION`, `UNAVAILABLE` 판정
- [x] 판정 근거와 준비물 응답 및 화면 표시
- [x] 공식정보와 PetWay 판정 분리
- [ ] Compatibility 최소 체중 미달 안내 문구 인코딩 수정
- [ ] 구비시설·비치품목·구매품목·대여품목 source field 보존
- [ ] 마릿수·예방접종·예약·문의·추가 요금·연령 규칙 구조화
- [ ] 실제 등록 견종과 맹견 대상 규칙 비교

### 날씨 및 반복 사용

- [x] 단기예보
- [x] 중기예보
- [x] 3개월 전망
- [x] 장소 좌표 기반 날씨 조회
- [x] 비로그인 `GET /api/weather` 허용
- [x] Browser Geolocation 기반 주변 장소 탐색
- [x] 대표 반려견·Compatibility·장소 유형·거리·날씨 기반 WalkFit
- [x] WalkFit 최대 8개 평가 및 반환
- [ ] 실제 기상청 Key를 사용한 배포 환경 smoke test
- [ ] 위치 권한 거부 시 지역 직접 선택 흐름 검증

### 여행 코스 및 AI

- [x] 여행 코스 Builder 화면
- [x] 자동차 길찾기
- [x] 대중교통 길찾기
- [x] 날짜별 날씨 조회
- [x] OpenAI 여행 코스 추천 Client와 응답 검증
- [x] AI 장애 시 Compatibility·거리·장소 유형 기반 fallback
- [x] 여행일정 저장·조회·삭제 Backend API
- [ ] 여행 코스 화면의 저장 버튼과 `saveCurrentTrip` 연결
- [ ] AI 추천 결과와 fallback 결과의 구조적 상태 구분
- [ ] 서버가 실제 TourAPI 후보와 Compatibility를 검증한 뒤 AI에 전달하는 구조
- [ ] 장소별 Compatibility snapshot 저장
- [ ] 출발 전 최신 `detailPetTour2` 재호출 및 Compatibility 재판정

### 부가 기능

- [x] 즐겨찾기
- [x] 리뷰
- [x] 동물병원 및 동물약국 화면과 API
- [ ] CSV가 없는 환경에서도 동물병원 조회가 가능한 source 분기 수정
- [ ] 사용자 현장 제보
- [ ] 관리자 제보 검토
- [ ] AI 제보 요약·유형 분류·공식정보 충돌 탐지
- [ ] 정보 신뢰도
- [ ] 방문 기록
- [ ] 장소 비교
- [ ] 여러 반려견 동시 Compatibility

### 품질 및 배포

- [x] Backend 전체 테스트 통과
- [x] Frontend production build 통과
- [x] 주변 장소 2분 bounded cache
- [x] 반려동물 상세 cache 및 동일 `contentId` single-flight
- [x] TravelCourse KTO cold worst case를 기존 약 170회에서 최대 약 50회로 축소
- [ ] 핵심 사용자 E2E 브라우저 smoke test
- [ ] 외부 API Key와 팀 실행 환경 통일
- [ ] 실제 KTO 호출량 측정 및 429 회귀 검증
- [ ] 운영 Secret 공급 절차 문서화
- [ ] `api_request_log` 구현
- [ ] 제출 URL 자동배포 및 반복 시연 검증

## Frontend 실행

필요 환경: Node.js 20.19 이상 또는 22.12 이상 (검증 환경: Node.js 24.13.0, npm 11.6.2)

```bash
cd frontend
npm ci
npm run dev
```

개발 서버 기본 주소는 `http://localhost:5173`입니다.

프로덕션 빌드는 다음 명령으로 확인합니다.

```bash
npm run build
```

## Backend 실행

필요 환경: JDK 17 이상, MySQL 8

먼저 아래 환경변수를 설정하고 `petway` 데이터베이스를 준비합니다. Windows에서는 `gradlew.bat`, macOS/Linux에서는 `./gradlew`를 사용합니다.

```bash
cd backend
./gradlew bootRun
```

빌드와 테스트는 다음 명령으로 실행합니다.

```bash
./gradlew clean build
```

## 필요한 환경변수

| 변수 | 필수 여부 | 설명 | 기본값 |
|---|---|---|---|
| `DB_URL` | 선택 | MySQL JDBC URL | `jdbc:mysql://localhost:3306/petway?serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| `DB_USERNAME` | 선택 | MySQL 사용자명 | `petway` |
| `DB_PASSWORD` | 필수 | MySQL 비밀번호 | 없음 |
| `TOUR_API_KEY` | 향후 필요 | 한국관광공사 TourAPI 키 | 없음 |
| `WEATHER_API_KEY` | 향후 필요 | 날씨 API 키 | 없음 |
| `OPENAI_API_KEY` | 향후 필요 | OpenAI API 키 | 없음 |

`backend/.env.example`은 변수명만 제공하는 템플릿입니다. `.env`, `application-secret.yml`, `application-local.yml`과 실제 비밀값은 Git에서 제외됩니다. 현재 단계에서는 외부 API 키를 사용하는 코드가 없습니다.

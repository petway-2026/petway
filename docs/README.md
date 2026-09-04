# PetWay Docs

PetWay의 설계, API 명세, 운영 문서는 이후 작업 단위에서 이 디렉터리에 추가합니다.
현재 초기 골격 단계에서는 별도 비즈니스 명세를 생성하지 않습니다.

## 현재 문서 및 구현 체크리스트

아래 항목은 기존 안내를 유지하면서 최신 `main`을 기준으로 추가한 상태 기록이다.

- `[x]`: 구현 또는 현재 문서에 존재
- `[ ]`: 문서화, 구현 또는 실제 환경 검증이 남음

### Contract 문서

- [ ] `docs/contracts/pet-api.md`
- [ ] `docs/contracts/place-api.md`
- [ ] `docs/contracts/weather-api.md`
- [ ] `docs/contracts/pet-rule.md`
- [ ] `docs/contracts/compatibility.md`
- [ ] `docs/contracts/course-api.md`
- [ ] `docs/deployment.md`

### 구현 현황

- [x] Auth 및 Kakao OAuth
- [x] 다견 Pet CRUD와 대표견
- [x] 동물등록정보 조회 및 등록 폼 자동 입력
- [x] KorPetTourService2 지역·주변 장소 검색
- [x] `detailPetTour2` 기반 반려동물 동반정보 조회
- [x] PetRule Parser와 Compatibility
- [x] Kakao 지도와 Browser Geolocation
- [x] 단기·중기·3개월 날씨
- [x] WalkFit
- [x] AI 여행 코스와 자동차·대중교통 경로
- [x] 여행일정 저장 Backend API
- [x] 즐겨찾기와 리뷰
- [x] 동물병원·동물약국 화면과 API
- [ ] `detailCommon2`
- [ ] 전체 `detailPetTour2` source field 보존
- [ ] `csv-*` 장소 상세 및 Compatibility 계약
- [ ] 여행 코스 저장 버튼 연결
- [ ] 출발 전 최신 규정 재검증
- [ ] 사용자 현장 제보와 관리자 검토
- [ ] 여러 반려견 동시 Compatibility

### 테스트 및 운영 문서

- [x] Backend 전체 테스트 통과
- [x] Frontend production build 통과
- [ ] 핵심 E2E 브라우저 테스트 절차
- [ ] 외부 API별 실제 smoke test 절차
- [ ] KTO 호출량 및 429 확인 절차
- [ ] 문화정보원 CSV 로컬·CI·배포 공급 절차
- [ ] 환경변수와 Secret 목록 최신화
- [ ] 공개 API와 인증 필요 API 목록
- [ ] 공모전 테스트 계정 생성 및 검증 절차
- [ ] 운영 CSRF, Cookie, CORS 정책

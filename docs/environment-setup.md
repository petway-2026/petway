# 환경별 설정과 실행

최상위 `.env.production`과 Compose 파일은 기존 서버 설정 그대로 유지한다.

| 파일 | 역할 |
| --- | --- |
| `.env.production` | 서버 Docker Compose 변수 원본. 수정하지 않음 |
| `backend/src/main/resources/application.yml` | 공통 OAuth, API, JPA 설정. 비밀 값은 변수로 참조 |
| `backend/src/main/resources/application-dev.yml` | 기본 `dev` 프로필. 로컬 DB 3307, schema update, HTTP 쿠키 |
| `backend/src/main/resources/application-prod.yml` | 명시적 `prod` 프로필. 환경변수 DB, schema validate, HTTPS 쿠키 |
| `backend/.env.dev.properties` | Git 제외 로컬 백엔드 값. dev에서만 명시적으로 읽음 |
| `frontend/.env.development.local` | `npm run dev`용 API 주소와 카카오 JavaScript 키 |
| `frontend/.env.production.local` | 로컬 `npm run build`용 동일 origin API(`/`)와 지도 키 |

## 로컬 백엔드

처음 설정할 때 `backend/.env.example`을 `backend/.env.dev.properties`로 복사한다.
현재 작업 PC에는 최상위 ENV의 서버 API 키를 복사해 두었다. 원본이 바뀌어도 자동 동기화하지 않는다.
`DB_PASSWORD`는 **로컬 MySQL 비밀번호로 교체해야 한다**. 로컬 DB 주소/계정이 다르면 `DB_URL`, `DB_USERNAME`도 설정한다.
운영 MySQL 계정이나 Docker 내부 호스트 `mysql`을 로컬 연결 정보로 추정해서 사용하지 않는다.

```powershell
cd D:\team_repo\petway\backend
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

IDE의 Active profiles는 `dev`, working directory는 backend 또는 저장소 최상위로 지정한다.
프로필을 생략해도 기본값은 `dev`다. 환경변수나 실행 인자로 지정한 프로필이 우선한다.
로컬 값 파일은 Java properties 문법이다. 값을 따옴표로 감싸지 않고, 백슬래시는 `\\`로 쓴다.
Spring은 임의 `.env`를 자동으로 읽지 않으므로 dev YML에 해당 파일을 명시적으로 import한다.

## 로컬 프론트

새 PC에서는 `frontend/.env.example`을 `.env.development.local`로 복사하고 지도 키를 입력한다.
기존 `.env.local`은 모든 모드에 적용되므로 이번 작업에서 `.env.development.local`로 옮겼다.

```powershell
cd D:\team_repo\petway\frontend
npm run dev
```

로컬 배포 빌드는 `.env.production.example`을 `.env.production.local`로 복사해서 준비한다.
`VITE_API_URL=/` 빌드 결과는 `/api`와 OAuth 경로를 백엔드로 전달하는 기존 Nginx 같은 reverse proxy에서 제공한다.
단독 `vite preview`에는 API proxy가 없으므로 API 연동 검증용으로 사용하지 않는다.
`VITE_` 변수는 브라우저에 포함되므로 DB 비밀번호나 서버용 API 키를 넣지 않는다.

## 서버 배포

기존 HTTPS 배포 명령을 그대로 사용한다.

```sh
docker compose --env-file .env.production -f compose.yaml -f compose.production.yaml up -d --build
```

Compose가 `MYSQL_*`를 백엔드 `DB_*`로 변환하고 `SPRING_PROFILES_ACTIVE=prod`를 전달한다.
프론트에는 빌드 시 `VITE_API_URL=/`, `VITE_KAKAO_MAP_KEY`를 전달한다.
로컬 ENV는 Docker build context에서 제외되어 서버 이미지에 복사되지 않는다.
프론트 환경변수 변경은 재빌드가 필요하다. 이 작업에서는 서버 배포를 실행하지 않았다.

참고: [Spring 외부 설정](https://docs.spring.io/spring-boot/reference/features/external-config.html), [Vite 환경변수와 모드](https://vite.dev/guide/env-and-mode).

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

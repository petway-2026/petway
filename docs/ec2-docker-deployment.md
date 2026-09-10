# AWS EC2 단일 서버 Docker Compose 배포

이 문서는 서울 리전(`ap-northeast-2`)의 EC2 한 대에서 PetWay의 nginx/React,
Spring Boot, MySQL 8을 Docker Compose로 운영하는 절차를 설명한다. 외부에는 nginx의
80/443만 공개하며 backend 8081과 MySQL 3306은 Docker bridge network에서만 사용한다.

## 1. 준비 사항

- 권장 운영체제: Ubuntu 24.04 LTS
- 권장 메모리: 2GB 이상
- Docker Engine 및 Docker Compose plugin
- EC2에 연결된 고정 공인 IP 또는 Elastic IP
- EC2를 가리키는 DNS A/AAAA 레코드
- Kakao Dashboard에 등록할 실제 HTTPS 도메인

보안 그룹의 inbound는 다음으로 제한한다.

- TCP 80: 인터넷
- TCP 443: 인터넷
- 관리 접속은 가능하면 AWS Systems Manager Session Manager를 사용한다.
- SSH가 필요하면 TCP 22를 관리자 고정 IP에만 허용한다.
- TCP 8081과 3306은 열지 않는다.

## 2. 환경변수

예제 파일을 복사한 후 EC2 안에서만 실제 값을 입력한다.

```bash
cp .env.production.example .env.production
chmod 600 .env.production
```

`.env.production`은 Git과 Docker build context에서 제외된다. `VITE_KAKAO_MAP_KEY`는
브라우저 공개 키이며, DB 비밀번호나 REST/OAuth secret을 `VITE_*`에 넣지 않는다.

동일 도메인 구성에서는 다음 값이 Compose에서 자동으로 만들어진다.

- 프런트 API origin: `https://${APP_DOMAIN}`
- OAuth callback: `https://${APP_DOMAIN}/login/oauth2/code/kakao`
- CORS origin: `https://${APP_DOMAIN}`
- Session cookie: `Secure`, `HttpOnly`, `SameSite=Lax`

Kakao Dashboard에는 JavaScript 도메인과 Redirect URI를 위 값과 정확히 일치시킨다.

## 3. 인증서 없이 로컬 HTTP 검증

기본 Compose 파일은 인증서 파일을 요구하지 않는다. 로컬 placeholder는 외부 API 호출용이
아니며, DB/컨테이너/프록시 배선 검증에만 사용한다.

```bash
docker compose config
docker compose build
docker compose up -d
docker compose ps
curl -fsS http://localhost/healthz
curl -I http://localhost/
curl -i http://localhost/api/auth/me
```

`/api/auth/me`는 로그인하지 않은 요청이므로 HTTP 401이 정상이다. 이 경로는 이미 존재하는
보호 API이며 healthcheck도 401을 정상 상태로 판단한다. healthcheck용 운영 API를 새로
공개하지 않는다.

로컬 HTTP에서는 운영 프로필의 Secure cookie를 브라우저가 다시 전송하지 않으므로 실제
로그인 유지 검증은 HTTPS 배포 후 수행한다.

컨테이너만 종료할 때는 다음을 사용한다. MySQL named volume은 남는다.

```bash
docker compose down
```

`docker compose down -v`, `docker volume prune`, `docker volume rm`은 사용하지 않는다.

## 4. 최초 HTTPS 인증서 발급

먼저 기본 HTTP 구성을 실행해 ACME webroot를 외부에 제공한다.

```bash
docker compose --env-file .env.production up -d --build
```

EC2 호스트에 Certbot을 설치하고 저장소의 webroot를 지정한다.

```bash
sudo apt-get update
sudo apt-get install -y certbot
sudo certbot certonly --webroot \
  --webroot-path "$(pwd)/deploy/certbot/www" \
  --domain "$APP_DOMAIN" \
  --email "$LETSENCRYPT_EMAIL" \
  --agree-tos --no-eff-email
```

셸에서 `.env.production`을 자동으로 실행하지 않는다. 위 `$APP_DOMAIN`과
`$LETSENCRYPT_EMAIL`은 운영자가 안전하게 export하거나 실제 값으로 입력한다.
인증서가 만들어진 후 HTTPS override를 적용한다.

```bash
docker compose \
  --env-file .env.production \
  -f compose.yaml \
  -f compose.production.yaml \
  up -d --build
```

운영 nginx는 HTTP의 ACME challenge를 제외한 요청을 HTTPS로 리다이렉트한다.

## 5. 인증서 갱신

Ubuntu의 Certbot systemd timer를 활성화하고 갱신 성공 후 nginx를 reload한다.

```bash
sudo systemctl enable --now certbot.timer
sudo certbot renew --dry-run
```

갱신 후 적용 명령:

```bash
docker compose \
  --env-file .env.production \
  -f compose.yaml \
  -f compose.production.yaml \
  exec frontend nginx -s reload
```

`certbot renew --deploy-hook`에는 저장소 절대 경로에서 위 reload 명령을 실행하는 root 소유
스크립트를 연결한다. `.env.production`의 내용을 로그로 출력하지 않는다.

## 6. MySQL 초기화와 영속성

MySQL은 `mysql_data` named volume의 `/var/lib/mysql`에 저장된다. 새 볼륨에서만
`deploy/mysql/init/001-schema.sql`이 실행된다. 이 파일은 Hibernate validation을 통과한
로컬 MySQL의 여섯 테이블을 schema-only로 덤프한 것이며 INSERT와 로컬 AUTO_INCREMENT
시작값을 포함하지 않는다.

기존 named volume에서는 init SQL이 다시 실행되지 않는다. 이후 스키마 변경은 검토된
마이그레이션 SQL로 별도 적용해야 한다.

백업 예시:

```bash
docker compose --env-file .env.production exec -T mysql \
  mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --routines --triggers "$MYSQL_DATABASE" \
  > "petway-$(date +%Y%m%d-%H%M%S).sql"
```

비밀번호가 명령 기록에 남지 않도록 실제 운영에서는 MySQL client option file 또는
Docker secret 도입을 권장한다. 복구 명령은 대상 DB를 덮어쓸 수 있으므로 자동 실행하지 않는다.

## 7. 문화시설 CSV

Docker image가 외부 다운로드 상태에 의존하지 않도록 한국문화정보원의 공개 문화시설 CSV를
backend resource로 함께 배포한다.

- 파일 크기: 30,633,222 bytes (약 29.21 MiB)
- SHA-256: `2F88BEDFF41A8B9F032ABD16CE2FB0BC31D91EC28EE559E6E79C2559A2F45928`
- 데이터 출처: 공공데이터포털의 한국문화정보원 「전국 반려동물 동반 가능 문화시설 위치 데이터」
- 활용 목적: 반려인 대상 앱 서비스 및 전국 반려동물 동반 가능 시설 분석

GitHub 단일 파일 제한 100MB보다 작다. 데이터 출처와 최신 작성일을 유지하고 재배포 전에
공공데이터포털의 이용 조건 변경 여부를 확인한다.

## 8. 배포 확인

```bash
docker compose \
  --env-file .env.production \
  -f compose.yaml \
  -f compose.production.yaml \
  ps

curl -fsS "https://${APP_DOMAIN}/healthz"
curl -I "https://${APP_DOMAIN}/"
curl -i "https://${APP_DOMAIN}/api/auth/me"
```

추가 확인 항목:

- `/some/react/route`가 `index.html`을 반환한다.
- `/api/auth/me`가 nginx를 거쳐 401을 반환한다.
- Kakao OAuth callback이 `/login/oauth2/code/kakao`로 돌아온다.
- 로그인 후 새로고침해도 세션이 유지된다.
- `JSESSIONID`에 Secure, HttpOnly, SameSite=Lax가 있다.
- EC2 외부에서 8081과 3306에 연결할 수 없다.
- `docker compose ps`에서 세 핵심 서비스가 healthy다.

## 9. 업데이트와 롤백

배포 전 DB를 백업하고 새 이미지를 빌드한다.

```bash
docker compose \
  --env-file .env.production \
  -f compose.yaml \
  -f compose.production.yaml \
  up -d --build
```

애플리케이션 롤백은 이전 Git commit 또는 이미지 tag로 되돌린 뒤 같은 `up -d --build`를
실행한다. `mysql_data`는 그대로 유지한다. DB 스키마 롤백은 사전 검토한 SQL 또는 백업 복구가
필요하며 데이터 손실 가능성이 있으므로 자동화하지 않는다.

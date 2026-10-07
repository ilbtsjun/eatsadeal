# EatsADeal (잇츠 어 딜)

치킨·피자·햄버거 프랜차이즈 15개 브랜드의 진행 중인 이벤트를 한곳에 모아 보여주는 서비스입니다.
브랜드별 공식 이벤트 페이지를 주기적으로 수집(크롤링)하고, 카테고리·브랜드·혜택 유형·키워드로 검색할 수 있습니다.

> 포트폴리오 목적의 개인 프로젝트입니다. 수집한 이벤트의 원본 출처는 각 브랜드 공식 사이트이며, 이벤트 클릭 시 원본 페이지로 이동합니다.

- 서비스: https://www.eatsadeal.kr
- 저장소: https://github.com/ilbtsjun/eatsadeal

## 주요 기능

**사용자**
- 이벤트 목록/검색: 카테고리, 브랜드, 혜택 유형(정액 할인, 1+1, 배달비 무료 등 10종), 키워드 검색, 정렬(최신순/인기순/마감임박순/오래된순), 페이지네이션
- 이벤트 상세 조회, 조회수 집계(같은 IP는 3시간에 1회만 반영, Redis)
- 회원가입(이메일 인증코드), 로그인(JWT), 로그아웃(토큰 블랙리스트), 비밀번호 재설정, 회원 탈퇴(개인정보 마스킹)
- 즐겨찾기, 댓글(작성/수정/삭제)
- 이벤트 제보 (비로그인 가능, 관리자 메일로 발송)

**관리자**
- 브랜드·카테고리·이벤트 CRUD (브랜드 삭제는 소프트 삭제라 수집된 이벤트는 유지)
- 회원 조회 및 이용 정지, 댓글 숨김/해제
- 크롤링 수동 실행 (전체 / 카테고리 / 브랜드 단위)

**크롤러**
- 15개 브랜드를 6시간마다 자동 수집 (`0 0 0/6 * * *`, Asia/Seoul)
- 동시 실행 방지(이미 실행 중이면 건너뜀), 브랜드 단위 실패 격리, 이벤트 URL 기준 upsert

| 카테고리 | 브랜드 |
|---|---|
| 치킨 | BHC, BBQ, 교촌, 페리카나, 굽네 |
| 피자 | 도미노, 파파존스, 피자마루, 피자에땅, 피자스쿨 |
| 햄버거 | 버거킹, 롯데리아, KFC, 맘스터치, 프랭크버거 |

## 기술 스택

| 영역 | 사용 기술 |
|---|---|
| Backend | Java 17, Spring Boot 4.1, Spring Security, Spring Data JPA, Validation, Spring Mail, Actuator |
| 인증 | JWT (jjwt 0.12, HS256), Redis 토큰 블랙리스트, 로그인 시도 제한 |
| DB / 캐시 | MySQL 8, Redis (조회수 중복 방지, 인증코드, rate limit, 블랙리스트), Flyway |
| 크롤링 | `java.net.http.HttpClient`, Jackson, 정규식 파싱 (공개 API가 있는 사이트는 API 사용) |
| API 문서 | springdoc-openapi (Swagger UI, 개발 환경에서만 활성화) |
| Frontend | React 19, Vite 8 (라우터 라이브러리 없이 상태 기반 화면 전환) |
| 인프라 | Docker Compose, Nginx, Caddy (자동 HTTPS), GitHub Actions → AWS EC2 |
| 테스트 | JUnit 5, Mockito (서비스 계층 단위 테스트) |

## 아키텍처

```
Client ──HTTPS──▶ Caddy ──▶ Nginx (React 정적 파일, /api 프록시) ──▶ Spring Boot ──▶ MySQL
                                                                         │
                                                                         └──────────▶ Redis
                                                                         │
                                       @Scheduled / 관리자 API ──▶ Crawlers ──▶ 각 브랜드 사이트
```

백엔드는 도메인 단위로 패키지를 나눈 구조입니다 (`controller / service / repository / entity / dto`).

```
backend/src/main/java/com/backend
├── auth/       로그인, 회원가입, JWT, 이메일 인증, 로그인 시도 제한
├── user/       회원 정보, 마이페이지, 정지/탈퇴
├── brand/      브랜드, 브랜드-카테고리
├── category/   카테고리
├── event/      이벤트 검색/조회/관리, 혜택 유형(EventCode)
├── comment/    댓글
├── favorite/   즐겨찾기
├── report/     제보 (메일 발송, rate limit)
├── mail/       메일 발송
├── crawler/    Crawler 인터페이스, 브랜드별 구현(target/), 스케줄러, 수동 실행 API
└── common/     보안 설정, 예외 처리, Redis 유틸, CUD 로깅(AOP)
```

새 브랜드를 추가하려면 `Crawler` 인터페이스를 구현하고, `CrawlTarget`과 `CrawlerService`에 등록한 뒤, 브랜드 데이터를 Flyway 마이그레이션으로 추가합니다.

## 로컬 실행

### 사전 준비
- JDK 17, Node.js 22, MySQL 8, Redis

### 1) Redis
```bash
docker compose up -d        # docker-compose.yml: 개발용 Redis(6379)만 실행
```
MySQL은 로컬에 설치된 것을 사용합니다. DB(`eatsadeal`)는 없으면 자동 생성되고, 스키마와 초기 데이터(카테고리/브랜드)는 Flyway가 적용합니다.

### 2) 백엔드 (8080)
개발 프로파일(`dev`)은 아래 환경변수를 필요로 합니다.

```bash
export SPRING_PROFILES_ACTIVE=dev
export DB_USERNAME=root
export DB_PASSWORD=<MySQL 비밀번호>
export JWT_SECRET=<32바이트 이상 랜덤 문자열>
export MAIL_USERNAME=<Mailtrap 사용자명>     # dev는 Mailtrap sandbox SMTP 사용
export MAIL_PASSWORD=<Mailtrap 비밀번호>
export REPORT_MAIL_TO=<제보 수신 주소>
export ADMIN_EMAIL=<초기 관리자 이메일>      # 비워 두면 관리자 계정을 만들지 않음
export ADMIN_PASSWORD=<초기 관리자 비밀번호>

cd backend
./gradlew bootRun
```

Swagger UI: http://localhost:8080/swagger-ui.html

### 3) 프론트엔드 (5173)
```bash
cd frontend
npm ci
npm run dev
```
`/api` 요청은 `vite.config.js`의 프록시를 통해 `localhost:8080`으로 전달됩니다.

### 4) 크롤링 실행
관리자로 로그인해 발급받은 토큰으로 호출합니다. (스케줄러는 서버 기동 후 6시간 주기로 자동 실행)

```bash
curl -X POST http://localhost:8080/api/crawl            -H "Authorization: Bearer $TOKEN"   # 전체
curl -X POST http://localhost:8080/api/crawl/chicken    -H "Authorization: Bearer $TOKEN"   # 카테고리
curl -X POST http://localhost:8080/api/crawl/bhc        -H "Authorization: Bearer $TOKEN"   # 브랜드
```
자동 크롤링은 `app.crawler.schedule-enabled=false`로 끄고, `app.crawler.cron`으로 주기를 바꿀 수 있습니다.

### 테스트
```bash
cd backend
./gradlew test
```

## API 개요

전체 명세는 개발 환경의 Swagger UI에서 확인할 수 있습니다. (운영에서는 비활성화)

| 영역 | 엔드포인트 | 권한 |
|---|---|---|
| 인증 | `POST /api/auth/signup`, `/login`, `/email-verification`, `/password-change`, `/password-verification` | 공개 |
| | `POST /api/auth/logout` | 로그인 |
| 이벤트 | `GET /api/events`, `/api/events/{id}`, `/api/events/event-codes`, `/api/events/{id}/comments` | 공개 |
| | `POST /api/events/{id}/comments`, `POST·DELETE /api/events/{id}/favorite` | 로그인 |
| | `POST /api/events`, `PATCH·DELETE /api/events/{id}` | 관리자 |
| 브랜드/카테고리 | `GET /api/brands`, `/api/categories` (+ `/{id}`) | 공개 |
| | `POST`, `PATCH`, `DELETE`, `PATCH /api/brands/{id}/active` | 관리자 |
| 댓글 | `PATCH·DELETE /api/comments/{id}`, `GET /api/comments` | 로그인 |
| | `PATCH /api/comments/{id}/hide`, `/unhide`, `GET /api/comments/user/{userId}` | 관리자 |
| 회원 | `GET /api/user/email-availability`, `/nickname-availability` | 공개 |
| | `GET·PATCH·DELETE /api/user/me`, `PATCH /api/user/me/password`, `GET /api/user/me/favorites` | 로그인 |
| | `GET /api/user/admin/user`, `PATCH /api/user/admin/{id}/suspension` | 관리자 |
| 제보 | `POST /api/reports` | 공개 (rate limit) |
| 크롤링 | `POST /api/crawl`, `/api/crawl/{group 또는 brand}` | 관리자 |

## 배포

`main` 브랜치에 push하면 GitHub Actions(`.github/workflows/deploy.yml`)가 EC2에 SSH로 접속해 배포합니다.

1. 서버에서 해당 커밋으로 `git reset --hard`
2. `docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build`
3. 사이트의 `/version.txt`가 방금 배포한 커밋 SHA와 일치하는지 확인
4. `/api/categories` 호출로 API 응답 확인

구성 (`docker-compose.prod.yml`): `mysql`, `redis`(비밀번호 + AOF), `backend`(healthcheck), `frontend`(Nginx), `caddy`(80/443, 자동 HTTPS). DB와 Redis는 외부 포트를 열지 않고 내부 네트워크로만 접근합니다.

### 서버 최초 설정
```bash
git clone https://github.com/ilbtsjun/eatsadeal.git ~/eatsadeal && cd ~/eatsadeal
cp .env.example .env.prod      # 값을 채운다 ($ 문자는 $$ 로 이스케이프)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

GitHub Secrets: `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `DOMAIN`

### 환경변수 (`.env.example` 참고)

| 변수 | 설명 |
|---|---|
| `MYSQL_ROOT_PASSWORD`, `DB_USERNAME`, `DB_PASSWORD` | MySQL (앱은 root가 아닌 전용 계정 사용) |
| `REDIS_PASSWORD` | Redis 비밀번호 |
| `JWT_SECRET`, `JWT_EXPIRATION_MS` | JWT 서명 키(32바이트 이상), 만료 시간(기본 1시간) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | 운영 SMTP (Gmail 앱 비밀번호) |
| `REPORT_MAIL_TO` | 제보 수신 주소 |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | 최초 관리자 계정 (비우면 생성 안 함) |

## 보안 / 운영 참고

- 비밀번호는 DelegatingPasswordEncoder로 해시, 로그인 실패 횟수 제한, 인증 메일/제보에 Redis 기반 rate limit 적용
- 운영 환경에서는 Swagger와 상세 SQL 로그를 끄고, Actuator는 `health`만 노출
- 크롤러 요청의 User-Agent에 프로젝트와 연락처를 명시합니다. 사이트별 이용약관·robots.txt를 확인하고 요청 빈도를 낮게 유지해야 하며, 상업적으로 전환할 경우 각 브랜드의 약관(스크래핑 금지 조항 포함)을 먼저 검토해야 합니다.

## 로드맵

- [ ] 제보 내용 DB 저장 (현재는 메일 발송만)
- [ ] 크롤러 테스트 (HTML/JSON 응답 픽스처 기반)
- [ ] 컨트롤러·통합 테스트 (Testcontainers)
- [ ] 댓글 대댓글, 이벤트 상세 댓글 UI 보강
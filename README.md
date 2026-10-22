# EatsADeal (잇츠 어 딜)

치킨·피자·햄버거 프랜차이즈 14개 브랜드의 진행 중인 이벤트를 한곳에 모아 보여주는 서비스입니다.
브랜드별 공식 이벤트 페이지를 주기적으로 수집(크롤링)하고, 카테고리·브랜드·혜택 유형·키워드로 검색할 수 있습니다.
(롯데리아는 robots.txt가 크롤링을 막아 수집 대상에서 제외했습니다.)

> 포트폴리오 목적의 개인 프로젝트입니다. 수집한 이벤트의 원본 출처는 각 브랜드 공식 사이트이며, 이벤트 클릭 시 원본 페이지로 이동합니다.

- 서비스: https://www.eatsadeal.kr
- 저장소: https://github.com/ilbtsjun/eatsadeal

## 주요 기능

**사용자**
- 이벤트 목록/검색: 카테고리, 브랜드, 혜택 유형(정액 할인, 1+1, 배달비 무료 등 10종, 필터 버튼 제공), 키워드 검색, 정렬(최신순/인기순/마감임박순/오래된순), 페이지네이션
- 이벤트 상세 조회, 조회수 집계(같은 IP는 3시간에 1회만 반영, Redis)
- 회원가입(이메일 인증코드), 로그인(JWT), 로그아웃(토큰 블랙리스트), 비밀번호 재설정, 회원 탈퇴(개인정보 마스킹)
- 즐겨찾기, 댓글(작성/수정/삭제)
- 이벤트 제보 (비로그인 가능, 관리자 메일로 발송, 10분에 5건·같은 내용 중복 제한)

**관리자**
- 브랜드·카테고리·이벤트 CRUD (브랜드 삭제는 소프트 삭제라 수집된 이벤트는 유지)
- 회원 조회 및 이용 정지, 댓글 숨김/해제
- 이벤트 상세에서 수정 모달: 내용 수정, 사용자 화면 숨김, 크롤러 잠금(잠근 이벤트는 크롤러가 덮어쓰지 않음)
- 크롤링 수동 실행 (전체 / 카테고리 / 브랜드 단위), 실행 이력(브랜드별 수집·신규·변경·동일·오류 건수와 소요 시간) 대시보드
- 혜택 유형이 없는 기존 이벤트 일괄 분류 (규칙 + AI): 크롤링 관리 탭의 "이벤트 코드 분류 실행" 버튼(50건씩 반복 호출, 끝나면 오늘 AI 호출 수 표시) 또는 API

**크롤러**
- 14개 브랜드를 6시간마다 자동 수집 (`0 0 0/6 * * *`, Asia/Seoul), 브랜드는 병렬로 수집(`app.crawler.parallelism`, 기본 5)
- 공통 HTTP 클라이언트: 호스트별 요청 간격, 타임아웃, 429/5xx 재시도(지수 백오프), 출처를 밝힌 User-Agent
- 동시 실행 방지(이미 실행 중이면 건너뜀), 브랜드 단위 실패 격리, 이벤트 URL(추적 파라미터·끝 슬래시 정규화) 기준 upsert
- 신규/변경/동일을 구분해 집계하고, 내용이 같으면 쓰지 않음. 실행 결과는 `crawl_run`에 브랜드별로 저장(성공/부분 실패/수집 0건/실패)
- 실패·부분 실패·수집 0건이면 관리자에게 메일 알림 (`app.crawler.alert-enabled`)
- 오류 없이 끝난 브랜드에서 사이트 목록에서 사라진 이벤트는 비활성화(`app.crawler.deactivate-missing`). 수집 건수가 평소의 절반 미만이면 사이트 오류로 보고 건너뜀
- 날짜 정보가 없는 사이트는 처음 수집한 날짜부터 60일을 기간으로 사용
- 관리자가 수정한 이벤트(`admin_locked`)는 덮어쓰지 않음

**성능**
- 이벤트 목록 조회의 N+1 개선: `JOIN FETCH`(브랜드)와 `@BatchSize`(혜택 유형)로 요청당 쿼리를 20개에서 3개로 줄임 (`@QueryTrace`로 측정, 상세는 `portfolio/n-plus-1-improvement.md`)

**혜택 유형(EventCode) 분류**
- 새 이벤트를 저장하거나 제목·이미지가 바뀐 이벤트를 갱신할 때만 분류 (내용이 같으면 다시 분류하지 않음, 관리자 잠금 이벤트 제외)
- 1단계 키워드 규칙 → 2단계 AI(제목) → 3단계 AI(이벤트 배너 이미지) 순서로 시도하고, 앞 단계에서 정해지면 멈춤
- 크롤링이 끝날 때마다 혜택 유형이 비어 있는 이벤트를 자동으로 이어서 분류 (한 번에 최대 100건, `app.crawler.classify-after-crawl`로 끌 수 있고, 실패해도 크롤링 결과에는 영향 없음)
- AI는 Anthropic Claude Haiku 5.5. 하루 호출 상한(`app.ai.daily-limit`, 기본 200)과 짧은 응답 제한으로 비용을 제어하고, `ANTHROPIC_API_KEY`가 없거나 `app.ai.enabled=false`이면 AI 없이 규칙만 사용

| 카테고리 | 브랜드 |
|---|---|
| 치킨 | BHC, BBQ, 교촌, 페리카나, 굽네 |
| 피자 | 도미노, 파파존스, 피자마루, 피자에땅, 피자스쿨 |
| 햄버거 | 버거킹, KFC, 맘스터치, 프랭크버거 (롯데리아는 robots.txt 때문에 수집 제외) |

## 기술 스택

| 영역 | 사용 기술 |
|---|---|
| Backend | Java 17, Spring Boot 4.1, Spring Security, Spring Data JPA, Validation, Spring Mail, Actuator |
| 인증 | JWT (jjwt 0.12, HS256), Redis 토큰 블랙리스트, 로그인 시도 제한 |
| DB / 캐시 | MySQL 8, Redis (조회수 중복 방지, 인증코드, rate limit, 블랙리스트), Flyway |
| 크롤링 | `java.net.http.HttpClient`(공통 래퍼), Jackson, 정규식 파싱 (공개 API가 있는 사이트는 API 사용) |
| AI | Anthropic Java SDK, Claude Haiku 5.5 (혜택 유형 분류, 선택 기능) |
| API 문서 | springdoc-openapi (Swagger UI, 개발 환경에서만 활성화) |
| Frontend | React 19, Vite 8, React Router 8 (URL 기반 라우팅, 로그인 필요 화면은 보호 라우트) |
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
├── event/      이벤트 검색/조회/관리, 혜택 유형(EventCode)과 분류(규칙 + AI)
├── comment/    댓글
├── favorite/   즐겨찾기
├── report/     제보 (메일 발송, rate limit)
├── mail/       메일 발송
├── crawler/    Crawler 인터페이스, 공통 HTTP 클라이언트(common/), 브랜드별 구현(target/), 스케줄러, 수동 실행·이력 API
└── common/     보안 설정, 예외 처리, Redis 유틸, CUD 로깅(AOP)
```

프론트엔드는 기능 단위로 나눈 구조입니다.

```
frontend/src
├── api/        API 호출 (도메인별 파일, 공통 client)
├── auth/       로그인 상태(AuthContext), 토큰 관리
├── routes/     라우트 래퍼, 보호 라우트(RequireAuth), 약관 오버레이
├── pages/      화면 단위 컴포넌트
├── features/   도메인별 컴포넌트·훅 (events, admin, comments, favorites, reports, account, categories)
└── common/     공통 컴포넌트, 훅, 유틸
```

새 브랜드를 추가하려면 `Crawler` 인터페이스를 구현하고(공통 HTTP 클라이언트 `CrawlHttpClient`와 `CrawlerUtil`을 사용), `CrawlTarget`에 등록한 뒤, 브랜드 데이터를 Flyway 마이그레이션으로 추가합니다.

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
export ANTHROPIC_API_KEY=<선택: Anthropic API 키>  # 없으면 AI 분류 없이 규칙만 사용

cd backend
./gradlew bootRun
```

Swagger UI: http://localhost:8080/swagger-ui.html

개발 프로파일에서는 `app.query-trace.enabled=true`가 기본값이라, `@QueryTrace`가 붙은 서비스 메서드의 SQL 수와 반복 쿼리가 로그에 남습니다.

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
실행 이력은 `GET /api/crawl/runs`(최근 이력), `GET /api/crawl/runs/latest`(직전 실행)로 조회할 수 있고, 관리자 화면에서도 볼 수 있습니다.
자동 크롤링은 `app.crawler.schedule-enabled=false`로 끄고, `app.crawler.cron`으로 주기를 바꿀 수 있습니다.

### 5) 혜택 유형 일괄 분류
혜택 유형이 없는 진행 중 이벤트를 규칙과 AI로 분류합니다. 응답의 `nextAfterId`를 `afterId`로 넘겨 이어서 호출합니다.
```bash
curl -X POST "http://localhost:8080/api/events/event-codes/backfill?limit=50" -H "Authorization: Bearer $TOKEN"
```

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
| | `POST /api/events`, `PATCH·DELETE /api/events/{id}`, `PATCH /api/events/{id}/hidden`, `/lock`, `POST /api/events/event-codes/backfill` | 관리자 |
| 브랜드/카테고리 | `GET /api/brands`, `/api/categories` (+ `/{id}`) | 공개 |
| | `POST`, `PATCH`, `DELETE`, `PATCH /api/brands/{id}/active` | 관리자 |
| 댓글 | `PATCH·DELETE /api/comments/{id}`, `GET /api/comments` | 로그인 |
| | `PATCH /api/comments/{id}/hide`, `/unhide`, `GET /api/comments/user/{userId}` | 관리자 |
| 회원 | `GET /api/user/email-availability`, `/nickname-availability` | 공개 |
| | `GET·PATCH·DELETE /api/user/me`, `PATCH /api/user/me/password`, `GET /api/user/me/favorites` | 로그인 |
| | `GET /api/user/admin/user`, `PATCH /api/user/admin/{id}/suspension` | 관리자 |
| 제보 | `POST /api/reports` | 공개 (rate limit) |
| 크롤링 | `POST /api/crawl`, `/api/crawl/{group 또는 brand}`, `GET /api/crawl/runs`, `/api/crawl/runs/latest` | 관리자 |

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

GitHub Secrets: `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `DOMAIN`, `ANTHROPIC_API_KEY`(선택)

`ANTHROPIC_API_KEY` 시크릿이 있으면 배포 때 서버의 `.env.prod`에 키를 반영합니다. 키는 SSH 표준입력으로만 전달해 서버 프로세스 목록에 남지 않고, 시크릿이 없으면 서버의 기존 값을 그대로 둡니다.

### 환경변수 (`.env.example` 참고)

| 변수 | 설명 |
|---|---|
| `MYSQL_ROOT_PASSWORD`, `DB_USERNAME`, `DB_PASSWORD` | MySQL (앱은 root가 아닌 전용 계정 사용) |
| `REDIS_PASSWORD` | Redis 비밀번호 |
| `JWT_SECRET`, `JWT_EXPIRATION_MS` | JWT 서명 키(32바이트 이상), 만료 시간(기본 1시간) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | 운영 SMTP (Gmail 앱 비밀번호) |
| `REPORT_MAIL_TO` | 제보 수신 주소 |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | 최초 관리자 계정 (비우면 생성 안 함) |
| `ANTHROPIC_API_KEY`, `AI_ENABLED`, `AI_DAILY_LIMIT` | 혜택 유형 AI 분류 (키가 없으면 꺼짐, 하루 호출 상한 기본 200) |

## 보안 / 운영 참고

- 비밀번호는 DelegatingPasswordEncoder로 해시, 로그인 실패 횟수 제한, 인증 메일/제보에 Redis 기반 rate limit 적용
- 운영 환경에서는 Swagger와 상세 SQL 로그를 끄고, Actuator는 `health`만 노출
- API 키는 코드·설정 파일·git에 두지 않고 환경변수(서버 `.env.prod`, GitHub Secrets)로만 전달합니다. AI 호출은 하루 상한과 Console의 월 사용 한도로 비용을 제한합니다.
- 크롤러 요청의 User-Agent에 프로젝트와 연락처를 명시합니다. 사이트별 이용약관·robots.txt를 확인하고 요청 빈도를 낮게 유지해야 하며, 상업적으로 전환할 경우 각 브랜드의 약관(스크래핑 금지 조항 포함)을 먼저 검토해야 합니다.

## 로드맵

- [ ] 제보 내용 DB 저장 (현재는 메일 발송만)
- [ ] 크롤러 파싱 테스트 (HTML/JSON 응답 픽스처 기반)
- [ ] 이벤트 검색 통합 테스트 (Testcontainers)
- [ ] 그 외 도메인 컨트롤러·통합 테스트
- [ ] 크롤러 수집 건수 급감 감지 알림
- [ ] 댓글 대댓글, 이벤트 상세 댓글 UI 보강
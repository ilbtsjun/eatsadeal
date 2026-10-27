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
- 설명이 없는 이벤트는 AI가 배너 이미지를 읽고 1~2문장 설명을 자동 작성 (배너에 적힌 내용만 사용)
- 카카오톡·슬랙 등에 이벤트 링크를 공유하면 제목·설명·배너 이미지 미리보기 표시
- 회원가입(이메일 인증코드), 로그인(JWT), 로그아웃(토큰 블랙리스트), 비밀번호 재설정, 회원 탈퇴(개인정보 마스킹)
- 즐겨찾기, 댓글(작성/수정/삭제)
- 이벤트 제보 (비로그인 가능, 관리자 메일로 발송, 10분에 5건·같은 내용 중복 제한)

**관리자**
- 대시보드(관리자 페이지 첫 탭): 노출·숨김·잠금 이벤트 수, 최근 7일 신규, 3일 안 마감, 혜택 유형·설명이 빈 이벤트 수 / 마지막 크롤링 시각, 성공·실패 브랜드, 오늘 AI 사용량(상한 대비) / 회원·정지·댓글 수와 최근 7일 증가 / 조회수 상위 5개 / 브랜드별 노출 이벤트 수 막대 그래프
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

**검색 노출 (SEO)**
- 페이지별 `<title>`, description, canonical, og 태그를 화면 이동 때마다 갱신 (`usePageTitle`)
- `robots.txt`(관리자·마이페이지·API 제외), `sitemap.xml`(정적 페이지 + 노출 중인 이벤트 상세, 백엔드에서 생성)
- 미리보기 봇(카카오톡, 페이스북, 슬랙, 네이버 등)이 `/events/{id}`에 오면 Nginx가 User-Agent로 구분해 백엔드가 만든 og HTML을 응답 (SPA라 봇이 JS를 실행하지 않아도 미리보기가 보이도록)

**혜택 유형(EventCode) 분류**
- 새 이벤트를 저장하거나 제목·이미지가 바뀐 이벤트를 갱신할 때만 분류 (내용이 같으면 다시 분류하지 않음, 관리자 잠금 이벤트 제외)
- 1단계 키워드 규칙 → 2단계 AI(제목) → 3단계 AI(이벤트 배너 이미지) 순서로 시도하고, 앞 단계에서 정해지면 멈춤
- 배너 이미지를 볼 때는 한 번의 AI 호출로 혜택 유형과 이벤트 설명을 함께 받음 (설명이 이미 있으면 덮어쓰지 않고, 크롤링 때 빈 설명이 와도 기존 설명 유지)
- 크롤링이 끝날 때마다 혜택 유형이나 설명이 비어 있는 이벤트를 자동으로 이어서 분류 (한 번에 최대 100건, `app.crawler.classify-after-crawl`로 끌 수 있고, 실패해도 크롤링 결과에는 영향 없음)
- AI는 Anthropic Claude Haiku 5.5. 하루 호출 상한(`app.ai.daily-limit`, 기본 200)과 짧은 응답 제한으로 비용을 제어하고(사용량은 Redis `ai:usage:{서울 날짜}` 키에 세어 서버를 재시작·배포해도 유지, Redis 오류 시 AI 호출을 건너뜀), `ANTHROPIC_API_KEY`가 없거나 `app.ai.enabled=false`이면 AI 없이 규칙만 사용

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
| DB / 캐시 | MySQL 8, Redis (조회수 중복 방지, 인증코드, rate limit, 블랙리스트, AI 일일 사용량), Flyway |
| 크롤링 | `java.net.http.HttpClient`(공통 래퍼), Jackson, 정규식 파싱 (공개 API가 있는 사이트는 API 사용) |
| AI | Anthropic Java SDK, Claude Haiku 5.5 (혜택 유형 분류·이벤트 설명 작성, 선택 기능) |
| API 문서 | springdoc-openapi (Swagger UI, 개발 환경에서만 활성화) |
| Frontend | React 19, Vite 8, React Router 8 (URL 기반 라우팅, 로그인 필요 화면은 보호 라우트) |
| 인프라 | Docker Compose, Nginx, Caddy (자동 HTTPS), GitHub Actions(CI 테스트 → 배포) → AWS EC2 |
| 테스트 | JUnit 5, Mockito (서비스 계층 단위 테스트), Testcontainers 이벤트 검색 통합 테스트(가상 데이터, 쿼리 수 회귀 포함) |

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
├── admin/      관리자 전용 API 모음(/api/admin): 대시보드, 회원 조회·정지, 댓글 숨김
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
| 회원 | `GET /api/user/email-availability`, `/nickname-availability` | 공개 |
| | `GET·PATCH·DELETE /api/user/me`, `PATCH /api/user/me/password`, `GET /api/user/me/favorites` | 로그인 |
| 관리자 | `GET /api/admin/dashboard`, `GET /api/admin/users`, `PATCH /api/admin/users/{id}/suspension`, `GET /api/admin/users/{id}/comments`, `PATCH /api/admin/comments/{id}/hide`, `/unhide` | 관리자 |
| 제보 | `POST /api/reports` | 공개 (rate limit) |
| 크롤링 | `POST /api/crawl`, `/api/crawl/{group 또는 brand}`, `GET /api/crawl/runs`, `/api/crawl/runs/latest` | 관리자 |
| SEO | `GET /api/seo/sitemap.xml`(사이트에서는 `/sitemap.xml`), `GET /api/seo/events/{id}`(미리보기 봇용 og HTML) | 공개 |

## 배포

`main` 브랜치에 push하면 GitHub Actions(`.github/workflows/deploy.yml`)가 먼저 CI(`ci.yml`)를 돌리고, 통과해야 EC2에 SSH로 접속해 배포합니다.

- CI: 백엔드 `./gradlew test`(Testcontainers 포함, 실패 시 테스트 리포트 업로드), 프론트 `npm run lint`(오류가 있으면 실패) + `npm run build`
- `dev` 브랜치에 push할 때와 `main` 대상 PR에서도 같은 CI가 실행됩니다.

배포 단계:

1. 서버에서 해당 커밋으로 `git reset --hard`
2. `docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build`
3. 사이트의 `/version.txt`가 방금 배포한 커밋 SHA와 일치하는지 확인
4. `/api/categories` 호출로 API 응답 확인

구성 (`docker-compose.prod.yml`): `mysql`, `redis`(비밀번호 + AOF), `backend`(healthcheck), `frontend`(Nginx), `caddy`(80/443, 자동 HTTPS). DB와 Redis는 외부 포트를 열지 않고 내부 네트워크로만 접근합니다.

## 보안 / 운영 참고

- 비밀번호는 DelegatingPasswordEncoder로 해시, 로그인 실패 횟수 제한, 인증 메일/제보에 Redis 기반 rate limit 적용
- 운영 환경에서는 Swagger와 상세 SQL 로그를 끄고, Actuator는 `health`만 노출
- API 키는 코드·설정 파일·git에 두지 않고 환경변수(서버 `.env.prod`, GitHub Secrets)로만 전달합니다. 배포 때 키는 SSH 표준입력으로 넘겨 서버 프로세스 목록이나 로그에 남지 않습니다. AI 호출은 하루 상한과 Console의 월 사용 한도로 비용을 제한합니다.
- 크롤러 요청의 User-Agent에 프로젝트와 연락처를 명시합니다. 사이트별 이용약관·robots.txt를 확인하고 요청 빈도를 낮게 유지해야 하며, 상업적으로 전환할 경우 각 브랜드의 약관(스크래핑 금지 조항 포함)을 먼저 검토해야 합니다.

## 로드맵

- [ ] 제보 내용 DB 저장 (현재는 메일 발송만)
- [ ] 크롤러 파싱 테스트 (HTML/JSON 응답 픽스처 기반)
- [x] 이벤트 검색 통합 테스트 (Testcontainers, 가상 데이터)
- [x] 배포 전 CI 테스트
- [x] SEO 기본 설정 (메타·og 태그, robots.txt, sitemap.xml, 공유 미리보기)
- [x] 관리자 대시보드 (이벤트·크롤링·AI 사용량·회원 지표)
- [x] 프론트 lint 오류 정리, CI에서 lint 실패 시 차단
- [x] AI 일일 사용량을 Redis로 관리 (재시작에도 상한 유지)
- [ ] 운영 모니터링(헬스체크 알림)
- [ ] 즐겨찾기 브랜드 새 이벤트·마감 임박 알림
- [ ] 대시보드 추이(최근 7일 수집·실패, 일별 AI 사용량)
- [ ] 보안 헤더, 리프레시 토큰
- [ ] 그 외 도메인 컨트롤러·통합 테스트
- [ ] 크롤러 수집 건수 급감 감지 알림
- [ ] 댓글 대댓글, 이벤트 상세 댓글 UI 보강
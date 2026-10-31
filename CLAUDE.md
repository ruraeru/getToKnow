# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# 프로젝트 개요
Next.js 프론트엔드 + Spring Boot 백엔드 + MySQL을 docker compose 하나로 묶은 모노레포.

- 개발: Proxmox 개발 CT(Ubuntu 24.04)에서 작업 — 이 CT에는 Docker 권한이 없고 MySQL도 없음
- 배포: Docker VM(172.30.1.11)에서 docker compose로 실행
- 외부 노출: 프록시 CT의 Caddy가 gettoknow.ruraeru.xyz → 172.30.1.11:`FRONTEND_PORT`(기본 3001),
  gettoknow-api.ruraeru.xyz → 172.30.1.11:`BACKEND_PORT`(기본 8081) 으로 reverse proxy
- DB 포트는 외부에 열지 않음 — `db` 서비스는 `internal: true`인 `db` 네트워크에만 있고 backend만 접근
- 비밀값은 `.env`에 두고 Git에는 올리지 않음 (`.env.example`이 템플릿). `JWT_SECRET`이 없으면 compose가 시작을 거부함

# 서비스 개념
모바일 우선 커플 웹사이트. "물어봐야만 알 수 있는" 질문(키, 좋아하는 색깔, 싫어하는 음식…)에 각자 답하고 상대에게 공유한다.

1. 로그인한 사용자가 서비스가 제공하는 질문에 답변을 작성한다.
2. 공유 전에 **잠금 질문 하나**와 정답을 정한다 (예: "우리가 처음 만난 곳은?" → "강남역"). 사람마다 하나이며 답변 전체를 잠근다.
3. 공유 링크(`/s/{shareToken}`)를 상대에게 보낸다.
4. 링크를 연 사람은 로그인 없이 잠금 질문에 답하고, 맞히면 답변 전체를 본다.

- 커플 연결(초대 코드) 기능은 없다 — 링크가 연결 수단이다.
- 답변별 잠금은 쓰지 않는다 (초기 구현에서 잘못 이해했다가 제거함).
- 잠금 정답 비교는 공백 제거 + 소문자화 후 BCrypt 비교 (`ShareController.normalize`).

# 아키텍처

## backend/ — Spring Boot 4.1.2, Java 21, Gradle
패키지 `xyz.ruraeru.gettoknow` 하나에 평평하게 둔다.

- `SecurityConfig` — stateless, CSRF off, CORS(`app.cors-origin` 단일 origin). JWT는 Spring Security oauth2-resource-server의 Nimbus HS256 인코더/디코더 사용(직접 만든 필터 없음). 토큰 subject = user id, 유효기간 30일.
- `User`(`users` 테이블: email, password(BCrypt), nickname, shareToken, lockQuestion, lockAnswer(BCrypt)), `Question`, `Answer`(user+question 유니크).
- `Repositories.java` — 리포지토리 인터페이스 모음. `UserRepository.current(jwt)`로 현재 사용자 조회.
- `BackendApplication.QUESTIONS` — 기본 질문 목록. 시작 시 없는 질문만 추가한다(기존 것은 건드리지 않음).
- 스키마는 `ddl-auto=update`로 관리 (마이그레이션 도구 없음).
- 에러는 `ResponseStatusException` + 한국어 메시지, `spring.web.error.include-message=always`로 (Boot 4 이름. 옛 `server.error.*`는 무시됨) 프론트에 `message` 전달.

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/auth/signup` | X | `{email,password,nickname}` → `{token}` |
| POST | `/api/auth/login` | X | `{email,password}` → `{token}` |
| GET | `/api/me` | O | `{nickname, shareToken, lockQuestion}` (lock 미설정이면 `null`) |
| PUT | `/api/me/lock` | O | `{lockQuestion, lockAnswer}` |
| GET | `/api/questions` | O | `[{id, content, answer}]` 내 답변 |
| PUT | `/api/questions/{id}/answer` | O | `{content}` |
| GET | `/api/share/{token}` | X | `{nickname, lockQuestion}` (잠금 미설정이면 404) |
| POST | `/api/share/{token}/unlock` | X | `{guess}` → `{nickname, answers:[{question, answer}]}` / 틀리면 400 |

설정은 환경변수로: `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`(32바이트 이상), `CORS_ORIGIN`.

## frontend/ — Next.js 16.4 (App Router, TS, Tailwind v4), `output: "standalone"`
`frontend/AGENTS.md` 참고: 이 Next 버전은 학습 데이터와 다르다. 코드 작성 전 `node_modules/next/dist/docs/`를 확인할 것.

- `cacheComponents: true` 상태다. 동적 라우트에서 `useParams` 같은 요청 시점 훅은 `<Suspense>` 안에 둬야 빌드가 통과한다 (`s/[token]/page.tsx`가 `SharePage.tsx`를 감싸는 이유).
- 모든 페이지는 클라이언트 컴포넌트. 토큰은 `localStorage`의 `token`. 401이면 토큰 삭제 후 `/login`으로.
- `src/lib/api.ts` — fetch 래퍼 + 타입. API 주소는 `NEXT_PUBLIC_API_URL`(빌드 시 번들에 박힘).
- 페이지: `/login`(로그인/가입), `/`(내 답변 작성 + 잠금 설정 + 공유 버튼), `/s/[token]`(공개, 잠금 해제 후 답변 열람).
- 공통 클래스 `.input`, `.btn`, `.btn-sm`, `.label`, `.card`(흰 카드), `.tile`(파스텔 타일, 색은 따로 지정), `.link`는 `globals.css`의 `@layer components`. 입력창 16px 이상(iOS 확대 방지).
- 디자인 토큰은 `globals.css`의 `@theme` → Tailwind 유틸리티로 씀 (`bg-me-soft`, `text-you-ink`, `bg-field`, `text-muted`, `text-negative` …). 하드코딩 색 대신 토큰 사용.
  - 사람마다 색 하나: `me`(핑크) = 답변 주인, `you`(하늘) = 링크를 여는 사람. 파스텔 배경(`-soft`) + 같은 색의 진한 글자(`-ink`), 진한 색(`me`/`you`)은 버튼·진행 막대.
  - 질문/답변 카드는 `src/lib/tints.ts`의 `tint(i)`로 핑크→하늘→민트→피치→레몬 순환 (위치 기준이라 홈과 공유 페이지에서 같은 질문이 다른 색일 수 있음).
- 폰트는 Pretendard Variable — Google Fonts에 없어서 `layout.tsx`에서 jsdelivr CDN `<link>`로 로드 (CDN 막히면 시스템 폰트로 대체).
- 디자인 규칙: 이모지·그라데이션·그림자 안 씀. 폼 필드는 placeholder 대신 위에 `<label>`. 디자인 시트는 `frontend/.taste-skill/direction.json` (`design-taste-frontend` 스킬 산출물).

## 배포 (compose.yaml)
- `db`(mysql:8.4, `db` internal 네트워크, 볼륨 `db-data`, 호스트 포트 없음 — 기존 서비스의 MySQL 3306과 충돌 안 함) → `backend`(db+web 네트워크, 8080) → `frontend`(web, 3000).
- `.env`: `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET`, `APP_URL`(CORS origin), `API_URL`(프론트 빌드 인자), `FRONTEND_PORT`/`BACKEND_PORT`(호스트 포트, 기본 3001/8081 — VM의 기존 서비스가 3000/8080 사용 중).
- `API_URL`을 바꾸면 frontend 이미지를 다시 빌드해야 한다.
- Docker VM에 다른 서비스도 함께 배포한다. 서비스마다 폴더(compose 프로젝트) 하나, 겹치는 건 호스트 포트뿐이라 `.env`의 포트로 피하고 프록시 CT Caddy가 도메인별로 그 포트에 연결한다.
  - compose 맨 위 `name: gettoknow`로 프로젝트명을 고정 — 폴더 이름과 무관하게 컨테이너·네트워크·볼륨이 `gettoknow_*`.
  - 포트를 바꾸면 Caddyfile의 `reverse_proxy 172.30.1.11:<포트>`도 같이 바꿔야 한다. 배포 전 `docker ps --format '{{.Names}}\t{{.Ports}}'`로 빈 포트 확인.
  - 프록시 CT의 Caddyfile 하나에 서비스별 사이트 블록을 나란히 둔다 (기존 서비스 블록은 그대로 두고 아래에 추가). 도메인(서브도메인)은 서비스마다 달라야 하며, 새 서브도메인은 DNS 레코드가 프록시를 가리켜야 Caddy가 인증서를 받는다.
    ```
    gettoknow.ruraeru.xyz { reverse_proxy 172.30.1.11:3001 }
    gettoknow-api.ruraeru.xyz { reverse_proxy 172.30.1.11:8081 }
    ```
    수정 후 `caddy validate --config /etc/caddy/Caddyfile` → `systemctl reload caddy` (reload는 무중단, 문법 오류면 기존 설정 유지).
- frontend 이미지는 `ENV HOSTNAME=0.0.0.0` 필수 — Docker가 HOSTNAME을 컨테이너 id로 넣고 Next standalone `server.js`가 거기에 bind하기 때문.

### 배포 순서 (Docker VM 172.30.1.11)
코드는 GitHub `ruraeru/getToKnow`(`main`)에서 받는다. 이 개발 CT에서는 Docker를 못 쓰므로 빌드·실행은 VM에서 한다.

1. VM에서 클론: `git clone https://github.com/ruraeru/getToKnow.git && cd getToKnow` (비공개 레포면 VM SSH 키를 GitHub에 등록 후 `git@github.com:ruraeru/getToKnow.git`).
2. `.env` 작성: `cp .env.example .env` 후 `MYSQL_PASSWORD`/`MYSQL_ROOT_PASSWORD` 변경, `JWT_SECRET`에 `openssl rand -base64 48` 값. 도메인·포트는 기본값 그대로.
3. 빈 포트 확인: `docker ps --format '{{.Names}}\t{{.Ports}}'` — 3001/8081이 비어 있어야 함.
4. `docker compose up -d --build` — 첫 빌드는 Gradle/npm 의존성 받느라 수 분, 인터넷과 RAM 2~3GB 필요.
5. VM에서 확인:
   - `docker compose ps` (db healthy), `docker compose logs -f backend` ("Started BackendApplication")
   - `curl -s -o /dev/null -w '%{http_code}\n' localhost:8081/api/share/x` → 404면 정상
   - `curl -s -o /dev/null -w '%{http_code}\n' localhost:3001/login` → 200
6. DNS: `gettoknow`, `gettoknow-api` 레코드를 기존 `app` 레코드와 같은 대상으로 추가 (와일드카드 있으면 생략).
7. 프록시 CT Caddyfile에 위 두 블록 추가 → validate → reload.
8. 폰으로 https://gettoknow.ruraeru.xyz 에서 가입 → 답변 → 잠금 → 공유 링크 열람 확인.

업데이트: `cd getToKnow && git pull && docker compose up -d --build`. DB는 볼륨 `gettoknow_db-data`에 있어 유지된다. **`docker compose down -v`는 볼륨까지 지워 데이터가 전부 사라진다.**

# 개발 / 테스트
- 백엔드 테스트: `cd backend && ./gradlew test` — H2(MySQL 모드)로 가입→답변→잠금→공유 열람 전체 흐름을 검증 (`BackendApplicationTests`).
- 로컬 실행(MySQL 없이): `./gradlew bootTestRun --args='--app.cors-origin=http://<호스트>:3000'` — 테스트 클래스패스(H2 메모리 DB)로 띄움. 재시작하면 데이터 사라짐.
- 프론트: `NEXT_PUBLIC_API_URL=http://<호스트>:8080 npx next dev -H <호스트> -p 3000`. 다른 호스트명으로 접속하면 dev 서버가 막으므로 `-H`로 접속할 주소를 지정.
- 이 CT 주소: LAN 172.30.1.12, Tailscale 100.115.251.71 (현재 테스트는 Tailscale 주소로 접속).
- `npm run lint`, `npm run build`로 프론트 검증.

# 진행 상황
- [x] 백엔드: 인증, 질문/답변, 잠금, 공유 링크 API + 통합 테스트
- [x] 프론트: 로그인/가입, 답변 작성, 잠금 설정, 링크 공유, 공개 열람 페이지
- [x] Dockerfile(백/프론트), compose.yaml, .env.example
- [x] 개발 CT에서 로컬 실행 확인 (H2)
- [x] Docker VM 배포 (2026-10-31) — gettoknow(-api).ruraeru.xyz HTTPS·CORS·번들 API 주소 확인. 가입부터 공유까지 실제 흐름은 사용자가 폰으로 확인할 것
- [x] UI 디자인 파스텔 톤으로 개편 (아래 변경 이력 참고)
- [ ] 실제 브라우저로 화면 확인 (이 CT에는 Chrome이 없어 `inspect.mjs` 미실행)

# 변경 이력
- 2026-10-30: 초기 구현. 처음엔 초대 코드로 커플을 연결하고 답변마다 잠금 질문을 두는 방식이었음 → 사용자 의도와 달라 "사람당 잠금 질문 하나 + 링크 공유" 방식으로 변경. `inviteCode`/`partner`/답변별 잠금 필드 제거, `ShareController` 추가.
- 2026-10-30~31: UI 디자인 개편 (프론트만, API 변경 없음).
  - 1차: 핑크 배경 기본 스타일 → 무채색 + Gowun Batang 세리프 제목(`editorial-greeting`). 사용자가 "올드하고 밋밋하다"고 해서 폐기.
  - 2차(현재): 파스텔 스타일(`pastel-consumer`). 회색 바탕(`#f4f5f7`) 위 흰 카드 + 파스텔 타일, Pretendard 폰트, pill 버튼.
    - 홈: 핑크 타일에 "OO님에 대해 물어봐야 아는 것들" + 답한 수 `7 / 12` + 진행 막대. 하늘 타일에 공유(잠금 질문·링크 텍스트·보내기 버튼). 질문 카드는 파스텔 순환색, 답한 질문에 "답함" 칩, 저장 후 "저장했어요" 표시.
    - 로그인: 핑크 타일에 서비스 설명 + 예시 질문/답변(문구는 샘플, 바꿔도 됨) + 흰 카드 폼.
    - 공유 페이지: 핑크 타일 제목, 잠금 질문을 입력창 label로 크게, 열린 뒤 답변 카드도 파스텔 순환색.
  - 공유 링크를 화면에 텍스트로 표시 — http에서 공유/클립보드가 안 될 때 길게 눌러 복사 가능.
  - 🔒 이모지 제거, 잠금 설정에 "취소" 버튼 추가.
- 2026-10-31: Docker VM에 이미 다른 서비스(프론트 3000, 백엔드 8080, MySQL 기본 포트)가 배포 중이라 함께 배포하도록 변경.
  - `compose.yaml`: 호스트 포트를 `FRONTEND_PORT`/`BACKEND_PORT`로 변수화, 기본값 3001/8081. 맨 위 `name: gettoknow`로 프로젝트명 고정. `.env.example`에 두 변수 추가.
  - MySQL은 호스트 포트를 안 열어서 기존 3306과 충돌 없음.
  - 프록시 CT Caddyfile에 이 서비스용 블록을 기존 블록 옆에 추가하는 방식 (위 "배포" 참고). 아직 실제 VM 배포·Caddy 반영은 안 함.
  - 도메인 변경: 기존 서비스가 app/api.ruraeru.xyz를 쓰고 있어서 getToKnow는 `gettoknow.ruraeru.xyz`(프론트) / `gettoknow-api.ruraeru.xyz`(API)로. compose 기본값·`.env.example`의 `APP_URL`/`API_URL` 반영. 두 서브도메인 DNS 레코드는 아직 추가 안 함.
- 2026-10-31: VM 배포 후 점검에서 에러 응답에 `message`가 빠진 것을 발견 (처음부터 있던 버그 — 프론트가 "틀렸어요" 대신 "요청에 실패했어요"만 보여줌). Boot 4는 `server.error.include-message`를 무시하고 `spring.web.error.include-message`를 읽음. main/test 두 `application.properties` 모두 수정 (test 쪽이 bootTestRun에서 main을 가림). MockMvc는 `/error`를 안 거쳐 테스트로는 못 잡음 — curl로 확인.

# 알려진 제약 / TODO
- 잠금 정답 시도 횟수 제한 없음 — 링크를 가진 사람은 짧은 정답을 대입해볼 수 있음 (`ShareController`의 `shortcut:` 주석).
- 공유 링크 재발급 기능 없음 (유출 시 잠금 질문 변경만 가능).
- 로그인 토큰이 만료된 상태로 공유 링크를 열면 공개 페이지 대신 `/login`으로 튕김 (`api.ts`가 401에 토큰을 보냈기 때문).
- `navigator.share`/클립보드는 HTTPS에서만 동작 — http로 테스트하면 공유 버튼이 안 될 수 있음.
- 비밀번호 찾기, 질문 직접 추가, 계정 삭제 없음.
- 폰트가 외부 CDN(jsdelivr)에 의존함. 로딩 화면은 스켈레톤 없이 "불러오는 중..." 텍스트뿐.

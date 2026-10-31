# getToKnow

물어봐야만 알 수 있는 서로의 이야기. 키, 좋아하는 색깔, 싫어하는 음식처럼 직접 묻지 않으면 모르는 것들에 답하고, 정답을 아는 사람에게만 보여주는 모바일 우선 커플 웹사이트입니다.

## 어떻게 쓰나요

1. 가입하고 서비스가 준비한 질문 12개("키는 몇이야?", "MBTI는?", "인생 영화는?" …)에 답합니다.
2. **잠금 질문 하나**와 정답을 정합니다. 예: "우리가 처음 만난 곳은?" → "강남역".
3. 공유 링크(`/s/{shareToken}`)를 상대에게 보냅니다.
4. 링크를 연 사람은 로그인 없이 잠금 질문에 답하고, 맞히면 답변 전체를 봅니다.

- 커플 연결(초대 코드) 기능은 없습니다. 링크가 곧 연결 수단입니다.
- 잠금 정답은 띄어쓰기와 대소문자를 무시하고 비교합니다 (BCrypt로 저장).

## 기술 스택

| 영역 | 스택 |
|---|---|
| 프론트엔드 | Next.js 16 (App Router, TypeScript), Tailwind CSS v4, Pretendard |
| 백엔드 | Spring Boot 4, Java 21, Gradle, Spring Security (JWT, HS256) |
| DB | MySQL 8.4 (테스트·로컬은 H2) |
| 배포 | Docker Compose |

## 프로젝트 구조

```
.
├── backend/         Spring Boot API (패키지 xyz.ruraeru.gettoknow)
├── frontend/        Next.js 앱
│   └── src/app/     /login, / (내 답변·잠금·공유), /s/[token] (공개 열람)
├── compose.yaml     db + backend + frontend
└── .env.example     배포용 환경변수 템플릿
```

## 로컬 개발

MySQL 없이 H2 메모리 DB로 띄울 수 있습니다. 재시작하면 데이터가 사라집니다.

```bash
# 백엔드 (8080)
cd backend
./gradlew bootTestRun --args='--app.cors-origin=http://localhost:3000'

# 프론트엔드 (3000)
cd frontend
npm install
NEXT_PUBLIC_API_URL=http://localhost:8080 npx next dev
```

다른 기기(폰 등)에서 접속하려면 `localhost` 대신 개발 머신의 주소를 쓰고, `next dev`에 `-H <주소>`를 붙입니다.

### 검사

```bash
cd backend && ./gradlew test             # 가입 → 답변 → 잠금 → 공유 열람 통합 테스트
cd frontend && npm run lint && npm run build
```

## 배포

```bash
cp .env.example .env    # 값 채우기
docker compose up -d --build
```

| 변수 | 설명 |
|---|---|
| `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` | MySQL 비밀번호 |
| `JWT_SECRET` | 32바이트 이상. 없으면 compose가 시작을 거부합니다 (`openssl rand -base64 48`) |
| `APP_URL` | 프론트 주소, 백엔드 CORS 허용 origin |
| `API_URL` | 백엔드 주소. 프론트 빌드 시 번들에 들어가므로 바꾸면 `--build`로 다시 빌드해야 합니다 |
| `FRONTEND_PORT`, `BACKEND_PORT` | 호스트 포트 (기본 3001 / 8081) |

- MySQL은 내부 네트워크에만 있고 호스트 포트를 열지 않습니다.
- 앞단의 리버스 프록시(Caddy 등)가 도메인별로 두 포트에 연결하는 구성을 전제로 합니다.

```
gettoknow.ruraeru.xyz     { reverse_proxy <docker-host>:3001 }
gettoknow-api.ruraeru.xyz { reverse_proxy <docker-host>:8081 }
```

## API

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/auth/signup` | X | `{email, password, nickname}` → `{token}` |
| POST | `/api/auth/login` | X | `{email, password}` → `{token}` |
| GET | `/api/me` | O | `{nickname, shareToken, lockQuestion}` |
| PUT | `/api/me/lock` | O | `{lockQuestion, lockAnswer}` |
| GET | `/api/questions` | O | `[{id, content, answer}]` |
| PUT | `/api/questions/{id}/answer` | O | `{content}` |
| GET | `/api/share/{token}` | X | `{nickname, lockQuestion}` (잠금 미설정이면 404) |
| POST | `/api/share/{token}/unlock` | X | `{guess}` → `{nickname, answers}` (틀리면 400) |

인증이 필요한 요청은 `Authorization: Bearer <token>` 헤더를 씁니다. 토큰 유효기간은 30일입니다.

## 알려진 제약

- 잠금 정답 시도 횟수 제한이 없습니다.
- 공유 링크 재발급, 비밀번호 찾기, 질문 직접 추가, 계정 삭제 기능이 없습니다.
- 공유 버튼(`navigator.share`, 클립보드)은 HTTPS에서만 동작합니다. http에서는 화면의 링크를 길게 눌러 복사하세요.

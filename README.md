# 로또 번호 추천

로또 6/45의 역대 당첨번호를 모두 모아 통계를 보여 주고, 몇 가지 방식으로 번호를 추천하는 웹앱입니다.
로그인한 사용자만 쓸 수 있고, 메뉴마다 권한을 따로 줍니다.

> 로또 추첨은 매 회차 독립적인 무작위 추첨입니다. 과거 통계는 당첨 확률을 높여 주지 않으니 재미로만 봐 주세요.

## 기능

- **번호 추천**: 4가지 방식 중 하나를 골라 1~5게임을 추천받고, 결과는 본인의 추천 기록에 저장됩니다.
- **추천 기록**: 내가 받은 추천 번호입니다. 추첨이 끝난 회차는 맞힌 번호와 등수를 함께 보여 줍니다.
- **회차별 당첨번호**: 1회부터 최신 회차까지의 당첨번호, 등수별 당첨자 수와 당첨금을 보여 줍니다.
- **통계**: 번호별 출현 횟수 차트(전체, 최근 100/50/20회), 많이 나온 번호, 적게 나온 번호, 오래 안 나온 번호를 보여 줍니다.
- **회원가입과 로그인**: 아이디/비밀번호로 로그인합니다. 비밀번호 변경은 오른쪽 위 이름 메뉴에 있습니다.
- **관리자 화면**: 사용자 권한 관리, 사용자 관리(정지, 잠금 해제, 시스템 관리자 지정)
- **자동 수집**: 앱이 시작될 때, 그리고 매주 토요일 22시와 일요일 9시(한국 시간)에 새 회차를 가져옵니다.

### 추천 방식

| 방식 | 설명 |
|---|---|
| 무작위 + 조건 (`FILTERED`) | 무작위로 뽑되 홀수 2~4개, 합계 100~175, 연속번호 최대 2개를 만족하는 조합만 씁니다. |
| 자주 나온 번호 (`FREQUENCY`) | 역대 출현 횟수에 비례하는 가중치로 뽑습니다. |
| 오래 안 나온 번호 (`OVERDUE`) | 마지막으로 나온 뒤 지난 회차 수에 비례하는 가중치로 뽑습니다. |
| 완전 무작위 (`RANDOM`) | 아무 조건 없이 뽑습니다. |

## 권한

메뉴는 추천(`RECOMMEND`), 추천 기록(`HISTORY`), 당첨번호(`DRAWS`), 통계(`STATS`) 네 가지이고, 메뉴마다 권한 수준이 있습니다.

| 권한 | 할 수 있는 일 |
|---|---|
| 없음 (`NONE`) | 메뉴가 보이지 않고 API도 403 |
| 사용 (`USE`) | 메뉴 사용 |
| 관리 (`ADMIN`) | 메뉴 사용 + 다른 사용자에게 이 메뉴의 사용 권한 부여/회수. 당첨번호 관리자는 수동 수집도 할 수 있습니다. |
| 시스템 관리자 (`SYSTEM_ADMIN`) | 모든 메뉴 관리 + 사용자 관리 + 메뉴 관리자 지정/해제 |

- 새로 가입하면 **통계 사용** 권한만 받습니다. 다른 메뉴는 관리자가 줍니다.
- 메뉴 관리자는 사용자 목록에서 이메일과 휴대폰 번호를 가려서(`ho**@example.com`, `010-****-5678`) 봅니다. 전체 값은 시스템 관리자만 봅니다.
- 권한은 요청마다 DB에서 읽기 때문에, 권한을 바꾸거나 계정을 정지하면 바로 적용됩니다.
- 자기 계정은 정지하거나 시스템 관리자에서 해제할 수 없습니다(관리자가 아무도 없게 되는 것을 막기 위해).

## 보안 설계

| 항목 | 방식 |
|---|---|
| 로그인 | 아이디/비밀번호. 비밀번호는 BCrypt로 저장합니다. |
| 토큰 | 액세스 토큰: JWT(HS256), 15분, 브라우저 메모리에만 보관. 리프레시 토큰: 14일, `HttpOnly; SameSite=Strict; Path=/api/auth` 쿠키, DB에는 SHA-256 해시만 저장 |
| 토큰 갱신 | 갱신할 때마다 리프레시 토큰을 새로 바꿉니다. 이미 폐기된 토큰이 다시 쓰이면 탈취로 보고 그 사용자의 모든 로그인을 끊습니다. |
| 로그인 실패 | 5번 실패하면 15분 동안 잠깁니다. 없는 아이디와 틀린 비밀번호는 같은 메시지를 줍니다. |
| 개인정보 | 이름, 이메일, 휴대폰 번호를 AES-256-GCM으로 암호화해 저장합니다(`v1:` + Base64). 이메일 중복 확인과 검색은 HMAC-SHA256 해시로 합니다. |
| 설정 비밀값 | DB 비밀번호, AES/HMAC 키, JWT 키, 초기 관리자 비밀번호를 jasypt로 암호화해 `ENC(...)`로 넣었습니다. |

### MFA를 붙일 자리

지금은 아이디/비밀번호만 쓰지만, 나중에 TOTP 같은 2단계 인증을 붙일 수 있게 자리를 만들어 두었습니다.

- `app_user` 테이블에 `mfa_enabled`, 암호화된 `mfa_secret` 칸이 있습니다(지금은 항상 꺼져 있음).
- 로그인 응답에 `status`가 있습니다. 지금은 항상 `OK`이고, MFA를 켜면 `MFA_REQUIRED`를 돌려준 뒤 OTP 확인 API에서 토큰을 발급하면 됩니다.
- 분기할 곳: `AuthService.login()`의 주석, 프론트엔드 `auth.js`의 `login()` 주석

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Backend | Java 21+, Spring Boot 4.1, Spring Security(OAuth2 Resource Server, JWT), Spring Data JPA (Hibernate), jasypt-spring-boot 4.0, Maven |
| Database | PostgreSQL |
| Frontend | Vue 3, Vue Router, Vite 8, axios, Chart.js (vue-chartjs) |

## 폴더 구조

```
.
├── backend/                         Spring Boot API 서버 (포트 8080)
│   ├── config/application.yml       jasypt 마스터 비밀번호 (로컬 전용, git 제외)
│   └── src/main/java/com/example/lotto/
│       ├── admin/                   사용자 관리, 권한 관리 API
│       ├── auth/                    회원가입, 로그인, 토큰 갱신, 비밀번호 변경
│       ├── security/                Spring Security 설정, JWT → 권한 변환
│       │   └── crypto/              개인정보 암호화 (AES-GCM, HMAC), JPA 컨버터
│       ├── user/                    사용자 엔티티, 메뉴, 권한 수준, 첫 관리자 생성
│       ├── collect/                 동행복권 데이터 수집
│       ├── draw/                    회차 엔티티와 리포지토리
│       ├── recommend/               추천 로직, 추천 기록, 등수 계산
│       ├── stats/                   번호별 출현 통계
│       └── web/                     REST 컨트롤러, 예외 처리
└── frontend/                        Vue 앱 (개발 서버 포트 5173)
    └── src/
        ├── auth.js                  로그인 상태, 권한 확인
        ├── api.js                   API 호출, 토큰 자동 갱신
        ├── router/                  화면별 권한 검사
        ├── views/                   로그인, 회원가입, 추천, 당첨번호, 통계, 추천 기록, 내 정보
        │   └── admin/               권한 관리, 사용자 관리
        └── components/
```

## 실행 방법

### 준비물

- JDK 21 이상
- Maven 3.9 이상
- Node.js 22.12 이상 (nvm을 쓴다면 `nvm use 23.0.0`처럼 맞춰 주세요)
- PostgreSQL

### 1. 데이터베이스

테이블은 Hibernate가 자동으로 만듭니다(`ddl-auto: update`). 스키마만 미리 만들어 두면 됩니다.

```sql
CREATE SCHEMA IF NOT EXISTS lotto;
```

접속 정보는 환경변수로 바꿀 수 있습니다. 기본값은 `backend/src/main/resources/application.yml`에 있습니다.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `DB_HOST` | `localhost` | DB 호스트 |
| `DB_PORT` | `5432` | DB 포트 |
| `DB_NAME` | `stock_trading_db` | 데이터베이스 이름 |
| `DB_SCHEMA` | `lotto` | 테이블을 만들 스키마 |
| `DB_USERNAME` | `stock_user` | 계정 |
| `DB_PASSWORD` | jasypt로 암호화된 값 | 지정하면 설정 파일의 값 대신 씁니다. |

### 2. jasypt 마스터 비밀번호

`ENC(...)` 값을 풀려면 마스터 비밀번호가 필요합니다. 이 값은 저장소에 없습니다. 둘 중 하나로 넣습니다.

- **로컬 개발**: `backend/config/application.yml` (git 제외, `backend` 폴더에서 실행할 때 자동으로 읽힘)
  ```yaml
  jasypt:
    encryptor:
      password: 마스터비밀번호
  ```
- **운영**: 환경변수 `JASYPT_ENCRYPTOR_PASSWORD`

마스터 비밀번호를 잃어버리면 설정 파일의 비밀값을 풀 수 없습니다. 비밀번호 관리자 같은 안전한 곳에 따로 보관하세요.
특히 AES 키를 잃어버리면 DB에 암호화된 개인정보를 영영 복호화할 수 없습니다.

새 비밀값을 암호화하거나 기존 값을 확인할 때는 jasypt CLI를 씁니다(`mvn package` 뒤 로컬 Maven 저장소의 `org/jasypt/jasypt/1.9.3/jasypt-1.9.3.jar`).

```bash
# 암호화 → 출력값을 ENC(...) 안에 넣는다
java -cp jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI \
  input="암호화할값" password="$JASYPT_ENCRYPTOR_PASSWORD" \
  algorithm=PBEWITHHMACSHA512ANDAES_256 ivGeneratorClassName=org.jasypt.iv.RandomIvGenerator

# 복호화 확인
java -cp jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringDecryptionCLI \
  input="ENC 괄호 안의 값" password="$JASYPT_ENCRYPTOR_PASSWORD" \
  algorithm=PBEWITHHMACSHA512ANDAES_256 ivGeneratorClassName=org.jasypt.iv.RandomIvGenerator
```

AES/HMAC 키는 32바이트, JWT 키는 32바이트 이상의 무작위 값을 Base64로 만들어 암호화합니다(예: `openssl rand -base64 32`).

### 3. 백엔드

```powershell
cd backend
mvn spring-boot:run
```

- 처음 실행하면 1회차부터 최신 회차까지 수집합니다. 1분 정도 걸립니다.
- 시스템 관리자가 한 명도 없으면 첫 관리자 계정 `admin`을 만듭니다. 초기 비밀번호는 `lotto.admin.initial-password`(jasypt로 암호화됨)이고, 로그인한 뒤 바로 바꾸세요.
- HTTPS로 서비스할 때는 `lotto.security.secure-cookie: true`로 바꾸세요.

### 4. 프론트엔드

```powershell
cd frontend
npm install
npm run dev
```

브라우저에서 http://localhost:5173 을 엽니다. 개발 서버가 `/api` 요청을 백엔드(8080)로 넘겨 주므로 CORS 설정은 필요 없습니다.
같은 네트워크의 다른 기기에서 보려면 `npm run dev -- --host`로 실행하세요.

### 테스트

```powershell
cd backend
mvn test
```

DB 없이 실행되는 단위 테스트입니다. 추천 번호와 등수 계산, 개인정보 암호화(변조와 잘못된 키 검출 포함), 계정 잠금, 권한 계산, 마스킹을 검사합니다.

## API

로그인이 필요한 API는 `Authorization: Bearer <액세스 토큰>` 헤더가 필요합니다.

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/auth/signup` | 누구나 | 회원가입 |
| POST | `/api/auth/login` | 누구나 | 로그인. 액세스 토큰을 주고 리프레시 토큰은 쿠키로 설정 |
| POST | `/api/auth/refresh` | 쿠키 | 토큰 갱신 |
| POST | `/api/auth/logout` | 쿠키 | 로그아웃 |
| GET | `/api/auth/me` | 로그인 | 내 정보와 메뉴 권한 |
| POST | `/api/auth/password` | 로그인 | 비밀번호 변경 (모든 기기 로그아웃) |
| GET | `/api/draws/latest` | 로그인 | 최신 회차 |
| GET | `/api/draws`, `/api/draws/{drawNo}` | 당첨번호 사용 | 회차 목록, 회차 상세 |
| POST | `/api/draws/sync` | 당첨번호 관리 | 새 회차 수동 수집 |
| GET | `/api/stats/frequency?recent=N` | 통계 사용 | 번호별 출현 횟수와 미출현 기간 |
| POST | `/api/recommendations` | 추천 사용 | 번호 추천. 본문 예: `{"strategy": "FILTERED", "count": 5}` |
| GET | `/api/recommendations` | 추천 기록 사용 | 내 추천 기록과 당첨 확인 결과 |
| GET | `/api/admin/users?q=` | 메뉴 관리자 이상 | 사용자 목록. `q`에 `@`가 있으면 이메일, 없으면 아이디로 검색 |
| PUT | `/api/admin/users/{id}/permissions` | 메뉴 관리자 이상 | `{"menu": "DRAWS", "level": "USE"}` |
| PUT | `/api/admin/users/{id}/status` | 시스템 관리자 | `{"status": "DISABLED"}` |
| POST | `/api/admin/users/{id}/unlock` | 시스템 관리자 | 로그인 잠금 해제 |
| PUT | `/api/admin/users/{id}/system-admin` | 시스템 관리자 | `{"value": true}` |

## 데이터 출처

당첨번호는 [동행복권](https://www.dhlottery.co.kr) 결과 페이지가 내부에서 쓰는 주소(`/lt645/selectPstLt645InfoNew.do`)에서 가져옵니다.
공식 공개 API가 아니어서 예고 없이 바뀔 수 있습니다. 바뀌면 `backend/.../collect/DhLotteryClient.java`와 `DhLotteryResponse.java`만 고치면 됩니다.
서버에 부담을 주지 않도록 요청 사이에 300ms씩 쉬고(`lotto.sync.request-delay-ms`), 이미 저장한 회차는 다시 요청하지 않습니다.
자동 수집을 끄려면 `lotto.sync.enabled: false`로 설정하세요.

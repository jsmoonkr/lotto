# 로또 번호 추천

로또 6/45의 역대 당첨번호를 모두 모아 통계를 보여 주고, 몇 가지 방식으로 번호를 추천하는 웹앱입니다.

> 로또 추첨은 매 회차 독립적인 무작위 추첨입니다. 과거 통계는 당첨 확률을 높여 주지 않으니 재미로만 봐 주세요.

## 기능

- **번호 추천**: 4가지 방식 중 하나를 골라 1~5게임을 추천받고, 결과는 추천 기록에 저장됩니다.
- **회차별 당첨번호**: 1회부터 최신 회차까지의 당첨번호, 등수별 당첨자 수와 당첨금을 보여 줍니다.
- **통계**: 번호별 출현 횟수 차트(전체, 최근 100/50/20회), 많이 나온 번호, 적게 나온 번호, 오래 안 나온 번호를 보여 줍니다.
- **추천 기록**: 추첨이 끝난 회차는 맞힌 번호와 등수를 함께 보여 줍니다.
- **자동 수집**: 앱이 시작될 때, 그리고 매주 토요일 22시와 일요일 9시(한국 시간)에 새 회차를 가져옵니다.

### 추천 방식

| 방식 | 설명 |
|---|---|
| 무작위 + 조건 (`FILTERED`) | 무작위로 뽑되 홀수 2~4개, 합계 100~175, 연속번호 최대 2개를 만족하는 조합만 씁니다. |
| 자주 나온 번호 (`FREQUENCY`) | 역대 출현 횟수에 비례하는 가중치로 뽑습니다. |
| 오래 안 나온 번호 (`OVERDUE`) | 마지막으로 나온 뒤 지난 회차 수에 비례하는 가중치로 뽑습니다. |
| 완전 무작위 (`RANDOM`) | 아무 조건 없이 뽑습니다. |

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Backend | Java 21+, Spring Boot 4.1, Spring Data JPA (Hibernate), Maven |
| Database | PostgreSQL |
| Frontend | Vue 3, Vue Router, Vite 8, axios, Chart.js (vue-chartjs) |

## 폴더 구조

```
.
├── backend/                         Spring Boot API 서버 (포트 8080)
│   └── src/main/java/com/example/lotto/
│       ├── collect/                 동행복권 데이터 수집 (클라이언트, 동기화, 스케줄러)
│       ├── draw/                    회차 엔티티와 리포지토리
│       ├── recommend/               추천 로직, 추천 기록 엔티티, 등수 계산
│       ├── stats/                   번호별 출현 통계
│       └── web/                     REST 컨트롤러, 예외 처리
└── frontend/                        Vue 앱 (개발 서버 포트 5173)
    └── src/
        ├── views/                   추천, 당첨번호, 통계, 추천 기록 화면
        ├── components/              로또 공 컴포넌트
        └── api.js                   API 호출 모음
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
| `DB_PASSWORD` | (없음) | 비밀번호. 저장소에는 넣지 말고 실행할 때 지정하세요. |

### 2. 백엔드

```powershell
cd backend
$env:DB_PASSWORD="비밀번호"
mvn spring-boot:run
```

처음 실행하면 1회차부터 최신 회차까지 수집합니다. 1분 정도 걸립니다.

### 3. 프론트엔드

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

추천 번호가 1~45 사이의 서로 다른 6개인지, 조건 필터와 등수 계산이 맞는지 검사합니다. DB 없이 실행됩니다.

## API

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/draws?page=0&size=20` | 회차 목록 (최신순) |
| GET | `/api/draws/latest` | 최신 회차 |
| GET | `/api/draws/{drawNo}` | 회차 상세 |
| POST | `/api/draws/sync` | 새 회차 수동 수집. `{"added": n}`을 돌려줍니다. |
| GET | `/api/stats/frequency?recent=N` | 번호별 출현 횟수와 미출현 기간. `recent`를 빼면 전체 회차 기준입니다. |
| POST | `/api/recommendations` | 번호 추천. 본문 예: `{"strategy": "FILTERED", "count": 5}` |
| GET | `/api/recommendations?page=0&size=20` | 추천 기록과 당첨 확인 결과 |

## 데이터 출처

당첨번호는 [동행복권](https://www.dhlottery.co.kr) 결과 페이지가 내부에서 쓰는 주소(`/lt645/selectPstLt645InfoNew.do`)에서 가져옵니다.
공식 공개 API가 아니어서 예고 없이 바뀔 수 있습니다. 바뀌면 `backend/.../collect/DhLotteryClient.java`와 `DhLotteryResponse.java`만 고치면 됩니다.
서버에 부담을 주지 않도록 요청 사이에 300ms씩 쉬고(`lotto.sync.request-delay-ms`), 이미 저장한 회차는 다시 요청하지 않습니다.
자동 수집을 끄려면 `lotto.sync.enabled: false`로 설정하세요.

# Yona System Map

## 문서 목적

이 문서는 Yona를 "어떤 기술을 썼는가"보다 "어떻게 돌아가는가" 기준으로 이해하기 위한 시스템 지도다.  
특히 커스터마이징을 시작하기 전에 아래 질문에 답할 수 있게 만드는 것이 목적이다.

- 요청은 어디서 시작해서 어디서 끝나는가
- 어떤 패키지가 어떤 책임을 가지는가
- 런타임 초기화는 어디서 일어나는가
- 팀이 어떤 순서로 읽어야 가장 안전한가

## 1. 한눈에 보는 시스템 구조

### 사실

- 현재 저장소는 Play Framework 2.3.10 + sbt 0.13.5 기반이다.
- 핵심 구현 코드는 Java 중심이고, UI 렌더링은 Twirl 템플릿으로 구성된다.
- ORM은 Ebean, 비동기/스케줄링은 Akka, 저장소 처리는 JGit/SVNKit을 사용한다.
- `app`에는 578개 파일이 있고, 이 중 Java 315개, Twirl 242개, LESS 20개, Scala 1개다.
- 루트 기준 주요 디렉토리는 `app`, `conf`, `public`, `test`, `docs`, `project`, `support-script`, `lib`다.

### 판단

- 이 프로젝트는 "현대적인 계층형 서비스"보다 "기능이 많이 축적된 서버 렌더링 협업 플랫폼"에 가깝다.
- 프레임워크보다 도메인 흐름으로 읽어야 진입 비용이 낮다.

## 2. 런타임 진입점

### 핵심 진입점

| 레이어 | 핵심 파일 | 역할 |
| --- | --- | --- |
| 라우팅 | `conf/routes` | URL -> controller 액션 연결 |
| 글로벌 부트스트랩 | `app/Global.java` | 설정 파일 생성, 초기화, 요청 훅, 에러 처리 |
| 컨트롤러 | `app/controllers/*` | 요청 처리, 폼 바인딩, 권한 진입 |
| 모델 | `app/models/*` | 도메인 상태 + 핵심 비즈니스 로직 |
| 저장소 추상화 | `app/playRepository/*` | Git/SVN 처리 |
| 뷰 | `app/views/*` | Twirl 기반 HTML 렌더링 |

### 요청 생명주기

1. `conf/routes`가 요청을 controller 메서드로 라우팅한다.
2. 액션 컴포지션(`@AnonymousCheck`, `@IsAllowed`, `@IsCreatable` 등)이 먼저 실행된다.
3. `Global.onRequest()`가 현재 사용자 초기화, 언어 업데이트, 응답 헤더, access log를 처리한다.
4. controller가 폼/쿼리 파라미터를 해석하고 model을 호출한다.
5. model이 Ebean 저장, 번호 증가, watch/notification, 저장소 연동 등을 수행한다.
6. controller가 Twirl 템플릿을 렌더링하거나 redirect/JSON 응답을 반환한다.

### 관련 사실

- `Global.onRequest()`는 `UserApp.initTokenUser()`와 `UserApp.updatePreferredLanguage()`를 호출한다.
- `Global.onRouteRequest()`는 WebDAV 메서드를 `SvnApp`으로 우회한다.
- `docs/ko/technical/current-user.md`에는 `action composition -> Global#onRequest -> controller method` 순서가 문서화되어 있다.

## 3. 부트스트랩과 초기화

### `Global.onStart()`에서 일어나는 일

| 순서 | 초기화 항목 | 의미 |
| --- | --- | --- |
| 1 | `insertInitialData()` | DB가 비어 있으면 `initial-data.yml` 적재 |
| 2 | `Config.onStart()` | 환경/진단 초기화 |
| 3 | `Property.onStart()` | 시스템 속성 로딩 |
| 4 | `PullRequest.onStart()` | PR 번호/상태 보정 |
| 5 | `NotificationMail.onStart()` | 메일 발송 스케줄러 시작 |
| 6 | `NotificationEvent.onStart()` | 오래된 알림 정리 스케줄러 시작 |
| 7 | `Attachment.onStart()` | 임시 파일 정리 |
| 8 | `AccessControl.onStart()` | 익명 접근 정책 반영 |
| 9 | `YobiUpdate.onStart()` | 업데이트 체크 |
| 10 | `MailboxService.start()` | IMAP 메일 수신 시작 |

### 사실

- `Global`은 `application.conf`, `application-logger.xml`, `social-login.conf`를 기본 파일에서 복사 생성할 수 있다.
- secret key가 기본값이면 관리자 설정/재시작 유도 화면으로 흐름이 바뀐다.

### 판단

- `Global.java`는 단순 훅이 아니라 시스템 오케스트레이터다.
- 초기화/운영 이슈가 생기면 가장 먼저 볼 파일은 `Global.java`다.

## 4. 패키지별 책임 지도

| 패키지 | 역할 | 읽는 이유 |
| --- | --- | --- |
| `controllers` | 웹/UI/API 진입점 | 어떤 요청이 어디서 시작되는지 찾기 좋다 |
| `models` | 도메인 상태와 핵심 로직 | 실제 커스터마이징 영향 범위를 파악하는 핵심이다 |
| `playRepository` | Git/SVN 추상화 | Yona가 단순 tracker가 아니라 저장소 호스팅 서비스임을 보여 준다 |
| `utils` | 권한, 설정, 마크다운, LDAP, 파일/날짜 유틸 | 횡단 로직 이해에 필요하다 |
| `data` | JSON export/import | 마이그레이션/백업 전략 파악에 중요하다 |
| `mailbox` | 이메일 기반 이슈/댓글 생성 | 외부 입력 채널 구조를 보여 준다 |
| `actors` | 비동기 후처리 | PR 머지, 알림성 후속 작업 파악에 필요하다 |
| `views` | 서버 렌더링 UI | 문구, 레이아웃, 메뉴 수정 포인트 찾기 좋다 |

## 5. 업무 흐름 기준 핵심 축

현재 코드베이스에서 커스터마이징 우선순위가 높은 축은 아래 6개다.

1. 로그인/권한/사용자
2. 프로젝트/조직/멤버십
3. 이슈 생성-조회-수정-댓글-알림
4. Pull Request/리뷰/머지
5. Watch/Notification/Mailbox/Webhook
6. Git/SVN 저장소 연동

이 6개 축을 기준으로 읽으면, Play/Twirl/Ebean/Akka를 기술 문법으로 공부하지 않고도 제품 흐름을 먼저 잡을 수 있다.

## 6. 온보딩 순서

### 추천 읽기 순서

1. `conf/routes`
2. `app/Global.java`
3. `docs/ko/technical/current-user.md`
4. `app/utils/AccessControl.java`
5. `app/models/Project.java`
6. `app/models/User.java`
7. `app/models/Issue.java`
8. `app/models/PullRequest.java`
9. `app/models/NotificationEvent.java`
10. `app/playRepository/RepositoryService.java`

### 읽는 원칙

- `Akka`, `LESS`, `Twirl`을 먼저 깊게 공부하지 않는다.
- 먼저 route와 controller에서 "무슨 일이 일어나는지"를 잡는다.
- 이후 model에서 "실제 저장과 후처리"를 읽는다.
- 마지막으로 view와 비동기 코드를 읽는다.

## 7. 빌드/검증 상태

### 사실

- 현재 환경에서 `java.exe`와 `javac.exe`는 존재한다.
- 저장소 안에는 `activator` 또는 `sbt` 실행 파일이 포함되어 있지 않다.
- 임시로 Typesafe Activator 1.2.12 minimal을 내려받아 `compile`을 시도했다.
- Activator 기본 배치 스크립트는 OpenJDK의 `java -version` 출력을 제대로 인식하지 못했다.
- 오래된 `http` 기반 부트 의존성(`org.fusesource.jansi#jansi;1.11`, `com.typesafe.activator#activator-launcher;1.2.12`)도 즉시 문제를 일으켰다.
- 위 문제를 우회한 뒤에도 런처가 `No Scala version specified or detected` 오류에서 멈춰, 현재 환경에서 컴파일 성공까지는 확인하지 못했다.

### 판단

- 이 저장소는 "코드 이해"보다 "재현 가능한 빌드 환경 확보"가 먼저 막히는 전형적인 레거시 시스템이다.
- 착수 초기에 별도 과제로 `build reproducibility`를 분리하는 것이 맞다.

## 8. 지금 단계에서의 해석

### 사실

- `views`와 `models`의 비중이 매우 크다.
- `controllers`와 `models` 사이에 얇은 service 계층이 거의 없다.
- `NotificationEvent`, `Project`, `User`, `PullRequest`, `Issue`가 핵심 응집점이다.

### 판단

- 팀은 "패키지 구조"보다 "핵심 응집점 5개"를 먼저 익혀야 한다.
- 첫 커스터마이징은 UI보다 workflow와 정책에 들어갈 가능성이 높으므로, `Project / User / Issue / PullRequest / NotificationEvent`를 우선 학습 대상으로 삼는 것이 가장 효율적이다.

## 9. 다음 문서와의 연결

- `workflow-map.md`: 업무 흐름별 route-controller-model-view 연결
- `domain-map.md`: 핵심 모델 관계와 책임
- `customization-seams.md`: 어디를 먼저/나중에 바꾸면 되는지
- `risk-register.md`: 지금 드러난 기술/운영 리스크
- `company-fit-gap.md`: 일반적인 기업 협업 요구와 Yona 원본의 차이

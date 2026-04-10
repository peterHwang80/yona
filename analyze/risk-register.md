# Yona Risk Register

## 문서 목적

이 문서는 현재 코드베이스를 실무에 도입하고 커스터마이징할 때 마주칠 리스크를 "사실"과 "초기 대응" 기준으로 정리한다.

## 1. 리스크 요약 표

| ID | 리스크 | 심각도 | 현재 사실 | 초기 대응 |
| --- | --- | --- | --- | --- |
| R1 | 레거시 프레임워크 고착 | High | Play 2.3.10, sbt 0.13.5, Activator 1.2.12 계열 | 업그레이드는 별도 과제로 분리하고 먼저 재현 가능한 빌드 환경 확보 |
| R2 | 빌드 재현성 저하 | High | 현재 환경에서 `compile` 성공 확인 실패 | 사내 표준 JDK + launcher + resolver mirror 기준으로 빌드 체인 재정비 |
| R3 | 오래된 외부 저장소 의존 | High | 부트 의존성이 `http` 저장소에 묶여 즉시 실패 | 사내 artifact proxy/Nexus/Artifactory 미러링 |
| R4 | 컨트롤러/모델 비대 | High | service 계층이 얇고 로직이 `IssueApp`, `Project`, `PullRequest`, `NotificationEvent`에 집중 | 변경 전에 workflow 문서화와 영향 범위 체크리스트 의무화 |
| R5 | 저장소 연동 복잡도 | High | Git/SVN 모두 지원, Smart HTTP/DAV/push hook 내장 | 회사가 Git-only라면 기능 비활성화 전략 우선 검토 |
| R6 | 권한 규칙 분산 | High | annotation/action + `AccessControl` + author/assignee/sharer 예외가 함께 작동 | 권한 변경은 `workflow-map`과 `AccessControl` 리뷰를 세트로 수행 |
| R7 | 알림 회귀 위험 | High | `NotificationEvent`가 수신자 계산, 병합, 메시지 생성, 정리 스케줄링까지 담당 | 알림 규칙 변경 시 샘플 이벤트 시나리오 검증 문서화 |
| R8 | 메일박스 보안/운영 리스크 | High | IMAP 기반 이슈/댓글 생성, From 헤더 신뢰 전제 | 초기 운영에서는 비활성화 권장 |
| R9 | 테스트 공백 | Medium-High | 일부 테스트가 `@Ignore`, 프론트엔드 자동화 테스트 없음 | 핵심 흐름별 스모크 테스트 세트 우선 확보 |
| R10 | 명칭 혼재 | Medium | Yona/Yobi 이름이 코드와 자산에 혼재 | 문서와 UI에서 용어 통일, 내부 코드 명칭 혼재는 사실로 유지 |
| R11 | 전역 초기화 집중 | Medium-High | `Global.java`가 설정 생성/초기화/요청 훅/에러 처리 모두 담당 | 운영 이슈 대응 문서에서 `Global`을 최상위 진입점으로 명시 |
| R12 | 문서와 실제 코드 차이 가능성 | Medium | 기술 문서가 풍부하지만 오래된 이름(Yobi)과 구버전 설명이 섞임 | 문서 사용 시 항상 `routes`, `Global`, 실제 controller와 교차 검증 |

## 2. 빌드/도구 리스크 상세

### 사실

- 저장소 자체에는 `activator` 또는 `sbt` 실행 파일이 없다.
- 임시로 Activator 1.2.12 minimal을 내려받아 실행했을 때, 기본 배치 스크립트가 OpenJDK 출력을 인식하지 못했다.
- 부트 단계에서 `org.fusesource.jansi#jansi;1.11`, `com.typesafe.activator#activator-launcher;1.2.12` 해결이 즉시 실패했다.
- 의존성 일부를 우회한 뒤에도 `No Scala version specified or detected` 오류가 발생해 compile 성공을 확인하지 못했다.
- 같은 이유로 `testOnly` 기반 선택 테스트도 실행 가능한 상태까지 올리지 못했다.

### 판단

- 이 리스크는 코드 리스크보다 먼저 해결되어야 한다.
- "우리가 코드를 이해하지 못한다"보다 "우리가 안전하게 빌드할 수 없다"가 더 큰 초기 리스크다.

### 초기 대응

1. 사내에서 고정 가능한 JDK/launcher 버전을 문서화한다.
2. 외부 Maven/Typesafe 의존성을 사내 저장소로 미러링한다.
3. Windows보다 Linux 컨테이너 기반 재현이 쉬운지 비교 검증한다.
4. 빌드 성공 전에는 대규모 리팩터링을 금지한다.

## 3. 제품 구조 리스크 상세

### `Issue` / `PullRequest` / `NotificationEvent`

#### 사실

- `IssueApp`와 `PullRequestApp`는 단일 액션에서 많은 정책을 수행한다.
- `NotificationEvent`는 알림 생성과 수신자 정책의 중심이다.

#### 판단

- 변경 지점이 한 메서드처럼 보여도 실제 영향은 model, view, notification, permission으로 넓어진다.

### `RepositoryService`

#### 사실

- 저장소 선택, Git advertise/RPC, DAV servlet, push hook chain이 한 서비스에 모여 있다.

#### 판단

- 저장소 계층 변경은 회사 맞춤화가 아니라 사실상 제품 핵심 엔진 변경에 가깝다.

## 4. 운영 리스크 상세

### 사실

- 메일 발송/수신, 업데이트 체크, 알림 정리, 임시 파일 정리, IMAP polling/IDLE이 런타임에 함께 돈다.
- 설정 키가 많고 기본값이 "외부 서비스 사용"을 전제로 한 항목이 포함되어 있다.

### 판단

- 운영 시작 전 `application.conf`의 기본값을 그대로 쓰면 외부 연동/메일/업데이트 체크가 예상치 못하게 동작할 수 있다.

### 초기 대응

- `smtp.*`, `imap.*`, `application.update.*`, `application.send.yona.usage` 등 운영 민감 키를 별도 체크리스트로 분리
- 프로덕션 전용 설정 템플릿 작성

## 5. 1차 단계에서 받아들일 리스크 / 미루는 리스크

### 1차 단계에서 받아들일 것

- 구형 Play/Twirl/Ebean 문법에 대한 학습 비용
- Yobi/Yona 명칭 혼재
- 일부 문서의 구버전 표현

### 1차 단계에서 미룰 것

- Play 업그레이드
- Ebean 대체
- 프론트엔드 구조 전면 교체
- 저장소 처리 엔진 개편

## 6. 리스크 관리 원칙

1. 새 요구사항은 먼저 `Safe / Medium / High`로 분류한다.
2. `High` 등급 요구사항은 빌드 재현성과 테스트 전략이 없는 상태에서 착수하지 않는다.
3. 저장소, PR 머지, 메일박스, 알림 수신자 계산은 "핵심 엔진"으로 간주한다.
4. 운영/도입 초기에는 기능 추가보다 비활성화/단순화 전략을 우선 고려한다.

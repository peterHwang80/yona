# Yona Customization Seams

## 문서 목적

이 문서는 "어디를 먼저 바꾸면 안전한가 / 어디는 나중에 바꿔야 하는가"를 정리한 커스터마이징 진입 지도다.

## 1. 분류 기준

| 등급 | 의미 |
| --- | --- |
| `Safe` | view 문구, 메뉴 노출, 단순 UI/브랜딩 변경 |
| `Medium` | controller/model 로직 수정, 권한 조건 수정, 알림 규칙 조정 |
| `High` | DB relation, 저장소 처리, PR merge, mailbox, 비동기 이벤트 변경 |

## 2. 먼저 하기 좋은 변경

| 변경 후보 | 등급 | 주요 지점 | 이유 |
| --- | --- | --- | --- |
| 제품명/브랜딩/문구 정리 | `Safe` | `conf/messages*`, `app/views/common/*`, `app/views/project/*`, `public/images/*` | 기능 영향이 가장 적다 |
| 상단 메뉴/기본 홈 화면 조정 | `Safe` | `Application.index`, `views.html.index.*`, `views.html.common.navbar` | 업무 흐름 이해와 UI 적응에 좋다 |
| 프로젝트 메뉴 기본 정책 | `Safe-Medium` | `ProjectMenuSetting`, `ProjectApp.saveProjectMenuSetting`, project 관련 Twirl | 회사용 메뉴만 우선 노출 가능 |
| 로그인/프로필 필드 정책 | `Medium` | `UserApp`, `YonaUserServicePlugin`, `User`, `views.html.user.*` | 사내 사용자 경험에 직접적 |
| 프로젝트/조직 생성 정책 | `Medium` | `ProjectApp.newProject`, `OrganizationApp.newOrganization`, `AccessControl` | 실제 운영 규칙 반영 가능 |

## 3. 중간 난이도 변경

| 변경 후보 | 등급 | 주요 지점 | 주의점 |
| --- | --- | --- | --- |
| 권한 정책 세분화 | `Medium` | `AccessControl`, annotation/action, `ProjectUser`, `OrganizationUser` | 리소스별 예외(author/assignee/sharer)까지 함께 봐야 함 |
| 이슈 기본 상태/폼 정책 | `Medium-High` | `Issue`, `IssueApp`, `IssueEvent`, `views.html.issue.*` | 알림과 히스토리까지 영향 |
| 알림 기본 규칙 변경 | `Medium-High` | `NotificationEvent`, `UserProjectNotification`, `WatchApp` | 수신자 계산이 얽혀 있음 |
| 사내 계정 정책 연동 | `Medium-High` | `UserApp`, `User`, `UserCredential`, `YonaUserServicePlugin` | 세션/토큰/OAuth를 함께 고려해야 함 |
| 웹훅 정책 강화 | `Medium` | `ProjectApp.webhooks`, `Webhook`, 관련 기술 문서 | 재시도/서명 정책은 별도 설계 필요 |

## 4. 마지막에 해야 하는 변경

| 변경 후보 | 등급 | 주요 지점 | 위험 이유 |
| --- | --- | --- | --- |
| PR 머지 정책 대수술 | `High` | `PullRequest`, `PullRequestApp.accept`, `PullRequestMergingActor` | 저장소 상태와 직접 연결 |
| Git/SVN 저장소 처리 변경 | `High` | `RepositoryService`, `GitApp`, `SvnApp`, `playRepository/*` | 데이터 손상 가능성 |
| 메일 기반 입력 변경 | `High` | `MailboxService`, `EmailHandler`, `CreationViaEmail` | 보안/운영 리스크가 큼 |
| 이벤트 병합/수신자 계산 변경 | `High` | `NotificationEvent.add`, `filterReceivers` | 알림 회귀가 생기기 쉬움 |
| 번호 증가/게시글 공통 저장 규칙 변경 | `High` | `AbstractPosting`, `Project.increaseLastIssueNumber`, `increaseLastPostingNumber` | 무결성 영향 |

## 5. 첫 커스터마이징 후보 3개

### 후보 1. 제품 용어/브랜딩 정리

| 항목 | 내용 |
| --- | --- |
| 목표 | Yona/Yobi 혼재, 회사 용어 미스매치, 메뉴/문구를 빠르게 정리 |
| 주요 지점 | `conf/messages*`, `app/views/common/*`, `app/views/index/*`, `public/images/*` |
| 등급 | `Safe` |
| 기대 효과 | 사용자 혼란 감소, 팀이 UI 구조를 익히는 데 도움 |

### 후보 2. 프로젝트/조직/멤버십 정책 회사화

| 항목 | 내용 |
| --- | --- |
| 목표 | 누가 조직/프로젝트를 만들고 멤버를 추가할지 정책 반영 |
| 주요 지점 | `ProjectApp`, `OrganizationApp`, `AccessControl`, `ProjectUser`, `OrganizationUser` |
| 등급 | `Medium` |
| 기대 효과 | 실제 사내 운영 모델과 맞닿아 있어 도입 가치가 큼 |

### 후보 3. 이슈 + 알림 흐름 회사화

| 항목 | 내용 |
| --- | --- |
| 목표 | 이슈 생성/수정/댓글/알림의 기본 경험을 회사 업무 흐름에 맞춤 |
| 주요 지점 | `IssueApp`, `Issue`, `IssueEvent`, `NotificationEvent`, `views.html.issue.*` |
| 등급 | `Medium-High` |
| 기대 효과 | 협업 서비스로서 체감 가치가 가장 큼 |

## 6. 지금은 건드리지 말아야 할 영역

### 사실

- `RepositoryService`는 Git/SVN, Smart HTTP, DAV, post-receive hook을 함께 다룬다.
- `PullRequest.merge()`는 실제 머지 연산과 충돌 계산을 모델 내부에서 수행한다.
- `MailboxService`는 장기 스레드, IMAP 상태, `Property` 기반 UID 추적을 사용한다.

### 판단

- 아래 영역은 1차 커스터마이징 대상에서 제외하는 것이 안전하다.
  - `RepositoryService`
  - `PullRequest.merge()`와 관련 actor
  - `SvnApp`
  - `MailboxService`
  - `NotificationEvent.add()`의 이벤트 병합 규칙

## 7. 안전한 수정 원칙

1. `view`만 바꾸는지, `controller`까지 바꾸는지, `model`까지 바꾸는지를 먼저 표시한다.
2. `model`을 바꾸면 관련 `NotificationEvent`, `AccessControl`, `test`를 함께 조사한다.
3. `RepositoryService`, `PullRequest`, `MailboxService`가 얽히면 자동으로 `High` 등급으로 본다.
4. 새 요구사항이 들어오면 먼저 `workflow-map.md`에서 해당 흐름을 찾고, 그다음 이 문서에서 seam 등급을 확인한다.

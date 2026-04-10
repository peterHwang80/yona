# Yona Domain Map

## 문서 목적

이 문서는 Yona의 핵심 모델을 "엔티티 목록"이 아니라 "무엇을 책임지는 응집점인가" 기준으로 정리한다.  
커스터마이징 설계 시 가장 자주 부딪히는 5개 모델을 먼저 묶는다.

## 1. 핵심 응집점

| 모델 | 역할 | 왜 먼저 읽어야 하나 |
| --- | --- | --- |
| `User` | 계정, 멤버십, 선호, 즐겨찾기, 방문 이력 | 인증/권한/조직 연동의 시작점 |
| `Project` | 협업의 루트 집합체 | 저장소, 멤버십, 메뉴, 포크, 웹훅이 모두 연결됨 |
| `Issue` | 업무 추적의 중심 | 상태/담당자/라벨/댓글/알림이 집중됨 |
| `PullRequest` | 코드 리뷰와 머지의 중심 | 저장소 연산과 리뷰 정책이 결합됨 |
| `NotificationEvent` | 알림의 중심 | 이벤트 메시지, 수신자 계산, watch 정책이 모임 |

## 2. 관계 지도

```text
User
 |--< ProjectUser >-- Project --< Issue
 |                        |        |--< IssueComment
 |                        |        |--< IssueEvent
 |                        |        |--< IssueLabel
 |                        |
 |                        |--< PullRequest >-- Project (from/to)
 |                        |        |--< PullRequestEvent
 |                        |        |--< ReviewComment / CommentThread
 |                        |
 |                        |--< Watch / Unwatch
 |                        |--< Webhook
 |
 |--< OrganizationUser >-- Organization
 |
 \--< NotificationEvent.receivers
```

## 3. `User`

### 핵심 역할

- 로그인 식별자, 비밀번호, 이메일, 상태 관리
- 프로젝트/조직 멤버십 조회
- 즐겨찾기와 최근 방문 관리
- 토큰 기반 사용자 식별 보조
- 표시 이름/언어/프로필 관련 정책 보유

### 관련 관계

- `projectUser`
- `groupUser` / `organizationUsers`
- `enrolledProjects`
- `enrolledOrganizations`
- `notificationEvents`
- `emails`
- `favoriteProjects`, `favoriteOrganizations`, `favoriteIssues`

### 사실

- `User`는 계정 상태 변경 시 프로젝트/알림/담당자 관계까지 함께 정리한다.
- `findByUserToken`, `extractUserTokenFromRequestHeader` 같은 API 토큰 처리도 `User`가 가진다.
- 익명 사용자와 게스트 사용자를 별도로 다룬다.

### 판단

- 사내 계정 연동 시 `UserApp`만 보는 것으로는 부족하고 `User`, `UserCredential`, `YonaUserServicePlugin`까지 함께 봐야 한다.
- 사용자 모델은 "인증 모델"이 아니라 "업무 협업 허브"에 더 가깝다.

## 4. `Project`

### 핵심 역할

- 협업의 루트 컨테이너
- 저장소 타입(`GIT` / `Subversion`) 보유
- 이슈/게시판/마일스톤/멤버/웹훅/알림 설정 연결
- 포크/이전 이름/이전 소유자 이력 유지
- README, `ISSUE_TEMPLATE.md`, 기본 브랜치 조회

### 관련 관계

- `issues`
- `projectUser`
- `posts`
- `milestones`
- `labels`
- `originalProject`, `forkingProjects`
- `webhooks`
- `assignees`
- `pushedBranches`
- `organization`
- `menuSetting`
- `issueLabels`

### 사실

- 프로젝트 생성은 `Project.create()`와 `RepositoryService.createRepository()`가 세트로 움직인다.
- 이름 변경/이관 시 이전 위치 정보를 남겨 redirect 근거로 사용한다.
- 삭제 시 이슈, PR, 댓글 스레드, 라벨, 웹훅 등 다수의 연관 데이터를 직접 정리한다.

### 판단

- 프로젝트는 단순 엔티티가 아니라 aggregate root 역할을 한다.
- 프로젝트 정책 변경은 거의 항상 권한, 저장소, 알림, 메뉴까지 영향을 준다.

## 5. `Issue`

### 핵심 역할

- 업무 항목 본문과 상태 보유
- 담당자, 마일스톤, 라벨, 공유자, 투표, 댓글 연결
- 다른 프로젝트로 이동 가능
- 드래프트와 부모/하위 이슈 관계 지원

### 관련 관계

- `milestone`
- `labels`
- `assignee`
- `comments`
- `events`
- `sharers`
- `voters`
- `parent`
- 상위 클래스 `AbstractPosting.project`

### 사실

- `Issue`는 `AbstractPosting`을 상속해 공통 번호 증가/댓글 수 계산/mention 업데이트 흐름을 공유한다.
- `Issue.update()`와 `Issue.save()`는 담당자 보정 로직을 포함한다.
- `IssueApp.editIssue()`는 상태/라벨/담당자/마일스톤/프로젝트 이동을 한 액션에서 처리한다.

### 판단

- 상태 머신이나 필드 확장을 넣을 때 가장 먼저 부딪히는 모델이다.
- "이슈만 바꾸면 된다"는 가정이 가장 위험한 축이다. `IssueEvent`와 `NotificationEvent`를 반드시 함께 봐야 한다.

## 6. `PullRequest`

### 핵심 역할

- from/to project, from/to branch, 상태, 리뷰어, 관련 이벤트 보유
- 머지 가능성 계산, 충돌 여부 계산, 실제 merge 수행
- 리뷰 스레드와 브랜치 정리 정책 연결

### 관련 관계

- `toProject`
- `fromProject`
- `contributor`
- `receiver`
- `pullRequestCommits`
- `pullRequestEvents`
- `reviewers`
- `commentThreads`

### 사실

- `PullRequest`는 `isConflict`, `isMerging`, `mergedCommitIdFrom`, `mergedCommitIdTo`까지 저장한다.
- merge 로직이 모델 내부에 깊게 들어 있다.
- project-level reviewer count 정책(`toProject.isUsingReviewerCount`)의 영향을 받는다.

### 판단

- PR은 controller보다 model 로직이 더 중요하다.
- 기업 맞춤 머지 정책은 `PullRequestApp`보다 `PullRequest` 자체를 먼저 읽어야 한다.

## 7. `NotificationEvent`

### 핵심 역할

- 알림 이벤트의 제목, sender, receiver, resource, event type 저장
- 표시 메시지와 plain message 조합
- 수신자 필터링
- draft-time 내 이벤트 병합
- 오래된 알림 삭제 스케줄링

### 관련 관계

- `receivers`
- `notificationMail`
- 리소스 역참조(`resourceType`, `resourceId`)

### 사실

- `NotificationEvent.add()`는 단순 저장이 아니라 이벤트 병합과 receiver filter를 수행한다.
- receiver filter는 `AccessControl.isAllowed`와 `UserProjectNotification`까지 함께 본다.
- `scheduleDeleteOldNotifications()`가 Akka scheduler로 동작한다.

### 판단

- 알림은 별도 서비스가 아니라 도메인 로직의 일부다.
- 수신자 계산을 바꾸는 순간 watch, unwatch, 권한, 메일 발송 정책이 함께 흔들린다.

## 8. 보조 축

### `AbstractPosting`

- `Issue`, `Posting`, 일부 댓글 흐름의 공통 저장 규칙을 제공
- 번호 증가, 댓글 수 계산, mention/title keyword 업데이트를 공통 처리

### `RepositoryService`

- 도메인 모델은 아니지만 `Project`와 `PullRequest`를 실질적으로 지배하는 인프라 축
- `GitRepository` / `SVNRepository` 선택, Smart HTTP, DAV, push hook 연결 담당

### `AccessControl`

- 역할 기반 권한보다 "resource + operation" 해석기 역할
- 프로젝트 공개범위, group membership, author/assignee/sharer 예외까지 반영

## 9. 커스터마이징 관점 요약

### 사실

- 업무 흐름의 핵심 상태는 여러 모델에 분산되어 있다.
- 모델이 controller보다 많은 정책을 직접 가진다.

### 판단

- 첫 커스터마이징 설계는 아래 모델 조합 단위로 보는 것이 맞다.
  - 계정/권한: `User` + `AccessControl`
  - 프로젝트: `Project` + `ProjectUser` + `OrganizationUser`
  - 이슈: `Issue` + `IssueEvent` + `NotificationEvent`
  - PR: `PullRequest` + `PullRequestEvent` + `RepositoryService`
  - 알림: `NotificationEvent` + `Watch` + `UserProjectNotification`

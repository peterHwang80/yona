# Yona Workflow Map

## 문서 목적

이 문서는 업무 흐름별로 `route -> controller -> model -> view -> async -> config -> test`를 한 번에 볼 수 있도록 만든 작업 지도다.  
모든 섹션은 사실과 판단을 분리해서 기록한다.

## 1. 로그인 / 권한 / 사용자

| 항목 | 내용 |
| --- | --- |
| 대표 route | `GET /users/loginform`, `POST /users/login`, `GET /users/signupform`, `POST /users/signup`, `GET /user/editform` |
| 진입 controller | `UserApp.loginForm`, `UserApp.login`, `UserApp.signupForm`, `UserApp.newUser`, `UserApp.editUserInfoForm`, `UserApp.editUserInfo` |
| 공통 진입 | `Global.onRequest`, `UserApp.initTokenUser`, `UserApp.currentUser`, `AccessControl.isAllowed` |
| 핵심 model | `User`, `UserCredential`, `UserSetting`, `SiteAdmin`, `Email`, `LinkedAccount` |
| view / Twirl | `views.html.user.login`, `views.html.user.signup`, `views.html.user.edit`, `edit_password`, `edit_notifications`, `edit_emails`, `edit_token`, `views.html.user.view` |
| 비동기 후처리 | 명시적 사용자 액터는 적음. 이메일 검증 관련으로 `ValidationEmailSender`가 존재 |
| 관련 config | `application.allowsAnonymousAccess`, `application.use.social.login.only`, `application.use.social.login.name.sync`, `application.social.login.support`, `application.use.ldap.login.supoort`, `application.login.page.*` |
| 관련 test | `test/controllers/UserAppTest.java`, `test/controllers/PasswordResetAppTest.java`, `test/models/UserTest.java`, `docs/ko/technical/current-user.md` |
| 커스터마이징 위험도 | `Medium` |

### 사실

- 현재 사용자 해석은 session -> token -> context 순으로 보완된다.
- 로그인/회원가입/편집 흐름은 대부분 `UserApp`에 모여 있다.
- 소셜 로그인 연동은 `play.plugins`와 `service/YonaUserServicePlugin.java`를 통해 연결된다.

### 판단

- 사내 SSO/사원정보 연동을 넣고 싶다면 가장 먼저 건드릴 축이다.
- 단, `AccessControl`과 세션/토큰 흐름을 함께 읽지 않고 `UserApp`만 수정하면 회귀 가능성이 높다.

### 회사 맞춤 변경 후보

- 사내 계정 체계에 맞춘 로그인 방식 변경
- 회원가입 제한/자동 승인 정책
- 프로필 필드, 부서명, 표시 이름 정책
- 기본 로그인 후 landing page 정책

## 2. 프로젝트 / 조직 / 멤버십

| 항목 | 내용 |
| --- | --- |
| 대표 route | `GET /organizations/new`, `POST /organizations/new`, `GET /organizations/:organizationName`, `GET /:user/:project`, `POST /:user/:project/members` |
| 진입 controller | `OrganizationApp.newForm`, `newOrganization`, `organization`, `members`, `addMember`, `ProjectApp.newProjectForm`, `newProject`, `project`, `members`, `settingForm` |
| 핵심 model | `Project`, `Organization`, `ProjectUser`, `OrganizationUser`, `ProjectMenuSetting`, `Role`, `ProjectTransfer` |
| view / Twirl | `views.html.organization.create`, `view`, `members`, `setting`, `deleteForm`, `views.html.project.create`, `home`, `members`, `setting` |
| 비동기 후처리 | 멤버 승인/추가 후 `NotificationEvent.afterMemberRequest` 호출 |
| 관련 config | `application.hide.project.listing`, `application.displayPrivateRepositories`, `project.default.scope.when.create` |
| 관련 test | `test/controllers/ProjectAppTest.java`, `test/controllers/EnrollProjectAppTest.java`, `test/models/ProjectTest.java`, `ProjectUserTest.java`, `OrganizationTest.java`, `OrganizationUserTest.java` |
| 커스터마이징 위험도 | `Medium-High` |

### 사실

- 프로젝트 생성 시 `Project.create()` 후 `ProjectUser.assignRole()`과 `RepositoryService.createRepository()`가 함께 호출된다.
- 조직과 프로젝트 멤버십은 별도 모델로 분리되어 있다.
- 프로젝트 홈은 `ProjectApp.project()`가 진입점이며 `views.html.project.home`을 렌더링한다.

### 판단

- 프로젝트/조직 생성 정책과 멤버십 승인 정책은 회사 맞춤화에서 거의 항상 손대게 된다.
- 저장소 생성이 프로젝트 생성과 강하게 연결되어 있어, "저장소 없이 프로젝트만" 같은 정책은 추가 설계가 필요하다.

### 회사 맞춤 변경 후보

- 조직/프로젝트 생성 권한 제한
- 프로젝트 기본 메뉴/기본 공개범위 정책
- 멤버 승인/자동 초대/조직 동기화 정책
- 프로젝트 화면의 메뉴/용어/대시보드 구성 변경

## 3. 이슈 생성 / 조회 / 수정 / 댓글 / 알림

| 항목 | 내용 |
| --- | --- |
| 대표 route | `GET /:user/:project/issueform`, `POST /:user/:project/issues/latest`, `GET /:user/:project/issue/$number`, `POST /:user/:project/issue/$number/edit`, `POST /:user/:project/issue/$number/comments` |
| 진입 controller | `IssueApp.newIssueForm`, `newIssue`, `issue`, `editIssue`, `newComment`, `issues`, `timeline` |
| 핵심 model | `Issue`, `IssueComment`, `IssueEvent`, `IssueLabel`, `IssueLabelCategory`, `Milestone`, `Assignee`, `Watch`, `NotificationEvent` |
| view / Twirl | `views.html.issue.create`, `edit`, `view`, `list`, `partial_comments`, `partial_list_wrap` |
| 비동기 후처리 | 직접 액터는 적지만 `NotificationMail.onStart()`와 `NotificationEvent.onStart()` 스케줄러가 후속 메일/정리 담당 |
| 관련 config | `notification.bymail.enabled`, `application.notification.bymail.*`, `application.notification.draft-time`, `application.maxFileSize` |
| 관련 test | `test/controllers/IssueAppTest.java`, `test/controllers/api/IssueApi*.java`, `test/models/IssueTest.java`, `test/models/NotificationEventTest.java` |
| 커스터마이징 위험도 | `High` |

### 사실

- 생성 진입점은 `IssueApp.newIssueForm()`과 `IssueApp.newIssue()`다.
- `Issue`는 `AbstractPosting`을 상속하고, 저장 시 번호 증가와 mention/title head 후처리를 함께 수행한다.
- 댓글 생성 후 `NotificationEvent.afterNewComment()`가 호출된다.
- 이슈 수정 로직은 상태 변경, 라벨, 담당자, 마일스톤, 프로젝트 이동까지 한 액션에 모여 있다.

### 판단

- 이슈 흐름은 컨트롤러와 모델이 모두 두꺼워, 커스터마이징 영향 범위가 가장 넓은 축 중 하나다.
- 워크플로우 상태나 필드 확장이 필요하다면 단순 UI 변경으로 끝나지 않고 `Issue`, `IssueApp`, `IssueEvent`, `NotificationEvent`를 함께 봐야 한다.

### 회사 맞춤 변경 후보

- 이슈 상태 체계와 기본 필드 확장
- 이슈 생성 템플릿/필수값 정책
- 담당자/공유자/라벨 정책
- 댓글과 알림 규칙 조정

### End-to-End 샘플: 이슈 생성

| 단계 | 코드 경로 |
| --- | --- |
| route | `POST /:user/:project/issues/latest` |
| controller | `IssueApp.newIssue()` |
| 저장 | `Issue.save()` -> `AbstractPosting.save()` |
| 번호 증가 | `Project.increaseLastIssueNumber()` |
| 후속 이벤트 | `NotificationEvent.afterNewIssue()` / `IssueEvent.addFromNotificationEvent()` |
| 응답 | 이슈 상세로 redirect, 이후 `views.html.issue.view` |

## 4. Pull Request / 리뷰 / 머지

| 항목 | 내용 |
| --- | --- |
| 대표 route | `GET /:ownerName/:project/newPullRequestForm`, `POST /:ownerName/:project/pullRequests`, `GET /:ownerName/:project/pullRequest/:id`, `POST /:ownerName/:project/pullRequest/:id/accept`, `POST /:user/:project/pullRequest/:number/review` |
| 진입 controller | `PullRequestApp.newPullRequestForm`, `newPullRequest`, `pullRequest`, `accept`, `newComment`, `close`, `open`, `editPullRequest`, `ReviewApp.review` |
| 핵심 model | `PullRequest`, `PullRequestCommit`, `PullRequestEvent`, `PullRequestMergeResult`, `ReviewComment`, `CommentThread`, `CodeCommentThread`, `CodeRange` |
| view / Twirl | `views.html.git.create`, `list`, `view`, `viewChanges`, `partial_state`, `partial_merge_result` |
| 비동기 후처리 | `PullRequestMergingActor`, `RelatedPullRequestMergingActor` |
| 관련 config | 전역 config보다 프로젝트 상태(`isUsingReviewerCount`, `menuSetting.code`, `menuSetting.pullRequest`) 영향이 큼 |
| 관련 test | `test/controllers/PullRequestAppTest.java`, `test/models/PullRequestTest.java`, `PullRequestEventTest.java`, `ReviewCommentTest.java` |
| 커스터마이징 위험도 | `High` |

### 사실

- PR 생성 화면은 `PullRequestApp.newPullRequestForm()`이 브랜치 목록을 읽어 `views.html.git.create`를 렌더링한다.
- PR 생성 후 `NotificationEvent.afterNewPullRequest()`와 `PullRequestEventMessage`가 만들어진다.
- 머지 수행은 `PullRequestApp.accept()`에서 비동기 흐름으로 이어진다.
- 리뷰 댓글은 `PullRequestApp.newComment()`에서 코드 범위 스레드(`CodeCommentThread`)와 연결된다.

### 판단

- 리뷰/머지 정책을 회사 방식으로 바꾸려면 가장 위험도가 높은 축이다.
- "승인자 수", "머지 조건", "브랜치 삭제/복구 정책"은 UI가 아니라 `PullRequest` 모델 로직이 중심이다.

### 회사 맞춤 변경 후보

- 승인자 수/리뷰 완료 기준
- 머지 가능 조건과 브랜치 정책
- PR 화면 정보 배치와 용어
- 리뷰 코멘트 정책과 템플릿

### End-to-End 샘플: PR 생성

| 단계 | 코드 경로 |
| --- | --- |
| route | `POST /:ownerName/:project/pullRequests` |
| controller | `PullRequestApp.newPullRequest()` |
| 저장 | `PullRequest.save()` |
| 후속 이벤트 | `NotificationEvent.afterNewPullRequest()` |
| 비동기 | `PullRequestMergingActor`에 `PullRequestEventMessage` 전달 |
| 응답 | PR 상세로 redirect, 이후 `views.html.git.view` |

## 5. Watch / Notification / Mailbox / Webhook

| 항목 | 내용 |
| --- | --- |
| 대표 route | `POST /watch`, `POST /unwatch`, `GET /:user/:project/watchers`, `GET/POST /:user/:project/webhooks` |
| 진입 controller | `WatchApp.watch`, `WatchApp.unwatch`, `ProjectApp.watchers`, `ProjectApp.webhooks`, `newWebhook`, `deleteWebhook` |
| 백그라운드 진입 | `MailboxService.start`, `NotificationMail.onStart`, `NotificationEvent.onStart` |
| 핵심 model | `Watch`, `Unwatch`, `NotificationEvent`, `NotificationMail`, `UserProjectNotification`, `Webhook`, `Property` |
| view / Twirl | `views.html.project.watchers`, `views.html.project.webhooks` |
| 비동기 후처리 | 알림 메일 스케줄러, 오래된 알림 정리 스케줄러, IMAP idle/polling thread |
| 관련 config | `notification.bymail.enabled`, `application.notification.*`, `smtp.*`, `imap.*`, `application.mailbox.polling.interval`(코드 참조) |
| 관련 test | `test/models/NotificationEventTest.java`, `NotificationMailTest.java`, `WatchTest.java`, `test/controllers/WatchProjectAppTest.java`, `test/mailbox/*` |
| 커스터마이징 위험도 | `High` |

### 사실

- `WatchApp`는 개별 리소스 watch/unwatch를 처리한다.
- `NotificationEvent`는 수신자 계산, 메시지 생성, 이벤트 병합, 알림 정리 스케줄링까지 담당한다.
- `MailboxService`는 IMAP IDLE 또는 polling으로 메일을 읽고, 이메일을 이슈/댓글로 변환한다.
- `docs/technical/watch.md`와 `docs/technical/mailbox.md`가 내부 동작을 설명한다.

### 판단

- 알림은 겉보기보다 깊은 기능이다. watch, explicit unwatch, event type 필터, 권한 필터가 모두 섞여 있다.
- 메일 기반 입력은 사내 보안 정책과 충돌할 수 있어, 초기에는 꺼 두고 검토하는 편이 안전하다.

### 회사 맞춤 변경 후보

- 알림 기본 정책 및 이벤트 타입별 수신 규칙
- 메일 답장 기반 이슈/댓글 생성 사용 여부
- 웹훅 보안 헤더/재시도 정책
- 사내 메신저 연동 지점 추가

## 6. Git / SVN 저장소 연동

| 항목 | 내용 |
| --- | --- |
| 대표 route | `GET /:ownerName/:project/info/refs`, `POST /:ownerName/:project/git-upload-pack|git-receive-pack`, `HEAD/GET/POST/PUT/DELETE /svn/*path`, `GET /:user/:project/code`, `GET /:user/:project/commits`, `GET /:user/:project/branches` |
| 진입 controller | `GitApp.advertise`, `GitApp.serviceRpc`, `SvnApp.serviceWithPath`, `CodeApp`, `CodeHistoryApp`, `BranchApp`, `CompareApp` |
| 핵심 model / infra | `RepositoryService`, `PlayRepository`, `GitRepository`, `SVNRepository`, `Commit`, `GitCommit`, `FileDiff`, `Project` |
| view / Twirl | `views.html.code.*`, `views.html.git.*` |
| 비동기 후처리 | `RepositoryService.receivePack()`/`uploadPack()` 내부 thread, SVN DAV service thread, post-receive hook 체인 |
| 관련 config | `application.context`, 프로젝트의 `vcs`, `menuSetting.code`, `menuSetting.pullRequest` |
| 관련 test | `test/playRepository/RepositoryServiceTest.java`, `GitRepositoryTest.java`, `CommitTest.java`, `FileDiffTest.java` |
| 커스터마이징 위험도 | `High` |

### 사실

- `GitApp`는 Smart HTTP 프로토콜을 직접 처리한다.
- `SvnApp`는 WebDAV 요청을 받아 `RepositoryService.createDavServlet()`로 넘긴다.
- `RepositoryService`는 프로젝트의 `vcs`에 따라 `GitRepository` 또는 `SVNRepository`를 선택한다.
- push 후처리는 `RejectPushToReservedRefs`, `UpdateLastPushedDate`, `UpdateRecentlyPushedBranch`, `IssueReferredFromCommitEvent`, `PullRequestCheck`, `NotifyPushedCommits` hook chain으로 이어진다.

### 판단

- 회사가 Git만 쓴다면 SVN을 기능적으로 비활성화하는 것만으로도 유지보수 난도를 크게 낮출 수 있다.
- 이 축은 데이터 손상 가능성이 있어, 가장 늦게 건드려야 하는 영역이다.

### 회사 맞춤 변경 후보

- Git-only 정책으로 단순화
- 브랜치 보호/푸시 정책 추가
- 저장소 경로/스토리지 운영 정책 정비
- 코드 브라우저/commit 화면의 정보 배치 조정

## 7. 공통 관찰

### 사실

- 대부분의 workflow가 controller와 model에 두껍게 분산되어 있다.
- 전용 service 레이어는 매우 얇다.
- 알림과 저장소 후처리처럼 중요한 부가 동작은 동기 요청 바깥에서 이어진다.

### 판단

- 커스터마이징은 "controller 한 곳 수정"으로 끝나는 경우가 드물다.
- 문서 없이 수정하면 회귀가 생기기 쉬운 구조다.

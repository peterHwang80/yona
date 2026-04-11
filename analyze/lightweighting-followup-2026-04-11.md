# Lightweighting Follow-up (2026-04-11)

## 이번 라운드 요약

- `CommentThreadApp`과 `/threads/:id/open`, `/threads/:id/close` route를 제거했다.
- `WatchApp`은 surviving-core 범위의 resource type만 처리하도록 줄였다.
  - 유지 범위: `ISSUE_POST`, `ISSUE_COMMENT`, `BOARD_POST`, `NONISSUE_COMMENT`, `PROJECT`
- 이슈 이벤트 타임라인 템플릿에서 `ISSUE_REFERRED_FROM_COMMIT`, `ISSUE_REFERRED_FROM_PULL_REQUEST`를 더 이상 렌더링하지 않도록 정리했다.
- `PullRequest`, `PullRequestCommit`, `CommitComment`, `CommentThread`에서 PR/review/commit 전용 dead helper를 지속적으로 제거했다.
- `PullRequestMergeResult`, `PullRequestException`을 삭제했다.
- `GitRepository`에서 PR 전용 helper를 정리했다.
  - 제거 대상: branch delete/restore, merging repository 생성, `diffCommits(PullRequest)`, `getPatch(PullRequest)`
- `NotificationEvent`에서 더 이상 호출되지 않는 PR/review notification 생성기와 code-review message builder를 제거했다.
- `NotificationMail`에서 review thread mail-threading과 removed-feature reply-to 생성을 중단했다.
- `AccessControl`, `RouteUtil`, `TemplateHelper`, `Resource` 주변에서 removed resource type 노출을 더 줄였다.
  - `CODE`, `COMMIT`, `COMMIT_COMMENT`, `COMMENT_THREAD`, `PULL_REQUEST`, `REVIEW_COMMENT`, `FORK`는 더 이상 creatable/allowed 대상으로 취급하지 않는다.
  - removed feature resource는 deep-link 대신 프로젝트 URL로 fallback 한다.
  - removed feature resource type은 `exists=false`, `getResourceObject=null`, `findByPath=null`로 안전하게 무시한다.
- `Project.deletePullRequests()`는 이제 `PullRequest.findByProject()`를 사용해 프로젝트와 연관된 legacy PR을 한 번만 정리한다.
  - `fromProject == project`, `toProject == project`가 동시에 성립할 수 있는 경계에서 중복 삭제 루프를 피하도록 단순화했다.
- 더 이상 사용되지 않는 `User.findPullRequestContributorsByProjectId()`를 제거했다.

## 검증

아래 명령을 순차 실행해서 모두 통과했다.

```powershell
cmd /c support-script\build-yona.cmd clean compile
cmd /c support-script\build-yona.cmd test:compile
cmd /c support-script\build-yona.cmd dist
```

배포 산출물:

- `target/universal/yona-1.16.0.zip`

## 현재 상태

- surviving-core 기준으로 `compile`, `test:compile`, `dist`가 계속 유지되고 있다.
- 제거 대상 기능의 사용자 노출 경로는 대부분 정리되었고, 모델/알림/메일/권한 계층의 레거시 참조도 상당 부분 줄였다.
- PR/review 관련 모델은 legacy data 호환을 위한 최소 구조만 남아 있으며, 실제 기능 경로는 계속 축소 중이다.

## 주의

- 현재 sbt 0.13 / Play 2.3 계열은 `compile`, `test:compile`, `dist`를 병렬로 돌리면 classfile manager 문제가 발생한다.
- 검증은 반드시 순차 실행 기준으로 유지한다.

## 다음 우선순위

1. `run` 기준 surviving-core 수동 스모크
   - 로그인
   - 프로젝트 생성/수정
   - 게시판 글/댓글
   - 이슈 생성/상태 변경/댓글/첨부
   - 알림 조회
   - 웹훅 생성 및 issue 이벤트 발송
2. `ResourceType`, `EventType`, `MenuType` 등 enum/상수층에서 removed feature 상수를 더 줄일 수 있는지 점검
3. `PullRequest`, `ReviewComment`, `CodeCommentThread` 계층에서 legacy data 호환을 해치지 않는 범위의 추가 축소 검토

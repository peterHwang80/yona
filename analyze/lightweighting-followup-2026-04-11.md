# Lightweighting Follow-up (2026-04-11)

## 이번 라운드 정리

- `CommentThreadApp`과 `/threads/:id/open`, `/threads/:id/close` route를 제거했다.
- `WatchApp`은 이제 surviving-core 범위의 resource type만 처리한다.
  - 허용 범위: `ISSUE_POST`, `ISSUE_COMMENT`, `BOARD_POST`, `NONISSUE_COMMENT`, `PROJECT`
- `NotificationEvent`에서 PR/review 전용 reviewer-action dead path를 제거했다.
- 이슈 타임라인 템플릿에서 `ISSUE_REFERRED_FROM_COMMIT`, `ISSUE_REFERRED_FROM_PULL_REQUEST`를 더 이상 렌더링하지 않도록 정리했다.
- `PullRequest`에서 더 이상 쓰이지 않는 PR merge/reviewer/query helper를 대거 제거했다.
  - 제거 예: merge 시뮬레이션, reviewer count helper, PR diff/patch 계산 경로, closed/open query helper
- `PullRequestMergeResult`, `PullRequestException`를 삭제했다.
- `GitRepository`에서 PR 전용 정적 helper를 삭제했다.
  - 제거 예: branch delete/restore, merging repository 생성, `diffCommits(PullRequest)`, `getPatch(PullRequest)`
- `PullRequestCommit`, `CommitComment`, `CommentThread`에서 commit/PR 전용 dead helper를 추가로 제거했다.

## 이번 라운드 검증

아래 명령을 순차 실행해서 모두 통과했다.

```powershell
cmd /c support-script\build-yona.cmd clean compile
cmd /c support-script\build-yona.cmd test:compile
cmd /c support-script\build-yona.cmd dist
```

배포 산출물:

- `target/universal/yona-1.16.0.zip`

## 현재 상태

- surviving-core 기준의 `compile`, `test:compile`, `dist`는 계속 유지되고 있다.
- 제거 대상 기능의 사용자 노출 경로는 대부분 정리됐고, 이번 라운드에서 모델/저장소 계층의 dead path도 한 단계 더 줄였다.
- `PullRequest`, `CommentThread`, `CommitComment`, `PullRequestCommit`는 여전히 legacy data 호환용 최소 모델은 남아 있지만, 실제 기능 경로는 크게 축소된 상태다.

## 주의

- 현재 sbt 0.13/Play 2.3 계열은 `compile`, `test:compile`, `dist`를 병렬로 돌리면 classfile manager가 깨질 수 있다.
- 검증은 반드시 순차 실행 기준으로 유지한다.

## 다음 우선순위

1. `ReviewComment`, `CodeCommentThread`, `NotificationMail` 쪽의 남은 PR/review 레거시 경로를 더 축소
2. `Project.deletePullRequests()`와 legacy PR data 정리 경계 재점검
3. `run` 기준 surviving-core 수동 스모크
   - 로그인
   - 프로젝트 생성/수정
   - 게시판 글/댓글
   - 이슈 생성/상태 변경/댓글/첨부
   - 알림 조회
   - 웹훅 생성 및 issue 이벤트 발송

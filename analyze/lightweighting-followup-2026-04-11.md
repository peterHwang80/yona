# Lightweighting Follow-up (2026-04-11)

## 이번 단계에서 정리한 내용

- `NotificationEvent`에서 더 이상 호출되지 않는 PR/review/commit comment 전용 notification helper를 제거했다.
- `NotificationEvent` 내부의 PR/review webhook 분기를 no-op으로 바꿔, removed-feature 이벤트가 webhook 계층으로 전달되지 않게 했다.
- `Webhook`에서 PR/review 전용 payload builder와 overload 메서드를 삭제했다.
- `RouteUtil`에서 사용되지 않는 comment-thread 관련 import를 정리했다.

## 테스트 정리

- `CreationViaEmailTest`에서 review comment 생성 테스트를 삭제했다.
- `SearchTests`에서 review 검색 fixture와 `findReviews(...)` 테스트 블록 전체를 삭제했다.
- `CommitsNotificationActorTest`를 삭제했다.
  - 삭제 이유: code browser route 제거 후 더 이상 유효하지 않은 테스트였다.
- `RecentlyVisitedProjectsTest`를 삭제했다.
  - 삭제 이유: 현재 코드베이스에는 `RecentlyVisitedProjects` 모델이 존재하지 않아 테스트가 오래된 API에 묶여 있었다.
- `ProjectAppTest`에서 삭제된 `deletePushedBranch` route 테스트와 stale recent-visitation 호출을 제거했다.
- `MarkdownAppTest`의 commit URL 기대값을 현재 lightweighted fallback 동작에 맞게 조정했다.

## 현재 검증 결과

아래 명령을 순차 실행해서 모두 통과했다.

```powershell
cmd /c support-script\build-yona.cmd clean compile
cmd /c support-script\build-yona.cmd dist
cmd /c support-script\build-yona.cmd test:compile
```

배포 산출물:

- `target/universal/yona-1.16.0.zip`

## 현재 판단

- 앱 빌드(`compile`)
- 배포 패키징(`dist`)
- 테스트 소스 컴파일(`test:compile`)

위 세 기준에서, 이번 단계에서 제거한 PR/review/code/import 계열 참조 때문에 막히는 지점은 정리된 상태다.

## 다음 우선순위

1. `PullRequest`, `ReviewComment`, `CommitComment`, `CodeCommentThread` 같은 레거시 모델 중 실제 surviving-core가 더 이상 사용하지 않는 부분을 추가로 축소
2. 알림/리소스 변환 계층에서 legacy resource type이 남은 데이터를 만났을 때도 목록/화면이 더 단순하게 동작하도록 정리
3. surviving-core 기준 수동 스모크 시나리오를 문서화하고 `run` 기준으로 확인

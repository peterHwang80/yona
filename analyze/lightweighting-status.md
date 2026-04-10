# Yona 경량화 진행 상태

## 목적

현재 Play 코드베이스에서 아래 기능을 실제 코드 삭제 기준으로 제거하고, 남는 핵심 협업 기능이 계속 동작하도록 정리한다.

- Git/SVN 저장소 호스팅
- 코드 브라우저, 커밋 히스토리, 브랜치, compare, commit comment
- 포크와 Pull Request
- 코드 리뷰와 리뷰 스레드
- 데이터 import/export 및 외부 서비스 migration
- site 관리자용 export/import

## 이번 단계에서 실제로 반영된 내용

### 1. 진입점 제거

- `conf/routes`에서 import/migration, Git/SVN, code browser, branch, compare, pull request, review 관련 route 삭제
- site admin의 data export/import route 삭제
- 프로젝트 생성/설정/메뉴/대시보드에서 code/PR/review/import 진입점 삭제

### 2. controller / view / asset 제거

- 삭제된 controller
  - `BranchApp`
  - `CodeApp`
  - `CodeHistoryApp`
  - `CompareApp`
  - `GitApp`
  - `ImportApp`
  - `MigrationApp`
  - `PullRequestApp`
  - `ReviewApp`
  - `ReviewThreadApp`
  - `SvnApp`
- 삭제된 view 묶음
  - `app/views/code/*`
  - `app/views/git/*`
  - `app/views/reviewthread/*`
  - migration/import/change-vcs 관련 화면
  - PR/review 전용 partial
- 삭제된 asset
  - `public/javascripts/service/yona.Migration.js`
  - `app/assets/stylesheets/less/_migration.less`
  - `public/javascripts/service/yobi.project.ChangeVCS.js`

### 3. 데이터 이전 모듈 제거

- `app/data/*` 전체 삭제
- `SiteApp`의 data import/export 의존성 제거
- migration 전용 layout 제거

### 4. 백엔드 잔여 경로 정리

- `RepositoryService`에서 `PullRequestCheck` post-receive hook 제거
- PR merge actor 체인 삭제
  - `PullRequestActor`
  - `PullRequestMergingActor`
  - `RelatedPullRequestMergingActor`
- `PullRequest.merge(...)`에서 actor dispatch 제거
- review 검색 dead code 제거
  - `ReviewSearchCondition` 삭제
  - `Search`의 review 검색 메서드 제거
  - `SearchResult`의 review 결과 필드 제거
  - `CommentThread.countReviewsBy(...)` 제거

### 5. 메일/알림 레거시 차단

- `EmailHandler`에서 review thread reply 메일 무시 처리
- `CreationViaEmail`의 review comment 저장 메서드 삭제
- `NotificationEvent.findByReceiver(...)`, `getNotificationsCount(...)`에서 removed-feature 이벤트 필터링
- `NotificationMail`에서 removed-feature 이벤트 병합/발송 대상 제외
- `Webhook`의 PR/review 관련 overload를 no-op 처리
  - 숨김 대상
    - `NEW_PULL_REQUEST`
    - `PULL_REQUEST_STATE_CHANGED`
    - `NEW_REVIEW_COMMENT`
    - `PULL_REQUEST_MERGED`
    - `ISSUE_REFERRED_FROM_COMMIT`
    - `PULL_REQUEST_COMMIT_CHANGED`
    - `NEW_COMMIT`
    - `PULL_REQUEST_REVIEW_STATE_CHANGED`
    - `ISSUE_REFERRED_FROM_PULL_REQUEST`
    - `REVIEW_THREAD_STATE_CHANGED`

### 6. UI 레거시 흔적 제거

- project/organization/search/user 화면에서 fork/original project 표시 제거
- 프로젝트 이슈 멘션 검색이 더 이상 원본 프로젝트를 따라가지 않고 현재 프로젝트만 보도록 변경
- 프로젝트 author 집계에서 PR contributor 제외
- project association 계산을 현재 프로젝트 단일 기준으로 축소
- 호출 경로가 완전히 끊긴 `changeVCS`, `nextVCS`, fork association helper 제거
- 기본 프로젝트 생성 메뉴 기준을 `issue, milestone, board`로 축소

### 7. removed-feature 테스트 제거

- controller/model/playRepository 기준의 PR/review/repository/import 전용 테스트 삭제
- 예시
  - `PullRequestAppTest`
  - `ReviewThreadAppTest`
  - `PullRequestTest`
  - `ReviewCommentTest`
  - `RepositoryServiceTest`
  - `GitRepositoryTest`
  - `ReviewSearchConditionTest`

## 현재 검증 결과

아래 명령 기준으로 통과를 다시 확인했다.

```powershell
cmd /c support-script\build-yona.cmd clean compile
cmd /c support-script\build-yona.cmd dist
```

배포 산출물:

- `target/universal/yona-1.16.0.zip`

현재 남아 있는 빌드 경고는 removed-feature와 직접 관련 없는 기존 템플릿 경고 1건 수준이다.

## 아직 남아 있는 제거 대상 잔재

아래는 route/UI 기준으로는 이미 사라졌지만, 모델/enum/알림/webhook/DB 호환성 때문에 코드베이스에 아직 남아 있는 축이다.

- `PullRequest`, `PullRequestEvent`, `PullRequestCommit`
- `ReviewComment`, `CodeCommentThread`, `CommentThread`의 PR/review 중심 모델 구조
- `Webhook`의 PR/review payload 분기
- `NotificationEvent` 내부의 PR/review 관련 팩토리 메서드
- `Project` 내부의 `originalProject`, `forkingProjects`, reviewer count 관련 필드/메서드
- 메시지 리소스와 evolution의 레거시 enum/table 정의

이 영역은 DB 호환성과 기존 레거시 데이터 해석에 걸쳐 있으므로, 다음 단계에서도 “호출 경로 차단 -> 목록/화면 숨김 -> 내부 참조 정리 -> 최종 삭제” 순으로 진행하는 편이 안전하다.

## 다음 단계 제안

1. `Webhook`, `NotificationEvent`, `NotificationMail`에서 PR/review 전송 경로를 더 축소
2. `Project`의 fork/reviewer 설정 잔재 정리
3. `PullRequest*`, `ReviewComment*`, `CodeComment*` 모델 중 실제 생존 기능에서 더 이상 참조하지 않는 부분 정리
4. surviving-core 기준의 스모크 시나리오 문서화와 수동 실행 확인

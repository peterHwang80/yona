# Yona 디렉토리 구조 정리

## 1. 루트 레벨 구조

저장소 루트에서 보이는 핵심 디렉토리와 파일 수는 아래와 같다.

| 경로 | 파일 수 | 역할 |
| --- | ---: | --- |
| `app/` | 578 | 애플리케이션 본체 |
| `public/` | 723 | 정적 자산 |
| `docs/` | 103 | 설치/운영/기술/사용자 문서 |
| `test/` | 77 | 테스트 |
| `conf/` | 46 | 설정, 메시지, 라우팅, DB evolutions |
| `project/` | 2 | sbt 프로젝트 설정 |
| `support-script/` | 3 | 운영 보조 스크립트 |
| `lib/` | 1 | 로컬 jar |

대표 루트 파일은 다음과 같다.

- `build.sbt`: 메인 빌드 정의
- `README.md`: 제품 소개와 설치/운영 안내
- `.travis.yml`: Travis CI용 컴파일 설정
- `dev.sh`, `dist.sh`, `restart.sh`: 예시 운영/배포 스크립트
- `minify-js.sh`: JavaScript 번들 압축 스크립트
- `is-alive-bot.sh`: 생존 체크용 스크립트

## 2. 상위 트리 요약

```text
yona/
+-- app/
|   +-- actions/
|   +-- actors/
|   +-- assets/
|   +-- controllers/
|   |   +-- api/
|   |   \-- annotation/
|   +-- data/
|   |   \-- exchangers/
|   +-- mailbox/
|   +-- models/
|   |   +-- enumeration/
|   |   +-- resource/
|   |   \-- support/
|   +-- notification/
|   +-- playRepository/
|   |   \-- hooks/
|   +-- service/
|   +-- utils/
|   +-- validation/
|   \-- views/
+-- conf/
|   \-- evolutions/default/
+-- docs/
|   +-- ko/
|   +-- relnotes/
|   +-- technical/
|   \-- userManual/
+-- lib/
+-- project/
+-- public/
|   +-- bootstrap/
|   +-- images/
|   \-- javascripts/
+-- support-script/
\-- test/
```

## 3. `app/` 상세 구조

### 3.1 디렉토리별 파일 수

| 디렉토리 | 파일 수 | 역할 |
| --- | ---: | --- |
| `views` | 242 | Twirl 서버 템플릿 |
| `models` | 106 | 도메인 모델과 비즈니스 로직 |
| `utils` | 52 | 공통 유틸리티와 횡단 로직 |
| `controllers` | 51 | 웹/API 엔드포인트 |
| `data` | 50 | import/export용 데이터 교환 |
| `playRepository` | 23 | Git/SVN 추상화 |
| `assets` | 20 | LESS 스타일 자산 |
| `mailbox` | 11 | 이메일 입력 처리 |
| `actions` | 10 | 요청 전처리/권한 검사 액션 |
| `actors` | 7 | 비동기 작업 |
| `notification` | 2 | 알림 인터페이스/병합 이벤트 |
| `service` | 1 | Play 플러그인 연동 서비스 |
| `validation` | 1 | 검증 확장 |
| `errors` | 1 | 예외 타입 |

### 3.2 `controllers/`

상위 컨트롤러 파일은 39개이고, 추가로 `api/`와 `annotation/` 하위 패키지가 있다.

- 기능 컨트롤러: `ProjectApp`, `IssueApp`, `PullRequestApp`, `UserApp`, `SiteApp`, `OrganizationApp` 등
- API 컨트롤러: 7개
- 애노테이션 정의: 5개

라우팅 수가 특히 많은 컨트롤러는 아래와 같다.

| 컨트롤러 | 라우트 수 |
| --- | ---: |
| `ProjectApp` | 30 |
| `UserApp` | 26 |
| `IssueApi` | 21 |
| `PullRequestApp` | 21 |
| `SiteApp` | 21 |
| `IssueApp` | 18 |

이 수치는 실제 기능 중심축이 어디인지 그대로 보여 준다.

### 3.3 `models/`

`models/`는 주 도메인 엔티티 74개 수준에 더해 세부 하위 패키지가 있다.

| 하위 패키지 | 파일 수 | 내용 |
| --- | ---: | --- |
| `enumeration` | 14 | 상태, 작업, 범위 등 enum |
| `resource` | 5 | 권한/리소스 추상화 |
| `support` | 13 | 검색 조건, Finder 보조, 비교기 등 |

대표 모델은 아래와 같다.

- 사용자/권한: `User`, `Role`, `SiteAdmin`, `UserCredential`, `UserSetting`
- 프로젝트: `Project`, `ProjectUser`, `ProjectMenuSetting`, `ProjectTransfer`
- 조직: `Organization`, `OrganizationUser`
- 이슈: `Issue`, `IssueComment`, `IssueEvent`, `IssueLabel`, `Assignee`
- 게시판: `Posting`, `PostingComment`
- PR/리뷰: `PullRequest`, `PullRequestEvent`, `ReviewComment`, `CommentThread`
- 알림: `NotificationEvent`, `NotificationMail`, `Watch`, `Unwatch`

### 3.4 `playRepository/`

Git/SVN 관련 파일은 23개이며, 저장소 호스팅 성격을 이해할 때 가장 중요한 패키지다.

주요 클래스:

- `RepositoryService`
- `PlayRepository`
- `GitRepository`
- `SVNRepository`
- `Commit`, `GitCommit`, `SvnCommit`
- `FileDiff`, `DiffLine`, `Hunk`

하위 `hooks/`에서는 push 전후 후킹을 담당한다.

- `RejectPushToReservedRefs`
- `UpdateLastPushedDate`
- `UpdateRecentlyPushedBranch`
- `IssueReferredFromCommitEvent`
- `PullRequestCheck`
- `NotifyPushedCommits`

### 3.5 `actions/`

액션 레이어는 Play의 액션 컴포지션을 이용해 권한과 프로젝트 존재 여부를 검사한다.

- `AbstractProjectCheckAction`
- `DefaultProjectCheckAction`
- `NullProjectCheckAction`
- `AnonymousCheckAction`
- `IsAllowedAction`
- `IsCreatableAction`
- `IsOnlyGitAvailableAction`
- `GuestProhibitAction`
- `CodeAccessCheckAction`

### 3.6 `actors/`

비동기 액터는 7개다.

- `CommitsNotificationActor`
- `IssueReferredFromCommitEventActor`
- `PostReceiveActor`
- `PullRequestActor`
- `PullRequestMergingActor`
- `RelatedPullRequestMergingActor`
- `ValidationEmailSender`

PR 머지, 커밋 후속 처리, 메일 전송이 동기 HTTP 흐름 밖으로 빠져 있다.

### 3.7 `data/`

`DataService`와 47개의 exchanger가 있다. 기능은 명확하다.

- 전체 사이트 export/import
- 프로젝트/사용자/이슈/알림/라벨/멤버십/PR 등 테이블 단위 교환

이 패키지는 일반 서비스 레이어라기보다 "데이터 이동 전용 서브시스템"에 가깝다.

## 4. `views/` 구조

`app/views`는 서버 렌더링 UI의 중심이다.

| 디렉토리 | 파일 수 |
| --- | ---: |
| `common` | 35 |
| `issue` | 30 |
| `project` | 26 |
| `organization` | 17 |
| `git` | 17 |
| `user` | 17 |
| `index` | 16 |
| `site` | 14 |
| `code` | 13 |
| `search` | 10 |
| `error` | 9 |
| `board` | 6 |
| `milestone` | 5 |
| `help` | 5 |

구조상 특징:

- 공통 템플릿이 크다
- 이슈, 프로젝트, 조직, Git 영역이 서로 분리되어 있다
- 사이트 관리자 화면도 별도 뷰 세트를 가진다
- 부분 템플릿(`partial_*`) 활용 비중이 높다

## 5. `conf/` 구조

루트 설정 파일 목록:

- `application.conf.default`
- `application-logger.xml.default`
- `social-login.conf.default`
- `routes`
- `play.plugins`
- `ehcache.xml`
- `initial-data.yml`
- `test-data.yml`
- `shiro.ini`
- `messages`, `messages.ko-KR`, `messages.ja-JP`, `messages.ru-RU`, `messages.uz-UZ`

추가로 DB evolutions는 `conf/evolutions/default/1.sql`부터 `32.sql`까지 존재한다.

`application.conf.default`가 다루는 범주:

- 사이트 기본값
- 익명 접근과 게스트 정책
- DB 연결
- SMTP
- IMAP mailbox
- 서버 URL 구성
- 업데이트 체크
- GitHub migration
- 소셜 로그인
- LDAP 로그인

## 6. `public/` 구조

`public/`은 정적 자산 저장소다.

주요 하위 디렉토리:

- `bootstrap/`
- `images/`
- `javascripts/`

파일 유형 분포:

| 확장자 | 개수 |
| --- | ---: |
| `.js` | 430 |
| `.png` | 134 |
| `.css` | 94 |
| `.jpg` | 24 |
| `.gif` | 10 |

### `public/javascripts/`

총 526개 파일이며 아래와 같이 나뉜다.

| 디렉토리 | 파일 수 | 설명 |
| --- | ---: | --- |
| `lib` | 446 | 서드파티 라이브러리 |
| `service` | 44 | 화면/기능별 스크립트 |
| `common` | 32 | 공통 UI 모듈 |
| `template` | 1 | 코드 템플릿 |

대표 `service` 모듈:

- `yobi.issue.*`
- `yobi.project.*`
- `yobi.organization.*`
- `yobi.code.*`
- `yobi.git.*`
- `yobi.review.*`
- `yona.detectChange.js`
- `yona.temporarySaveHandler.js`

즉, 화면별 진입 스크립트와 공통 스크립트가 느슨하게 분리된 구조다.

## 7. `test/` 구조

`test/`는 모델과 유틸 테스트가 중심이다.

| 디렉토리 | 파일 수 |
| --- | ---: |
| `test/models` | 27 |
| `test/utils` | 14 |
| `test/controllers` | 13 |
| `test/support` | 5 |
| `test/controllers/api` | 5 |
| `test/playRepository` | 4 |
| `test/mailbox` | 3 |

관찰 포인트:

- 도메인 모델 검증이 비교적 많은 편이다
- API와 저장소 계층도 최소한의 테스트가 있다
- 프론트엔드 테스트 디렉토리는 없다

## 8. `docs/` 구조

`docs/`는 총 103개 파일이며 다음 성격으로 구분된다.

- 설치/운영 문서
- 릴리즈 노트
- 기술 문서
- 사용자 매뉴얼
- 한국어 문서와 영어 문서

눈에 띄는 하위 구조:

- `docs/ko/technical/`
- `docs/technical/`
- `docs/userManual/`
- `docs/relnotes/`
- `docs/ko/relnotes/`

즉, 문서 저장소 자체도 꽤 체계적이다.

## 9. 기타 디렉토리

### `project/`

- `build.properties`
- `plugins.sbt`

빌드 시스템의 핵심 버전과 sbt 플러그인 정의가 들어 있다.

### `lib/`

- `js-engine.jar`

로컬 jar가 하나 포함되어 있다. 외부 Maven 의존성만으로 끝나지 않는 흔적이다.

### `support-script/`

- `init.d/yona.sh`
- `mariadb/my.cnf`
- `mariadb/.my.cnf`

운영 환경 준비를 위한 샘플 자산으로 볼 수 있다.

## 10. 구조를 읽는 추천 순서

이 저장소를 처음 읽을 때는 아래 순서가 효율적이다.

1. 루트의 `README.md`, `build.sbt`
2. `conf/routes`
3. `app/Global.java`
4. `app/controllers/`
5. `app/models/`
6. `app/playRepository/`
7. `app/utils/AccessControl.java`
8. `conf/application.conf.default`
9. `test/`
10. `docs/technical/` 또는 `docs/ko/technical/`

이 순서대로 읽으면 "무슨 제품인가"에서 시작해 "어떤 계층이 책임을 가지는가"까지 자연스럽게 연결된다.

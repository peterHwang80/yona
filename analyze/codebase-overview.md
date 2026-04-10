# Yona 코드베이스 개요

## 1. 한눈에 보는 프로젝트

Yona는 웹 기반 프로젝트 호스팅 플랫폼이다. 단순한 이슈 트래커가 아니라 다음 기능을 한 제품 안에 함께 담고 있다.

- 프로젝트/조직 관리
- Git, SVN 저장소 호스팅
- 이슈, 게시판, 마일스톤
- 포크와 Pull Request
- 코드 리뷰와 리뷰 스레드
- 알림, Watch, 메일 알림
- 이메일을 통한 이슈/댓글 생성
- 데이터 import/export 및 외부 서비스 마이그레이션
- 사이트 관리자 기능

저장소의 중심 인상은 "레거시이지만 기능적으로 매우 넓은 올인원 협업 플랫폼"이다. 최신 프레임워크 스타일과는 거리가 있지만, 도메인 기능은 상당히 풍부하다.

## 2. 기술 스택과 런타임 성격

### 핵심 스택

- Play Framework 2.3.10
- sbt 0.13.5
- Java 8 계열 런타임 전제
- Java 중심 코드베이스 + Twirl 템플릿 + 소량의 Scala
- Ebean ORM
- Akka 기반 비동기 작업 및 스케줄링
- JGit 기반 Git 처리
- SVNKit 기반 SVN 처리
- LESS 기반 스타일 자산

### `build.sbt` 기준 특징

- 애플리케이션 버전은 `1.16.0`
- Play 플러그인, Twirl, LESS, buildinfo, findbugs를 사용
- `javaJdbc`, `javaEbean`, `javaWs`, `cache`를 포함
- `play-authenticate`로 소셜 로그인 처리
- `org.eclipse.jgit` 계열 의존성으로 Git Smart HTTP와 LFS/Archive 확장 지원
- `svnkit`, `svnkit-dav`로 Subversion 및 WebDAV 처리
- `shiro-core`, `commons-email`, `play-2-mailplugin`, `spring-jdbc`, `jsoup`, `tika-core` 등 유틸리티 의존성이 넓다

### 코드베이스 구성 비율

`app` 기준 파일 수는 총 578개다.

| 종류 | 개수 |
| --- | ---: |
| Java | 315 |
| Twirl 템플릿(`*.scala.html`) | 242 |
| LESS | 20 |
| Scala | 1 |

즉, 런타임은 Play/Scala 계열이지만 실제 도메인 코드는 거의 Java로 작성된 혼합형 Play 애플리케이션이다.

## 3. 이 저장소의 가장 중요한 구조적 특징

### 3.1 MVC이지만 "Fat Controller + Fat Model" 성향이 강하다

최근의 계층형 아키텍처처럼 `controller -> service -> repository -> domain`이 명확히 나뉘어 있지 않다. 대신 다음과 같은 구조를 가진다.

- `controllers/`: HTTP 요청 처리, 폼 바인딩, 렌더링, 권한 진입점
- `models/`: 엔티티이면서 동시에 비즈니스 로직의 중심
- `playRepository/`: Git/SVN 추상화 계층
- `utils/`: 권한, 설정, 마크다운, URL, 파일, 날짜, LDAP 등 횡단 관심사
- `data/`: export/import 전용 데이터 교환 계층

즉, 서비스 레이어가 아주 얇거나 거의 없고, 중요한 로직이 컨트롤러와 모델에 넓게 퍼져 있다.

### 3.2 "Yobi"라는 과거 이름이 코드 전반에 남아 있다

제품 이름은 Yona지만, 내부에는 과거 이름인 Yobi의 흔적이 많다.

- `build.sbt`의 `lazy val yobi`
- `YobiUpdate`
- `public/javascripts/common/yobi.*`
- 일부 문서 제목과 설명

이 때문에 처음 읽을 때 "Yobi와 Yona가 다른 프로젝트인가?"라는 혼동이 생길 수 있지만, 현재 저장소는 사실상 동일 계보의 코드다.

### 3.3 서버 렌더링 중심의 구형 웹 애플리케이션 스타일

프론트엔드는 SPA가 아니라 Play Twirl 기반 서버 렌더링이 중심이다.

- 서버가 HTML을 직접 렌더링
- 일부 화면은 PJAX/JSON 응답으로 부분 갱신
- 프론트엔드 로직은 jQuery 스타일 네임스페이스 모듈
- 번들링은 `minify-js.sh` + Closure Compiler

즉, 현대 프론트엔드 빌드 체인보다 "전통적인 서버 렌더링 웹앱"에 가깝다.

## 4. 요청 처리 흐름

### 4.1 진입점

중요한 진입점은 두 곳이다.

- `conf/routes`: 총 353라인의 라우팅 정의
- `app/Global.java`: 전역 초기화, 요청 훅, 에러 처리

### 4.2 `Global.java`가 담당하는 일

`Global.java`는 단순 훅 파일이 아니라 런타임 부트스트랩의 중심이다.

- 기본 설정 파일 자동 생성
- `social-login.conf`, logger 설정 파일 자동 생성
- DB가 비어 있으면 `initial-data.yml`로 초기 데이터 삽입
- `Config`, `Property`, `PullRequest`, `NotificationMail`, `NotificationEvent`, `Attachment`, `AccessControl`, `YobiUpdate` 초기화
- `MailboxService` 시작
- OAuth 리졸버 설정
- 모든 요청에 대해 사용자/언어 초기화 및 Access log 처리
- WebDAV 메서드를 `SvnApp`로 우회 라우팅
- 글로벌 에러/404/400 처리

초기화 책임이 많이 몰려 있기 때문에, 운영 이슈를 파악할 때는 `Global.java`를 가장 먼저 보는 것이 맞다.

### 4.3 권한 진입 방식

권한은 컨트롤러 메서드 위의 애노테이션과 액션 클래스로 진입한다.

- `@AnonymousCheck`
- `@IsAllowed`
- `@IsCreatable`
- `@IsOnlyGitAvailable`
- `@GuestProhibit`

실제 검사 로직은 `actions/*Action.java`와 `utils/AccessControl.java`에 있다.

## 5. 도메인별 해석

### 5.1 사용자, 조직, 멤버십

핵심 모델:

- `User`
- `Organization`
- `ProjectUser`
- `OrganizationUser`
- `Role`
- `SiteAdmin`
- `UserSetting`
- `UserVerification`
- `LinkedAccount`

특징:

- 일반 사용자, 사이트 관리자, 게스트 사용자 개념이 분리되어 있다
- 프로젝트 멤버와 조직 멤버를 별도 모델로 관리한다
- 소셜 로그인, 이메일 검증, 토큰 인증, LDAP 연동을 모두 고려한다
- 즐겨찾기, 최근 방문, 메인 이메일/보조 이메일까지 사용자 모델에 함께 묶여 있다

`User.java`는 단순 엔티티가 아니라 계정 상태, 멤버십, 즐겨찾기, 방문 이력, 표시 이름 정책까지 포함한 대형 도메인 객체다.

### 5.2 프로젝트와 저장소

핵심 모델:

- `Project`
- `ProjectMenuSetting`
- `ProjectTransfer`
- `PushedBranch`
- `Webhook`

특징:

- 프로젝트가 제품의 중심 집합체다
- 프로젝트는 조직에 속할 수도 있고, 개인 소유일 수도 있다
- 프로젝트별 메뉴 사용 여부를 `ProjectMenuSetting`으로 제어한다
- 프로젝트는 포크 관계, 이전 이름/소유자 이력, 최근 푸시 브랜치, 기본 리뷰어 수까지 관리한다
- README와 `ISSUE_TEMPLATE.md`를 저장소에서 직접 읽어 온다

`Project.java`는 이 코드베이스에서 가장 중요한 모델 중 하나이며, 저장소 연동, 포크 관계, 이름 변경 이력, 라벨, 웹훅, 멤버십, 삭제 정리 로직까지 폭넓게 가진다.

### 5.3 이슈, 게시판, 댓글

핵심 모델:

- `Issue`
- `Posting`
- `Comment`
- `IssueComment`
- `PostingComment`
- `IssueEvent`
- `IssueLabel`, `IssueLabelCategory`
- `Milestone`
- `Assignee`
- `IssueSharer`

특징:

- 이슈와 게시판 포스트는 `AbstractPosting` 계층으로 묶여 있다
- 댓글, 타임라인, 라벨, 마일스톤, 담당자, 공유자 개념이 세밀하게 분리된다
- 이슈는 다른 프로젝트로 이동 가능하다
- 대량 수정, 드래프트, 하위 작업, due date, Excel export까지 지원한다

`IssueApp.java`는 가장 무거운 컨트롤러 중 하나이며, 목록/검색/엑셀/PJAX/JSON/상세/편집/이동/댓글/상태 전환까지 모두 다룬다.

### 5.4 코드 브라우징, Git, SVN

핵심 패키지:

- `app/playRepository`
- `controllers/CodeApp`
- `controllers/CodeHistoryApp`
- `controllers/GitApp`
- `controllers/SvnApp`
- `controllers/BranchApp`
- `controllers/CompareApp`

특징:

- Git과 SVN을 둘 다 1급 시민으로 다룬다
- Git은 Smart HTTP (`git-upload-pack`, `git-receive-pack`)를 직접 처리한다
- SVN은 WebDAV 요청을 `SvnApp`으로 우회해 처리한다
- 커밋 이력, diff, 브랜치, raw file, 이미지 파일, 다운로드, compare 기능이 있다

`RepositoryService.java`는 이 계층의 중심이다.

- 프로젝트의 `vcs` 값에 따라 `GitRepository` 또는 `SVNRepository` 반환
- Git advertise/RPC 처리
- push 전/후 hook 체인 연결
- DAV servlet 생성

즉, 이 제품은 단순히 GitHub API를 감싸는 앱이 아니라, 저장소 호스팅 자체를 내장한 서버에 가깝다.

### 5.5 포크, Pull Request, 코드 리뷰

핵심 모델/컨트롤러:

- `PullRequest`
- `PullRequestCommit`
- `PullRequestEvent`
- `PullRequestMergeResult`
- `ReviewComment`
- `CommentThread`, `CodeCommentThread`
- `PullRequestApp`
- `ReviewApp`
- `ReviewThreadApp`

특징:

- 포크 후 로컬 저장소 복제
- PR 생성 시 브랜치 선택, 머지 가능성 계산
- 리뷰 코멘트 스레드와 코드 범위(`CodeRange`) 관리
- 머지 시 비동기 액터 사용
- 리뷰어 수 정책과 병합 가능 여부 체크

`PullRequestApp.java`는 포크, PR 생성, 브랜치 삭제/복구, 머지, 리뷰 댓글까지 담당한다. 저장소 연산과 웹 UI 흐름이 강하게 결합된 영역이다.

### 5.6 알림, Watch, 메일, 웹훅

핵심 모델/문서:

- `NotificationEvent`
- `NotificationMail`
- `Watch`, `Unwatch`
- `UserProjectNotification`
- `Webhook`
- `docs/technical/watch.md`

특징:

- 객체 단위 Watch/Unwatch
- 프로젝트 단위 알림 타입 설정
- 이벤트 병합 로직
- 멘션, 작성자, 담당자, 댓글 작성자, 프로젝트 watch 대상자를 함께 계산
- 리소스 접근 권한이 없는 수신자는 마지막 단계에서 제거

`NotificationEvent.java`는 이벤트 생성 공장, 수신자 계산, 메시지 조합, 오래된 알림 정리 스케줄링까지 담당하는 핵심 모델이다. 이 영역 역시 모델이 매우 두껍다.

### 5.7 메일박스 연동

핵심 패키지:

- `app/mailbox`
- `MailboxService`
- `EmailHandler`
- `CreationViaEmail`

기능:

- IMAP IDLE 또는 polling으로 메일 수신
- 특정 주소 규칙으로 프로젝트/리소스 식별
- 메일을 이슈 생성 또는 댓글 추가로 변환
- 마지막 읽은 UID를 `Property`에 저장

이 기능은 협업 시스템으로서의 Yona 성격을 잘 보여 준다. 단순 웹 UI 외에 "메일 기반 입력 채널"까지 내장한다.

### 5.8 데이터 export/import와 마이그레이션

핵심 패키지:

- `app/data`
- `data/exchangers/*`
- `controllers/MigrationApp`
- `controllers/ImportApp`
- `controllers/SiteApp`

특징:

- `DataService`가 47개의 exchanger를 통해 JSON 기반 export/import 처리
- 사이트 단위 import/export와 프로젝트 마이그레이션이 분리되어 있다
- GitHub migration 설정이 `application.conf.default`에 노출되어 있다

즉, 운영 중인 인스턴스를 백업하거나 외부 시스템에서 가져오는 기능이 코드 구조에 깊게 자리 잡고 있다.

## 6. 디렉토리별 중요도와 무게 중심

`app` 하위 파일 수를 보면 무게 중심이 명확하다.

| 디렉토리 | 파일 수 | 해석 |
| --- | ---: | --- |
| `views` | 242 | 서버 렌더링 UI가 매우 큼 |
| `models` | 106 | 도메인과 비즈니스 로직이 모델에 많이 응집 |
| `utils` | 52 | 횡단 관심사와 레거시 보조 코드 비중 큼 |
| `controllers` | 51 | 기능별 엔드포인트가 넓게 분산 |
| `data` | 50 | 데이터 교환/마이그레이션 계층이 독립적으로 큼 |
| `playRepository` | 23 | Git/SVN 저장소 추상화의 존재감이 큼 |

이 숫자만 봐도, 이 프로젝트는 "템플릿이 많은 웹 제품"이면서 동시에 "모델 로직이 강한 도메인 시스템"이다.

## 7. 라우팅 기준으로 본 기능 중심점

라우팅에서 호출 수가 많은 컨트롤러는 다음과 같다.

| 컨트롤러 | 라우트 수 |
| --- | ---: |
| `ProjectApp` | 30 |
| `UserApp` | 26 |
| `IssueApi` | 21 |
| `PullRequestApp` | 21 |
| `SiteApp` | 21 |
| `IssueApp` | 18 |
| `OrganizationApp` | 15 |
| `Application` | 14 |

즉, 이 제품의 운영 중심은 명확히 다음 영역이다.

- 프로젝트
- 사용자/계정
- 이슈
- Pull Request
- 사이트 관리자

## 8. 프론트엔드 구조 해석

### 8.1 서버 템플릿

`app/views`는 242개 파일로 상당히 크다. 하위 모듈 분포는 아래와 같다.

| 뷰 모듈 | 파일 수 |
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

공통 템플릿과 이슈/프로젝트 화면의 비중이 특히 크다.

### 8.2 정적 자산

`public`은 총 723개 파일이다.

- JavaScript 430개
- PNG 134개
- CSS 94개
- JPG 24개

`public/javascripts`만 보아도 총 526개 파일이며, 구조는 다음과 같다.

| 하위 디렉토리 | 파일 수 | 의미 |
| --- | ---: | --- |
| `lib` | 446 | 벤더 라이브러리 집합 |
| `service` | 44 | 기능별 페이지 스크립트 |
| `common` | 32 | 재사용 UI/동작 모듈 |
| `template` | 1 | 코드 템플릿 자산 |

### 8.3 프론트엔드 스타일

코드 스타일은 전형적인 레거시 jQuery/Bootstrap 계열이다.

- `bootstrap-responsive.css` 존재
- `jquery.pjax`, `jquery.tmpl`, `jquery.validate` 사용
- `yobi.*`, `yona.*` 네임스페이스로 기능 분리
- `minify-js.sh`에서 Closure Compiler로 수동 번들링

현대적인 컴포넌트 프레임워크는 아니지만, 기능 단위 JS 모듈이 꾸준히 분리되어 있다.

## 9. 설정과 운영 구조

### 핵심 설정 파일

- `conf/application.conf.default`
- `conf/social-login.conf.default`
- `conf/application-logger.xml.default`
- `conf/play.plugins`
- `conf/routes`

### `application.conf.default`에서 보이는 운영 관심사

- 사이트 이름, 컨텍스트 경로, 익명 접근 제어
- MariaDB 기본 DB 설정, H2 대안 설정
- SMTP 메일 발송
- IMAP mailbox 연동
- 업데이트 체크 URL
- GitHub migration
- 소셜 로그인
- LDAP 로그인
- Google Analytics 관련 옵션

또한 `conf/evolutions/default`에는 `1.sql`부터 `32.sql`까지의 DB 마이그레이션이 존재한다.

### 배포 방식

코드와 README, 스크립트 기준으로 보면 다음 운영 모델을 상정한다.

- `YONA_DATA` 기반 외부 데이터 디렉토리
- zip 배포본 실행
- MariaDB 권장, H2 임베디드 대안
- Linux/macOS 계열 쉘 스크립트 중심 운영
- Windows 배치 스크립트도 `build.sbt`에 포함

`build.sbt`의 native packager 설정은 `YONA_HOME`, `YONA_DATA`, `config.file`, `logger.file`를 주입하도록 구성되어 있다.

## 10. 테스트와 문서화 상태

### 테스트

`test`에는 총 77개 파일이 있다.

| 영역 | 파일 수 |
| --- | ---: |
| `test/models` | 29 |
| `test/utils` | 14 |
| `test/controllers` | 13 |
| `test/controllers/api` | 5 |
| `test/playRepository` | 4 |
| `test/mailbox` | 3 |

해석:

- 모델/유틸 테스트 비중이 높다
- 컨트롤러 테스트도 어느 정도 있다
- 저장소 계층과 메일박스 계층도 최소한의 검증이 있다
- 프론트엔드 자동화 테스트는 보이지 않는다

### 문서

`docs`에는 총 103개 파일이 있으며 다음 계층으로 나뉜다.

- 설치/운영 문서
- 릴리즈 노트
- 기술 문서
- 사용자 매뉴얼
- 한국어 문서와 영어 문서 병행

특히 다음 문서들이 코드 읽기에 직접 도움 된다.

- `docs/technical/watch.md`
- `docs/ko/technical/access-control.md`
- `docs/ko/technical/view-hierarchy.md`

즉, 코드 외부 설명 자산은 꽤 풍부한 편이다.

## 11. 유지보수 관점에서의 핵심 포인트

### 장점

- 제품 기능 범위가 넓고 도메인 모델이 풍부하다
- Git/SVN을 함께 다루는 저장소 호스팅 기능이 내장되어 있다
- 권한, Watch, 알림, 메일, 마이그레이션처럼 운영에 필요한 기능이 잘 갖춰져 있다
- 설치/운영/기술 문서가 비교적 충실하다

### 주의점

- Play 2.3, sbt 0.13, Java 8 전제 등 스택이 매우 오래되었다
- 컨트롤러와 모델이 커서 변경 영향 범위를 읽기 어렵다
- 서비스 계층이 얇아 테스트 대역과 책임 분리가 현대적이지 않다
- 정적 메서드와 전역 상태 사용이 많아 리팩터링 난도가 높다
- Yobi/Yona 명칭이 혼재해 신규 진입자의 인지 부하가 있다

### 가장 먼저 숙지해야 할 파일

신규 참여자가 먼저 읽어야 할 우선순위는 아래가 적절하다.

1. `conf/routes`
2. `app/Global.java`
3. `app/utils/AccessControl.java`
4. `app/models/Project.java`
5. `app/models/User.java`
6. `app/playRepository/RepositoryService.java`
7. `app/controllers/ProjectApp.java`
8. `app/controllers/IssueApp.java`
9. `app/controllers/PullRequestApp.java`
10. `app/models/NotificationEvent.java`

## 12. 종합 평가

이 저장소는 "현대적인 경량 웹앱"이 아니라 "오랜 기간 기능을 축적한 협업 플랫폼"이다. 코드 스타일은 분명 레거시하고, 컨트롤러/모델이 비대하지만, 반대로 제품 기능은 꽤 깊고 실무적인 요구를 넓게 흡수하고 있다.

가장 중요한 이해 포인트는 세 가지다.

- 이 앱은 저장소 브라우저가 아니라 저장소 호스팅 서버다
- 이 앱은 단순 이슈 시스템이 아니라 조직/알림/메일/마이그레이션까지 포함한 협업 허브다
- 구조는 계층형 서비스보다 도메인 모델과 컨트롤러에 책임이 많이 실린 전통적 Play 애플리케이션이다

이 관점으로 보면, 이후 세부 기능을 읽을 때 어디서 책임을 찾아야 하는지 훨씬 빨라진다.

# Non-Destructive Verification (2026-04-11)

## 범위

- 실DB에 쓰기 작업을 하지 않는 확인만 수행했다.
- 사용자 생성, 프로젝트 생성/삭제, 게시판/이슈/댓글 생성, 웹훅 발송 같은 mutation 테스트는 제외했다.
- 현재 코드베이스 기준 빌드/실행 가능성과 removed-feature 노출 여부를 점검했다.

## 이번에 확인한 것

### 1. build/run 설정 전달 수정

- [build.sbt](C:/Workspaces/yona/build.sbt)에서 `run` JVM 옵션 scope를 `javaOptions in (Compile, run)`으로 수정했다.
- 함께 `fork in (Compile, run) := true`로 맞췄다.
- 이 수정으로 `YONA_DATA`, `YONA_PORT`, `YONA_CONFIG_FILE`, `YONA_LOGGER_FILE`이 실제 `run` JVM에 전달되는 것을 확인했다.

확인 명령:

```powershell
.\support-script\build-yona.cmd "show compile:run::javaOptions"
```

확인 결과:

- `-Dyona.data=...`
- `-Dhttp.port=...`
- `-Dconfig.file=...`
- `-Dlogger.file=...`

### 2. 빌드 체인 재검증

아래 명령을 순차 실행해서 모두 통과했다.

```powershell
cmd /c support-script\build-yona.cmd clean compile
cmd /c support-script\build-yona.cmd test:compile
cmd /c support-script\build-yona.cmd dist
```

산출물:

- [yona-1.16.0.zip](C:/Workspaces/yona/target/universal/yona-1.16.0.zip)

주의:

- 현재 sbt 0.13 / Play 2.3 계열은 `compile`, `test:compile`, `dist`를 병렬로 돌리면 classfile manager 문제가 발생한다.
- 검증은 계속 순차 실행 기준으로 유지해야 한다.

### 3. route/controller 노출 확인

`conf/routes`와 `app/controllers` 기준으로 아래 제거 대상 진입점은 더 이상 남아 있지 않다.

- `GitApp`
- `SvnApp`
- `PullRequestApp`
- `ReviewApp`
- `ReviewThreadApp`
- `MigrationApp`
- `ImportApp`
- site export/import route

즉, 현재는 제거 대상 기능의 주 라우트 진입점이 코드베이스에서 빠진 상태다.

### 4. 읽기 전용 HTTP 확인

현재 설정 파일 [application.conf](C:/Workspaces/yona/conf/application.conf) 기준으로 `run` 인스턴스를 읽기 전용으로 띄운 뒤 아래 GET 요청만 확인했다.

- `GET /` -> `200`
- `GET /users/loginform` -> `200`
- `GET /users/signupform` -> `200`
- `GET /projects` -> `200`

추가 관찰:

- 공개 프로젝트 목록은 빈 상태였고, 페이지에 `Project is non existent` empty state가 노출됐다.
- 즉, 읽기 전용 기준으로는 인스턴스가 정상 응답하고 공개 페이지는 깨지지 않는다.

## 남아 있는 노출/정리 포인트

### 1. 랜딩 페이지 문구는 아직 removed-feature를 홍보한다

파일:

- [partial_intro.scala.html](C:/Workspaces/yona/app/views/index/partial_intro.scala.html)

현재 남아 있는 항목:

- `Code management`
- `Private repositories`
- `Code review`

이 항목들은 이미 제거한 제품 범위와 맞지 않으므로 다음 정리 대상이다.

### 2. dead view 디렉토리가 아직 남아 있다

현재 디렉토리:

- [app/views/code](C:/Workspaces/yona/app/views/code)
- [app/views/git](C:/Workspaces/yona/app/views/git)
- [app/views/migration](C:/Workspaces/yona/app/views/migration)
- [app/views/reviewthread](C:/Workspaces/yona/app/views/reviewthread)

라우트와 컨트롤러 기준 진입점은 제거됐지만, 뷰 디렉토리 자체는 아직 남아 있다.
즉 “사용자 노출”은 대부분 막혔지만 “코드베이스 완전 제거” 관점에서는 추가 정리가 필요하다.

### 3. help/문구 계층도 추가 점검이 필요하다

파일:

- [toc.scala.html](C:/Workspaces/yona/app/views/help/toc.scala.html)
- [messages](C:/Workspaces/yona/conf/messages)
- [messages.ko-KR](C:/Workspaces/yona/conf/messages.ko-KR)

도움말/메시지 계층에는 저장소, 코드, Git 관련 레거시 설명이 남아 있을 가능성이 높다.

## 사용자 직접 확인 예정 항목

실DB를 쓰는 만큼 아래는 사용자가 직접 수행하고 결과를 공유하는 방식으로 남긴다.

1. 회원가입 / 로그인
2. 프로젝트 생성 / 설정 / 삭제
3. 게시판 글 / 댓글
4. 이슈 생성 / 댓글 / 상태 변경 / 첨부
5. 알림 생성 / 조회
6. 웹훅 생성 및 실제 발송

## 다음 권장 작업

1. [partial_intro.scala.html](C:/Workspaces/yona/app/views/index/partial_intro.scala.html)에서 removed-feature 홍보 문구를 surviving-core 기준으로 교체
2. dead view 디렉토리(`code`, `git`, `migration`, `reviewthread`)와 관련 message/help 잔재 정리
3. 사용자가 공유할 실DB 수동 테스트 결과를 받아 남은 런타임 blocker를 확정

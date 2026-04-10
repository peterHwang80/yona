# Yona Build Environment

## 목적

이 문서는 Yona 소스를 우리 팀 환경에서 다시 빌드하기 위해 필요한 최소 조건과 실행 절차를 정리한다.  
이번 기준은 "빌드가 재현된다"이며, Play/Twirl/Ebean/Akka 자체를 업그레이드하는 것은 포함하지 않는다.

## 현재 기준 환경

### 사실

- JDK 8 계열이 필요하다.
- 소스는 Play 2.3.10, sbt 0.13.5, Typesafe Activator 1.2.12 조합을 전제로 한다.
- 저장소에는 `activator` 실행 파일이 포함되어 있지 않으므로, 빌드 스크립트가 Activator minimal 배포본을 자동 다운로드한다.
- 레거시 resolver 중 일부는 현재 그대로는 접근이 불안정하므로, `support-script/sbt-repositories`로 부트/플러그인 저장소를 고정한다.

### 판단

- Yona 빌드는 "소스만 있으면 바로 되는" 구조가 아니다.
- 팀 공용 빌드 명령은 직접 `activator`를 다루기보다 저장소에 포함된 스크립트로 통일하는 편이 안전하다.

## 팀 표준 명령

### Windows PowerShell

```powershell
.\support-script\build-yona.ps1 compile
.\support-script\build-yona.ps1 test:compile
.\support-script\build-yona.ps1 dist
```

### Windows CMD

```bat
support-script\build-yona.cmd compile
support-script\build-yona.cmd dist
```

## 개발 실행

개발 중에는 `dist` 산출물을 다시 풀어 실행하기보다 `run` 태스크로 소스를 직접 띄우는 편이 낫다.

### 개발용 데이터 디렉토리 지정

스크립트는 아래 환경 변수를 읽는다.

- `YONA_DATA`: 외부 데이터 디렉토리
- `YONA_PORT`: 개발 서버 포트
- `YONA_CONFIG_FILE`: 사용할 `application.conf` 경로
- `YONA_LOGGER_FILE`: 사용할 `application-logger.xml` 경로

가장 일반적인 사용 예시는 아래와 같다.

```powershell
$env:YONA_DATA = "C:\Workspaces\yona-dev-data"
$env:YONA_PORT = "9000"
.\support-script\build-yona.ps1 run
```

`YONA_DATA\conf\application.conf`와 `YONA_DATA\conf\application-logger.xml`가 존재하면 스크립트가 자동으로 읽는다.

## 결과물 위치

- `compile`: 클래스 산출물은 `target/` 아래에 생성된다.
- `dist`: 배포 zip은 `target/universal/` 아래에 생성된다.
- Activator 다운로드/부트 캐시는 `.build/` 아래에 생성된다.

## 스크립트가 고정하는 값

- Activator: `1.2.12`
- sbt: `0.13.5`
- sbt launcher scala: `2.10.4`
- repository config: `support-script/sbt-repositories`

## 알려진 주의사항

- Java 8이 아니면 빌드는 예상치 못한 오류를 낼 수 있다.
- 최초 실행은 레거시 의존성 다운로드 때문에 오래 걸릴 수 있다.
- 저장소 resolver를 개별 개발자 PC에서 임의로 바꾸기보다, 먼저 `support-script/sbt-repositories`를 기준으로 팀 공통 구성을 유지하는 편이 좋다.

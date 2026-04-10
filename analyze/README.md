# Analyze Index

## 추천 읽기 순서

1. `system-map.md`
2. `workflow-map.md`
3. `domain-map.md`
4. `customization-seams.md`
5. `risk-register.md`
6. `company-fit-gap.md`

## 문서 설명

| 문서 | 목적 |
| --- | --- |
| `system-map.md` | 전체 구조, 런타임, 초기화, 읽기 순서 정리 |
| `workflow-map.md` | 업무 흐름별 route-controller-model-view 연결표 |
| `domain-map.md` | 핵심 모델 관계와 책임 정리 |
| `customization-seams.md` | 어디를 먼저/나중에 바꿔야 하는지 정리 |
| `risk-register.md` | 레거시/빌드/운영 리스크 기록 |
| `company-fit-gap.md` | 일반적인 사내 협업 요구와 원본 Yona의 차이표 |
| `build-environment.md` | 빌드 전제, 표준 명령, 결과물 위치 정리 |
| `codebase-overview.md` | 저장소 전체 개요와 기술 스택 해설 |
| `directory-structure.md` | 디렉토리 구조와 파일 분포 정리 |

## 사용 방법

- 새로운 요구사항이 들어오면 먼저 `workflow-map.md`에서 해당 흐름을 찾는다.
- 수정 지점을 판단할 때는 `customization-seams.md`의 `Safe / Medium / High` 등급을 먼저 확인한다.
- 빌드/운영 이슈가 생기면 `risk-register.md`와 `system-map.md`를 함께 본다.

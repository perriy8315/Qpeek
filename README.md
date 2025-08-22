# Qpeek

> 개인 작업을 **저장소(Database) → 큐(Queue) → 작업(Task)** 단위로 관리하고, 마감 알림과 데일리 클로징을 지원하는 프로젝트.

![Gradle](https://img.shields.io/badge/Gradle-8.14.3-02303A?logo=gradle)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-6DB33F?logo=springboot)
![Java](https://img.shields.io/badge/Java-24-007396?logo=openjdk)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%2B-336791?logo=postgresql)

<br>

## 목차
- [Qpeek](#qpeek)
  - [목차](#목차)
  - [개요](#개요)
  - [폴더 구조](#폴더-구조)
  - [Git Rule](#git-rule)
    - [브랜치 모델 \& 보호 규칙](#브랜치-모델--보호-규칙)
    - [커밋 메시지 규칙](#커밋-메시지-규칙)
    - [PR 규칙](#pr-규칙)


<br>

## 개요
- **핵심 기능**
  - 저장소(Repository)별 **작업 큐** 운영
  - 마감 임박/초과 **알림**
  - 작업 **완료 로그(CompletionLog)**
  - **데일리 리포트**



<br>


## 폴더 구조
```
qpeek/
├─ common/
│  ├─ config/                     # 전역 설정
│  ├─ entity/                     # (가능하면 최소화; JPA BaseEntity면 도메인 의존 유의)
│  ├─ enums/                      # 여러 컨텍스트에서 진짜 공용인 enum만
│  └─ persistence/                # <권장: infrastructure/common 으로 이동> converter, seq 등
│
├─ domain/
│  ├─ member/
│  │  ├─ entity/                  # Member 등 Aggregate, @Embeddable VO는 value/ 로
│  │  ├─ value/                   # LoginId, PasswordHash
│  │  └─ service/                 # 순수 도메인 서비스(있다면)
│  ├─ notification/...
│  └─ report/...
│
├─ application/
│  ├─ member/
│  │  ├─ service/                 # 유스케이스 구현(@Transactional 경계)
│  │  ├─ repository/              # 인터페이스: MemberRepository(도메인 반영)
│  │  ├─ client/                  # 외부 시스템 인터페이스(SmsClient 등)
│  │  └─ dto/                     # Command/Query/Result
│  ├─ notification/...
│  └─ report/...
│
├─ presentation/
│  └─ web/
│     ├─ member/                  # Controller + request/response mapper
│     ├─ notification/
│     └─ report/
│
├─ infrastructure/
│  ├─ persistence/
│  │  ├─ member/                  # JPA 구현체, Spring Data, 매퍼(JPA↔Domain)
│  │  └─ common/                  # ZoneIdAttributeConverter, GlobalSeqGenerator 등
│  ├─ external/                   # 외부 API 클라이언트 구현
│  ├─ messaging/                  # Kafka/SQS 등
│  ├─ config/                     # infra 관련 빈/설정
│  └─ scripts/                    # 배포/운영 스크립트(예: 마이그레이션, 헬스체크)
│
└─ docs/                          # 문서 루트(개발/운영/아키텍처 문서의 단일 진실 원천)
   ├─ README.md                   # 문서 인덱스/목차(링크)
   ├─ architecture/...            # 아키텍처 문서(C4/배포/경계)
   ├─ domain/...                  # 도메인 정보
   ├─ api/...                     # 외부 API 문서
   ├─ data/...                    # 데이터 모델/정책
   ├─ guides/...                  # 팀 운영/개발 가이드
   ├─ operations/...              # 운영 문서(릴리스/모니터링)
   └─ templates/...               # 템플릿 모음
```

<br>


## Git Rule

### 브랜치 모델 & 보호 규칙

> 메인 원칙
- `main`은 **항상 배포 가능** 상태로 유지한다.
- 작업은 **짧은 기능 브랜치**에서 진행하고, PR을 통해 `main`에 **Squash merge** 한다.

<br>

> 브랜치 보호(필수)
- `main`:
    - **force-push 금지**
    - **required checks**(test / lint / build / security scan) 통과 없으면 머지 금지
- 머지 방식: **Squash only**

<br>

> 운영 흐름
- 기능 개발: `feature/*` → PR → CI green → 리뷰 → **squash** → `main`
- 릴리스: 안정된 커밋에 **`V (MAJOR).(MINOR).(PATCH)`** 태그를 달아 배포
- 핫픽스: `hotfix/*`를 `main`에서 분기 → 빠른 PR → 머지 후 **태그**


<br>

> 브랜치 이름 규칙
- **형식**: `<type>/<scope>-<short-desc>`
    - **type**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf` *(+ 필요 시 `build`, `ci`, `revert`)*
    - **scope**: 도메인/모듈 이름 (`member`, `notification`, `report` …)
    - **short-desc**: 3~4 단어, **kebab-case**, 영어 권장

<br>

> 예시
- `feat/member-entity` *(Member 엔티티 + VO + 단위테스트)*
- `feat/notification-entity`
- `feat/member-repository` *(repo 인터페이스/구현 + 테스트)*
- `refactor/member-nickname-validation`
- `fix/member-timezone-null-bug`
- `hotfix/production-npe-20250822` *(긴급 수정은 `main`에서 분기)*

---

<br>

### 커밋 메시지 규칙

- **형식**: `type(scope): subject`
- **type**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf` (옵션: `build`, `ci`, `revert`)
- **scope**: 도메인/모듈 이름 (member, notification, report, infra, ci …)
- **subject**: 명령형 현재형, 50자 이내 권장

<br>

> 예시

- `feat(member): add Member entity and LoginId/PasswordHash`
- `fix(member): reject whitespace-only nickname`
- `refactor(member): extract nickname validation to helper`
- `test(member): add MemberTest for status transitions`
- `docs(api): add error code table`
- `chore(build): enable jacoco and test reports`
- `ci(pipeline): run e2e only on tags`

---

<br>

### PR 규칙

> 원칙
- **작고 자주**: 변경 라인 **≈ 300 이하** 권장(크면 쪼개기)
- **제목 형식**: 커밋과 동일한 **Conventional Commits** 헤더 사용  
  예) `feat(member): add Member aggregate and tests`

<br>

> 본문 템플릿(권장)
- **What**: 주요 변경 요약(기능/범위)
- **Why**: 배경/문제/의도
- **Test**: 추가/수정된 테스트, 로컬/CI 결과
- **Links**: 관련 이슈(예: `Closes #123`)
- **Screenshot/Log**: UI/동작 증빙(해당 시)


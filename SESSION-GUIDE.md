# SESSION-GUIDE — 새 세션에서 이어 작업하기

> **새 세션(사람이든 새 Claude 세션이든)에서 이 프로젝트를 이어 작업할 때 가장 먼저 읽는 진입 문서입니다.**
> 상태·결정의 단일 출처는 `HANDOVER.md`이고, 이 문서는 **시작 절차 + 현재 스냅샷 + 운영 함정 + 빠른 명령**만 담습니다(중복 대신 가리킵니다).

## 0. 읽는 순서
1. **이 문서** — 어디서 시작하고 무엇을 조심할지
2. [`HANDOVER.md`](./HANDOVER.md) — 전체 그림·경과·설계 결정·**남은 일(9장)**
3. [`CLAUDE.md`](./CLAUDE.md) — 작업 규칙(빌드 명령·반드시 지킬 규칙·구조)
4. [`MAINTAINER-GUIDE.md`](./MAINTAINER-GUIDE.md) — 수정·확장·배포 상세 / [`USER-GUIDE.md`](./USER-GUIDE.md) — 사용법

## 1. 세션 시작 체크리스트
- [ ] **작업 위치**: `C:\Users\ADMIN\Desktop\pjt\egov-codegen-studio` 이 폴더가 활성 작업 대상이다.
      `pjt\egov-crud-gen`은 **백업(CLI 전용 원본) — 건드리지 않는다.**
- [ ] **원격**: https://github.com/Ohmneong/egov-codegen-studio (`main`).
- [ ] **최근 상태 파악**: `git log --oneline -8`, 미커밋 확인 `git status`
- [ ] **빌드 되는지 확인**:
      ```powershell
      powershell -ExecutionPolicy Bypass -File .\build.ps1   # → dist\egov-codegen-studio.jar
      ```
- [ ] 다음 작업은 `feature/<요약>` 브랜치에서 시작(‘작업 루프’ 참고)

## 2. 현재 상태 스냅샷
> 수치는 작성 시점 기준. **최신은 항상 `git log`로 확인**하라.

| 항목 | 값 |
|---|---|
| 패키지 | `dev.myoh.egovgen` |
| 산출물 jar | `dist\egov-codegen-studio.jar` |
| 설치본 vendor | `myoh` / 아이콘 | `icon.ico`(루트, 교체하면 재패키징 시 자동 적용) |
| 실행 경로 | CLI(`run.ps1`) · GUI(`run-gui.ps1`) · 배포본(`package.ps1`) |

**완료된 기능**: CRUD 풀세트 + 채번(`--idgnr`) · 여러 테이블 일괄 생성(DDL 다중 `CREATE TABLE`) · GUI(설정 프로파일·미리보기·덮어쓰기 경고·출력폴더 탐색기) · 감사컬럼 자동 처리(eGov 표준명 + `created_by/at`·`updated_by/at`) · 검색 조건 PK 기준 · FK·Y/N 컬럼 드롭다운 · `mapperRoot`/`jspRoot` 경로 변수화 · 배포본(app-image / `.exe` 인스톨러).

## 3. 다음 작업 후보
> 상세·우선순위는 **HANDOVER 9장**. 핵심만:
- 재생성(round-trip) diff — 손수정 보존(도구 수명 직결)
- 다른 DB 파서(Oracle 등) · 화면 플랫폼(eXBuilder/WebSquare)
- FK 드롭다운의 **옵션 데이터 자동 연동**(현재는 골격만)
- 등록자/수정자 ID 로그인(LoginVO) 연동
- AI 보조(코멘트→라벨/검색조건) · 코드 서명(SmartScreen 근본 해결)

## 4. 작업 루프
```
수정 → build.ps1 → sample/*.sql 로 생성해 눈으로 확인
     → (가능하면) 생성 자바 컴파일 0 + 톰캣 1회
     → 문서 동기화(USER-GUIDE/gen.properties 등)
     → feature/<요약> 커밋 → main 병합 → git push
```
회귀 체크리스트는 MAINTAINER-GUIDE 4장.

## 5. 운영 함정 (실전 기록 — 새 세션이 똑같이 헤매지 않게)
- ⚠ **GUI/인스톨러가 실행 중이면 jar·exe가 잠겨 빌드·패키징이 실패한다.** 빌드/패키징 전에 관련 프로세스를 종료하라:
  ```powershell
  Get-CimInstance Win32_Process -Filter "Name='javaw.exe'" | Where-Object { $_.CommandLine -like '*egov-codegen-studio.jar*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
  ```
  설치된 앱/인스톨러(`egov-codegen-studio*`) 프로세스도 마찬가지.
- ⚠ **패키지명·jar명 등 대량 변경 후엔 `build/`·`dist/`에 옛 산출물이 섞일 수 있다.** 깨끗이:
  `Remove-Item build,dist -Recurse -Force` 후 재빌드. (jar 안 잔재는 `jar tf dist\*.jar`로 확인)
- ⚠ **콘솔 한글이 깨져 보이는 건 표시 인코딩 문제일 뿐**이다. 결과 검증은 콘솔 글자가 아니라 **생성된 파일 내용 / `jar tf` / 종료코드**로 한다.
- **배포본**: `package.ps1`(app-image, JRE 내장 폴더) / `package.ps1 -Type exe`(WiX 3.x 필요). 루트에 `icon.ico` 있으면 아이콘 자동 적용.
- ⚠ **인스톨러를 더블클릭해도 "안 뜨는" 건 보통 같은 `app-version`(1.0.0)이 이미 설치돼 있어서다**(WiX/MSI 표준 — 마법사 생략). 실행은 인스톨러가 아니라 **설치된 앱 아이콘**으로. 새 빌드로 재설치하려면 기존 앱 제거 후 설치하거나 `package.ps1`의 `--app-version`을 올려라.
- 미서명 exe는 받는 PC에서 **SmartScreen** 경고 → "추가 정보 → 실행", 또는 USB로 직접 복사(MOTW 미부착).

## 6. 빠른 명령
```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1                       # 빌드
.\run.ps1 --ddl sample\sample.sql --config gen.properties                  # CLI 실행
.\run.ps1 --ddl sample\verify.sql --config gen.properties --idgnr          # CLI 채번
.\run-gui.ps1                                                              # GUI
.\package.ps1            # 배포본(app-image, JRE 내장 폴더)
.\package.ps1 -Type exe  # .exe 인스톨러(WiX 필요)
```
> 멀티테이블은 `sample\multi.sql`(여러 `CREATE TABLE` + FK)로 확인할 수 있다.

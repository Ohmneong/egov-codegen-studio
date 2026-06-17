# egov-codegen-studio

eGov 표준프레임워크(5.0.1) CRUD 풀세트 코드 생성기. **외부 의존성 0, 순수 Java(텍스트 블록 템플릿)** 로 작성되어 번들 JDK만으로 빌드·실행된다. 폐쇄망에 그대로 복사해 쓸 수 있다. **CLI와 Swing GUI** 두 방식으로 쓸 수 있고, JRE를 내장한 **설치본(.exe)** 까지 만들 수 있다.

- 원격: https://github.com/Ohmneong/egov-codegen-studio (public). CLI 전용 `egov-crud-gen`에서 분기한 GUI 확장판.

## 📖 문서 안내
- **[HANDOVER.md](./HANDOVER.md)** — 프로젝트를 이어받는 분용. 전체 그림·작업 경과·설계 결정·남은 일 (먼저 읽기 권장)
- **[QUICKSTART.md](./QUICKSTART.md)** — 처음 쓰는 분용. 복붙 따라하기 (DDL 작성 → 실행 → 결과)
- **[USER-GUIDE.md](./USER-GUIDE.md)** — 사용자 매뉴얼. 옵션·설정·경로 규칙·트러블슈팅
- **[MAINTAINER-GUIDE.md](./MAINTAINER-GUIDE.md)** — 관리자용. 도구 수정·확장·배포·버전관리

## 빌드

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1
# → dist\egov-crud-gen.jar 생성
```

빌드 스크립트는 eGovFrameDev 번들 JDK(justj openjdk 21)의 `javac`/`jar`를 자동 탐색한다. 다른 JDK를 쓰려면 `build.ps1`의 `$jdkRoot`를 수정.

## 실행

```powershell
java -jar dist\egov-crud-gen.jar --ddl sample\sample.sql --config gen.properties
```

| 옵션 | 설명 |
|---|---|
| `--ddl <파일>` | (필수) MySQL `CREATE TABLE` DDL 파일 |
| `--config <파일>` | 프로젝트 설정 properties |
| `--package <pkg>` | 루트 패키지 (config 덮어쓰기) |
| `--module <경로>` | 모듈 경로 (예: `sym/cal`) |
| `--prefix <prefix>` | 엔티티명 도출 시 제거할 테이블 prefix |
| `--out <디렉터리>` | 출력 루트 (기본 `./output`) |
| `--mapperRoot <경로>` | Mapper XML 출력/스캔 루트 (기본 `egovframework/mapper`) |
| `--jspRoot <경로>` | JSP 출력 루트 (기본 `WEB-INF/jsp`) |
| `--idgnr` | 채번 적용 (String PK일 때만 동작) |

### GUI로 실행 (Swing)

CLI 대신 화면에서 입력·생성할 수도 있다. 같은 생성 엔진(`GenerationService`)을 호출하므로 결과는 동일하다.

```powershell
powershell -ExecutionPolicy Bypass -File run-gui.ps1
```

DDL을 붙여넣거나 파일로 열고, 설정 폼(시작 시 `gen.properties`로 채워짐)을 조정한 뒤 `[생성]`을 누르면 파일 목록과 접속 URL이 표시된다. 출력 폴더는 `[찾아보기…]`로 탐색기에서 고를 수 있다(대상 eGov 프로젝트 루트를 고르면 `src/main/...`에 바로 병합). (Swing은 JDK 내장 — 추가 의존성 없음)

GUI 부가 기능:
- **설정 프로파일** — 상단 프로파일 바에서 `[현재 설정 저장]`으로 프로젝트별 설정을 `profiles/`에 저장하고, 콤보박스 + `[불러오기]`로 전환한다.
- **미리보기** — `[미리보기]`는 생성하지 않고 만들어질 파일 목록과 기존 덮어쓸 파일을 보여준다. `[생성]` 시 기존 파일이 있으면 덮어쓰기 확인을 묻는다.

### 설치본/인스톨러 만들기

`jpackage`(JDK 14+ 필요)로 두 가지 방식의 배포본을 만들 수 있다.

**(A) app-image — JRE 내장 실행 폴더** (WiX 불필요, 가장 단순):
```powershell
powershell -ExecutionPolicy Bypass -File package.ps1
# → package\egov-codegen-studio\egov-codegen-studio.exe  (내장 JRE, 약 150MB)
```
`egov-codegen-studio` 폴더를 통째로 복사하면 어디서든(폐쇄망 PC 포함) Java 설치 없이 실행된다.

**(B) .exe 인스톨러** — 설치 마법사·시작메뉴/바탕화면 바로가기·제거 등록. WiX 3.x 필요:
```powershell
choco install wixtoolset -y          # 최초 1회 (관리자 권한)
powershell -ExecutionPolicy Bypass -File package.ps1 -Type exe
# → package\egov-codegen-studio-1.0.0.exe  (사용자 단위 설치, 관리자 권한 불필요, 약 55MB)
```

## 산출물 (테이블 1개 → 11파일)

```
output/src/main/java/{package}/service/        {Entity}.java, {Entity}VO.java, {Entity}ManageService.java
output/src/main/java/{package}/service/impl/    {Entity}ManageServiceImpl.java, {Entity}ManageDAO.java
output/src/main/java/{package}/web/             {Entity}ManageController.java
output/src/main/resources/egovframework/mapper/{module}/  {Entity}Manage_SQL_mysql.xml
output/src/main/webapp/WEB-INF/jsp/{module}/    {Entity}{List,Detail,Regist,Modify}.jsp
```

> DDL 하나에 `CREATE TABLE`이 여러 개면 **테이블마다 위 한 세트씩** 생성된다.
> 등록/수정 폼에서 여부 컬럼(`CHAR(1)` + 이름 `_AT`/`_YN`)은 **Y/N 드롭다운**, FK 컬럼(`REFERENCES`)은 **드롭다운**으로 만들어진다.

## 적응형 설정 (gen.properties)

프로젝트가 바뀌면 `gen.properties`만 교체한다. 핵심 항목: `basePackage`, `module`, `tablePrefix`, `dbType`, **Mapper/JSP 스캔·출력 루트(`mapperRoot`/`jspRoot`)**, 공통 컴포넌트 베이스 경로(`daoBase`/`serviceBase`/`paginationInfo`).

## 구조

```
src/dev/myoh/egovgen/
  Main.java                 CLI 진입점 (얇은 어댑터)
  config/GenConfig.java     적응형 설정 로드 + 폼 주입 setter
  model/                    TableMeta, ColumnMeta (중간 메타모델)
  parser/                   DdlParser(인터페이스), MySqlDdlParser
  gen/                      NameUtil, TypeMapper, CodeGenerator(템플릿)
  service/                  GenerationService, GenerationResult (CLI/GUI 공유 생성 엔진)
  ui/                       GenGuiApp (Swing GUI 진입점)
```

## 1차(MVP) 범위와 한계 — 정직하게

**됨**: MySQL DDL 파싱(여러 줄 컬럼 정의 + 한 DDL에 여러 `CREATE TABLE` 일괄 생성) → 테이블마다 백엔드 6 + Mapper XML + JSP 4 생성, 논리삭제(USE_AT) 자동 분기, 코멘트→한글 라벨, 감사 컬럼(eGov 표준명 + 관례명 `created_by/at`·`updated_by/at`) 폼 입력 제외·시점 `SYSDATE()` 자동, 검색조건 PK 기준, 여부(`_AT`/`_YN`)·FK(`REFERENCES`) 컬럼은 폼 드롭다운, 생성물 클래스/파일명 `Egov` 접두어 없음, 적응형 설정(`mapperRoot`/`jspRoot` 포함). **CLI·GUI 두 방식 + JRE 내장 설치본(.exe).** GUI는 설정 프로파일(`profiles/`)·생성 미리보기·덮어쓰기 경고 제공.

**아직 안 됨 (2차/추후)**:
- **생성 자바 코드의 컴파일은 이 도구만으로 검증 불가** — eGov 의존성(spring/jakarta/commons-lang3/rte) 클래스패스가 필요. 실제 eGov 프로젝트(예: bp.enter)에 산출물을 넣고 컴파일해야 최종 확인된다. **이게 1차 MVP의 핵심 검증 단계다.**
- 재생성(round-trip) diff 전략 — 현재는 매번 덮어쓰기.
- 엑셀/CSV 일괄 입력 — POI 의존성 회피 위해 2차로 미룸.
- MySQL 외 DB 파서 — `DdlParser` 인터페이스만 열려 있음.
- AI 보조(라벨/검색조건 추론) — `LlmAssist` 미구현.
- 등록자/수정자 ID 서버 연동 — 감사 시점은 `SYSDATE()` 자동·폼 제외 완료, ID는 Controller `LoginVO` 연동 미구현.
- 검색조건 select의 옵션 라벨 — JSP 목록 화면 검색 select 옵션이 비어 있음(검색 동작 자체는 PK 기준).
- FK 드롭다운의 옵션 항목 — `<select>` 골격과 참조 테이블 표시까지만. 옵션(연관 데이터)은 수동 연동 필요(연관 조회는 런타임 데이터라 결정적 생성 범위 밖).
- 코드 서명 — 배포 `.exe`가 미서명이라 다른 PC에서 SmartScreen 경고(우회는 USER-GUIDE 트러블슈팅 참고).

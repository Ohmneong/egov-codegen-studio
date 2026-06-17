# egov-crud-gen

eGov 표준프레임워크(5.0.1) CRUD 풀세트 코드 생성기. **외부 의존성 0, 순수 Java(텍스트 블록 템플릿)** 로 작성되어 번들 JDK만으로 빌드·실행된다. 폐쇄망에 그대로 복사해 쓸 수 있다.

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

## 산출물 (테이블 1개 → 11파일)

```
output/src/main/java/{package}/service/        {Entity}.java, {Entity}VO.java, Egov{Entity}ManageService.java
output/src/main/java/{package}/service/impl/    Egov{Entity}ManageServiceImpl.java, {Entity}ManageDAO.java
output/src/main/java/{package}/web/             Egov{Entity}ManageController.java
output/src/main/resources/egovframework/mapper/{module}/  Egov{Entity}Manage_SQL_mysql.xml
output/src/main/webapp/WEB-INF/jsp/{module}/    Egov{Entity}{List,Detail,Regist,Modify}.jsp
```

## 적응형 설정 (gen.properties)

프로젝트가 바뀌면 `gen.properties`만 교체한다. 핵심 항목: `basePackage`, `module`, `tablePrefix`, `dbType`, 공통 컴포넌트 베이스 경로(`daoBase`/`serviceBase`/`paginationInfo`).

## 구조

```
src/com/hanbit/egovgen/
  Main.java                 CLI 진입점
  config/GenConfig.java     적응형 설정 로드
  model/                    TableMeta, ColumnMeta (중간 메타모델)
  parser/                   DdlParser(인터페이스), MySqlDdlParser
  gen/                      NameUtil, TypeMapper, CodeGenerator(템플릿)
```

## 1차(MVP) 범위와 한계 — 정직하게

**됨**: MySQL DDL 1개 파싱 → 백엔드 6 + Mapper XML + JSP 4 생성, 논리삭제(USE_AT) 자동 분기, 코멘트→한글 라벨, 적응형 설정.

**아직 안 됨 (2차/추후)**:
- **생성 자바 코드의 컴파일은 이 도구만으로 검증 불가** — eGov 의존성(spring/jakarta/commons-lang3/rte) 클래스패스가 필요. 실제 eGov 프로젝트(예: bp.enter)에 산출물을 넣고 컴파일해야 최종 확인된다. **이게 1차 MVP의 핵심 검증 단계다.**
- 재생성(round-trip) diff 전략 — 현재는 매번 덮어쓰기.
- 엑셀/CSV 일괄 입력 — POI 의존성 회피 위해 2차로 미룸.
- MySQL 외 DB 파서 — `DdlParser` 인터페이스만 열려 있음.
- AI 보조(라벨/검색조건 추론) — `LlmAssist` 미구현.
- 등록자/시각 컬럼(FRST_REGIST_PNTTM 등) 자동 `SYSDATE()` 처리 — 현재는 일반 컬럼과 동일 매핑.
- 검색조건 select의 옵션 라벨 — JSP 목록 화면 select가 비어 있어 컬럼 라벨 채우기 필요.

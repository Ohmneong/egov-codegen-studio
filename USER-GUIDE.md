# egov-crud-gen 사용자 매뉴얼

> 💡 **처음 쓰신다면 [QUICKSTART.md](./QUICKSTART.md)** (복붙 따라하기)부터 보세요. 이 문서는 옵션·규칙을 더 자세히 설명합니다.

eGov 표준프레임워크(검증: 5.0.1) 프로젝트에서 **MySQL DDL 한 개 → CRUD 풀세트(백엔드 6 + Mapper XML + JSP 4)** 를 생성하는 CLI 도구입니다. 외부 의존성 0(순수 Java), 폐쇄망에서 그대로 동작합니다.

---

## 0. 한눈에 보는 흐름

```
① 빌드(최초 1회)  →  ② DDL 준비  →  ③ 설정(gen.properties)  →  ④ 생성 실행
     →  ⑤ 프로젝트에 배치  →  ⑥ (채번 시) 채번 테이블/빈 확인  →  ⑦ 빌드·배포·실행
```

---

## 1. 사전 준비

- **JDK 17 이상** (eGov 5.0.1은 Java 17). 별도 설치가 없으면 eGovFrameDev 번들 JDK를 씁니다.
  - 예: `C:\eGovFrameDev-5.0.1-Windows-64bit\eclipse\plugins\org.eclipse.justj.openjdk...\jre\bin\`
- **대상 eGov 프로젝트** (생성 코드를 넣을 곳). 본 예시는 `bp.enter`.
- Maven은 **필요 없습니다**(도구가 의존성 0).

---

## 2. 빌드 (최초 1회)

```powershell
cd egov-crud-gen
powershell -ExecutionPolicy Bypass -File .\build.ps1
# → dist\egov-crud-gen.jar 생성
```

> **주의**: 위 명령은 `powershell`까지 통째로 입력해야 합니다. 이미 PowerShell 창 안이라면 `.\build.ps1` 만으로도 됩니다. `powershell`을 빼고 `-ExecutionPolicy ...`만 입력하면 `'-ExecutionPolicy' 용어가 인식되지 않습니다` 에러가 납니다. (실행 정책 때문에 `.\build.ps1`이 막히면 `powershell -ExecutionPolicy Bypass -File .\build.ps1`을 쓰세요.)

`build.ps1`이 번들 JDK의 `javac`/`jar`를 자동 탐색합니다. 다른 JDK 경로를 쓰려면 `build.ps1`의 `$jdkRoot`만 수정하세요.

---

## 3. 설정 — gen.properties (프로젝트 적응형)

프로젝트가 바뀌면 **이 파일만 교체**합니다.

```properties
basePackage=egovframework.let.sym.cal     # 생성 코드 루트 패키지
module=let/sym/cal                         # ★ URL/뷰/Mapper 경로 (아래 주의 참고)
tablePrefix=LETTN                          # 엔티티명 도출 시 제거할 테이블 prefix
dbType=mysql                               # 1차 mysql 고정
outputDir=./output                         # 산출물 출력 위치
useIdgnr=false                             # 채번 사용 여부 (true면 String PK 필요)
baseUrl=http://localhost:8080              # 생성 후 안내 URL의 앞부분 (톰캣 포트/컨텍스트)

# 공통 컴포넌트 베이스 (eGov 5.0.1 기본값 — 다른 버전이면 여기만 교체)
daoBase=org.egovframe.rte.psl.dataaccess.EgovAbstractMapper
serviceBase=org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl
paginationInfo=org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo
```

> **★ module 경로 주의 (매우 중요)**
> eGov의 MyBatis Mapper 스캔 경로가 `classpath:/egovframework/mapper/let/**/*_${DbType}.xml` 입니다.
> 즉 **module 값이 `let/` 으로 시작해야** 생성된 Mapper XML이 자동 로드됩니다.
> 예: `module=let/sym/cal` (O) / `module=sym/cal` (X — Mapper 안 잡힘)

---

## 4. DDL 준비

생성하려는 테이블의 `CREATE TABLE` 문을 텍스트 파일로 저장합니다. (`COMMENT`를 달면 화면 라벨로 쓰입니다.)

```sql
CREATE TABLE `LETTNRESTDE` (
    `RESTDE_NO`  INT          NOT NULL COMMENT '휴일일련번호',
    `RESTDE_DE`  VARCHAR(20)  NOT NULL COMMENT '휴일일자',
    `RESTDE_NM`  VARCHAR(100) NOT NULL COMMENT '휴일명',
    `USE_AT`     CHAR(1)      DEFAULT 'Y' COMMENT '사용여부',
    PRIMARY KEY (`RESTDE_NO`)
);
```

> **채번(useIdgnr=true)을 쓸 거라면 PK를 `VARCHAR`로 정의하세요.** eGov 채번은 `RESTDE_0000…` 형태의 String ID를 만듭니다. (정수 PK는 보통 MySQL `AUTO_INCREMENT`가 더 적합합니다.)

---

## 5. 생성 실행

```powershell
$java = "C:\eGovFrameDev-5.0.1-Windows-64bit\eclipse\plugins\...\jre\bin\java.exe"

# (1) 출력 디렉터리에 생성 — 결과를 먼저 확인하고 싶을 때
& $java -jar dist\egov-crud-gen.jar --ddl sample\restde.sql --config gen.properties

# (2) 대상 프로젝트에 바로 배치 — out 을 프로젝트 루트로
& $java -jar dist\egov-crud-gen.jar --ddl sample\restde.sql --config gen.properties `
        --out "C:\...\workspace-egov\bp.enter"
```

`--out`을 프로젝트 루트로 주면 `src/main/java`, `src/main/resources`, `src/main/webapp` 구조에 **그대로 병합**됩니다(새 패키지라 기존 파일과 충돌하지 않습니다).

### 옵션 레퍼런스

| 옵션 | 설명 |
|---|---|
| `--ddl <파일>` | (필수) MySQL CREATE TABLE DDL 파일 |
| `--config <파일>` | 설정 properties |
| `--package <pkg>` | 루트 패키지 (config 덮어쓰기) |
| `--module <경로>` | 모듈 경로 (예: `let/sym/cal`) |
| `--prefix <prefix>` | 테이블 prefix 제거 |
| `--out <디렉터리>` | 출력 루트 |
| `--idgnr` | 채번 적용 (String PK일 때만 동작) |

---

## 6. 산출물 (테이블 1개 → 11~12파일)

```
src/main/java/{package}/service/        {Entity}.java, {Entity}VO.java, Egov{Entity}ManageService.java
src/main/java/{package}/service/impl/    Egov{Entity}ManageServiceImpl.java, {Entity}ManageDAO.java
src/main/java/{package}/web/             Egov{Entity}ManageController.java
src/main/resources/egovframework/mapper/{module}/   Egov{Entity}Manage_SQL_mysql.xml
src/main/webapp/WEB-INF/jsp/{module}/    Egov{Entity}{List,Detail,Regist,Modify}.jsp
src/main/resources/egovframework/spring/com/        context-idgen-{entity}.xml  (채번 시에만)
```

### 자동으로 잡히는 것 (eGov 표준 설정 기준)
- **컴포넌트 스캔**: `base-package="egovframework"` → `egovframework.*` 패키지면 자동 등록
- **Mapper XML**: `mapper/let/**/*_mysql.xml` → module이 `let/`로 시작하면 자동 로드
- **채번 빈**: `spring/com/context-*.xml` 패턴 → `context-idgen-{entity}.xml` 자동 로드 (수동 등록 불필요)

---

## 7. 채번(--idgnr) 사용 시 추가 준비

1. **PK는 `VARCHAR`** (5번 참고). 길이는 `prefix길이 + 숫자자리수`로 잡힙니다.
   - 생성기가 `cipers`(숫자 자리수)를 `PK길이 − prefix길이`로 자동 계산합니다.
   - 예: PK `VARCHAR(20)`, prefix `RESTDE_`(7) → `cipers=13` → `RESTDE_0000000000001`
2. **채번 관리 테이블 `IDS`** 가 DB에 있어야 합니다(보통 eGov 프로젝트엔 이미 존재). 없으면:
   ```sql
   CREATE TABLE IF NOT EXISTS `IDS` (
       `table_name` VARCHAR(16) NOT NULL,
       `next_id`    DECIMAL(30) NOT NULL,
       PRIMARY KEY (`table_name`)
   );
   ```
   해당 테이블의 채번 행은 첫 등록 시 자동 생성됩니다.
3. prefix를 바꾸려면 생성된 `context-idgen-{entity}.xml`의 `prefix` 값을 수정하면 됩니다.

---

## 8. 빌드 · 배포 · 실행 (이클립스 + 톰캣)

1. 대상 프로젝트 **Refresh (F5)** — 새 소스/리소스 인식
2. 자동 빌드 확인 (Problems 뷰에 에러 없어야 함)
3. (채번 테이블 등) **DB 준비**
4. 톰캣 **republish 후 Start**
5. 접속: `http://localhost:{포트}/{컨텍스트}/{module}/Egov{Entity}List.do`
   - 예: `/let/sym/cal/EgovRestdeList.do`

---

## 9. 트러블슈팅 (실전에서 겪은 것들)

| 증상 | 원인 / 해결 |
|---|---|
| 컴파일 시 `package org.egovframe... does not exist` | 클래스패스 누락. eGov 의존성(.m2 또는 프로젝트 lib)을 classpath에 포함해야 함. |
| `Data too long for column 'USE_AT'` | `CHAR(1)` 컬럼에 2자 이상 입력. 폼에서 `Y`/`N` 한 글자만. (생성 폼은 `maxlength`로 제한됨) |
| `Data too long for column '{PK}'` (채번) | 채번 ID가 PK 길이 초과. PK를 `VARCHAR`로, 길이가 `prefix+숫자`보다 큰지 확인. |
| Mapper 쿼리를 못 찾음 / SQL 미실행 | module이 `let/`로 시작하지 않아 Mapper XML이 스캔 경로 밖. `module=let/...`로 재생성. |
| 톰캣 기동 시 `Cannot find class egovframework.com.cmm.*` | **WTP 배포 깨짐**(컴파일은 정상인데 배포 폴더 `wtpwebapps/.../WEB-INF/classes`가 빔). Servers 뷰 → 서버 **Stop → Clean… → Add and Remove로 모듈 재등록 → Start**. `mvn clean package`로는 해결 안 됨(배포 폴더는 WTP가 관리). |
| 채번 등록 시 `IDS` 관련 오류 | `IDS` 채번 테이블 미존재. 7번 DDL로 생성. |
| 한글 라벨이 필드명으로 나옴 | DDL 컬럼에 `COMMENT`가 없음. DDL에 코멘트 추가 후 재생성. |

---

## 10. 현재 한계 (다음 단계)

- **재생성(round-trip)**: 현재는 덮어쓰기. 손으로 고친 ServiceImpl/Controller가 있으면 재생성 시 보존 안 됨 → 2차에서 diff 전략 예정.
- **화면 플랫폼**: JSP만. eXBuilder/WebSquare는 추후(출력 어댑터 인터페이스만 열려 있음).
- **DB**: MySQL만. 다른 DB는 `DdlParser` 인터페이스로 확장.
- **엑셀 일괄 입력**: 미지원(POI 의존성 회피). 1차는 DDL 파일 단건.
- **검색 select 라벨**: 목록 화면 검색 select 옵션이 비어 있어 필요 시 채워야 함.
- **감사 컬럼 자동 처리**: eGov 표준 감사 컬럼(`FRST_REGISTER_ID`/`FRST_REGIST_PNTTM`/`LAST_UPDUSR_ID`/`LAST_UPDT_PNTTM`)은 등록·수정 화면에서 입력칸 제외, 시점(`_PNTTM`)은 INSERT/UPDATE 시 `SYSDATE()` 자동 입력. 단 **등록자/수정자 ID는 서버 로그인 연동 미구현**이라 채워지지 않음(필요 시 Controller에서 LoginVO로 set).
- 등록/수정 폼 입력칸 옆에 **데이터 타입·길이**(예: `VARCHAR(200)`)가 표시됨.

---

## 부록: 빠른 예시 (휴일관리 생성)

```powershell
# 1. DDL 저장: sample\restde.sql  (위 4번 예시)
# 2. gen.properties: basePackage=egovframework.let.sym.cal, module=let/sym/cal, tablePrefix=LETTN
# 3. 생성 + 배치
& $java -jar dist\egov-crud-gen.jar --ddl sample\restde.sql --config gen.properties --out "C:\...\bp.enter"
# 4. 이클립스 Refresh → republish → 접속
#    http://localhost:8080/.../let/sym/cal/EgovRestdeList.do
```

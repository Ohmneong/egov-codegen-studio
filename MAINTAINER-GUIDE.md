# egov-codegen-studio 관리자(유지보수) 매뉴얼

이 문서는 **도구를 수정·확장·배포·관리**하는 담당자를 위한 것입니다. 단순 사용법은 [`USER-GUIDE.md`](./USER-GUIDE.md)를 보세요.

---

## 1. 설계 개요

### 파이프라인
```
DDL 텍스트 → [DdlParser] → TableMeta/ColumnMeta(메타모델) → [CodeGenerator + 템플릿] → 산출물 파일
                  ▲                                              ▲
            DB별 교체 가능                                  text block 템플릿 + 치환
```

### 핵심 원칙
- **외부 의존성 0** — 순수 Java만. 라이브러리를 추가하지 말 것(폐쇄망 반입·빌드 단순성 때문). 새 의존성이 꼭 필요하면 먼저 팀과 합의하고 사유·대안을 기록.
- **결정적 생성** — 같은 입력 → 항상 같은 출력. 골격 생성에 LLM/난수를 쓰지 않는다(균질성).
- **템플릿은 코드 안에** — Java 21 text block(`"""..."""`)에 `__PLACEHOLDER__`를 두고 치환.

---

## 2. 소스 구조와 역할

```
src/dev/myoh/egovgen/
├─ Main.java                  CLI 진입점(얇은 어댑터). 인자 파싱(parseArgs) + 파일 읽기 → GenerationService 호출 → 콘솔/URL 출력
├─ config/GenConfig.java      설정 로드(properties) + CLI 덮어쓰기(override) + GUI 폼 주입(setXxx). 적응형 설정의 단일 출처
├─ model/
│  ├─ ColumnMeta.java         컬럼 1개 메타(컬럼명/필드명/자바타입/PK/코멘트/size/fkTable) + label(), searchable(), isAudit()/isAuditTimestamp()/isUpdateTimestamp(), isYesNo()/isForeignKey()
│  └─ TableMeta.java          테이블 메타(엔티티명 + 컬럼 목록) + primaryKey(), searchableColumns()(현재 PK 기준)
├─ parser/
│  ├─ DdlParser.java          파서 인터페이스 (parse, parseAll, dbType) — DB별 교체 지점
│  └─ MySqlDdlParser.java     MySQL 정규식 파서(여러 줄 컬럼·한 DDL 다중 CREATE TABLE·FK REFERENCES 파싱)
├─ gen/
│  ├─ NameUtil.java           snake↔camel↔Pascal, 테이블명→엔티티명
│  ├─ TypeMapper.java         DDL 타입 → 자바 타입 매핑, size 추출
│  └─ CodeGenerator.java      ★ 템플릿 + 파일 생성 (가장 자주 수정하는 곳)
├─ service/
│  ├─ GenerationService.java  파서 선택(selectParser) + 파싱 + 생성 + URL 조립. CLI/GUI 공유, 콘솔 비의존
│  │                          generate(단일)·generateAll(다중 테이블)·preview(생성 전 미리보기) 제공
│  ├─ GenerationResult.java   생성 결과 DTO(파싱 메타·파일 목록·접속 URL)
│  └─ PreviewEntry.java       미리보기 항목 DTO(생성될 경로 + 기존 존재 여부)
└─ ui/
   └─ GenGuiApp.java          Swing GUI 진입점(JDK 내장만 사용). 프로파일 바·미리보기·생성, GenerationService 호출
```

### CodeGenerator 내부 구조 (가장 중요)
- `build(TableMeta)` — 산출물 (파일경로 → 내용) 맵(`LinkedHashMap`)을 만든다(파일 쓰기 없음). **산출물을 추가/제거하려면 여기.** 미리보기(`GenerationService.preview`)도 이 맵의 경로를 재사용한다.
- `generate(TableMeta)` — `build()` 결과를 순회하며 파일을 쓴다(경로 목록 반환).
- `base(TableMeta)` — 모든 템플릿이 공유하는 치환 변수 맵(`LinkedHashMap`)을 만든다. **새 플레이스홀더는 여기에 추가.**
- `render(tpl, vars)` — `__KEY__`를 값으로 치환. **고정점까지 반복**하므로 값 안에 또 다른 `__KEY__`가 있어도 해결됨.
- 산출물별 메서드: `domainVo`, `searchVo`, `serviceInterface`, `serviceImpl`, `dao`, `controller`, `mapperXml`, `jspList`, `jspDetail`, `jspForm`, `idgnrBeanXml`.
- `jspForm` 입력 위젯: 기본 `<input>`이나, Y/N 여부 컬럼(`isYesNo`)은 Y/N `<select>`, FK 컬럼(`isForeignKey`)은 참조 테이블 `<select>`(옵션은 수동 연동)로 분기.
- 출력 경로: `generate()`에서 `cfg.mapperRoot()`/`cfg.jspRoot()`로 Mapper·JSP 루트를 조립한다(설정 변수화 — 5장·`gen.properties`).
- 생성물 클래스명/파일명/URL에는 `Egov` 접두어를 붙이지 않는다(엔티티명 그대로). `EgovIdGnrService`·`EgovAbstractMapper` 등은 eGov **라이브러리** 클래스라 별개로 유지된다.

> 진입점 분리: 파서 선택→파싱→생성→URL 조립은 `service/GenerationService`에 있고 `CodeGenerator`는 파일 생성만 한다. CLI(`Main`)·GUI(`GenGuiApp`)는 둘 다 `GenerationService`를 호출한다.

---

## 3. 자주 하는 수정 시나리오 (How-to)

### 3-1. 템플릿(생성 코드 모양) 수정
예: Controller에 메서드 추가, JSP 레이아웃 변경.
1. `CodeGenerator`의 해당 메서드(`controller`, `jspList` 등)에서 text block을 수정.
2. 테이블/컬럼에 따라 달라지는 부분은 `__PLACEHOLDER__`로 두고, 값이 `base()`에 없으면 추가.
3. 반복되는 컬럼 단위 출력은 `StringBuilder`로 만들어 `v.put("KEY", sb.toString())`.
4. 빌드 → `sample/sample.sql`로 생성해 눈으로 확인 → 컴파일 검증(4장).

> **함정**: 값 문자열 안에 `__KEY__`를 넣으면 `render`가 반복 치환으로 풀어준다. 단 **값에 우연히 `__`가 들어가지 않게** 주의(컬럼 코멘트 등 사용자 입력은 안전 범위).

### 3-2. 새 컬럼 타입 매핑 추가
`TypeMapper.toJava()`의 `switch`에 케이스 추가. 예: `bit`/`boolean` → `"String"` 또는 `"boolean"`.

### 3-3. 새 설정 항목 추가 (적응형 설정 확장)
`mapperRoot`/`jspRoot` 추가가 최신 예시다(전 경로에 걸쳐 손대는 항목 참고).
1. `GenConfig`에 필드 + getter + GUI 폼 주입용 setter(`setXxx`) 추가.
2. `load()`에 `p.getProperty(...)` 한 줄, `override()`에 CLI 키 추가.
3. `Main`에서 `cfg.override("키", opt.get("키"))` (CLI로 받을 경우).
4. GUI에 노출하려면 `GenGuiApp`에 입력 필드 + `buildSettingsPanel()` 행 + `loadConfigIntoForm()`/`onGenerate()` 연결.
5. `CodeGenerator`(경로 조립 또는 `base()`/템플릿)에서 사용.
6. `gen.properties`와 `USER-GUIDE.md`에 항목 문서화.

### 3-4. 새 공통 컴포넌트 옵션 (검증/메시지/로깅 등)
채번(`useIdgnr`)이 모범 사례다. 동일 패턴:
- `GenConfig`에 `useXxx` 플래그
- `CodeGenerator`에 `xxxApplicable(t)` 판정 메서드
- 템플릿에서 조건부 블록을 `__XXX_IMPORT__`/`__XXX_FIELD__`/`__XXX_BODY__` 변수로 주입
- 필요하면 별도 산출물(빈 XML 등) 생성 — `idgnrBeanXml` 참고

### 3-5. 새 DB 파서 추가 (Oracle/PostgreSQL 등)
1. `DdlParser`를 구현하는 `OracleDdlParser` 작성(`dbType()`="oracle").
2. `GenerationService.selectParser()`의 `switch`에 `case "oracle" -> new OracleDdlParser();`.
3. Mapper XML의 DB 종속 SQL(페이징 `LIMIT/OFFSET`, `SYSDATE()` 등)은 `CodeGenerator.mapperXml`에서 `dbType`별로 분기 필요. **현재 MySQL 전용이므로 여기 분기 추가가 핵심 작업.**

### 3-6. 새 화면 플랫폼 (eXBuilder/WebSquare)
현재 JSP만 직접 생성한다. 확장 시:
- 화면 생성을 인터페이스(`ViewGenerator`)로 분리하고 JSP 구현을 옮긴 뒤, 플랫폼별 구현 추가.
- 설정에 `viewType`(jsp/exbuilder/websquare) 추가, `generate()`에서 분기.

### 3-7. 채번 전략 조정
- prefix/자리수 규칙: `CodeGenerator.base()`의 `IDGNR_PREFIX`, `IDGNR_CIPERS` 계산.
  - **불변식**: `prefix길이 + cipers = PK컬럼 길이`. (eGov `EgovIdGnrStrategyImpl`의 `cipers`는 "숫자 자리수"이고 prefix는 별도)
- 정수 채번이 필요하면 `getNextIntegerId()` + strategy 없는 빈으로 분기(현재 미구현).

### 3-8. 감사 컬럼 규칙 조정
등록자/등록시점/수정자/수정시점은 화면 입력에서 빼고, 시점은 `SYSDATE()`로 자동 채운다.
- 판정은 `ColumnMeta`의 세 메서드: `isAudit()`(폼 입력 제외 여부), `isAuditTimestamp()`(INSERT 시 `SYSDATE()`), `isUpdateTimestamp()`(UPDATE 시에도 `SYSDATE()`). eGov 표준명(`FRST_REGIST_PNTTM` 등)과 관례명(`created_at`/`updated_at`/`created_by`/`updated_by`)을 모두 처리.
- 인식 컬럼명을 늘리려면 이 세 메서드의 집합만 고치면 INSERT/UPDATE/폼이 일관 적용된다(`CodeGenerator.mapperXml`·`jspForm`이 이 판정을 사용).

### 3-9. 검색 대상 컬럼 조정
목록 검색 조건은 `TableMeta.searchableColumns()`가 결정한다. **현재는 PK 기준**(`isPrimaryKey`)이다. 범위를 넓히려면 이 필터를 `ColumnMeta.searchable()`(문자열·비PK) 등으로 바꾼다. `CodeGenerator.mapperXml`이 이 목록으로 `searchCondition` 동적조건을 만든다.

### 3-10. 등록/수정 폼 입력 위젯(드롭다운 등) 추가
`CodeGenerator.jspForm`의 분기에서 결정한다. 현재 Y/N 여부 컬럼(`ColumnMeta.isYesNo()` — CHAR(1) + 이름이 `_AT`/`_YN`)은 Y/N `<select>`, FK 컬럼(`isForeignKey()` — `MySqlDdlParser`가 `FOREIGN KEY ... REFERENCES`/인라인 `REFERENCES`로 `fkTable` 채움)은 `<select>`로 만든다. 새 위젯 규칙은 `ColumnMeta`에 판정 메서드를 추가하고 `jspForm`에 분기를 더한다. (FK `<select>` 옵션을 실제 데이터로 채우는 건 런타임 조회라 현재는 골격만 — 8장 로드맵.)

### 3-11. 여러 테이블 일괄 / 미리보기 / 설정 프로파일
- **여러 테이블**: `MySqlDdlParser.parseAll`이 한 DDL의 모든 `CREATE TABLE`을 파싱(`parseBody`로 본문 파싱 분리), `GenerationService.generateAll`이 테이블별로 생성한다(`Main`·`GenGuiApp`이 결과 목록 출력). 단일 `parse`/`generate`는 첫 테이블 위임.
- **미리보기/덮어쓰기**: `GenerationService.preview`가 `CodeGenerator.build()`의 경로로 `PreviewEntry`(경로 + 기존 존재 여부) 목록을 만든다. GUI `[미리보기]`와 생성 전 덮어쓰기 확인에 쓴다.
- **설정 프로파일(GUI)**: `GenConfig.saveTo(Path)`로 현재 설정을 properties로 저장하고, `GenGuiApp`이 `profiles/` 폴더를 콤보박스로 전환한다.

---

## 4. 빌드 · 검증 절차

### 빌드
```powershell
powershell -ExecutionPolicy Bypass -File build.ps1   # dist\egov-codegen-studio.jar
```

### 생성 자바의 컴파일 검증 (회귀 테스트의 핵심)
생성 코드는 eGov 의존성이 필요하므로, 로컬 `.m2` 또는 대상 프로젝트의 의존성으로 컴파일해본다.
```powershell
# 1) 충돌 없는 패키지로 생성
java -jar dist\egov-codegen-studio.jar --ddl sample\verify.sql `
     --package egovframework.let.gen.sample --module let/gen/sample --prefix LETTN_ --idgnr --out .\verify-output

# 2) .m2 의존성으로 classpath 구성 (소스/자바독 jar 제외, 백슬래시→슬래시)
#    환경변수 CLASSPATH는 길이 제한이 있으므로 argfile 사용. argfile 안 "" 의 백슬래시는
#    이스케이프되니 반드시 '/' 로 치환할 것. (build.ps1 외 별도 스크립트로 관리 권장)

# 3) javac 컴파일 → 종료코드 0 확인
```
> 위 절차에서 부딪힌 함정은 6장 참고. **회귀 검증 = 생성 → 컴파일 종료코드 0 + 실제 톰캣 1회 구동**.

### 회귀 체크리스트 (수정 후 매번)
- [ ] `sample/sample.sql`(int PK), `sample/verify.sql`(String PK, 채번) 둘 다 생성 성공
- [ ] 생성 자바 컴파일 종료코드 0
- [ ] 목록/상세/등록/수정/삭제 URL 동작 (최소 1회 톰캣)
- [ ] 채번 등록 시 PK 자동 부여
- [ ] `USER-GUIDE.md` / `gen.properties` 문서 동기화

---

## 5. eGov 환경 고정값 (5.0.1 기준 — 버전 올릴 때 점검)

| 항목 | 값 | 확인 방법 |
|---|---|---|
| DAO 베이스 | `org.egovframe.rte.psl.dataaccess.EgovAbstractMapper` | 대상 프로젝트 소스 import |
| ServiceImpl 베이스 | `org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl` | 〃 |
| 페이징 | `org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo` | 〃 |
| 채번 | `org.egovframe.rte.fdl.idgnr.*` + `IDS` 테이블 | `context-idgen.xml` |
| Java / 네임스페이스 | Java 17, **jakarta.*** (javax 아님) | `pom.xml` |
| 컴포넌트 스캔 | `base-package="egovframework"` | `context-common.xml` |
| Mapper 스캔 | `mapper/let/**/*_${DbType}.xml` (프로젝트별로 다름 → `gen.properties`의 `mapperRoot`/`module`로 맞춤) | `context-mapper.xml` |
| 컨텍스트 로딩 | `classpath*:egovframework/spring/com/context-*.xml` | `web.xml` |

> **다른 eGov 버전/프로젝트로 이식 시**: 이 표의 값들을 대상 프로젝트에서 다시 확인하고, `gen.properties`의 `daoBase`/`serviceBase`/`paginationInfo`, 그리고 Mapper·JSP 스캔·출력 경로(`mapperRoot`/`jspRoot`/`module`)를 맞춰라. 이게 "프로젝트 적응형"의 실체다.

---

## 6. 코딩 규칙 / 알려진 함정 (실전 기록)

1. **render는 1패스가 아니라 반복** — 중첩 플레이스홀더 때문. 절대 단순 1패스로 되돌리지 말 것.
2. **javac `@argfile`의 `"..."` 안 백슬래시는 이스케이프됨** → classpath 경로는 `/`로 치환.
3. **`CLASSPATH` 환경변수 길이 제한**(~32KB) → 다수 jar는 argfile로.
4. **WTP 배포 깨짐** — 이클립스 컴파일(`target/classes`)은 되는데 톰캣 배포본(`wtpwebapps/.../WEB-INF/classes`)이 비는 경우. `mvn clean package`로 안 풀리고 **서버 Clean/모듈 재등록**이 답. (급할 땐 `target/classes` → 배포본 수동 복사)
5. **PK 타입과 채번** — 채번(String ID)은 `VARCHAR` PK 전제. `cipers = PK길이 − prefix길이`.
6. **`CHAR(1)` 입력** — 폼 input에 `maxlength` 적용됨. 새 입력 위젯 추가 시 size 반영 유지.
7. **인코딩** — 소스/생성물 UTF-8. PowerShell `Set-Content -Encoding utf8`은 BOM을 붙이므로 argfile엔 `[IO.File]::WriteAllLines` 사용.
8. **CRLF 경고**는 무해(Windows). 필요하면 `.gitattributes`로 정책화.
9. **여러 줄 컬럼 정의** — `DEFAULT ... ON UPDATE ...`처럼 한 컬럼이 여러 줄에 걸치면, `MySqlDdlParser`가 `splitTopLevel` 조각의 공백(줄바꿈)을 한 칸으로 정규화해 파싱한다. 컬럼 정규식의 `.`은 줄바꿈을 안 먹으므로 이 정규화를 빼면 컬럼이 누락된다.
10. **GUI가 jar를 잠금** — `run-gui.ps1`로 띄운 GUI(`javaw`)가 `dist\egov-codegen-studio.jar`를 물고 있으면 `build.ps1`/`package.ps1`이 jar 교체에 실패한다. 빌드·패키징 전 GUI 프로세스를 종료할 것.

---

## 7. 버전 관리 / 배포 / 재배포

### 브랜치 전략
- `main` — 안정 버전. 직접 커밋 지양.
- `feature/<요약>` — 기능/수정 단위 브랜치. 회귀 체크리스트(4장) 통과 후 `main`에 병합.
- 원격: https://github.com/Ohmneong/egov-codegen-studio (public)

### 배포본 3종 (용도에 맞게 선택)

| 배포본 | 만드는 법 | 크기 | 받는 쪽 요건 | 용도 |
|---|---|---|---|---|
| **jar** | `build.ps1` | 수십 KB | Java 17+ 설치 필요 | CLI/스크립트 자동화, 소스째 반입 |
| **app-image** | `package.ps1` | ~150MB | 없음(JRE 내장) | 폴더 복사 실행, 폐쇄망 USB |
| **.exe 인스톨러** | `package.ps1 -Type exe` | ~55MB | 없음(JRE 내장) | 일반 배포(설치 마법사·시작메뉴/바탕화면 바로가기·제거) |

> `.exe`/`.msi` 인스톨러는 **WiX 3.x** 필요(`choco install wixtoolset` 최초 1회). app-image·jar은 WiX 불필요.
> jpackage 자체는 **풀 JDK(14+)** 에 들어 있다(번들 justj 는 JRE라 없음 — `package.ps1`이 시스템 JDK를 탐색).

### 배포 채널
- **인터넷 되는 팀 → GitHub Releases**:
  ```powershell
  gh release create v1.0.1 package\egov-codegen-studio-1.0.1.exe --title "v1.0.1" --notes "변경 요약"
  ```
  팀원은 releases 페이지에서 내려받는다.
- **폐쇄망 SI 현장** → `.exe`(또는 app-image 폴더를 zip 압축)를 USB/사내 파일서버로 전달. 받는 PC에 Java 불필요.

### 받는 쪽 SmartScreen / "알 수 없는 게시자" 경고 대응
배포본 exe는 **코드 서명이 없어** 다른 PC에서 실행 시 Windows Defender SmartScreen이 막을 수 있다(서명 미적용 exe의 공통 현상 — 우리 코드 문제 아님). 서명 인증서 없이 받는 쪽이 우회하는 방법:
- SmartScreen 창에서 **"추가 정보 → 실행"** 클릭
- 받은 exe 우클릭 → 속성 → **"차단 해제"** 체크 (또는 PowerShell `Unblock-File 파일.exe`)
- **USB로 직접 복사**하면 "인터넷에서 받음" 표식(MOTW)이 안 붙어 경고가 뜨지 않는다 — **폐쇄망 반입엔 이게 사실상 해결책**

> 경고를 근본적으로 없애려면 코드 서명 인증서가 필요하다(사내: 자체서명 + 사내 신뢰 루트 등록 / 공개: OV·EV 상용 인증서 → `signtool`로 exe 서명). 현재는 미적용.

### 수정 → 재배포 사이클 (가장 자주 하는 일)
1. `feature/<요약>` 브랜치에서 소스 수정.
2. `build.ps1` → `sample/*.sql`로 생성해 회귀 확인(4장 체크리스트).
3. **버전 올리기** — `package.ps1`의 `--app-version "1.0.0"` 값을 올린다(예: `1.0.1`).
   > ⚠ 안 올리면 인스톨러가 같은 버전으로 인식해 업그레이드/재설치가 깔끔히 안 된다.
4. `package.ps1 -Type exe`(또는 app-image) → 새 배포본.
5. `main` 병합 → 커밋 → 태그(`git tag -a v1.0.1 -m "..."`) → `git push --tags`.
6. (인터넷팀) `gh release create`로 배포본 첨부 / (폐쇄망) 새 배포본 전달.

받는 쪽 업데이트 방법:
- **인스톨러(.exe)**: 새 버전 exe 실행 → 기존 위에 업그레이드 설치.
- **app-image**: 폴더를 새 것으로 통째 교체.

> `dist/`·`build/`·`package/`는 `.gitignore` 대상. 배포본은 git에 커밋하지 말고 빌드로 재생산하거나 GitHub Releases/사내 아티팩트에 보관.

### 변경 시 함께 갱신할 것
- 생성 산출물 구조가 바뀌면 → `USER-GUIDE.md` 6장, `SKELETON.md`
- 설정 항목이 바뀌면 → `gen.properties`, `USER-GUIDE.md` 3장
- eGov 좌표가 바뀌면 → 5장 표, `gen.properties` 기본값
- 배포 버전을 올릴 땐 → `package.ps1`의 `--app-version` (+ git 태그)

---

## 8. 확장 로드맵 (PRD 2차/추후와 연계)

| 우선순위 | 항목 | 작업 위치 |
|---|---|---|
| 2차 | 재생성(round-trip) diff 전략 | `generate()` — 기존 파일 비교/패치 출력 |
| 2차 | 엑셀/CSV 일괄 입력 | 새 입력 어댑터 + `Main` (한 DDL의 다중 테이블은 `parseAll`로 완료. POI는 의존성 0 충돌 → CSV 우선 검토) |
| 2차 | 다른 DB 파서 | 3-5 참고 |
| 추후 | 화면 플랫폼(eXBuilder/WebSquare) | 3-6 참고 |
| 추후 | AI 보조(라벨/검색조건 추론) | 사내 LLM CLI 연동 인터페이스 `LlmAssist` |
| 개선 | FK 드롭다운 옵션 자동 채우기 | `jspForm`/연관 조회 (현재 FK는 `<select>` 골격만, 옵션은 수동 — 3-10) |
| 개선 | 등록자/수정자 ID 서버(LoginVO) 연동 | `controller` (감사 시점 `SYSDATE()`·폼 제외·검색 PK 기준은 완료) |
| 개선 | 목록 검색 select 옵션 라벨 채우기 | `jspList` |

> 완료: Swing GUI·설치본(app-image/.exe), 감사 컬럼 자동 처리(표준명+관례명), 검색 PK 기준, `Egov` 접두어 제거, Mapper/JSP 경로 변수화(`mapperRoot`/`jspRoot`), **여러 테이블 일괄 생성(`parseAll`/`generateAll`), GUI 설정 프로파일, 생성 미리보기·덮어쓰기 경고, FK·Y/N 드롭다운**.

---

## 부록: 빠른 수정→검증 루프

```powershell
# 1. CodeGenerator 등 수정
# 2. 빌드
powershell -ExecutionPolicy Bypass -File build.ps1
# 3. 생성 + 눈으로 확인
java -jar dist\egov-codegen-studio.jar --ddl sample\verify.sql --config gen.properties --idgnr
# 4. (필요시) 컴파일 검증 → 톰캣 1회 구동
# 5. 문서 동기화 후 feature 브랜치 커밋
```
> GUI로 확인하려면 `run-gui.ps1`(빌드 후), 배포본은 `package.ps1`(app-image) / `package.ps1 -Type exe`(인스톨러 — 7장). GUI가 떠 있으면 jar 잠금으로 빌드가 막히니 먼저 종료.

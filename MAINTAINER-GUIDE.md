# egov-crud-gen 관리자(유지보수) 매뉴얼

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
src/com/hanbit/egovgen/
├─ Main.java                  CLI 진입점. 인자 파싱(parseArgs), 파서 선택(selectParser), 실행 오케스트레이션
├─ config/GenConfig.java      설정 로드(properties) + CLI 덮어쓰기(override). 적응형 설정의 단일 출처
├─ model/
│  ├─ ColumnMeta.java         컬럼 1개 메타(컬럼명/필드명/자바타입/PK/코멘트/size) + label(), searchable()
│  └─ TableMeta.java          테이블 메타(엔티티명 + 컬럼 목록) + primaryKey(), searchableColumns()
├─ parser/
│  ├─ DdlParser.java          파서 인터페이스 (parse, dbType) — DB별 교체 지점
│  └─ MySqlDdlParser.java     MySQL CREATE TABLE 정규식 파서
└─ gen/
   ├─ NameUtil.java           snake↔camel↔Pascal, 테이블명→엔티티명
   ├─ TypeMapper.java         DDL 타입 → 자바 타입 매핑, size 추출
   └─ CodeGenerator.java      ★ 템플릿 + 파일 생성 (가장 자주 수정하는 곳)
```

### CodeGenerator 내부 구조 (가장 중요)
- `generate(TableMeta)` — 산출물 경로 계산 + 각 템플릿 메서드 호출 + 파일 쓰기. **산출물을 추가/제거하려면 여기.**
- `base(TableMeta)` — 모든 템플릿이 공유하는 치환 변수 맵(`LinkedHashMap`)을 만든다. **새 플레이스홀더는 여기에 추가.**
- `render(tpl, vars)` — `__KEY__`를 값으로 치환. **고정점까지 반복**하므로 값 안에 또 다른 `__KEY__`가 있어도 해결됨.
- 산출물별 메서드: `domainVo`, `searchVo`, `serviceInterface`, `serviceImpl`, `dao`, `controller`, `mapperXml`, `jspList`, `jspDetail`, `jspForm`, `idgnrBeanXml`.

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
1. `GenConfig`에 필드 + getter 추가.
2. `load()`에 `p.getProperty(...)` 한 줄, 필요하면 `override()`에 CLI 키 추가.
3. `Main`에서 `cfg.override("키", opt.get("키"))` (CLI로 받을 경우).
4. `CodeGenerator.base()` 또는 해당 템플릿에서 사용.
5. `gen.properties`와 `USER-GUIDE.md`에 항목 문서화.

### 3-4. 새 공통 컴포넌트 옵션 (검증/메시지/로깅 등)
채번(`useIdgnr`)이 모범 사례다. 동일 패턴:
- `GenConfig`에 `useXxx` 플래그
- `CodeGenerator`에 `xxxApplicable(t)` 판정 메서드
- 템플릿에서 조건부 블록을 `__XXX_IMPORT__`/`__XXX_FIELD__`/`__XXX_BODY__` 변수로 주입
- 필요하면 별도 산출물(빈 XML 등) 생성 — `idgnrBeanXml` 참고

### 3-5. 새 DB 파서 추가 (Oracle/PostgreSQL 등)
1. `DdlParser`를 구현하는 `OracleDdlParser` 작성(`dbType()`="oracle").
2. `Main.selectParser()`의 `switch`에 `case "oracle" -> new OracleDdlParser();`.
3. Mapper XML의 DB 종속 SQL(페이징 `LIMIT/OFFSET`, `SYSDATE()` 등)은 `CodeGenerator.mapperXml`에서 `dbType`별로 분기 필요. **현재 MySQL 전용이므로 여기 분기 추가가 핵심 작업.**

### 3-6. 새 화면 플랫폼 (eXBuilder/WebSquare)
현재 JSP만 직접 생성한다. 확장 시:
- 화면 생성을 인터페이스(`ViewGenerator`)로 분리하고 JSP 구현을 옮긴 뒤, 플랫폼별 구현 추가.
- 설정에 `viewType`(jsp/exbuilder/websquare) 추가, `generate()`에서 분기.

### 3-7. 채번 전략 조정
- prefix/자리수 규칙: `CodeGenerator.base()`의 `IDGNR_PREFIX`, `IDGNR_CIPERS` 계산.
  - **불변식**: `prefix길이 + cipers = PK컬럼 길이`. (eGov `EgovIdGnrStrategyImpl`의 `cipers`는 "숫자 자리수"이고 prefix는 별도)
- 정수 채번이 필요하면 `getNextIntegerId()` + strategy 없는 빈으로 분기(현재 미구현).

---

## 4. 빌드 · 검증 절차

### 빌드
```powershell
powershell -ExecutionPolicy Bypass -File build.ps1   # dist\egov-crud-gen.jar
```

### 생성 자바의 컴파일 검증 (회귀 테스트의 핵심)
생성 코드는 eGov 의존성이 필요하므로, 로컬 `.m2` 또는 대상 프로젝트의 의존성으로 컴파일해본다.
```powershell
# 1) 충돌 없는 패키지로 생성
java -jar dist\egov-crud-gen.jar --ddl sample\verify.sql `
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
| Mapper 스캔 | `mapper/let/**/*_${DbType}.xml` | `context-mapper.xml` |
| 컨텍스트 로딩 | `classpath*:egovframework/spring/com/context-*.xml` | `web.xml` |

> **다른 eGov 버전으로 이식 시**: 이 표의 값들을 대상 프로젝트에서 다시 확인하고, `gen.properties`의 `daoBase`/`serviceBase`/`paginationInfo` 및 `module` 규칙을 맞춰라. 이게 "프로젝트 적응형"의 실체다.

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

---

## 7. 버전 관리 / 릴리스

### 브랜치 전략
- `master` — 안정 버전. 직접 커밋 금지(가능하면).
- `feature/<요약>` — 기능/수정 단위 브랜치. 회귀 체크리스트 통과 후 `master`에 병합.

### 릴리스 절차
1. 회귀 체크리스트(4장) 통과 확인.
2. `USER-GUIDE.md`/`MAINTAINER-GUIDE.md`/`gen.properties` 문서 동기화.
3. 커밋 → 태그: `git tag -a v1.1 -m "채번 옵션 추가"` → (원격이 있으면) `git push --tags`.
4. 배포물 빌드: `build.ps1` → `dist/egov-crud-gen.jar`.
5. **폐쇄망 반입**: `dist/egov-crud-gen.jar` 1개 파일만 옮기면 됨(의존성 0). 또는 소스째 옮겨 현장에서 빌드.

> `dist/`·`build/`는 `.gitignore` 대상이다. 배포물(jar)은 git에 넣지 말고 빌드로 재생산하거나 사내 아티팩트 저장소에 보관.

### 변경 시 함께 갱신할 것
- 생성 산출물 구조가 바뀌면 → `USER-GUIDE.md` 6장, `SKELETON.md`
- 설정 항목이 바뀌면 → `gen.properties`, `USER-GUIDE.md` 3장
- eGov 좌표가 바뀌면 → 5장 표, `gen.properties` 기본값

---

## 8. 확장 로드맵 (PRD 2차/추후와 연계)

| 우선순위 | 항목 | 작업 위치 |
|---|---|---|
| 2차 | 재생성(round-trip) diff 전략 | `generate()` — 기존 파일 비교/패치 출력 |
| 2차 | 엑셀/CSV 일괄 입력 | 새 입력 어댑터 + `Main` (POI는 의존성 0 원칙과 충돌 → CSV 우선 검토) |
| 2차 | 다른 DB 파서 | 3-5 참고 |
| 추후 | 화면 플랫폼(eXBuilder/WebSquare) | 3-6 참고 |
| 추후 | AI 보조(라벨/검색조건 추론) | 사내 LLM CLI 연동 인터페이스 `LlmAssist` |
| 개선 | 검색 select 라벨 채우기, 시각/등록자 컬럼 `SYSDATE()` 자동 | `jspList`, `mapperXml` |

---

## 부록: 빠른 수정→검증 루프

```powershell
# 1. CodeGenerator 등 수정
# 2. 빌드
powershell -ExecutionPolicy Bypass -File build.ps1
# 3. 생성 + 눈으로 확인
java -jar dist\egov-crud-gen.jar --ddl sample\verify.sql --config gen.properties --idgnr
# 4. (필요시) 컴파일 검증 → 톰캣 1회 구동
# 5. 문서 동기화 후 feature 브랜치 커밋
```

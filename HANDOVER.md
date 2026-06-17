# 인수인계 문서 (HANDOVER)

> 이 프로젝트를 **처음 이어받는 사람**을 위한 종합 안내입니다. 이 문서 하나로 "무엇을·왜 만들었고, 어디까지 됐고, 어떻게 이어가는지"를 파악할 수 있게 썼습니다.
> 세부는 [README](./README.md) · [QUICKSTART](./QUICKSTART.md) · [USER-GUIDE](./USER-GUIDE.md) · [MAINTAINER-GUIDE](./MAINTAINER-GUIDE.md)를 참고하세요.
>
> **버전 노트**: 이 저장소는 **`egov-codegen-studio`** — CLI 전용 `egov-crud-gen`(백업)에서 분기한 **GUI 확장판**입니다. CLI(`run.ps1`)와 Swing GUI(`run-gui.ps1`)를 함께 제공하며, 생성 엔진(`GenerationService`)을 공유합니다.

---

## 1. 프로젝트 한 줄 요약

**eGov 표준프레임워크(검증: 5.0.1) 프로젝트에서, MySQL DDL 한 개를 넣으면 CRUD 풀세트(백엔드 6 + Mapper XML + JSP 4)를 자동 생성하는 CLI·GUI 도구.** 순수 Java, 외부 의존성 0.

- 저장소: https://github.com/Ohmneong/egov-codegen-studio (public, `main`) — egov-crud-gen 백업본에서 분기
- 언어/런타임: Java 17+ (검증은 번들 JDK 21)
- 빌드 도구: 없음(순수 `javac`/`jar`, `build.ps1` 래퍼)

## 2. 왜 만들었나 (배경)

- **문제**: eGov SI 프로젝트에서 테이블마다 VO/DAO/Service/Controller/Mapper/JSP를 손으로 찍고 공통 컴포넌트 규약(페이징·베이스클래스)까지 맞추는 반복 노동이 큼. 기존 방식은 "기존 코드 복붙 + 컬럼 치환"이라 누락·오타·동기화 문제.
- **배경 미션**: 조직의 "AI로 개발 생산성 향상". 단, **SI 폐쇄망**이라 외부 플러그인/인터넷 의존이 어려움 → 그래서 외부 의존성 0의 결정적(deterministic) 생성기로 설계. (AI는 보조 영역으로만 두는 방향)
- **사용자**: SI 실무 개발자(속도), 신규 투입 개발자(규약 학습). 팀이 공유하고 **프로젝트마다 들고 다니는** 도구를 지향 → "프로젝트 적응형 설정"이 핵심 차별점.

## 3. 현재 상태 (✅ 동작·검증 완료)

- ✅ MySQL `CREATE TABLE` 파싱 (컬럼명/타입/PK/NOT NULL/COMMENT)
- ✅ CRUD 풀세트 생성: VO, 검색VO, Service, ServiceImpl, DAO, Controller, Mapper XML, JSP(목록/상세/등록/수정)
- ✅ 공통 컴포넌트 규약: 페이징(`PaginationInfo`) + 베이스클래스 상속 항상 적용
- ✅ 채번(`EgovIdGnrService`) 옵션 (`--idgnr`, String PK 전제) + 채번 빈 XML 자동 생성
- ✅ 프로젝트 적응형 설정(`gen.properties`): 패키지/모듈/prefix/DB/공통컴포넌트 경로/baseUrl
- ✅ 생성 후 **접속 URL 자동 출력**
- ✅ **실제 검증 완료**: 컴파일(종료코드 0) + 실제 프로젝트(`bp.enter`) 톰캣 런타임에서 목록/상세/등록/채번까지 동작 확인
- ✅ **Swing GUI**(`run-gui.ps1`): DDL 입력·설정 폼·출력폴더 탐색기 선택·생성. CLI와 생성 엔진(`GenerationService`) 공유
- ✅ 감사 컬럼: eGov 표준명 + **관례명(`created_by/at`, `updated_by/at`)** 자동 인식(폼 입력 제외·시점 `SYSDATE()`)
- ✅ 검색 조건은 **PK 기준**으로 생성(이전 문자열 컬럼 전체에서 축소)
- ✅ 여러 줄에 걸친 컬럼 정의(`DEFAULT … ON UPDATE …`)도 파싱
- ⚙ (변경) 생성물 클래스명/파일명/URL에서 **`Egov` 접두어 제거** (eGov 라이브러리 클래스는 유지)
- ✅ **여러 테이블 일괄 생성**: DDL 하나에 `CREATE TABLE`이 여럿이면 각각 생성(`parseAll`/`generateAll`)
- ✅ **설정 프로파일(GUI)**: 프로파일 바(콤보 + 저장/불러오기)로 프로젝트별 `gen.properties`를 `profiles/`에 저장·전환
- ✅ **생성 미리보기 + 덮어쓰기 경고(GUI)**: `[미리보기]`로 생성 예정 파일·기존 덮어쓸 파일 확인, `[생성]` 시 기존 파일 있으면 확인 다이얼로그
- ✅ **FK·Y/N 드롭다운**: 여부 컬럼(`CHAR(1)`+`_AT`/`_YN`)은 등록/수정 폼에 Y/N `select`, FK 컬럼(`REFERENCES`)은 `select`(옵션 데이터는 수동 연동)

## 4. 지금까지 한 작업 (경과)

이 도구는 다음 순서로 만들어졌습니다(맥락 이해용):

1. **PRD 작성** — 문제·사용자·기능·환경·성공지표 정의. (PRD 원본은 작업 PC의 상위 폴더 `pjt/PRD-egov-crud-gen.md`에 있음. 이 저장소엔 없음)
2. **골격 분석** — 실제 `bp.enter`의 휴일관리(Restde) CRUD 한 세트를 분석해 "고정 보일러플레이트 vs 변수화 지점"을 도출. (`pjt/SKELETON.md`)
3. **생성기 구현** — 파서 → 메타모델 → 템플릿(text block) → CLI.
4. **컴파일 검증** — 실제 eGov 의존성(`.m2`)으로 생성 자바 컴파일 성공 확인.
5. **런타임 검증** — `bp.enter`에 배치 → 톰캣 기동 → 목록/상세 화면 확인.
6. **채번 확장** — `EgovIdGnrService` 옵션 추가, String PK·`cipers` 계산·빈 XML 자동 생성.
7. **문서화** — QUICKSTART/USER-GUIDE/MAINTAINER-GUIDE.
8. **배포** — GitHub 공개 저장소.

이후 **egov-codegen-studio**(GUI 확장판)로 분기해 다음을 진행했습니다:

9. **생성 엔진 분리** — `GenerationService`/`GenerationResult` 추출(CLI/GUI 공유, 콘솔 비의존). `Main`은 얇은 어댑터로 축소.
10. **Swing GUI** — `GenGuiApp`(`run-gui.ps1`): DDL 입력/파일 열기, 설정 폼, 출력폴더 `[찾아보기]` 탐색기, 채번 체크, 결과 표시.
11. **파서 보강** — 여러 줄에 걸친 컬럼 정의(`DEFAULT … ON UPDATE …`) 파싱.
12. **감사 컬럼 관례명** — `created_by/at`·`updated_by/at`도 자동 인식.
13. **검색 PK 기준** + **생성물 `Egov` 접두어 제거**(라이브러리 클래스는 유지).
14. **배포본** — jpackage app-image(JRE 내장 폴더) + `.exe` 인스톨러(WiX 3.x), `package.ps1 [-Type exe]`.
15. **경로 변수화** — `mapperRoot`/`jspRoot` 설정화(프로젝트별 스캔 경로 대응).
16. **문서 전체 최신화** — README/QUICKSTART/USER-GUIDE/MAINTAINER/HANDOVER/CLAUDE.
17. **여러 테이블 일괄 생성** — `parseAll`/`generateAll`, DDL 하나에 여러 `CREATE TABLE`.
18. **설정 프로파일(GUI)** — `profiles/` 저장·전환, `GenConfig.saveTo`.
19. **생성 미리보기 + 덮어쓰기 경고(GUI)** — `CodeGenerator.build`(경로/내용 분리)·`GenerationService.preview`/`PreviewEntry`.
20. **FK·Y/N 드롭다운** — `ColumnMeta.isYesNo()`/`fkTable`, 파서 `REFERENCES` 인식, `jspForm` `select` 분기.

커밋 히스토리(최근순):
```
2483cb6 feat: FK·Y/N 여부 컬럼을 등록/수정 폼 드롭다운으로 생성
67a4f45 feat: 생성 미리보기 + 덮어쓰기 경고
067bf9a feat: GUI 설정 프로파일 (프로젝트별 gen.properties 저장/전환)
285feb8 feat: 여러 테이블 일괄 생성 (DDL 안의 모든 CREATE TABLE)
c8e0c99 docs: 전체 문서 오늘 작업까지 최신화
aea07f8 feat: Mapper/JSP 출력 루트를 설정으로 변수화 (mapperRoot/jspRoot)
7530125 docs: SmartScreen(미서명 exe) 경고 우회 방법 정리
7d5ad40 docs: MAINTAINER-GUIDE 7장 배포/재배포 정리
3df992a feat: .exe 인스톨러 생성 지원 (package.ps1 -Type exe, WiX 3.x)
4c86c9e feat: jpackage 설치본(app-image) 생성 스크립트 추가
1351b50 Merge feature/swing-gui: Swing GUI + 파서/감사/검색/네이밍 개선
a432036 change: 생성물에서 'Egov' 접두어 제거 + 출력폴더 탐색기 선택 버튼
851465f change: 검색 조건을 PK 기준으로만 제한
748a25b feat: 관례적 영문 감사 컬럼명 자동 인식
078411a fix: 여러 줄에 걸친 컬럼 정의 파싱 누락 수정
6520dd0 feat: Swing GUI 추가 (CLI와 생성 엔진 공유)
65e7c34 refactor: 생성 핵심을 GenerationService로 추출
10ed0c5 chore: egov-crud-gen 복사본으로 egov-codegen-studio 시작
(이전 egov-crud-gen 시절: a4f2efc 접속 URL 자동 출력 … b52a5cc 초기 구현)
```

## 5. 아키텍처 / 소스 맵

```
DDL 텍스트 → [DdlParser] → TableMeta/ColumnMeta → [CodeGenerator + text block 템플릿] → 파일
```

```
src/dev/myoh/egovgen/
├─ Main.java                  CLI 진입점. 인자 파싱, 파서 선택, 실행 + 결과/URL 출력
├─ config/GenConfig.java      설정 로드(gen.properties) + CLI 덮어쓰기. 적응형 설정의 단일 출처
├─ model/
│  ├─ ColumnMeta.java         컬럼 메타 + label(), searchable(), 감사 판정, isYesNo()/fkTable(FK·드롭다운 판정)
│  └─ TableMeta.java          테이블 메타 + primaryKey(), searchableColumns()(현재 PK 기준)
├─ parser/
│  ├─ DdlParser.java          파서 인터페이스(parse/parseAll) — DB 교체 지점
│  └─ MySqlDdlParser.java     MySQL 정규식 파서. parseAll(여러 테이블), FK(REFERENCES) 인식
├─ gen/
│  ├─ NameUtil.java           snake↔camel↔Pascal, 테이블명→엔티티명
│  ├─ TypeMapper.java         DDL 타입 → Java 타입, size 추출
│  └─ CodeGenerator.java      ★ 템플릿 + 파일 생성. build(경로/내용 맵)·generate(쓰기) 분리, jspForm 드롭다운 분기
├─ service/                   GenerationService(generateAll·preview)·GenerationResult·PreviewEntry (CLI/GUI 공유, 콘솔 비의존)
└─ ui/                        GenGuiApp (Swing GUI 진입점: 프로파일 바·미리보기·드롭다운)
```

`CodeGenerator`가 핵심: `generate()`(산출물 목록), `base()`(공통 치환변수), `render()`(치환), 산출물별 메서드(`domainVo`/`serviceImpl`/`mapperXml`/`jspList` 등). 자세한 수정법은 **MAINTAINER-GUIDE 2·3장**.

## 6. 핵심 설계 결정과 이유 (⚠ 이어받기 전 꼭 읽기 — 함부로 뒤집지 말 것)

| 결정 | 이유 |
|---|---|
| **외부 의존성 0 (순수 Java)** | 폐쇄망 반입·빌드 단순화. Freemarker/POI 등 추가하면 의존성 관리 부담 → 추가 전 팀 합의 |
| **결정적 생성(LLM/난수 미사용)** | 균질성이 목적. 같은 입력→같은 출력 보장 |
| **text block 템플릿 + `__KEY__` 치환** | 외부 템플릿 엔진 없이 Java 21만으로 |
| **`render`는 고정점까지 반복** | 값 안에 중첩된 `__KEY__`(예: JSP의 module/entity)까지 풀기 위함. **1패스로 되돌리면 버그 재발** |
| **채번은 String PK 전제** | eGov 표준 채번이 `PREFIX_0000…` String. 정수 PK엔 `AUTO_INCREMENT`가 더 적합 |
| **`cipers = PK길이 − prefix길이`** | eGov `EgovIdGnrStrategyImpl`의 `cipers`는 "숫자 자리수", prefix는 별도. 둘의 합이 PK 컬럼 길이 |
| **Mapper/JSP 출력 루트는 설정(`mapperRoot`/`jspRoot`)** | eGov 스캔 경로가 프로젝트마다 달라 변수화(기본 `egovframework/mapper`·`WEB-INF/jsp`). `module`은 그 하위 세그먼트 — 표준 eGov면 `let/`로 시작 |
| **JSP taglib은 레거시 sun.com URI 유지** | eGov 5.0.1이 jakarta로 갔지만 JSP taglib은 `http://java.sun.com/jsp/jstl/core` 유지(실측). Controller/검증 import만 jakarta.* |

## 7. 검증된 eGov 환경 좌표 (5.0.1)

| 항목 | 값 |
|---|---|
| DAO 베이스 | `org.egovframe.rte.psl.dataaccess.EgovAbstractMapper` |
| ServiceImpl 베이스 | `org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl` |
| 페이징 | `org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo` |
| 채번 | `org.egovframe.rte.fdl.idgnr.*` + `IDS` 테이블 |
| Java / 네임스페이스 | 17 / **jakarta.*** |
| 컴포넌트 스캔 | `base-package="egovframework"` |
| Mapper 스캔 | `mapper/let/**/*_${DbType}.xml` |
| 컨텍스트 로딩 | `classpath*:egovframework/spring/com/context-*.xml` |
| 검증 프로젝트 | `bp.enter` (DB 스키마 `ebt`, MySQL) |

> 다른 eGov 버전으로 옮길 땐 이 표를 대상 프로젝트에서 재확인하고 `gen.properties`를 맞춘다. (MAINTAINER-GUIDE 5장)

## 8. 빌드 · 실행 · 검증 (빠른 시작)

```powershell
cd egov-codegen-studio
# 1) 빌드(최초 1회)
powershell -ExecutionPolicy Bypass -File .\build.ps1
# 2) 생성 (CLI)
.\run.ps1 --ddl sample\sample.sql --config gen.properties            # 기본
.\run.ps1 --ddl sample\verify.sql --config gen.properties --idgnr     # 채번
# 2') 또는 GUI로
.\run-gui.ps1
# 3) 결과: output\ 폴더 + 콘솔에 접속 URL 출력
# 4) 배포본
.\package.ps1            # app-image (JRE 내장 실행 폴더)
.\package.ps1 -Type exe  # .exe 인스톨러 (WiX 3.x 필요)
```
회귀 검증(수정 후 매번): MAINTAINER-GUIDE 4장 체크리스트.
> 빌드 시 GUI가 떠 있으면 `dist\*.jar`를 잡아 빌드(jar 교체)가 실패한다 — GUI(javaw)를 닫고 빌드할 것.

## 9. 남은 일 / 다음 단계 (우선순위 순)

| 우선순위 | 항목 | 메모 |
|---|---|---|
| 2차 | **재생성(round-trip) 전략** | 현재는 덮어쓰기. 손수정 코드 보존 위해 diff/패치 출력 필요. **도구 수명 직결** |
| 2차 | 엑셀/CSV 일괄 입력 | **DDL 안 여러 `CREATE TABLE`은 이미 지원**(`parseAll`). 엑셀/CSV 입력은 추후(POI는 의존성0 충돌 → CSV 우선) |
| 2차 | 다른 DB 파서(Oracle 등) | `DdlParser` 구현 + `mapperXml`의 DB별 SQL 분기(`LIMIT`/`SYSDATE()`) |
| 추후 | 화면 플랫폼(eXBuilder/WebSquare) | 화면 생성을 `ViewGenerator` 인터페이스로 분리 후 구현 추가 |
| 추후 | AI 보조 | 사내 폐쇄망 LLM(CLI) 연동, 컬럼 코멘트→라벨/검색조건 추론. `LlmAssist` 인터페이스 |
| 개선 | FK 옵션 데이터 자동 연동 | FK 컬럼은 `select` 골격까지 생성. 참조 테이블 목록 조회로 `option` 채우기는 수동(런타임 데이터) |
| 개선 | 등록자/수정자 ID 서버 연동 | 감사 시점은 `SYSDATE()` 자동·감사컬럼 폼 제외 완료. 등록자/수정자 ID만 Controller의 LoginVO 연동 남음 |
| 개선 | 배포본 코드 서명 | 미서명 `.exe`는 SmartScreen 경고. 사내 자체서명+신뢰 루트 또는 상용 인증서로 `signtool` 서명(`package.ps1`에 단계 추가 가능) |

> **이번에 처리(과거 "남은 일"에서 완료)**: Swing GUI, 감사 컬럼 관례명 인식, 검색 PK 기준, `Egov` 접두어 제거, `mapperRoot`/`jspRoot` 변수화, 설치본(app-image)/`.exe` 인스톨러, **여러 테이블 일괄 생성, 설정 프로파일(GUI), 생성 미리보기/덮어쓰기 경고, FK·Y/N 드롭다운**.

## 10. 이어받는 사람 체크리스트 (처음 할 일)

1. [ ] 저장소 clone, `build.ps1`로 빌드 성공 확인
2. [ ] `sample\sample.sql`로 생성 → `output\` 결과 눈으로 확인
3. [ ] (가능하면) 생성물을 eGov 프로젝트에 넣고 톰캣 1회 구동 — 전체 흐름 체감
4. [ ] **6장(설계 결정)** 정독 — 왜 이렇게 됐는지 이해하고 시작
5. [ ] MAINTAINER-GUIDE 2·3장으로 `CodeGenerator` 구조 파악
6. [ ] 9장 "남은 일"에서 다음 작업 고르기 → `feature/<요약>` 브랜치에서 작업
7. [ ] 회귀 체크리스트 통과 후 커밋, 문서 동기화

## 11. 문서 지도

| 문서 | 누구를 위해 |
|---|---|
| SESSION-GUIDE.md | 새 세션에서 이어 작업 — 시작 절차·현재 스냅샷·운영 함정 (진입점) |
| **HANDOVER.md** (이 문서) | 프로젝트를 이어받는 사람 — 전체 그림·경과·결정·다음 단계 |
| QUICKSTART.md | 처음 써보는 사람 — 복붙 따라하기 |
| USER-GUIDE.md | 사용자 — 옵션·설정·트러블슈팅 |
| MAINTAINER-GUIDE.md | 도구 수정·확장 담당 — 설계·수정법·릴리스 |
| README.md | 저장소 첫 화면 — 문서 인덱스 |

## 12. 알려진 함정 (실전 기록 — MAINTAINER-GUIDE 6장에 상세)

- `render` 1패스 금지(중첩 치환).
- javac `@argfile`의 `""` 안 경로는 `/`로(백슬래시 이스케이프).
- `CLASSPATH` 환경변수 길이 제한 → argfile.
- **WTP 배포 깨짐**: 이클립스 컴파일은 되는데 톰캣 배포본 `WEB-INF/classes`가 빔 → `mvn clean package`로 안 됨, **서버 Clean/모듈 재등록**이 답.
- `CHAR(1)` 입력 초과(`USE_AT`엔 `Y` 한 글자). 폼은 `maxlength`로 제한됨.
- 채번 시 PK는 `VARCHAR`, `IDS` 테이블 필요.
- **여러 줄 컬럼 정의**(`DEFAULT … ON UPDATE …`): `splitTopLevel` 조각의 공백을 정규화하지 않으면 그 컬럼이 통째로 누락됨(파서 정규식의 `.`이 줄바꿈 비매칭).
- **GUI가 jar를 잠금**: GUI(javaw)가 떠 있으면 `build.ps1`의 jar 교체가 실패 → GUI 종료 후 빌드.
- **미서명 `.exe`는 SmartScreen 차단**: "추가 정보→실행" / 속성 차단 해제 / USB 복사(MOTW 미부착)로 우회. 근본 해결은 코드 서명.

---

_최종 업데이트 기준 커밋: `2483cb6` (egov-codegen-studio). 이 문서는 프로젝트 상태가 크게 바뀌면 함께 갱신하세요._

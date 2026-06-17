# 인수인계 문서 (HANDOVER)

> 이 프로젝트를 **처음 이어받는 사람**을 위한 종합 안내입니다. 이 문서 하나로 "무엇을·왜 만들었고, 어디까지 됐고, 어떻게 이어가는지"를 파악할 수 있게 썼습니다.
> 세부는 [README](./README.md) · [QUICKSTART](./QUICKSTART.md) · [USER-GUIDE](./USER-GUIDE.md) · [MAINTAINER-GUIDE](./MAINTAINER-GUIDE.md)를 참고하세요.

---

## 1. 프로젝트 한 줄 요약

**eGov 표준프레임워크(검증: 5.0.1) 프로젝트에서, MySQL DDL 한 개를 넣으면 CRUD 풀세트(백엔드 6 + Mapper XML + JSP 4)를 자동 생성하는 CLI 도구.** 순수 Java, 외부 의존성 0.

- 저장소: https://github.com/Ohmneong/egov-crud-gen (public)
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

커밋 히스토리:
```
a4f2efc feat: 생성 완료 후 접속 URL 자동 출력 (baseUrl 설정 추가)
5b156fd docs: 입문자용 QUICKSTART + 실행 래퍼 run.ps1 추가
d90083e docs: build.ps1 실행 방법 명확화
b52a5cc feat: eGov 표준프레임워크 CRUD 코드 제너레이터 (초기 구현)
```

## 5. 아키텍처 / 소스 맵

```
DDL 텍스트 → [DdlParser] → TableMeta/ColumnMeta → [CodeGenerator + text block 템플릿] → 파일
```

```
src/com/hanbit/egovgen/
├─ Main.java                  CLI 진입점. 인자 파싱, 파서 선택, 실행 + 결과/URL 출력
├─ config/GenConfig.java      설정 로드(gen.properties) + CLI 덮어쓰기. 적응형 설정의 단일 출처
├─ model/
│  ├─ ColumnMeta.java         컬럼 메타 + label()(코멘트→라벨), searchable()
│  └─ TableMeta.java          테이블 메타 + primaryKey(), searchableColumns()
├─ parser/
│  ├─ DdlParser.java          파서 인터페이스 (DB 교체 지점)
│  └─ MySqlDdlParser.java     MySQL 정규식 파서
└─ gen/
   ├─ NameUtil.java           snake↔camel↔Pascal, 테이블명→엔티티명
   ├─ TypeMapper.java         DDL 타입 → Java 타입, size 추출
   └─ CodeGenerator.java      ★ 템플릿 + 파일 생성 (가장 자주 수정)
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
| **`module`은 `let/`로 시작** | eGov Mapper 스캔 경로가 `mapper/let/**`. 안 지키면 쿼리 로드 안 됨 |
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
cd egov-crud-gen
# 1) 빌드(최초 1회)
powershell -ExecutionPolicy Bypass -File .\build.ps1
# 2) 생성
.\run.ps1 --ddl sample\sample.sql --config gen.properties            # 기본
.\run.ps1 --ddl sample\verify.sql --config gen.properties --idgnr     # 채번
# 3) 결과: output\ 폴더 + 콘솔에 접속 URL 출력
```
회귀 검증(수정 후 매번): MAINTAINER-GUIDE 4장 체크리스트.

## 9. 남은 일 / 다음 단계 (우선순위 순)

| 우선순위 | 항목 | 메모 |
|---|---|---|
| 2차 | **재생성(round-trip) 전략** | 현재는 덮어쓰기. 손수정 코드 보존 위해 diff/패치 출력 필요. **도구 수명 직결** |
| 2차 | 엑셀/CSV 일괄 입력 | 여러 테이블 한 번에. POI는 의존성0 원칙과 충돌 → CSV 우선 검토 |
| 2차 | 다른 DB 파서(Oracle 등) | `DdlParser` 구현 + `mapperXml`의 DB별 SQL 분기(`LIMIT`/`SYSDATE()`) |
| 추후 | 화면 플랫폼(eXBuilder/WebSquare) | 화면 생성을 `ViewGenerator` 인터페이스로 분리 후 구현 추가 |
| 추후 | AI 보조 | 사내 폐쇄망 LLM(CLI) 연동, 컬럼 코멘트→라벨/검색조건 추론. `LlmAssist` 인터페이스 |
| 개선 | 검색 select 라벨 채우기 | 목록 화면 검색 select 옵션이 비어 있음 |
| 개선 | 등록자/수정자 ID 서버 연동 | 감사 시점(_PNTTM)은 SYSDATE() 자동·감사컬럼 폼 제외 완료. 등록자/수정자 ID만 Controller의 LoginVO 연동 남음 |

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

---

_최종 업데이트 기준 커밋: `a4f2efc`. 이 문서는 프로젝트 상태가 크게 바뀌면 함께 갱신하세요._

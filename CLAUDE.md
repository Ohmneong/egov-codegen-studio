# CLAUDE.md

이 파일은 Claude Code가 이 저장소에서 작업할 때 참고하는 지침입니다. 사람용 문서는 [HANDOVER](./HANDOVER.md) → [MAINTAINER-GUIDE](./MAINTAINER-GUIDE.md) 순으로 보세요.

## 프로젝트
eGov 표준프레임워크(검증 5.0.1) CRUD 코드 제너레이터. MySQL DDL 1개 → 백엔드 6 + Mapper XML + JSP 4 생성. **순수 Java, 외부 의존성 0.**

## 빌드 / 실행 / 검증
```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1          # 빌드 → dist\egov-crud-gen.jar
.\run.ps1 --ddl sample\sample.sql --config gen.properties      # 실행(기본)
.\run.ps1 --ddl sample\verify.sql --config gen.properties --idgnr   # 채번
```
- JDK는 번들(`build.ps1`/`run.ps1`이 `$jdkRoot`에서 자동 탐색). Maven 없음.
- 생성 자바 컴파일 검증은 `.m2` 의존성을 classpath로 사용(MAINTAINER-GUIDE 4장).

## 반드시 지킬 규칙 (어기면 회귀 발생)
1. **외부 의존성 추가 금지** — 순수 Java만. 새 라이브러리가 꼭 필요하면 사유·대안을 명시하고 합의 후.
2. **결정적 생성** — 골격 생성에 LLM/난수 쓰지 않는다(균질성).
3. **`CodeGenerator.render`는 고정점까지 반복** — 중첩 플레이스홀더(`__KEY__` 안의 `__KEY__`) 때문. 1패스로 되돌리지 말 것.
4. **`module`은 `let/`로 시작** — eGov Mapper 스캔 경로(`mapper/let/**`) 때문.
5. **채번(`--idgnr`)은 String PK 전제**, `cipers = PK길이 − prefix길이`.
6. **Controller/검증 import는 `jakarta.*`**, JSP taglib은 레거시 `http://java.sun.com/jsp/jstl/core` 유지.
7. 생성 코드 주석·식별자는 한글 주석 + eGov 네이밍 관례.

## 소스 구조
- `Main.java` — CLI 진입점(인자 파싱, 파서 선택, 실행, 결과/URL 출력)
- `config/GenConfig.java` — 설정(gen.properties) 로드 + CLI 덮어쓰기
- `model/` — `TableMeta`, `ColumnMeta` (중간 메타모델)
- `parser/` — `DdlParser`(인터페이스), `MySqlDdlParser`
- `gen/` — `NameUtil`, `TypeMapper`, **`CodeGenerator`(템플릿+생성, 가장 자주 수정)**

## 수정 시 워크플로
1. 수정 → `build.ps1` → `sample/*.sql`로 생성해 눈으로 확인
2. 생성 자바 컴파일 종료코드 0 확인 + 가능하면 톰캣 1회 구동
3. `USER-GUIDE`/`gen.properties` 등 문서 동기화
4. `feature/<요약>` 브랜치에서 작업 후 master 병합 (master 직접 커밋 지양)

## 작업할 거리 (우선순위)
재생성(round-trip) diff → 엑셀/CSV 입력 → 다른 DB 파서 → 화면 플랫폼(eXBuilder/WebSquare) → AI 보조. 상세는 HANDOVER 9장.

## 알려진 함정
`render` 1패스 금지 / javac argfile 경로는 `/` / CLASSPATH 길이 제한 → argfile / WTP 배포 깨짐은 서버 Clean / `CHAR(1)`엔 1글자 / 채번엔 `IDS` 테이블 필요. (MAINTAINER-GUIDE 6장)

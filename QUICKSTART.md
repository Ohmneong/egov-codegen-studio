# 빠른 시작 — 처음 쓰는 분용 (따라하기)

> 어려운 설명 없이 **순서대로 복붙**만 하면 됩니다. 더 자세한 내용은 [USER-GUIDE.md](./USER-GUIDE.md), 도구를 고치려면 [MAINTAINER-GUIDE.md](./MAINTAINER-GUIDE.md).

이 도구는 **테이블 정의(DDL)를 넣으면 → eGov CRUD 코드(자바·화면)를 자동으로 만들어** 줍니다.

---

## STEP 1. 최초 1회만 — 빌드

PowerShell 창을 열고, 도구 폴더로 이동한 뒤 빌드합니다.

```powershell
cd C:\Users\ADMIN\Desktop\pjt\egov-crud-gen
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

`빌드 완료: ...dist\egov-crud-gen.jar` 가 보이면 성공입니다. (이 단계는 처음 한 번만 하면 됩니다.)

---

## STEP 2. DDL 파일 만들기

**DDL이란?** 테이블을 만드는 SQL문(`CREATE TABLE ...`)이에요. 평소 DB에 테이블 만들 때 쓰는 그 문장입니다.

1. 메모장(또는 VS Code)을 엽니다.
2. 아래처럼 만들고 싶은 테이블을 적습니다. (컬럼 뒤 `COMMENT '...'` 가 화면 라벨로 쓰입니다)

   ```sql
   CREATE TABLE `LETTN_NOTICE` (
       `NOTICE_ID` VARCHAR(20)  NOT NULL COMMENT '공지ID',
       `TITLE`     VARCHAR(200) NOT NULL COMMENT '제목',
       `CONTENT`   VARCHAR(2000)         COMMENT '내용',
       `USE_AT`    CHAR(1)      DEFAULT 'Y' COMMENT '사용여부',
       PRIMARY KEY (`NOTICE_ID`)
   );
   ```

3. **어디에 저장하나요?** 도구 폴더 안 `sample` 폴더에 저장하면 편합니다.
   - 저장 위치: `C:\Users\ADMIN\Desktop\pjt\egov-crud-gen\sample\`
   - 파일 이름 예: `notice.sql`  (메모장 "다른 이름으로 저장" → 파일 형식 "모든 파일" → `notice.sql`)

> 채번(번호 자동 생성)을 쓸 거면 PK(여기선 `NOTICE_ID`)를 꼭 **`VARCHAR`** 로 만드세요.

---

## STEP 3. 설정 한 번 맞추기 (gen.properties)

도구 폴더의 `gen.properties` 파일을 메모장으로 열고, **이 3줄만** 내 프로젝트에 맞게 고칩니다.

```properties
basePackage=egovframework.let.cop.notice    # 코드가 들어갈 패키지
module=let/cop/notice                         # 화면/URL/쿼리 경로 (반드시 let/ 으로 시작!)
tablePrefix=LETTN_                            # 테이블 이름 앞에 떼어낼 부분 (없으면 비워두기)
```

> `module`은 꼭 **`let/`** 으로 시작해야 합니다. 안 그러면 쿼리(XML)를 프로그램이 못 찾아요.
> 채번을 쓰려면 맨 아래 `useIdgnr=true` 로 바꾸세요.

---

## STEP 4. 실행 (코드 생성)

PowerShell에서 (STEP 1의 그 창, 도구 폴더 안):

```powershell
.\run.ps1 --ddl sample\notice.sql --config gen.properties
```

채번까지 쓰려면 뒤에 `--idgnr` 만 붙이면 됩니다:

```powershell
.\run.ps1 --ddl sample\notice.sql --config gen.properties --idgnr
```

`[생성 완료] 11개 파일 → ...` 이 보이면 성공이에요.

그리고 **맨 끝에 접속 URL이 바로 출력**됩니다 — 톰캣 띄운 뒤 이 주소로 들어가면 화면이 떠요:
```
[접속 URL] 톰캣 기동 후 브라우저에서:
  목록  http://localhost:8080/let/cop/notice/EgovNoticeList.do
  등록  http://localhost:8080/let/cop/notice/EgovNoticeRegistView.do
```
> 주소 앞부분(포트/컨텍스트)은 `gen.properties`의 `baseUrl` 값으로 맞춰집니다. 내 톰캣에 맞게 바꿔두면 매번 정확한 주소가 나와요.

---

## STEP 5. 결과 확인

생성된 파일은 도구 폴더 안 **`output`** 폴더에 들어갑니다.

```
egov-crud-gen\output\src\main\java\...      ← 자바 (VO/Service/DAO/Controller)
egov-crud-gen\output\src\main\resources\... ← 쿼리(Mapper XML)
egov-crud-gen\output\src\main\webapp\...     ← 화면(JSP)
```

폴더를 열어 파일이 잘 만들어졌는지 눈으로 확인하세요.

---

## STEP 6. 내 프로젝트에 넣기

두 가지 방법이 있어요.

**(쉬운 방법) `output` 안의 `src` 폴더 내용을 내 프로젝트 `src` 위에 그대로 복사**
- `output\src\*` 를 `내프로젝트\src\` 에 덮어 넣으면 됩니다. (폴더 구조가 똑같이 맞춰져 있어요)

**(한 번에) 처음부터 내 프로젝트에 바로 만들기** — `--out` 뒤에 프로젝트 경로
```powershell
.\run.ps1 --ddl sample\notice.sql --config gen.properties --idgnr `
          --out "C:\eGovFrameDev-5.0.1-Windows-64bit\workspace-egov\bp.enter"
```

그 다음 **이클립스에서 프로젝트 새로고침(F5) → 톰캣 재시작** 하면 화면이 뜹니다.
접속 주소: `http://localhost:포트/.../let/cop/notice/EgovNoticeList.do`

---

## 막히면?

| 이럴 때 | 이렇게 |
|---|---|
| `-ExecutionPolicy ... 인식되지 않습니다` | 앞에 `powershell` 빼먹은 것. `powershell -ExecutionPolicy Bypass -File .\build.ps1` 통째로 입력 |
| `dist\egov-crud-gen.jar 가 없습니다` | STEP 1 빌드를 안 한 것. 빌드 먼저 |
| `Data too long for column` | 입력값이 컬럼 길이보다 김. 특히 `사용여부`엔 `Y` 한 글자만 |
| 화면은 뜨는데 저장이 안 됨 | DB에 그 테이블을 진짜로 만들었는지 확인 (DDL을 DB에서도 실행) |
| 채번 등록 오류 | DB에 `IDS` 테이블이 있어야 함 ([USER-GUIDE](./USER-GUIDE.md) 7장) |

그래도 막히면 화면에 뜬 **에러 메시지를 그대로 복사**해서 물어보세요.

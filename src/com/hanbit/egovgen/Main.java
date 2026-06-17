package com.hanbit.egovgen;

import com.hanbit.egovgen.config.GenConfig;
import com.hanbit.egovgen.model.TableMeta;
import com.hanbit.egovgen.service.GenerationResult;
import com.hanbit.egovgen.service.GenerationService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * eGov CRUD 코드 제너레이터 CLI 진입점.
 *
 * 인자 파싱 → 설정 로드 → DDL 파일 읽기까지가 여기 책임이고, 실제 생성은
 * {@link GenerationService}가 한다(GUI와 공유). 이 클래스는 그 결과를 콘솔에 출력한다.
 *
 * 사용법:
 *   java -jar egov-crud-gen.jar --ddl sample.sql [옵션]
 * 옵션:
 *   --ddl &lt;파일&gt;       (필수) MySQL CREATE TABLE DDL 파일
 *   --config &lt;파일&gt;    프로젝트 설정 properties (적응형 설정)
 *   --package &lt;pkg&gt;    루트 패키지 (config 덮어쓰기)
 *   --module &lt;경로&gt;     모듈 경로 (예: sym/cal)
 *   --prefix &lt;prefix&gt;  테이블 prefix (엔티티명 도출 시 제거)
 *   --out &lt;디렉터리&gt;    출력 루트 (기본 ./output)
 */
public class Main {

    public static void main(String[] args) {
        try {
            Map<String, String> opt = parseArgs(args);
            if (!opt.containsKey("ddl")) {
                printUsage();
                System.exit(1);
                return;
            }

            // 설정 로드 + CLI 덮어쓰기
            Path configPath = opt.containsKey("config") ? Path.of(opt.get("config")) : null;
            GenConfig cfg = GenConfig.load(configPath);
            cfg.override("package", opt.get("package"));
            cfg.override("module", opt.get("module"));
            cfg.override("prefix", opt.get("prefix"));
            cfg.override("out", opt.get("out"));
            cfg.override("dbType", opt.get("dbType"));
            cfg.override("idgnr", opt.get("idgnr"));
            cfg.override("baseUrl", opt.get("baseUrl"));
            cfg.override("mapperRoot", opt.get("mapperRoot"));
            cfg.override("jspRoot", opt.get("jspRoot"));

            // DDL 읽기
            Path ddlFile = Path.of(opt.get("ddl"));
            if (!Files.exists(ddlFile)) {
                System.err.println("[오류] DDL 파일을 찾을 수 없습니다: " + ddlFile.toAbsolutePath());
                System.exit(2);
                return;
            }
            String ddl = Files.readString(ddlFile);

            // 생성 (파서 선택·파싱·생성은 GUI와 공유하는 서비스가 담당). DDL에 여러 테이블이면 모두 생성.
            List<GenerationResult> results = new GenerationService().generateAll(cfg, ddl);

            if (results.size() > 1) {
                System.out.println("[테이블] " + results.size() + "개 일괄 생성");
                System.out.println();
            }
            for (GenerationResult result : results) {
                TableMeta table = result.table();
                System.out.println("[파싱] 테이블 " + table.getTableName()
                        + " → 엔티티 " + table.getEntityName()
                        + " (컬럼 " + table.getColumns().size() + "개, PK="
                        + (table.primaryKey() != null ? table.primaryKey().getColumnName() : "없음") + ")");

                System.out.println("[생성 완료] " + result.files().size() + "개 파일 → " + result.outputDir());
                for (Path f : result.files()) System.out.println("  - " + f);

                // 접속 URL 안내 (톰캣 기동 후 바로 쓸 수 있게)
                System.out.println();
                System.out.println("[접속 URL] 톰캣 기동 후 브라우저에서 (포트/컨텍스트는 환경에 맞게):");
                System.out.println("  목록  " + result.listUrl());
                System.out.println("  등록  " + result.registUrl());
                System.out.println();
            }

        } catch (IllegalArgumentException e) {
            System.err.println("[입력 오류] " + e.getMessage());
            System.exit(3);
        } catch (Exception e) {
            System.err.println("[실행 오류] " + e.getMessage());
            e.printStackTrace();
            System.exit(9);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                String key = a.substring(2);
                String val = (i + 1 < args.length && !args[i + 1].startsWith("--")) ? args[++i] : "true";
                m.put(key, val);
            }
        }
        return m;
    }

    private static void printUsage() {
        System.out.println("""
                eGov CRUD 코드 제너레이터

                사용법:
                  java -jar egov-crud-gen.jar --ddl <DDL파일> [옵션]

                옵션:
                  --ddl <파일>       (필수) MySQL CREATE TABLE DDL 파일
                  --config <파일>    프로젝트 설정 properties
                  --package <pkg>    루트 패키지 (예: egovframework.let.sym.cal)
                  --module <경로>    모듈 경로 (예: sym/cal)
                  --prefix <prefix>  테이블 prefix (엔티티명 도출 시 제거)
                  --out <디렉터리>   출력 루트 (기본 ./output)

                예:
                  java -jar egov-crud-gen.jar --ddl sample/sample.sql --config gen.properties
                """);
    }
}

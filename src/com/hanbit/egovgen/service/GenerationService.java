package com.hanbit.egovgen.service;

import com.hanbit.egovgen.config.GenConfig;
import com.hanbit.egovgen.gen.CodeGenerator;
import com.hanbit.egovgen.model.TableMeta;
import com.hanbit.egovgen.parser.DdlParser;
import com.hanbit.egovgen.parser.MySqlDdlParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD 생성 파이프라인의 핵심. CLI(Main)와 GUI가 함께 호출한다.
 *
 * 왜 분리: 기존엔 Main이 파싱·생성·콘솔출력을 한 덩어리로 했다. 콘솔(System.out/exit)에
 * 묶여 있으면 GUI가 같은 로직을 재사용할 수 없으므로, 입력은 DDL "텍스트"(파일 아님)로
 * 받고 출력은 콘솔 비의존 결과 객체로 돌려준다. 출력 포맷은 각 진입점이 책임진다.
 */
public class GenerationService {

    /**
     * 설정 + DDL 텍스트로 CRUD 풀세트를 생성한다.
     *
     * @param cfg 프로젝트 적응형 설정(파일/폼 어느 쪽에서 왔든 무관)
     * @param ddl MySQL CREATE TABLE DDL 원문
     * @return 생성 결과(파싱 메타·파일 목록·접속 URL)
     * @throws IOException 산출물 파일 쓰기 실패 시
     */
    public GenerationResult generate(GenConfig cfg, String ddl) throws IOException {
        // 단일 호환용 — 첫 테이블만 반환
        return generateAll(cfg, ddl).get(0);
    }

    /**
     * DDL 안의 <b>모든</b> 테이블을 생성한다(여러 CREATE TABLE 일괄 처리).
     * @return 테이블별 생성 결과 목록(입력 순서)
     */
    public List<GenerationResult> generateAll(GenConfig cfg, String ddl) throws IOException {
        DdlParser parser = selectParser(cfg.dbType());
        List<TableMeta> tables = parser.parseAll(ddl, cfg.tablePrefix());

        CodeGenerator gen = new CodeGenerator(cfg);
        Path outputDir = Path.of(cfg.outputDir()).toAbsolutePath();
        List<GenerationResult> results = new ArrayList<>();
        for (TableMeta table : tables) {
            List<Path> files = gen.generate(table);
            String urlBase = cfg.baseUrl() + "/" + cfg.module() + "/" + table.getEntityName();
            results.add(new GenerationResult(
                    table, files, outputDir,
                    urlBase + "List.do",
                    urlBase + "RegistView.do"));
        }
        return results;
    }

    /**
     * 생성하지 않고, 생성될 파일 목록과 기존 존재 여부만 산출한다(미리보기/덮어쓰기 경고용).
     */
    public List<PreviewEntry> preview(GenConfig cfg, String ddl) {
        DdlParser parser = selectParser(cfg.dbType());
        List<TableMeta> tables = parser.parseAll(ddl, cfg.tablePrefix());
        CodeGenerator gen = new CodeGenerator(cfg);
        List<PreviewEntry> entries = new ArrayList<>();
        for (TableMeta t : tables) {
            for (Path p : gen.build(t).keySet()) {
                entries.add(new PreviewEntry(p, Files.exists(p)));
            }
        }
        return entries;
    }

    /** 파서 선택 (DB 교체 지점). 1차는 mysql만 지원. */
    public static DdlParser selectParser(String dbType) {
        return switch (dbType == null ? "mysql" : dbType.toLowerCase()) {
            case "mysql" -> new MySqlDdlParser();
            default -> throw new IllegalArgumentException(
                    "현재 지원하지 않는 DB 타입입니다: " + dbType + " (1차는 mysql만 지원)");
        };
    }
}

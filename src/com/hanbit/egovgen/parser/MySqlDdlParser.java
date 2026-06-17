package com.hanbit.egovgen.parser;

import com.hanbit.egovgen.gen.NameUtil;
import com.hanbit.egovgen.gen.TypeMapper;
import com.hanbit.egovgen.model.ColumnMeta;
import com.hanbit.egovgen.model.TableMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQL CREATE TABLE 문 파서 (정규식 기반).
 * 지원: 백틱 식별자, 타입(size), NOT NULL, COMMENT '...', PRIMARY KEY (...), 여러 줄 컬럼 정의,
 *      한 DDL 안의 여러 CREATE TABLE(일괄 생성).
 * 미지원(1차): FK/INDEX/복합 제약 상세 — 파싱 시 무시한다.
 */
public class MySqlDdlParser implements DdlParser {

    private static final Pattern TABLE =
            Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?([A-Za-z0-9_]+)`?\\s*\\(",
                    Pattern.CASE_INSENSITIVE);

    // 컬럼 라인: `COL` TYPE(size) ... [COMMENT '...']
    private static final Pattern COLUMN =
            Pattern.compile("^`?([A-Za-z0-9_]+)`?\\s+([A-Za-z]+(?:\\s*\\([^)]*\\))?)(.*)$");

    private static final Pattern COMMENT =
            Pattern.compile("COMMENT\\s+'((?:[^'\\\\]|\\\\.)*)'", Pattern.CASE_INSENSITIVE);

    private static final Pattern PK =
            Pattern.compile("PRIMARY\\s+KEY\\s*\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);

    // 테이블 레벨 FK: FOREIGN KEY (col) REFERENCES reftbl
    private static final Pattern FK =
            Pattern.compile("FOREIGN\\s+KEY\\s*\\(\\s*`?([A-Za-z0-9_]+)`?\\s*\\)\\s+REFERENCES\\s+`?([A-Za-z0-9_]+)`?",
                    Pattern.CASE_INSENSITIVE);
    // 인라인 FK: <컬럼정의> ... REFERENCES reftbl
    private static final Pattern INLINE_REF =
            Pattern.compile("REFERENCES\\s+`?([A-Za-z0-9_]+)`?", Pattern.CASE_INSENSITIVE);

    @Override
    public String dbType() { return "mysql"; }

    @Override
    public TableMeta parse(String ddl, String tablePrefix) {
        // 첫 번째 테이블만 (parseAll 이 비면 예외를 던진다)
        return parseAll(ddl, tablePrefix).get(0);
    }

    @Override
    public List<TableMeta> parseAll(String ddl, String tablePrefix) {
        List<TableMeta> tables = new ArrayList<>();
        Matcher tm = TABLE.matcher(ddl);
        while (tm.find()) {
            String tableName = tm.group(1);
            // 테이블명 다음 '(' 부터 짝이 맞는 ')' 까지가 본문
            int bodyStart = ddl.indexOf('(', tm.end() - 1);
            int bodyEnd = lastMatchingParen(ddl, bodyStart);
            if (bodyStart < 0 || bodyEnd < 0) continue;
            String body = ddl.substring(bodyStart + 1, bodyEnd);
            TableMeta table = parseBody(tableName, body, tablePrefix);
            if (!table.getColumns().isEmpty()) tables.add(table);
        }
        if (tables.isEmpty()) {
            throw new IllegalArgumentException("CREATE TABLE 문을 찾을 수 없습니다. DDL을 확인하세요.");
        }
        return tables;
    }

    /** 테이블 본문(괄호 안)을 파싱해 TableMeta 한 개로 변환. */
    private TableMeta parseBody(String tableName, String body, String tablePrefix) {
        TableMeta table = new TableMeta();
        table.setTableName(tableName);
        table.setEntityName(NameUtil.entityFromTable(tableName, tablePrefix));

        // PK 컬럼명 먼저 수집
        String pkColumn = null;
        Matcher pkm = PK.matcher(body);
        if (pkm.find()) {
            pkColumn = pkm.group(1).split(",")[0].trim().replace("`", "");
        }

        // FK 컬럼 → 참조 테이블 (테이블 레벨 FOREIGN KEY ... REFERENCES)
        Map<String, String> fkMap = new HashMap<>();
        Matcher fkm = FK.matcher(body);
        while (fkm.find()) fkMap.put(fkm.group(1).toUpperCase(), fkm.group(2));

        // 컬럼 정의를 최상위 콤마 기준으로 분리
        for (String raw : splitTopLevel(body)) {
            // 컬럼 정의가 여러 줄에 걸칠 수 있어(예: DEFAULT ... ON UPDATE ...) 공백(줄바꿈 포함)을
            // 한 칸으로 정규화한다. 안 하면 컬럼 정규식의 '.'(줄바꿈 비매칭)이 라인을 놓쳐 컬럼이 누락됨.
            String line = raw.trim().replaceAll("\\s+", " ");
            if (line.isEmpty()) continue;
            String upper = line.toUpperCase();
            // 제약 라인은 건너뜀 (PRIMARY/FOREIGN/UNIQUE/KEY/INDEX/CONSTRAINT)
            if (upper.startsWith("PRIMARY ") || upper.startsWith("FOREIGN ")
                    || upper.startsWith("UNIQUE ") || upper.startsWith("KEY ")
                    || upper.startsWith("INDEX ") || upper.startsWith("CONSTRAINT ")) {
                continue;
            }
            Matcher cm = COLUMN.matcher(line);
            if (!cm.find()) continue;

            String colName = cm.group(1);
            String rawType = cm.group(2).trim();
            String rest = cm.group(3) == null ? "" : cm.group(3);

            ColumnMeta col = new ColumnMeta();
            col.setColumnName(colName);
            col.setFieldName(NameUtil.toCamel(colName));
            col.setJdbcType(rawType);
            col.setJavaType(TypeMapper.toJava(rawType));
            col.setSize(TypeMapper.extractSize(rawType));
            col.setNotNull(rest.toUpperCase().contains("NOT NULL"));
            col.setPrimaryKey(colName.equalsIgnoreCase(pkColumn)
                    || rest.toUpperCase().contains("PRIMARY KEY"));

            Matcher com = COMMENT.matcher(rest);
            if (com.find()) col.setComment(com.group(1).replace("\\'", "'"));

            // FK: 테이블 레벨 매핑 우선, 없으면 컬럼 정의의 인라인 REFERENCES
            String fk = fkMap.get(colName.toUpperCase());
            if (fk == null) {
                Matcher ir = INLINE_REF.matcher(rest);
                if (ir.find()) fk = ir.group(1);
            }
            if (fk != null) col.setFkTable(fk);

            table.addColumn(col);
        }
        return table;
    }

    /** bodyStart 위치의 '(' 와 짝이 맞는 ')' 인덱스. */
    private static int lastMatchingParen(String s, int open) {
        if (open < 0 || open >= s.length() || s.charAt(open) != '(') return -1;
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    /** 괄호 깊이를 고려해 최상위 콤마로만 분리 (decimal(10,2) 안의 콤마 무시). */
    private static java.util.List<String> splitTopLevel(String body) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(body.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(body.substring(start));
        return parts;
    }
}

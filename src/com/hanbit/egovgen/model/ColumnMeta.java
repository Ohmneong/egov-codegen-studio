package com.hanbit.egovgen.model;

/**
 * 테이블 컬럼 한 개의 메타정보.
 * DDL 파싱 결과를 코드 생성 템플릿이 소비하는 중간 모델이다.
 */
public class ColumnMeta {

    private String columnName;   // 원본 DB 컬럼명 (예: RESTDE_DE)
    private String fieldName;    // camelCase 자바 필드명 (예: restdeDe)
    private String javaType;     // 매핑된 자바 타입 (예: String, int, long, java.math.BigDecimal)
    private String jdbcType;     // 원본 DDL 타입 (예: VARCHAR, INT) — 참고용
    private int size;            // 길이/정밀도 (없으면 0)
    private boolean notNull;     // NOT NULL 여부
    private boolean primaryKey;  // PK 여부
    private String comment;      // 컬럼 코멘트 → 한글 라벨로 사용
    private String fkTable;      // FK 참조 테이블 (REFERENCES 로 파싱, 없으면 null)

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getJavaType() { return javaType; }
    public void setJavaType(String javaType) { this.javaType = javaType; }

    public String getJdbcType() { return jdbcType; }
    public void setJdbcType(String jdbcType) { this.jdbcType = jdbcType; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public boolean isNotNull() { return notNull; }
    public void setNotNull(boolean notNull) { this.notNull = notNull; }

    public boolean isPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    /** 화면 라벨: 코멘트가 있으면 코멘트, 없으면 필드명. */
    public String label() {
        return (comment != null && !comment.isBlank()) ? comment.trim() : fieldName;
    }

    /** 검색 후보 여부: 문자열 타입이면서 PK가 아닌 컬럼만 검색 대상으로 본다. */
    public boolean searchable() {
        return "String".equals(javaType) && !primaryKey;
    }

    /**
     * 감사 컬럼 여부(등록자/등록시점/수정자/수정시점).
     * 사용자가 화면에서 입력하는 값이 아니라 시스템이 채우는 컬럼이다.
     * eGov 표준 컬럼명과 더불어 관례적 영문 컬럼명(created_by/at, updated_by/at)도 인식한다.
     */
    public boolean isAudit() {
        String c = upperColumn();
        if (c == null) return false;
        return switch (c) {
            // eGov 표준 감사 컬럼
            case "FRST_REGISTER_ID", "FRST_REGIST_PNTTM", "LAST_UPDUSR_ID", "LAST_UPDT_PNTTM",
            // 관례적 영문 감사 컬럼명 (대소문자 무관)
                 "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT" -> true;
            default -> false;
        };
    }

    /**
     * 감사 컬럼 중 시점(일시) 컬럼 — INSERT 시 SYSDATE()로 자동 입력.
     * (등록시점·수정시점 모두 신규 등록 시에는 현재시각으로 채운다.)
     */
    public boolean isAuditTimestamp() {
        String c = upperColumn();
        if (c == null) return false;
        return switch (c) {
            case "FRST_REGIST_PNTTM", "LAST_UPDT_PNTTM", "CREATED_AT", "UPDATED_AT" -> true;
            default -> false;
        };
    }

    /** 감사 시점 중 '수정시점' — UPDATE 시에도 SYSDATE()로 갱신한다(등록시점은 INSERT에서만). */
    public boolean isUpdateTimestamp() {
        String c = upperColumn();
        if (c == null) return false;
        return c.equals("LAST_UPDT_PNTTM") || c.equals("UPDATED_AT");
    }

    /** Y/N 여부 컬럼(드롭다운 대상). CHAR(1) 성격이고 이름이 _AT/_YN 으로 끝난다(USE_AT, use_yn 등). */
    public boolean isYesNo() {
        if (columnName == null || size != 1 || primaryKey) return false;
        String c = upperColumn();
        return c.endsWith("_AT") || c.endsWith("_YN");
    }

    /** FK 참조 테이블(없으면 null). 있으면 등록/수정 폼을 드롭다운으로 만든다. */
    public String getFkTable() { return fkTable; }
    public void setFkTable(String fkTable) { this.fkTable = fkTable; }
    public boolean isForeignKey() { return fkTable != null && !fkTable.isBlank(); }

    private String upperColumn() {
        return columnName == null ? null : columnName.toUpperCase();
    }
}

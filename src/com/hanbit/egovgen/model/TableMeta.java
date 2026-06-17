package com.hanbit.egovgen.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 테이블 한 개의 메타정보 + 코드 생성에 필요한 파생 이름들.
 */
public class TableMeta {

    private String tableName;            // 원본 테이블명 (예: LETTNRESTDE)
    private String entityName;           // 엔티티/VO 클래스명 (PascalCase, 예: Restde)
    private final List<ColumnMeta> columns = new ArrayList<>();

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public List<ColumnMeta> getColumns() { return columns; }
    public void addColumn(ColumnMeta c) { columns.add(c); }

    /** PK 컬럼(첫 번째 PK). 없으면 null. */
    public ColumnMeta primaryKey() {
        return columns.stream().filter(ColumnMeta::isPrimaryKey).findFirst().orElse(null);
    }

    /**
     * 검색 후보 컬럼 목록. 현재 정책: <b>PK 기준으로만</b> 검색한다.
     * (이전의 "문자열 컬럼 전체" 정책은 {@link ColumnMeta#searchable()}에 남겨두었다 —
     *  검색 범위를 넓히려면 아래 필터를 searchable로 되돌리면 된다.)
     */
    public List<ColumnMeta> searchableColumns() {
        return columns.stream().filter(ColumnMeta::isPrimaryKey).toList();
    }
}

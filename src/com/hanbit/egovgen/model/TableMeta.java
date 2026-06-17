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

    /** 검색 후보 컬럼 목록. */
    public List<ColumnMeta> searchableColumns() {
        return columns.stream().filter(ColumnMeta::searchable).toList();
    }
}

package dev.myoh.egovgen.parser;

import dev.myoh.egovgen.model.TableMeta;

import java.util.List;

/**
 * DDL 파서 인터페이스. DB 종류별 구현을 갈아끼울 수 있도록 추상화한다.
 * 1차는 {@link MySqlDdlParser}만 제공.
 */
public interface DdlParser {

    /**
     * CREATE TABLE DDL 텍스트의 <b>첫 번째</b> 테이블을 파싱해 TableMeta로 변환.
     * @param ddl CREATE TABLE 문 (세미콜론 포함 가능)
     * @param tablePrefix 엔티티명 도출 시 제거할 테이블 prefix (없으면 null/빈문자열)
     */
    TableMeta parse(String ddl, String tablePrefix);

    /**
     * DDL 텍스트의 <b>모든</b> CREATE TABLE 을 파싱한다(여러 테이블 일괄 생성용).
     * 기본 구현은 단일 {@link #parse}를 감싼다 — 다중 지원 파서는 override 한다.
     */
    default List<TableMeta> parseAll(String ddl, String tablePrefix) {
        return List.of(parse(ddl, tablePrefix));
    }

    /** 이 파서가 지원하는 DB 타입 식별자 (예: "mysql"). */
    String dbType();
}

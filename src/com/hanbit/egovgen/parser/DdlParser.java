package com.hanbit.egovgen.parser;

import com.hanbit.egovgen.model.TableMeta;

/**
 * DDL 파서 인터페이스. DB 종류별 구현을 갈아끼울 수 있도록 추상화한다.
 * 1차는 {@link MySqlDdlParser}만 제공.
 */
public interface DdlParser {

    /**
     * CREATE TABLE DDL 텍스트 한 개를 파싱해 TableMeta로 변환.
     * @param ddl 단일 CREATE TABLE 문 (세미콜론 포함 가능)
     * @param tablePrefix 엔티티명 도출 시 제거할 테이블 prefix (없으면 null/빈문자열)
     */
    TableMeta parse(String ddl, String tablePrefix);

    /** 이 파서가 지원하는 DB 타입 식별자 (예: "mysql"). */
    String dbType();
}

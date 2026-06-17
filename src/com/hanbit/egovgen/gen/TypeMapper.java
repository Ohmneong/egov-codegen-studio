package com.hanbit.egovgen.gen;

import java.util.Locale;

/**
 * MySQL 컬럼 타입 → 자바 타입 매핑.
 * eGov 관례상 날짜/여부 컬럼은 String으로 다루는 경우가 많아 1차는 보수적으로 String 기본.
 */
public final class TypeMapper {

    private TypeMapper() {}

    /** DDL 타입명(소문자 무관, size 제외)을 받아 자바 타입 문자열을 반환. */
    public static String toJava(String rawType) {
        String t = rawType.toLowerCase(Locale.ROOT).trim();
        // 괄호 이하 제거 (varchar(100) → varchar)
        int paren = t.indexOf('(');
        if (paren >= 0) t = t.substring(0, paren).trim();

        return switch (t) {
            case "tinyint", "smallint", "mediumint", "int", "integer" -> "int";
            case "bigint" -> "long";
            case "decimal", "numeric" -> "java.math.BigDecimal";
            case "float" -> "float";
            case "double", "real" -> "double";
            // char/varchar/text 계열, date/time 계열, 기타 → String (eGov 관례)
            default -> "String";
        };
    }

    /** 괄호 안 size 추출. 예: varchar(100) → 100, 없으면 0. */
    public static int extractSize(String rawType) {
        int open = rawType.indexOf('(');
        int close = rawType.indexOf(')');
        if (open >= 0 && close > open) {
            String inside = rawType.substring(open + 1, close).trim();
            // decimal(10,2) 같은 경우 앞 숫자만
            int comma = inside.indexOf(',');
            if (comma >= 0) inside = inside.substring(0, comma).trim();
            try { return Integer.parseInt(inside); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }
}

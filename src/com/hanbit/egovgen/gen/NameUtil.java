package com.hanbit.egovgen.gen;

/**
 * 명명 규칙 변환 유틸. (RESTDE_DE ↔ restdeDe ↔ RestdeDe)
 */
public final class NameUtil {

    private NameUtil() {}

    /** snake_case(또는 SNAKE_CASE) → camelCase. 예: RESTDE_DE → restdeDe */
    public static String toCamel(String snake) {
        String s = snake.toLowerCase();
        StringBuilder sb = new StringBuilder(s.length());
        boolean upper = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '_') { upper = true; continue; }
            sb.append(upper ? Character.toUpperCase(ch) : ch);
            upper = false;
        }
        return sb.toString();
    }

    /** snake_case → PascalCase. 예: RESTDE_DE → RestdeDe */
    public static String toPascal(String snake) {
        String camel = toCamel(snake);
        if (camel.isEmpty()) return camel;
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    /** 첫 글자만 대문자. 예: restde → Restde */
    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * 테이블명에서 prefix를 제거한 뒤 PascalCase 엔티티명 도출.
     * 예: prefix=LETTN, table=LETTNRESTDE → Restde (단일 토큰은 그대로 Pascal)
     */
    public static String entityFromTable(String tableName, String prefix) {
        String t = tableName;
        if (prefix != null && !prefix.isBlank() && t.toUpperCase().startsWith(prefix.toUpperCase())) {
            t = t.substring(prefix.length());
        }
        // 언더스코어가 있으면 토큰 단위 Pascal, 없으면 첫 글자만 대문자 + 나머지 소문자
        if (t.contains("_")) return toPascal(t);
        return capitalize(t.toLowerCase());
    }
}

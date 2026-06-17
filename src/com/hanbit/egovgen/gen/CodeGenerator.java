package com.hanbit.egovgen.gen;

import com.hanbit.egovgen.config.GenConfig;
import com.hanbit.egovgen.model.ColumnMeta;
import com.hanbit.egovgen.model.TableMeta;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 메타모델 → eGov CRUD 풀세트 코드 생성기 (순수 Java text block 템플릿, 외부 의존성 0).
 * 산출물: 자바 6파일 + Mapper XML + JSP 4종.
 */
public class CodeGenerator {

    private final GenConfig cfg;

    public CodeGenerator(GenConfig cfg) { this.cfg = cfg; }

    /** 전체 생성. 생성된 파일 경로 목록을 반환. */
    public List<Path> generate(TableMeta t) throws IOException {
        String entity = t.getEntityName();
        String pkgPath = cfg.basePackage().replace('.', '/');
        Path out = Path.of(cfg.outputDir());

        Path javaServiceDir = out.resolve("src/main/java/" + pkgPath + "/service");
        Path javaImplDir = javaServiceDir.resolve("impl");
        Path javaWebDir = out.resolve("src/main/java/" + pkgPath + "/web");
        Path mapperDir = out.resolve("src/main/resources/egovframework/mapper/" + cfg.module());
        Path jspDir = out.resolve("src/main/webapp/WEB-INF/jsp/" + cfg.module());

        var written = new java.util.ArrayList<Path>();
        written.add(write(javaServiceDir.resolve(entity + ".java"), domainVo(t)));
        written.add(write(javaServiceDir.resolve(entity + "VO.java"), searchVo(t)));
        written.add(write(javaServiceDir.resolve("Egov" + entity + "ManageService.java"), serviceInterface(t)));
        written.add(write(javaImplDir.resolve("Egov" + entity + "ManageServiceImpl.java"), serviceImpl(t)));
        written.add(write(javaImplDir.resolve(entity + "ManageDAO.java"), dao(t)));
        written.add(write(javaWebDir.resolve("Egov" + entity + "ManageController.java"), controller(t)));
        written.add(write(mapperDir.resolve("Egov" + entity + "Manage_SQL_mysql.xml"), mapperXml(t)));
        written.add(write(jspDir.resolve("Egov" + entity + "List.jsp"), jspList(t)));
        written.add(write(jspDir.resolve("Egov" + entity + "Detail.jsp"), jspDetail(t)));
        written.add(write(jspDir.resolve("Egov" + entity + "Regist.jsp"), jspForm(t, false)));
        written.add(write(jspDir.resolve("Egov" + entity + "Modify.jsp"), jspForm(t, true)));

        // 채번 옵션: 자동 로드되는 spring/com/context-idgen-{entity}.xml 생성
        if (idgnrApplicable(t)) {
            Path idgenXml = out.resolve("src/main/resources/egovframework/spring/com")
                    .resolve("context-idgen-" + decap(entity) + ".xml");
            written.add(write(idgenXml, idgnrBeanXml(t)));
        }
        return written;
    }

    private Path write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    /**
     * 플레이스홀더 치환. 값 안에 또 다른 플레이스홀더가 들어있는 경우(중첩)를 위해
     * 더 이상 바뀌지 않을 때까지 반복한다. (예: JSP 목록의 PK 링크가 __MODULE__/__ENTITY__ 포함)
     */
    private static String render(String tpl, Map<String, String> vars) {
        String prev, s = tpl;
        int guard = 0;
        do {
            prev = s;
            for (var e : vars.entrySet()) s = s.replace("__" + e.getKey() + "__", e.getValue());
        } while (!s.equals(prev) && guard++ < 10);
        return s;
    }

    /** 공통 변수 묶음. */
    private Map<String, String> base(TableMeta t) {
        String entity = t.getEntityName();
        ColumnMeta pk = t.primaryKey();
        Map<String, String> v = new LinkedHashMap<>();
        v.put("PACKAGE", cfg.basePackage());
        v.put("ENTITY", entity);
        v.put("ENTITY_LOWER", decap(entity));
        v.put("TABLE", t.getTableName());
        v.put("MODULE", cfg.module());
        v.put("SERVICE_NAME", entity + "ManageService");
        v.put("DAO_NAME", entity + "ManageDAO");
        v.put("DAO_BASE", cfg.daoBase());
        v.put("DAO_BASE_SIMPLE", GenConfig.simpleName(cfg.daoBase()));
        v.put("SERVICE_BASE", cfg.serviceBase());
        v.put("SERVICE_BASE_SIMPLE", GenConfig.simpleName(cfg.serviceBase()));
        v.put("PAGINATION", cfg.paginationInfo());
        v.put("PAGINATION_SIMPLE", GenConfig.simpleName(cfg.paginationInfo()));
        v.put("PK_FIELD", pk != null ? pk.getFieldName() : "id");
        v.put("PK_COLUMN", pk != null ? pk.getColumnName() : "ID");
        v.put("PK_TYPE", pk != null ? pk.getJavaType() : "String");
        v.put("PK_FIELD_CAP", pk != null ? NameUtil.capitalize(pk.getFieldName()) : "Id");
        // 채번 관련 (useIdgnr 일 때만 실제 사용)
        v.put("IDGNR_SERVICE", "egov" + entity + "IdGnrService");
        v.put("IDGNR_STRATEGY", decap(entity) + "Strategy");
        v.put("IDGNR_KEY", pk != null ? pk.getColumnName() : "ID");
        String idgnrPrefix = entity.toUpperCase() + "_";
        v.put("IDGNR_PREFIX", idgnrPrefix);
        // cipers = 숫자 부분 자리수. eGov 관례상 prefix길이 + cipers = PK컬럼 길이.
        int pkSize = (pk != null && pk.getSize() > 0) ? pk.getSize() : 20;
        int cipers = Math.max(1, pkSize - idgnrPrefix.length());
        v.put("IDGNR_CIPERS", String.valueOf(cipers));
        return v;
    }

    /** 채번 적용 가능 여부: 옵션 on + PK가 String 타입일 때만. */
    private boolean idgnrApplicable(TableMeta t) {
        ColumnMeta pk = t.primaryKey();
        return cfg.useIdgnr() && pk != null && "String".equals(pk.getJavaType());
    }

    private static String decap(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    // ─────────────────────────────── 도메인 VO ───────────────────────────────
    private String domainVo(TableMeta t) {
        StringBuilder fields = new StringBuilder();
        StringBuilder accessors = new StringBuilder();
        for (ColumnMeta c : t.getColumns()) {
            String def = "String".equals(c.getJavaType()) ? " = \"\""
                    : ("int".equals(c.getJavaType()) || "long".equals(c.getJavaType())) ? " = 0" : "";
            fields.append("    /** ").append(c.label()).append(" */\n");
            fields.append("    private ").append(c.getJavaType()).append(" ")
                  .append(c.getFieldName()).append(def).append(";\n");
            String cap = NameUtil.capitalize(c.getFieldName());
            accessors.append("    public ").append(c.getJavaType()).append(" get").append(cap)
                     .append("() { return ").append(c.getFieldName()).append("; }\n");
            accessors.append("    public void set").append(cap).append("(")
                     .append(c.getJavaType()).append(" ").append(c.getFieldName())
                     .append(") { this.").append(c.getFieldName()).append(" = ")
                     .append(c.getFieldName()).append("; }\n\n");
        }
        var v = base(t);
        v.put("FIELDS", fields.toString());
        v.put("ACCESSORS", accessors.toString());
        return render("""
                package __PACKAGE__.service;

                import java.io.Serializable;
                import org.apache.commons.lang3.builder.ToStringBuilder;

                /**
                 * __ENTITY__ 도메인 VO (테이블 __TABLE__).
                 * 코드 제너레이터 자동 생성 — 비즈니스 로직은 추가하지 말고 VO 역할만.
                 */
                public class __ENTITY__ implements Serializable {

                __FIELDS__
                __ACCESSORS__    @Override
                    public String toString() {
                        return ToStringBuilder.reflectionToString(this);
                    }
                }
                """, v);
    }

    // ─────────────────────────────── 검색/페이징 VO ───────────────────────────────
    private String searchVo(TableMeta t) {
        return render("""
                package __PACKAGE__.service;

                import java.io.Serializable;

                /**
                 * __ENTITY__ 검색조건 + 페이징 VO.
                 */
                public class __ENTITY__VO extends __ENTITY__ implements Serializable {

                    /** 검색 조건(컬럼 선택 키) */
                    private String searchCondition = "";
                    /** 검색어 */
                    private String searchKeyword = "";

                    /** 현재 페이지 */
                    private int pageIndex = 1;
                    /** 페이지당 출력 건수 */
                    private int pageUnit = 10;
                    /** 페이지 네비게이션 사이즈 */
                    private int pageSize = 10;
                    /** 목록 시작 인덱스(OFFSET) */
                    private int firstIndex = 1;
                    /** 목록 끝 인덱스 */
                    private int lastIndex = 1;
                    /** 페이지당 레코드 수 */
                    private int recordCountPerPage = 10;

                    public String getSearchCondition() { return searchCondition; }
                    public void setSearchCondition(String searchCondition) { this.searchCondition = searchCondition; }
                    public String getSearchKeyword() { return searchKeyword; }
                    public void setSearchKeyword(String searchKeyword) { this.searchKeyword = searchKeyword; }
                    public int getPageIndex() { return pageIndex; }
                    public void setPageIndex(int pageIndex) { this.pageIndex = pageIndex; }
                    public int getPageUnit() { return pageUnit; }
                    public void setPageUnit(int pageUnit) { this.pageUnit = pageUnit; }
                    public int getPageSize() { return pageSize; }
                    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
                    public int getFirstIndex() { return firstIndex; }
                    public void setFirstIndex(int firstIndex) { this.firstIndex = firstIndex; }
                    public int getLastIndex() { return lastIndex; }
                    public void setLastIndex(int lastIndex) { this.lastIndex = lastIndex; }
                    public int getRecordCountPerPage() { return recordCountPerPage; }
                    public void setRecordCountPerPage(int recordCountPerPage) { this.recordCountPerPage = recordCountPerPage; }
                }
                """, base(t));
    }

    // ─────────────────────────────── Service 인터페이스 ───────────────────────────────
    private String serviceInterface(TableMeta t) {
        return render("""
                package __PACKAGE__.service;

                import java.util.List;

                /**
                 * __ENTITY__ 관리 서비스.
                 */
                public interface Egov__ENTITY__ManageService {

                    /** 목록 조회 */
                    List<?> select__ENTITY__List(__ENTITY__VO searchVO) throws Exception;

                    /** 목록 전체 건수 */
                    int select__ENTITY__ListTotCnt(__ENTITY__VO searchVO) throws Exception;

                    /** 단건 상세 조회 */
                    __ENTITY__ select__ENTITY__Detail(__ENTITY__ vo) throws Exception;

                    /** 등록 */
                    void insert__ENTITY__(__ENTITY__ vo) throws Exception;

                    /** 수정 */
                    void update__ENTITY__(__ENTITY__ vo) throws Exception;

                    /** 삭제 */
                    void delete__ENTITY__(__ENTITY__ vo) throws Exception;
                }
                """, base(t));
    }

    // ─────────────────────────────── ServiceImpl ───────────────────────────────
    private String serviceImpl(TableMeta t) {
        var v = base(t);
        boolean idgnr = idgnrApplicable(t);
        v.put("IDGNR_IMPORT", idgnr ? "import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;\n" : "");
        v.put("IDGNR_FIELD", idgnr
                ? "\n    @Resource(name = \"__IDGNR_SERVICE__\")\n    private EgovIdGnrService egovIdGnrService;\n"
                : "");
        v.put("INSERT_BODY", idgnr
                ? "        // 채번: PK 자동 생성\n"
                + "        String id = egovIdGnrService.getNextStringId();\n"
                + "        vo.set__PK_FIELD_CAP__(id);\n"
                + "        __ENTITY_LOWER__ManageDAO.insert__ENTITY__(vo);\n"
                : "        __ENTITY_LOWER__ManageDAO.insert__ENTITY__(vo);\n");
        return render("""
                package __PACKAGE__.service.impl;

                import java.util.List;

                import org.springframework.stereotype.Service;
                import __SERVICE_BASE__;

                import jakarta.annotation.Resource;
                __IDGNR_IMPORT__
                import __PACKAGE__.service.Egov__ENTITY__ManageService;
                import __PACKAGE__.service.__ENTITY__;
                import __PACKAGE__.service.__ENTITY__VO;

                /**
                 * __ENTITY__ 관리 서비스 구현. (DAO 위임 — 비즈니스 로직은 여기 추가)
                 */
                @Service("__SERVICE_NAME__")
                public class Egov__ENTITY__ManageServiceImpl extends __SERVICE_BASE_SIMPLE__
                        implements Egov__ENTITY__ManageService {

                    @Resource(name = "__DAO_NAME__")
                    private __DAO_NAME__ __ENTITY_LOWER__ManageDAO;
                __IDGNR_FIELD__
                    @Override
                    public List<?> select__ENTITY__List(__ENTITY__VO searchVO) throws Exception {
                        return __ENTITY_LOWER__ManageDAO.select__ENTITY__List(searchVO);
                    }

                    @Override
                    public int select__ENTITY__ListTotCnt(__ENTITY__VO searchVO) throws Exception {
                        return __ENTITY_LOWER__ManageDAO.select__ENTITY__ListTotCnt(searchVO);
                    }

                    @Override
                    public __ENTITY__ select__ENTITY__Detail(__ENTITY__ vo) throws Exception {
                        return __ENTITY_LOWER__ManageDAO.select__ENTITY__Detail(vo);
                    }

                    @Override
                    public void insert__ENTITY__(__ENTITY__ vo) throws Exception {
                __INSERT_BODY__    }

                    @Override
                    public void update__ENTITY__(__ENTITY__ vo) throws Exception {
                        __ENTITY_LOWER__ManageDAO.update__ENTITY__(vo);
                    }

                    @Override
                    public void delete__ENTITY__(__ENTITY__ vo) throws Exception {
                        __ENTITY_LOWER__ManageDAO.delete__ENTITY__(vo);
                    }
                }
                """, v);
    }

    // ─────────────────────────────── 채번 빈 설정 XML ───────────────────────────────
    private String idgnrBeanXml(TableMeta t) {
        return render("""
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

                    <!-- __ENTITY__ 채번(ID Generation) 설정 (자동 생성) -->
                    <bean name="__IDGNR_SERVICE__"
                        class="org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl"
                        destroy-method="destroy">
                        <property name="dataSource" ref="egov.dataSource" />
                        <property name="strategy"   ref="__IDGNR_STRATEGY__" />
                        <property name="blockSize"  value="10"/>
                        <property name="table"      value="IDS"/>
                        <property name="tableName"  value="__IDGNR_KEY__"/>
                    </bean>
                    <bean name="__IDGNR_STRATEGY__"
                        class="org.egovframe.rte.fdl.idgnr.impl.strategy.EgovIdGnrStrategyImpl">
                        <property name="prefix"   value="__IDGNR_PREFIX__" />
                        <property name="cipers"   value="__IDGNR_CIPERS__" />
                        <property name="fillChar" value="0" />
                    </bean>

                </beans>
                """, base(t));
    }

    // ─────────────────────────────── DAO ───────────────────────────────
    private String dao(TableMeta t) {
        return render("""
                package __PACKAGE__.service.impl;

                import java.util.List;

                import org.springframework.stereotype.Repository;
                import __DAO_BASE__;

                import __PACKAGE__.service.__ENTITY__;
                import __PACKAGE__.service.__ENTITY__VO;

                /**
                 * __ENTITY__ 관리 DAO. Mapper namespace "__DAO_NAME__" 와 매핑.
                 */
                @Repository("__DAO_NAME__")
                public class __DAO_NAME__ extends __DAO_BASE_SIMPLE__ {

                    public List<?> select__ENTITY__List(__ENTITY__VO searchVO) throws Exception {
                        return selectList("__DAO_NAME__.select__ENTITY__List", searchVO);
                    }

                    public int select__ENTITY__ListTotCnt(__ENTITY__VO searchVO) throws Exception {
                        return (Integer) selectOne("__DAO_NAME__.select__ENTITY__ListTotCnt", searchVO);
                    }

                    public __ENTITY__ select__ENTITY__Detail(__ENTITY__ vo) throws Exception {
                        return (__ENTITY__) selectOne("__DAO_NAME__.select__ENTITY__Detail", vo);
                    }

                    public void insert__ENTITY__(__ENTITY__ vo) throws Exception {
                        insert("__DAO_NAME__.insert__ENTITY__", vo);
                    }

                    public void update__ENTITY__(__ENTITY__ vo) throws Exception {
                        update("__DAO_NAME__.update__ENTITY__", vo);
                    }

                    public void delete__ENTITY__(__ENTITY__ vo) throws Exception {
                        delete("__DAO_NAME__.delete__ENTITY__", vo);
                    }
                }
                """, base(t));
    }

    // ─────────────────────────────── Controller ───────────────────────────────
    private String controller(TableMeta t) {
        return render("""
                package __PACKAGE__.web;

                import org.springframework.stereotype.Controller;
                import org.springframework.ui.ModelMap;
                import org.springframework.web.bind.annotation.ModelAttribute;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RequestParam;

                import __PAGINATION__;

                import jakarta.annotation.Resource;

                import __PACKAGE__.service.Egov__ENTITY__ManageService;
                import __PACKAGE__.service.__ENTITY__;
                import __PACKAGE__.service.__ENTITY__VO;

                import java.util.Map;

                /**
                 * __ENTITY__ 관리 컨트롤러.
                 */
                @Controller
                public class Egov__ENTITY__ManageController {

                    @Resource(name = "__SERVICE_NAME__")
                    private Egov__ENTITY__ManageService __ENTITY_LOWER__ManageService;

                    /** 목록 */
                    @RequestMapping(value = "/__MODULE__/Egov__ENTITY__List.do")
                    public String select__ENTITY__List(@ModelAttribute("searchVO") __ENTITY__VO searchVO,
                            ModelMap model) throws Exception {

                        __PAGINATION_SIMPLE__ paginationInfo = new __PAGINATION_SIMPLE__();
                        paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
                        paginationInfo.setRecordCountPerPage(searchVO.getPageUnit());
                        paginationInfo.setPageSize(searchVO.getPageSize());

                        searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
                        searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
                        searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

                        model.addAttribute("resultList", __ENTITY_LOWER__ManageService.select__ENTITY__List(searchVO));
                        int totCnt = __ENTITY_LOWER__ManageService.select__ENTITY__ListTotCnt(searchVO);
                        paginationInfo.setTotalRecordCount(totCnt);
                        model.addAttribute("paginationInfo", paginationInfo);
                        return "/__MODULE__/Egov__ENTITY__List";
                    }

                    /** 상세 */
                    @RequestMapping(value = "/__MODULE__/Egov__ENTITY__Detail.do")
                    public String select__ENTITY__Detail(__ENTITY__ vo, ModelMap model) throws Exception {
                        model.addAttribute("result", __ENTITY_LOWER__ManageService.select__ENTITY__Detail(vo));
                        return "/__MODULE__/Egov__ENTITY__Detail";
                    }

                    /** 등록 폼 */
                    @RequestMapping(value = "/__MODULE__/Egov__ENTITY__RegistView.do")
                    public String register__ENTITY__View(@ModelAttribute("searchVO") __ENTITY__VO searchVO,
                            ModelMap model) throws Exception {
                        model.addAttribute("__ENTITY_LOWER__", new __ENTITY__());
                        return "/__MODULE__/Egov__ENTITY__Regist";
                    }

                    /** 등록 처리 */
                    @RequestMapping(value = "/__MODULE__/Egov__ENTITY__Regist.do")
                    public String register__ENTITY__(@ModelAttribute("__ENTITY_LOWER__") __ENTITY__ vo) throws Exception {
                        __ENTITY_LOWER__ManageService.insert__ENTITY__(vo);
                        return "redirect:/__MODULE__/Egov__ENTITY__List.do";
                    }

                    /** 수정 폼/처리 (cmd 파라미터로 구분) */
                    @RequestMapping(value = "/__MODULE__/Egov__ENTITY__Modify.do")
                    public String modify__ENTITY__(@ModelAttribute("__ENTITY_LOWER__") __ENTITY__ vo,
                            @RequestParam Map<String, Object> commandMap, ModelMap model) throws Exception {
                        String cmd = commandMap.get("cmd") == null ? "" : (String) commandMap.get("cmd");
                        if ("Modify".equals(cmd)) {
                            __ENTITY_LOWER__ManageService.update__ENTITY__(vo);
                            return "redirect:/__MODULE__/Egov__ENTITY__List.do";
                        }
                        model.addAttribute("__ENTITY_LOWER__", __ENTITY_LOWER__ManageService.select__ENTITY__Detail(vo));
                        return "/__MODULE__/Egov__ENTITY__Modify";
                    }

                    /** 삭제 */
                    @RequestMapping(value = "/__MODULE__/Egov__ENTITY__Remove.do")
                    public String delete__ENTITY__(__ENTITY__ vo) throws Exception {
                        __ENTITY_LOWER__ManageService.delete__ENTITY__(vo);
                        return "redirect:/__MODULE__/Egov__ENTITY__List.do";
                    }
                }
                """, base(t));
    }

    // ─────────────────────────────── Mapper XML ───────────────────────────────
    private String mapperXml(TableMeta t) {
        boolean logical = t.getColumns().stream().anyMatch(c -> "useAt".equals(c.getFieldName()));
        ColumnMeta pk = t.primaryKey();
        List<ColumnMeta> cols = t.getColumns();

        // resultMap: 모든 컬럼(조회는 감사 컬럼도 보여줌)
        StringBuilder resultMap = new StringBuilder();
        for (ColumnMeta c : cols) {
            resultMap.append("        <result property=\"").append(c.getFieldName())
                     .append("\" column=\"").append(c.getColumnName()).append("\"/>\n");
        }

        // INSERT 대상: 감사 ID(등록자/수정자)는 제외, 감사 시점은 SYSDATE()
        List<ColumnMeta> insertCols = new java.util.ArrayList<>();
        for (ColumnMeta c : cols) {
            if (c.isAudit() && !c.isAuditTimestamp()) continue;
            insertCols.add(c);
        }
        StringBuilder insCols = new StringBuilder();
        StringBuilder insVals = new StringBuilder();
        for (int i = 0; i < insertCols.size(); i++) {
            ColumnMeta c = insertCols.get(i);
            boolean last = (i == insertCols.size() - 1);
            insCols.append("            ").append(c.getColumnName()).append(last ? "\n" : ",\n");
            String val = c.isAuditTimestamp() ? "SYSDATE()" : "#{" + c.getFieldName() + "}";
            insVals.append("            ").append(val).append(last ? "\n" : ",\n");
        }

        // UPDATE SET: PK 제외, 최초등록/수정자 감사 제외, 최종수정시점은 SYSDATE()
        StringBuilder updSet = new StringBuilder();
        for (ColumnMeta c : cols) {
            if (c.isPrimaryKey()) continue;
            if (c.isAudit()) {
                if (c.getColumnName().equalsIgnoreCase("LAST_UPDT_PNTTM")) {
                    updSet.append("            LAST_UPDT_PNTTM = SYSDATE(),\n");
                }
                continue; // 그 외 감사 컬럼은 수정에서 제외
            }
            updSet.append("            ").append(c.getColumnName()).append(" = #{")
                  .append(c.getFieldName()).append("},\n");
        }
        String updSetStr = updSet.toString();
        int lastComma = updSetStr.lastIndexOf(',');
        if (lastComma >= 0) updSetStr = updSetStr.substring(0, lastComma) + updSetStr.substring(lastComma + 1);

        // 검색 동적조건 (String 컬럼 OR LIKE, searchCondition 인덱스로 선택)
        StringBuilder search = new StringBuilder();
        List<ColumnMeta> sc = t.searchableColumns();
        for (int i = 0; i < sc.size(); i++) {
            search.append("        <if test=\"searchCondition == '").append(i).append("'\">\n");
            search.append("            AND ").append(sc.get(i).getColumnName())
                  .append(" LIKE CONCAT('%', #{searchKeyword}, '%')\n");
            search.append("        </if>\n");
        }

        // 플레이스홀더가 아닌 실제 값으로 직접 구성 (render는 1패스라 중첩 치환 불가)
        String tbl = t.getTableName();
        String pkCol = pk != null ? pk.getColumnName() : "ID";
        String pkFld = pk != null ? pk.getFieldName() : "id";
        String deleteSql = logical
                ? "        UPDATE " + tbl + " SET USE_AT = 'N' WHERE " + pkCol + " = #{" + pkFld + "}"
                : "        DELETE FROM " + tbl + " WHERE " + pkCol + " = #{" + pkFld + "}";

        var v = base(t);
        v.put("RESULT_MAP", resultMap.toString());
        v.put("INS_COLS", insCols.toString());
        v.put("INS_VALS", insVals.toString());
        v.put("UPD_SET", updSetStr);
        v.put("SEARCH", search.toString());
        v.put("DELETE_SQL", deleteSql);
        return render("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="__DAO_NAME__">

                    <resultMap id="__ENTITY_LOWER__" type="__PACKAGE__.service.__ENTITY__">
                __RESULT_MAP__    </resultMap>

                    <insert id="insert__ENTITY__" parameterType="__PACKAGE__.service.__ENTITY__">
                        INSERT INTO __TABLE__ (
                __INS_COLS__        ) VALUES (
                __INS_VALS__        )
                    </insert>

                    <select id="select__ENTITY__List" parameterType="__PACKAGE__.service.__ENTITY__VO" resultMap="__ENTITY_LOWER__">
                        SELECT * FROM __TABLE__
                        WHERE 1=1
                __SEARCH__        ORDER BY __PK_COLUMN__ DESC
                        LIMIT #{recordCountPerPage} OFFSET #{firstIndex}
                    </select>

                    <select id="select__ENTITY__ListTotCnt" parameterType="__PACKAGE__.service.__ENTITY__VO" resultType="java.lang.Integer">
                        SELECT COUNT(*) FROM __TABLE__
                        WHERE 1=1
                __SEARCH__    </select>

                    <select id="select__ENTITY__Detail" parameterType="__PACKAGE__.service.__ENTITY__" resultMap="__ENTITY_LOWER__">
                        SELECT * FROM __TABLE__ WHERE __PK_COLUMN__ = #{__PK_FIELD__}
                    </select>

                    <update id="update__ENTITY__" parameterType="__PACKAGE__.service.__ENTITY__">
                        UPDATE __TABLE__ SET
                __UPD_SET__        WHERE __PK_COLUMN__ = #{__PK_FIELD__}
                    </update>

                    <delete id="delete__ENTITY__" parameterType="__PACKAGE__.service.__ENTITY__">
                __DELETE_SQL__
                    </delete>

                </mapper>
                """, v);
    }

    // ─────────────────────────────── JSP: 목록 ───────────────────────────────
    private String jspList(TableMeta t) {
        StringBuilder ths = new StringBuilder();
        StringBuilder tds = new StringBuilder();
        ColumnMeta pk = t.primaryKey();
        for (ColumnMeta c : t.getColumns()) {
            ths.append("                <th>").append(c.label()).append("</th>\n");
            if (pk != null && c.isPrimaryKey()) {
                tds.append("                <td><a href=\"<c:url value='/__MODULE__/Egov__ENTITY__Detail.do'/>?")
                   .append(c.getFieldName()).append("=<c:out value='${result.").append(c.getFieldName())
                   .append("}'/>\"><c:out value=\"${result.").append(c.getFieldName()).append("}\"/></a></td>\n");
            } else {
                tds.append("                <td><c:out value=\"${result.").append(c.getFieldName()).append("}\"/></td>\n");
            }
        }
        int colspan = t.getColumns().size();
        var v = base(t);
        v.put("THS", ths.toString());
        v.put("TDS", tds.toString());
        v.put("COLSPAN", String.valueOf(colspan));
        return render("""
                <%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
                <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
                <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
                <%@ taglib prefix="ui" uri="http://egovframework.gov/ctl/ui" %>
                <%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
                <!-- __ENTITY__ 목록 화면 (자동 생성) -->
                <form name="frm" action="<c:url value='/__MODULE__/Egov__ENTITY__List.do'/>" method="post">
                    <input type="hidden" name="pageIndex" value="<c:out value='${searchVO.pageIndex}'/>"/>
                    <select name="searchCondition">
                        <option value="0">검색조건</option>
                    </select>
                    <input type="text" name="searchKeyword" value="<c:out value='${searchVO.searchKeyword}'/>"/>
                    <button type="submit">조회</button>
                    <a href="<c:url value='/__MODULE__/Egov__ENTITY__RegistView.do'/>">등록</a>
                </form>

                <table>
                    <thead>
                        <tr>
                __THS__            </tr>
                    </thead>
                    <tbody>
                        <c:if test="${fn:length(resultList) == 0}">
                            <tr><td colspan="__COLSPAN__">검색된 결과가 없습니다.</td></tr>
                        </c:if>
                        <c:forEach var="result" items="${resultList}" varStatus="status">
                            <tr>
                __TDS__                </tr>
                        </c:forEach>
                    </tbody>
                </table>

                <div class="paging">
                    <ui:pagination paginationInfo="${paginationInfo}" type="image" jsFunction="fn_search" />
                </div>

                <script type="text/javascript">
                    function fn_search(pageNo) {
                        document.frm.pageIndex.value = pageNo;
                        document.frm.submit();
                    }
                </script>
                """, v);
    }

    // ─────────────────────────────── JSP: 상세 ───────────────────────────────
    private String jspDetail(TableMeta t) {
        StringBuilder rows = new StringBuilder();
        for (ColumnMeta c : t.getColumns()) {
            rows.append("        <tr><th>").append(c.label())
                .append("</th><td><c:out value=\"${result.").append(c.getFieldName()).append("}\"/></td></tr>\n");
        }
        ColumnMeta pk = t.primaryKey();
        var v = base(t);
        v.put("ROWS", rows.toString());
        v.put("PK_VALUE", pk != null ? "${result." + pk.getFieldName() + "}" : "");
        return render("""
                <%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
                <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
                <!-- __ENTITY__ 상세 화면 (자동 생성) -->
                <table>
                __ROWS__</table>

                <a href="<c:url value='/__MODULE__/Egov__ENTITY__Modify.do'/>?__PK_FIELD__=<c:out value='__PK_VALUE__'/>">수정</a>
                <a href="<c:url value='/__MODULE__/Egov__ENTITY__Remove.do'/>?__PK_FIELD__=<c:out value='__PK_VALUE__'/>">삭제</a>
                <a href="<c:url value='/__MODULE__/Egov__ENTITY__List.do'/>">목록</a>
                """, v);
    }

    // ─────────────────────────────── JSP: 등록/수정 폼 ───────────────────────────────
    private String jspForm(TableMeta t, boolean modify) {
        StringBuilder rows = new StringBuilder();
        boolean idgnr = idgnrApplicable(t);
        for (ColumnMeta c : t.getColumns()) {
            // 등록 화면에서 채번 대상 PK는 입력란 생략(서버에서 자동 채번)
            if (c.isPrimaryKey() && !modify && idgnr) continue;
            // 감사 컬럼(등록자/등록시점/수정자/수정시점)은 시스템이 채우므로 화면 입력 제외
            if (c.isAudit()) continue;
            String valExpr = "<c:out value=\"${" + decap(t.getEntityName()) + "." + c.getFieldName() + "}\"/>";
            rows.append("        <tr><th>").append(c.label()).append("</th><td>");
            if (c.isPrimaryKey() && modify) {
                // 수정 시 PK는 읽기전용 + hidden
                rows.append("<c:out value=\"${").append(decap(t.getEntityName())).append(".")
                    .append(c.getFieldName()).append("}\"/>")
                    .append("<input type=\"hidden\" name=\"").append(c.getFieldName())
                    .append("\" value=\"").append(valExpr).append("\"/>");
            } else {
                String maxlen = c.getSize() > 0 ? " maxlength=\"" + c.getSize() + "\"" : "";
                rows.append("<input type=\"text\" name=\"").append(c.getFieldName())
                    .append("\"").append(maxlen).append(" value=\"").append(modify ? valExpr : "").append("\"/>");
                // 입력칸 옆에 데이터 타입·길이 안내
                rows.append(" <span style=\"color:#888;font-size:0.85em\">").append(c.getJdbcType()).append("</span>");
            }
            rows.append("</td></tr>\n");
        }
        var v = base(t);
        v.put("ROWS", rows.toString());
        v.put("ACTION", modify ? "Egov__ENTITY__Modify.do?cmd=Modify" : "Egov__ENTITY__Regist.do");
        v.put("TITLE", modify ? "수정" : "등록");
        // ACTION 안의 __ENTITY__ 도 치환되도록 한 번 더 render
        return render(render("""
                <%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
                <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
                <!-- __ENTITY__ __TITLE__ 화면 (자동 생성) -->
                <form name="frm" action="<c:url value='/__MODULE__/__ACTION__'/>" method="post">
                    <table>
                __ROWS__    </table>
                    <button type="submit">__TITLE__</button>
                    <a href="<c:url value='/__MODULE__/Egov__ENTITY__List.do'/>">목록</a>
                </form>
                """, v), v);
    }
}

package com.hanbit.egovgen.ui;

import com.hanbit.egovgen.config.GenConfig;
import com.hanbit.egovgen.service.GenerationResult;
import com.hanbit.egovgen.service.GenerationService;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * eGov CRUD 코드 제너레이터 GUI (Swing).
 *
 * CLI(Main)와 동일한 {@link GenerationService}를 호출한다 — 생성 로직은 한 곳에만 있다.
 * 외부 의존성 0 원칙에 따라 JDK 내장 Swing만 사용한다(추가 라이브러리 없음).
 *
 * 흐름: 시작 시 작업폴더의 gen.properties를 폼에 채움 → 사용자가 DDL 입력·설정 수정
 *       → [생성] → 서비스 호출 → 결과(파일 목록·접속 URL)를 우측에 출력.
 */
public class GenGuiApp {

    // 입력/결과
    private final JTextArea ddlArea = new JTextArea();
    private final JTextArea resultArea = new JTextArea();

    // 설정 폼 (gen.properties 키에 대응)
    private final JTextField pkgField = new JTextField();
    private final JTextField moduleField = new JTextField();
    private final JTextField prefixField = new JTextField();
    private final JTextField dbTypeField = new JTextField();
    private final JTextField outDirField = new JTextField();
    private final JTextField baseUrlField = new JTextField();
    private final JCheckBox idgnrCheck = new JCheckBox("채번(EgovIdGnrService) 사용 — String PK 전제");

    /** 시작 시·생성 시 기본 베이스로 쓰는 설정 파일(작업폴더 기준). 없으면 내장 기본값 사용. */
    private final Path defaultConfig = Path.of("gen.properties");

    private JFrame frame;

    public static void main(String[] args) {
        // 시스템 룩앤필(있으면) — 실패해도 기본 룩앤필로 진행
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
            // 룩앤필 설정 실패는 기능에 영향 없음
        }
        SwingUtilities.invokeLater(() -> new GenGuiApp().createAndShow());
    }

    private void createAndShow() {
        frame = new JFrame("eGov CRUD 코드 제너레이터 (Studio)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(960, 700);
        frame.setLocationByPlatform(true);
        frame.setLayout(new BorderLayout(8, 8));

        frame.add(buildSettingsPanel(), BorderLayout.NORTH);
        frame.add(buildCenter(), BorderLayout.CENTER);
        frame.add(buildSouth(), BorderLayout.SOUTH);

        loadConfigIntoForm(defaultConfig); // gen.properties 있으면 폼 초기값으로
        frame.setVisible(true);
    }

    // ── UI 구성 ─────────────────────────────────────────────

    private JPanel buildSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("프로젝트 설정 (gen.properties)"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 6, 3, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;
        addRow(p, g, r++, "루트 패키지(basePackage)", pkgField);
        addRow(p, g, r++, "모듈(module, let/ 권장)", moduleField);
        addRow(p, g, r++, "테이블 prefix(tablePrefix)", prefixField);
        addRow(p, g, r++, "DB 타입(dbType)", dbTypeField);

        // 출력 루트 + [찾아보기] — 탐색기로 폴더를 직접 고른다.
        // eGov 프로젝트 루트를 고르면 src/main/... 구조에 바로 병합된다.
        JPanel outPanel = new JPanel(new BorderLayout(4, 0));
        outDirField.setToolTipText("eGov 프로젝트 루트 폴더를 고르면 src/main/... 구조에 바로 병합됩니다. 결과만 보려면 ./output");
        outPanel.add(outDirField, BorderLayout.CENTER);
        JButton browseOut = new JButton("찾아보기…");
        browseOut.addActionListener(e -> chooseOutputDir());
        outPanel.add(browseOut, BorderLayout.EAST);
        addRow(p, g, r++, "출력 루트(outputDir)", outPanel);

        addRow(p, g, r++, "접속 URL 베이스(baseUrl)", baseUrlField);

        g.gridx = 1; g.gridy = r; g.weightx = 1;
        p.add(idgnrCheck, g);
        return p;
    }

    private void addRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        p.add(new JLabel(label), g);
        g.gridx = 1; g.gridy = row; g.weightx = 1;
        p.add(field, g);
    }

    private JComponent buildCenter() {
        // 좌: DDL 입력(+파일 열기), 우: 결과
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.setBorder(BorderFactory.createTitledBorder("DDL 입력 (MySQL CREATE TABLE)"));
        ddlArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        left.add(new JScrollPane(ddlArea), BorderLayout.CENTER);
        JButton open = new JButton("DDL 파일 열기…");
        open.addActionListener(e -> openDdlFile());
        left.add(open, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("결과"));
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        right.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.5);
        return split;
    }

    private JComponent buildSouth() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        JButton reload = new JButton("설정 다시 불러오기");
        reload.addActionListener(e -> loadConfigIntoForm(defaultConfig));
        JButton gen = new JButton("생성");
        gen.addActionListener(e -> onGenerate());
        p.add(reload);
        p.add(gen);
        return p;
    }

    // ── 동작 ────────────────────────────────────────────────

    private void openDdlFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                ddlArea.setText(Files.readString(fc.getSelectedFile().toPath()));
            } catch (IOException ex) {
                error("DDL 파일을 읽지 못했습니다:\n" + ex.getMessage());
            }
        }
    }

    /** 출력 폴더를 탐색기 창에서 고른다(폴더만 선택). */
    private void chooseOutputDir() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("출력 폴더 선택 — eGov 프로젝트 루트를 고르면 바로 병합됩니다");
        // 현재 입력된 경로가 실재하면 거기서 탐색을 시작한다.
        String cur = outDirField.getText().trim();
        if (!cur.isEmpty()) {
            java.io.File f = new java.io.File(cur);
            if (f.isDirectory()) fc.setCurrentDirectory(f);
        }
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            outDirField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    /** gen.properties를 읽어 폼 필드에 채운다(없으면 내장 기본값). */
    private void loadConfigIntoForm(Path configPath) {
        try {
            GenConfig c = GenConfig.load(Files.exists(configPath) ? configPath : null);
            pkgField.setText(c.basePackage());
            moduleField.setText(c.module());
            prefixField.setText(c.tablePrefix());
            dbTypeField.setText(c.dbType());
            outDirField.setText(c.outputDir());
            baseUrlField.setText(c.baseUrl());
            idgnrCheck.setSelected(c.useIdgnr());
        } catch (IOException ex) {
            error("설정 파일을 읽지 못했습니다:\n" + ex.getMessage());
        }
    }

    private void onGenerate() {
        String ddl = ddlArea.getText();
        if (ddl == null || ddl.isBlank()) {
            error("DDL을 입력하거나 파일을 여세요.");
            return;
        }
        try {
            // gen.properties를 베이스로 읽고(공통컴포넌트 베이스 등 폼 밖 값 보존) 폼값을 덮어쓴다.
            GenConfig cfg = GenConfig.load(Files.exists(defaultConfig) ? defaultConfig : null);
            cfg.setBasePackage(pkgField.getText().trim());
            cfg.setModule(moduleField.getText().trim());
            cfg.setTablePrefix(prefixField.getText().trim());
            cfg.setDbType(dbTypeField.getText().trim());
            cfg.setOutputDir(outDirField.getText().trim());
            cfg.setBaseUrl(baseUrlField.getText().trim());
            cfg.setUseIdgnr(idgnrCheck.isSelected());

            GenerationResult r = new GenerationService().generate(cfg, ddl);
            resultArea.setText(formatResult(r));
            resultArea.setCaretPosition(0);
        } catch (IllegalArgumentException ex) {
            // 파서/설정 입력 오류 (예: 지원하지 않는 DB, DDL 형식)
            resultArea.setText("[입력 오류] " + ex.getMessage());
            error(ex.getMessage());
        } catch (Exception ex) {
            resultArea.setText("[실행 오류] " + ex.getMessage());
            error("생성 중 오류가 발생했습니다:\n" + ex.getMessage());
        }
    }

    private String formatResult(GenerationResult r) {
        var t = r.table();
        StringBuilder sb = new StringBuilder();
        sb.append("[파싱] 테이블 ").append(t.getTableName())
          .append(" → 엔티티 ").append(t.getEntityName())
          .append(" (컬럼 ").append(t.getColumns().size()).append("개, PK=")
          .append(t.primaryKey() != null ? t.primaryKey().getColumnName() : "없음").append(")\n\n");
        sb.append("[생성 완료] ").append(r.files().size()).append("개 파일 → ").append(r.outputDir()).append('\n');
        for (Path f : r.files()) sb.append("  - ").append(f).append('\n');
        sb.append('\n');
        sb.append("[접속 URL] 톰캣 기동 후 브라우저에서 (포트/컨텍스트는 환경에 맞게):\n");
        sb.append("  목록  ").append(r.listUrl()).append('\n');
        sb.append("  등록  ").append(r.registUrl()).append('\n');
        return sb.toString();
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "알림", JOptionPane.WARNING_MESSAGE);
    }
}

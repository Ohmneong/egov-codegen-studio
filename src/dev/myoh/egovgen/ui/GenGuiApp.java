package dev.myoh.egovgen.ui;

import dev.myoh.egovgen.config.GenConfig;
import dev.myoh.egovgen.service.GenerationResult;
import dev.myoh.egovgen.service.GenerationService;
import dev.myoh.egovgen.service.PreviewEntry;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    private final JTextField mapperRootField = new JTextField();
    private final JTextField jspRootField = new JTextField();
    private final JCheckBox idgnrCheck = new JCheckBox("채번(EgovIdGnrService) 사용 — String PK 전제");

    /** 시작 시·생성 시 기본 베이스로 쓰는 설정 파일(작업폴더 기준). 없으면 내장 기본값 사용. */
    private final Path defaultConfig = Path.of("gen.properties");
    /** 프로파일(프로젝트별 설정) 저장 폴더. */
    private final Path profileDir = Path.of("profiles");
    private JComboBox<String> profileCombo;

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

        JPanel north = new JPanel(new BorderLayout());
        north.add(buildProfileBar(), BorderLayout.NORTH);
        north.add(buildSettingsPanel(), BorderLayout.CENTER);
        frame.add(north, BorderLayout.NORTH);
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
        addRow(p, g, r++, "모듈(module)", moduleField);
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
        mapperRootField.setToolTipText("Mapper XML 스캔/출력 루트. 프로젝트 스캔 경로가 다르면 변경(기본 egovframework/mapper)");
        addRow(p, g, r++, "Mapper 출력 루트(mapperRoot)", mapperRootField);
        addRow(p, g, r++, "JSP 출력 루트(jspRoot)", jspRootField);

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
        JButton preview = new JButton("미리보기");
        preview.addActionListener(e -> onPreview());
        JButton gen = new JButton("생성");
        gen.addActionListener(e -> onGenerate());
        p.add(reload);
        p.add(preview);
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
            mapperRootField.setText(c.mapperRoot());
            jspRootField.setText(c.jspRoot());
            idgnrCheck.setSelected(c.useIdgnr());
        } catch (IOException ex) {
            error("설정 파일을 읽지 못했습니다:\n" + ex.getMessage());
        }
    }

    // ── 설정 프로파일 (프로젝트별 gen.properties 저장/전환) ──────────

    private JPanel buildProfileBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        p.setBorder(BorderFactory.createTitledBorder("설정 프로파일 (프로젝트별 gen.properties)"));
        profileCombo = new JComboBox<>();
        refreshProfiles();
        JButton load = new JButton("불러오기");
        load.addActionListener(e -> loadSelectedProfile());
        JButton save = new JButton("현재 설정 저장…");
        save.addActionListener(e -> saveCurrentProfile());
        p.add(new JLabel("프로파일:"));
        p.add(profileCombo);
        p.add(load);
        p.add(save);
        return p;
    }

    /** profiles 폴더의 *.properties 를 콤보박스에 채운다. */
    private void refreshProfiles() {
        profileCombo.removeAllItems();
        if (!Files.isDirectory(profileDir)) return;
        try (var s = Files.list(profileDir)) {
            s.map(f -> f.getFileName().toString())
             .filter(n -> n.endsWith(".properties"))
             .sorted()
             .forEach(n -> profileCombo.addItem(n.substring(0, n.length() - ".properties".length())));
        } catch (IOException ex) {
            error("프로파일 목록을 읽지 못했습니다:\n" + ex.getMessage());
        }
    }

    private void loadSelectedProfile() {
        Object sel = profileCombo.getSelectedItem();
        if (sel == null) { error("불러올 프로파일이 없습니다. 먼저 '현재 설정 저장'으로 만드세요."); return; }
        loadConfigIntoForm(profileDir.resolve(sel + ".properties"));
    }

    private void saveCurrentProfile() {
        String name = JOptionPane.showInputDialog(frame, "프로파일 이름:", "현재 설정 저장", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        name = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_"); // 파일명 안전화
        try {
            formToConfig().saveTo(profileDir.resolve(name + ".properties"));
            refreshProfiles();
            profileCombo.setSelectedItem(name);
            JOptionPane.showMessageDialog(frame, "저장됨: profiles/" + name + ".properties");
        } catch (IOException ex) {
            error("프로파일 저장 실패:\n" + ex.getMessage());
        }
    }

    /** 현재 폼 입력값을 GenConfig 로 만든다(기본 설정을 베이스로 폼값 덮어쓰기). */
    private GenConfig formToConfig() throws IOException {
        GenConfig cfg = GenConfig.load(Files.exists(defaultConfig) ? defaultConfig : null);
        cfg.setBasePackage(pkgField.getText().trim());
        cfg.setModule(moduleField.getText().trim());
        cfg.setTablePrefix(prefixField.getText().trim());
        cfg.setDbType(dbTypeField.getText().trim());
        cfg.setOutputDir(outDirField.getText().trim());
        cfg.setBaseUrl(baseUrlField.getText().trim());
        cfg.setMapperRoot(mapperRootField.getText().trim());
        cfg.setJspRoot(jspRootField.getText().trim());
        cfg.setUseIdgnr(idgnrCheck.isSelected());
        return cfg;
    }

    /** 생성하지 않고, 만들어질 파일 목록과 기존(덮어쓸) 파일을 우측에 보여준다. */
    private void onPreview() {
        String ddl = ddlArea.getText();
        if (ddl == null || ddl.isBlank()) { error("DDL을 입력하거나 파일을 여세요."); return; }
        try {
            List<PreviewEntry> plan = new GenerationService().preview(formToConfig(), ddl);
            long existing = plan.stream().filter(PreviewEntry::exists).count();
            StringBuilder sb = new StringBuilder();
            sb.append("[미리보기] 생성 예정 ").append(plan.size()).append("개 파일")
              .append(existing > 0 ? "  (기존 " + existing + "개 덮어씀)" : "").append("\n\n");
            for (PreviewEntry e : plan) {
                sb.append(e.exists() ? "  [덮어씀] " : "  [신규]   ").append(e.path()).append('\n');
            }
            resultArea.setText(sb.toString());
            resultArea.setCaretPosition(0);
        } catch (Exception ex) {
            error("미리보기 실패:\n" + ex.getMessage());
        }
    }

    private void onGenerate() {
        String ddl = ddlArea.getText();
        if (ddl == null || ddl.isBlank()) {
            error("DDL을 입력하거나 파일을 여세요.");
            return;
        }
        try {
            // 현재 폼값으로 설정 구성
            GenConfig cfg = formToConfig();
            GenerationService svc = new GenerationService();
            // 덮어쓰기 경고: 기존 파일이 있으면 확인
            long existing = svc.preview(cfg, ddl).stream().filter(PreviewEntry::exists).count();
            if (existing > 0) {
                int ans = JOptionPane.showConfirmDialog(frame,
                        existing + "개 파일이 이미 있습니다. 덮어쓸까요?", "덮어쓰기 확인",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ans != JOptionPane.YES_OPTION) return;
            }
            List<GenerationResult> results = svc.generateAll(cfg, ddl);
            StringBuilder sb = new StringBuilder();
            if (results.size() > 1) sb.append("■ ").append(results.size()).append("개 테이블 일괄 생성\n\n");
            for (GenerationResult r : results) sb.append(formatResult(r)).append('\n');
            resultArea.setText(sb.toString());
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

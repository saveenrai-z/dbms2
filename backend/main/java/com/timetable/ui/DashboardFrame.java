package com.timetable.ui;

import com.timetable.db.DatabaseManager;
import com.timetable.model.*;
import com.timetable.algorithm.TimetableGenerator;
import com.timetable.ui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Premium dashboard frame serving as the central workspace for administrators.
 * Integrates CRUD panels, KPI cards, Timetable Grid View, Generator, Search queries, and DBMS docs.
 */
public class DashboardFrame extends JFrame {
    private JPanel contentCards;
    private CardLayout cardLayout;

    // Sidebar navigation buttons
    private JPanel sidebarPanel;
    private String activeTabName = "Overview";

    // Sub-panels
    private TimetableGridView gridView;
    private DBMSDocsPanel dbmsDocsPanel;

    // JTables for CRUD lists
    private JTable tblTeachers, tblSubjects, tblClasses, tblRooms, tblSlots;
    private DefaultTableModel modelTeachers, modelSubjects, modelClasses, modelRooms, modelSlots;

    // Dashboard Overview Stats Labels
    private JLabel lblStatClasses, lblStatTeachers, lblStatRooms, lblStatSemSecs;

    // Generator metrics labels
    private JLabel lblGenStatus, lblGenTime, lblGenBacktracks, lblGenTotal;

    public DashboardFrame() {
        super("Automated Time Table Generation System - Admin Workspace");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 650));

        // Main dark container
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(UIStyle.BG_DARK);
        setContentPane(mainContainer);

        // 1. Sleek Left Sidebar Menu
        createSidebar();
        mainContainer.add(sidebarPanel, BorderLayout.WEST);

        // 2. Central Workspace Content Deck
        cardLayout = new CardLayout();
        contentCards = new JPanel(cardLayout);
        contentCards.setOpaque(false);
        mainContainer.add(contentCards, BorderLayout.CENTER);

        // Create Sub-Panels and add them to Cards
        createOverviewCard();
        createTeacherCrudCard();
        createSubjectCrudCard();
        createClassCrudCard();
        createRoomCrudCard();
        createTimeslotCrudCard();
        createGeneratorCard();
        
        gridView = new TimetableGridView();
        contentCards.add(gridView, "View Timetable");

        createSearchQueryCard();

        dbmsDocsPanel = new DBMSDocsPanel();
        contentCards.add(dbmsDocsPanel, "DBMS Documentation");

        // Set default card on launch
        switchPanel("Overview");
    }

    // ==========================================
    // SIDEBAR NAVIGATION DESIGN
    // ==========================================

    private void createSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setPreferredSize(new Dimension(240, 0));
        sidebarPanel.setBackground(new Color(15, 23, 42)); // Slate 900
        sidebarPanel.setLayout(new BorderLayout());
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIStyle.BORDER_COLOR));

        // Sidebar Header / Brand
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 20));
        brandPanel.setOpaque(false);
        JLabel lblLogo = new JLabel("📅");
        lblLogo.setFont(new Font("SansSerif", Font.PLAIN, 28));
        brandPanel.add(lblLogo);

        JLabel lblBrandName = new JLabel("Time Table Management");
        lblBrandName.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblBrandName.setForeground(UIStyle.TXT_WHITE);
        brandPanel.add(lblBrandName);
        sidebarPanel.add(brandPanel, BorderLayout.NORTH);

        // Navigation links list
        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] navItems = {
            "Overview",
            "Manage Teachers",
            "Manage Subjects",
            "Manage Classes",
            "Manage Rooms",
            "Manage Timeslots",
            "Timetable Generator",
            "View Timetable",
            "Search & Queries",
            "DBMS Documentation"
        };

        for (String item : navItems) {
            navPanel.add(createNavButton(item));
            navPanel.add(Box.createVerticalStrut(6));
        }

        sidebarPanel.add(navPanel, BorderLayout.CENTER);

        // Sidebar Footer: Sign Out
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(15, 10, 15, 10));

        ModernButton btnLogout = new ModernButton("SIGN OUT", UIStyle.ACCENT_RED.darker(), UIStyle.ACCENT_RED);
        btnLogout.setFont(UIStyle.FONT_SUBTEXT);
        btnLogout.setPreferredSize(new Dimension(0, 36));
        btnLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                SwingUtilities.invokeLater(() -> {
                    LoginFrame login = new LoginFrame();
                    login.setVisible(true);
                });
            }
        });
        footerPanel.add(btnLogout, BorderLayout.SOUTH);
        sidebarPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    private JPanel createNavButton(String name) {
        JPanel btn = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (name.equals(activeTabName)) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    UIStyle.enableAntialiasing(g2);
                    g2.setColor(new Color(99, 102, 241, 40)); // Indigo transparent background
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    
                    g2.setColor(UIStyle.ACCENT_INDIGO);
                    g2.fillRect(0, 4, 4, getHeight() - 8); // Active pill indicator
                    g2.dispose();
                }
            }
        };
        btn.setMaximumSize(new Dimension(220, 38));
        btn.setPreferredSize(new Dimension(220, 38));
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lbl = new JLabel("  " + name);
        lbl.setFont(UIStyle.FONT_BODY);
        lbl.setForeground(name.equals(activeTabName) ? UIStyle.TXT_WHITE : UIStyle.TXT_GRAY);
        btn.add(lbl, BorderLayout.CENTER);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                activeTabName = name;
                switchPanel(name);
                sidebarPanel.repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!name.equals(activeTabName)) {
                    lbl.setForeground(UIStyle.TXT_WHITE);
                    btn.setOpaque(true);
                    btn.setBackground(new Color(30, 41, 59, 120)); // Hover slate highlight
                    btn.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setOpaque(false);
                if (!name.equals(activeTabName)) {
                    lbl.setForeground(UIStyle.TXT_GRAY);
                }
                btn.repaint();
            }
        });

        return btn;
    }

    private void switchPanel(String cardName) {
        cardLayout.show(contentCards, cardName);
        
        // Refresh specific panels on view
        if (cardName.equals("Overview")) {
            refreshOverviewStats();
        } else if (cardName.equals("View Timetable")) {
            gridView.refreshGrid();
        }
    }

    // ==========================================
    // 1. OVERVIEW DASHBOARD PANEL
    // ==========================================

    private void createOverviewCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Header
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = new JLabel("📈 Administrative Overview");
        title.setFont(UIStyle.FONT_TITLE);
        title.setForeground(UIStyle.TXT_WHITE);
        head.add(title, BorderLayout.NORTH);

        JLabel sub = new JLabel("Database status, scheduling statistics, and SemSec configurations.");
        sub.setFont(UIStyle.FONT_SUBTEXT);
        sub.setForeground(UIStyle.TXT_GRAY);
        sub.setBorder(new EmptyBorder(4, 0, 15, 0));
        head.add(sub, BorderLayout.SOUTH);
        p.add(head, BorderLayout.NORTH);

        // Grid of KPI Metrics Cards
        JPanel kpiContainer = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiContainer.setOpaque(false);
        kpiContainer.setPreferredSize(new Dimension(0, 120));

        lblStatClasses = new JLabel("0", JLabel.CENTER);
        kpiContainer.add(createKPICard("📅 Scheduled Units", lblStatClasses, "total periods scheduled"));

        lblStatTeachers = new JLabel("0", JLabel.CENTER);
        kpiContainer.add(createKPICard("👨‍🏫 Active Teachers", lblStatTeachers, "faculty members"));

        lblStatRooms = new JLabel("0", JLabel.CENTER);
        kpiContainer.add(createKPICard("🏫 Lecture Rooms", lblStatRooms, "physical classrooms"));

        lblStatSemSecs = new JLabel("0", JLabel.CENTER);
        kpiContainer.add(createKPICard("👥 Student SemSecs", lblStatSemSecs, "class groups"));

        // Content layout container
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        centerPanel.add(kpiContainer, BorderLayout.NORTH);

        // Visual DB Information card
        RoundedPanel dbInfoPanel = new RoundedPanel(18, UIStyle.CARD_BG, UIStyle.BORDER_COLOR);
        dbInfoPanel.setLayout(new BorderLayout());
        dbInfoPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel dbTitle = new JLabel("🛢️ Database Connection Profile");
        dbTitle.setFont(UIStyle.FONT_HEADER);
        dbTitle.setForeground(UIStyle.ACCENT_GLOW);
        dbTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        dbInfoPanel.add(dbTitle, BorderLayout.NORTH);

        DatabaseManager db = DatabaseManager.getInstance();
        StringBuilder dbSummary = new StringBuilder("<html>");
        if (db.isMockMode()) {
            dbSummary.append("<font color='#f59e0b'><b>RUNNING IN LOCAL PERSISTENCE MOCK MODE</b></font><br>");
            dbSummary.append("MongoDB offline. State is read/written cleanly to local JSON file:<br>");
            dbSummary.append("<code style='color:#38bdf8; font-size:12px; font-family:Consolas;'>").append(new java.io.File("local_database.json").getAbsolutePath()).append("</code><br><br>");
            dbSummary.append("All primary/foreign constraints and unique indexing validations are simulated inside the Mock Database Manager engine.");
        } else {
            dbSummary.append("<font color='#10b981'><b>CONNECTED TO ACTIVE MONGODB SERVER</b></font><br>");
            dbSummary.append("Database URL: <code style='color:#38bdf8; font-size:12px; font-family:Consolas;'>mongodb://localhost:27017</code><br>");
            dbSummary.append("Target Schema: <code style='color:#38bdf8; font-size:12px; font-family:Consolas;'>automated_timetable_system</code><br><br>");
            dbSummary.append("Documents are serialized using BSON protocol into collections: <i>teachers, subjects, classes, rooms, timeslots,</i> and <i>timetable</i>.");
        }
        dbSummary.append("</html>");

        JLabel lblDbDetail = new JLabel(dbSummary.toString());
        lblDbDetail.setFont(UIStyle.FONT_BODY);
        lblDbDetail.setForeground(UIStyle.TXT_LIGHT);
        dbInfoPanel.add(lblDbDetail, BorderLayout.CENTER);

        centerPanel.add(dbInfoPanel, BorderLayout.CENTER);
        p.add(centerPanel, BorderLayout.CENTER);

        contentCards.add(p, "Overview");
        refreshOverviewStats();
    }

    private RoundedPanel createKPICard(String title, JLabel valueLabel, String subtext) {
        RoundedPanel card = new RoundedPanel(16, UIStyle.CARD_BG, UIStyle.BORDER_COLOR);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UIStyle.FONT_SUBTEXT);
        lblTitle.setForeground(UIStyle.TXT_GRAY);
        card.add(lblTitle, BorderLayout.NORTH);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        valueLabel.setForeground(UIStyle.TXT_WHITE);
        card.add(valueLabel, BorderLayout.CENTER);

        JLabel lblSub = new JLabel(subtext, JLabel.CENTER);
        lblSub.setFont(UIStyle.FONT_SUBTEXT);
        lblSub.setForeground(UIStyle.TXT_GRAY);
        card.add(lblSub, BorderLayout.SOUTH);

        return card;
    }

    private void refreshOverviewStats() {
        DatabaseManager db = DatabaseManager.getInstance();
        lblStatClasses.setText(String.valueOf(db.getTimetable().size()));
        lblStatTeachers.setText(String.valueOf(db.getTeachers().size()));
        lblStatRooms.setText(String.valueOf(db.getRooms().size()));
        lblStatSemSecs.setText(String.valueOf(db.getClassGroups().size()));
    }

    // ==========================================
    // 2. CRUD PANEL: TEACHERS
    // ==========================================

    private void createTeacherCrudCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Title Header
        p.add(createHeaderLabel("👨‍🏫 Manage Faculty Members", "Add, edit, or delete teaching staff."), BorderLayout.NORTH);

        // Forms left + Tables right split
        JPanel split = new JPanel(new GridLayout(1, 2, 20, 0));
        split.setOpaque(false);
        split.setBorder(new EmptyBorder(15, 0, 0, 0));

        // Form Card Panel
        RoundedPanel formCard = new RoundedPanel(16, UIStyle.CARD_BG, UIStyle.BORDER_COLOR);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        JLabel lblFormHead = new JLabel("Teacher Registration Profile");
        lblFormHead.setFont(UIStyle.FONT_HEADER);
        lblFormHead.setForeground(UIStyle.ACCENT_GLOW);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 20, 0);
        formCard.add(lblFormHead, gbc);

        JLabel lblId = new JLabel("TEACHER ID");
        lblId.setFont(UIStyle.FONT_SUBTEXT);
        lblId.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 6, 0);
        formCard.add(lblId, gbc);

        ModernTextField txtId = new ModernTextField("e.g. T106");
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        formCard.add(txtId, gbc);

        JLabel lblName = new JLabel("FULL NAME");
        lblName.setFont(UIStyle.FONT_SUBTEXT);
        lblName.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        formCard.add(lblName, gbc);

        ModernTextField txtName = new ModernTextField("e.g. Dr. Richard Feynman");
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 25, 0);
        formCard.add(txtName, gbc);

        // Buttons row
        JPanel btns = new JPanel(new GridLayout(1, 2, 10, 0));
        btns.setOpaque(false);
        
        ModernButton btnSave = new ModernButton("SAVE RECORD");
        ModernButton btnReset = new ModernButton("RESET FORM", UIStyle.CARD_HOVER, UIStyle.BORDER_COLOR);
        btns.add(btnReset);
        btns.add(btnSave);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 10, 0);
        formCard.add(btns, gbc);

        split.add(formCard);

        // Table Panel (Right)
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);

        modelTeachers = new DefaultTableModel(new String[]{"Teacher ID", "Full Name"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblTeachers = new JTable(modelTeachers);
        ModernTableHelper.styleTable(tblTeachers);
        
        JScrollPane scroll = new JScrollPane(tblTeachers);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1));
        tableContainer.add(scroll, BorderLayout.CENTER);

        // Bottom Action delete button
        ModernButton btnDelete = new ModernButton("DELETE SELECTED TEACHER", UIStyle.ACCENT_RED.darker(), UIStyle.ACCENT_RED);
        btnDelete.setPreferredSize(new Dimension(0, 36));
        tableContainer.add(btnDelete, BorderLayout.SOUTH);

        split.add(tableContainer);
        p.add(split, BorderLayout.CENTER);

        contentCards.add(p, "Manage Teachers");

        // Database CRUD Actions
        Runnable loadAction = () -> {
            modelTeachers.setRowCount(0);
            for (Teacher t : DatabaseManager.getInstance().getTeachers()) {
                modelTeachers.addRow(new Object[]{t.getTeacherId(), t.getName()});
            }
        };
        loadAction.run();

        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = txtId.getText().trim();
                String name = txtName.getText().trim();

                if (id.isEmpty() || name.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Please enter both Teacher ID and Name!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Teacher t = new Teacher(id, name);
                boolean saved = DatabaseManager.getInstance().saveTeacher(t);
                if (saved) {
                    JOptionPane.showMessageDialog(null, "Teacher saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    txtId.setText("");
                    txtName.setText("");
                    loadAction.run();
                } else {
                    JOptionPane.showMessageDialog(null, "Unique Key Violation! ID '" + id + "' is already registered.", "Save Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnReset.addActionListener(e -> {
            txtId.setText("");
            txtName.setText("");
        });

        btnDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = tblTeachers.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(null, "Please select a teacher from the table to delete!", "Selection Empty", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String id = modelTeachers.getValueAt(row, 0).toString();
                int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete teacher " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    DatabaseManager.getInstance().deleteTeacher(id);
                    loadAction.run();
                }
            }
        });

        // Click row to edit
        tblTeachers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblTeachers.getSelectedRow();
                if (row >= 0) {
                    txtId.setText(modelTeachers.getValueAt(row, 0).toString());
                    txtName.setText(modelTeachers.getValueAt(row, 1).toString());
                }
            }
        });
    }

    // ==========================================
    // 3. CRUD PANEL: SUBJECTS
    // ==========================================

    private void createSubjectCrudCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(25, 25, 25, 25));

        p.add(createHeaderLabel("📚 Manage Academic Subjects", "Add, edit, or delete syllabus courses."), BorderLayout.NORTH);

        JPanel split = new JPanel(new GridLayout(1, 2, 20, 0));
        split.setOpaque(false);
        split.setBorder(new EmptyBorder(15, 0, 0, 0));

        // Form Card Panel
        RoundedPanel formCard = new RoundedPanel(16, UIStyle.CARD_BG, UIStyle.BORDER_COLOR);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        JLabel lblFormHead = new JLabel("Subject Specification");
        lblFormHead.setFont(UIStyle.FONT_HEADER);
        lblFormHead.setForeground(UIStyle.ACCENT_GLOW);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 15, 0);
        formCard.add(lblFormHead, gbc);

        JLabel lblId = new JLabel("SUBJECT ID");
        lblId.setFont(UIStyle.FONT_SUBTEXT);
        lblId.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 4, 0);
        formCard.add(lblId, gbc);

        ModernTextField txtId = new ModernTextField("e.g. S106");
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 12, 0);
        formCard.add(txtId, gbc);

        JLabel lblName = new JLabel("SUBJECT NAME");
        lblName.setFont(UIStyle.FONT_SUBTEXT);
        lblName.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 4, 0);
        formCard.add(lblName, gbc);

        ModernTextField txtName = new ModernTextField("e.g. Theory of Computation");
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 12, 0);
        formCard.add(txtName, gbc);

        JLabel lblHours = new JLabel("HOURS REQUIRED PER WEEK");
        lblHours.setFont(UIStyle.FONT_SUBTEXT);
        lblHours.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 4, 0);
        formCard.add(lblHours, gbc);

        ModernTextField txtHours = new ModernTextField("e.g. 4");
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 12, 0);
        formCard.add(txtHours, gbc);

        // Class and Teacher foreign key dropdowns
        JLabel lblClass = new JLabel("TARGET SEMSEC");
        lblClass.setFont(UIStyle.FONT_SUBTEXT);
        lblClass.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 4, 0);
        formCard.add(lblClass, gbc);

        JComboBox<ClassGroup> cbClass = new JComboBox<>();
        cbClass.setBackground(UIStyle.CARD_BG);
        cbClass.setForeground(UIStyle.TXT_WHITE);
        cbClass.setFont(UIStyle.FONT_BODY);
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 12, 0);
        formCard.add(cbClass, gbc);

        JLabel lblTeacher = new JLabel("ASSIGNED INSTRUCTOR");
        lblTeacher.setFont(UIStyle.FONT_SUBTEXT);
        lblTeacher.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 0, 4, 0);
        formCard.add(lblTeacher, gbc);

        JComboBox<Teacher> cbTeacher = new JComboBox<>();
        cbTeacher.setBackground(UIStyle.CARD_BG);
        cbTeacher.setForeground(UIStyle.TXT_WHITE);
        cbTeacher.setFont(UIStyle.FONT_BODY);
        gbc.gridy = 10;
        gbc.insets = new Insets(0, 0, 20, 0);
        formCard.add(cbTeacher, gbc);

        JPanel btns = new JPanel(new GridLayout(1, 2, 10, 0));
        btns.setOpaque(false);
        ModernButton btnSave = new ModernButton("SAVE RECORD");
        ModernButton btnReset = new ModernButton("RESET FORM", UIStyle.CARD_HOVER, UIStyle.BORDER_COLOR);
        btns.add(btnReset);
        btns.add(btnSave);

        gbc.gridy = 11;
        gbc.insets = new Insets(0, 0, 5, 0);
        formCard.add(btns, gbc);

        split.add(formCard);

        // Table Panel (Right)
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);

        modelSubjects = new DefaultTableModel(new String[]{"Sub ID", "Name", "Hrs/Wk", "SemSec", "Instructor"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblSubjects = new JTable(modelSubjects);
        ModernTableHelper.styleTable(tblSubjects);
        
        JScrollPane scroll = new JScrollPane(tblSubjects);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1));
        tableContainer.add(scroll, BorderLayout.CENTER);

        ModernButton btnDelete = new ModernButton("DELETE SELECTED SUBJECT", UIStyle.ACCENT_RED.darker(), UIStyle.ACCENT_RED);
        btnDelete.setPreferredSize(new Dimension(0, 36));
        tableContainer.add(btnDelete, BorderLayout.SOUTH);

        split.add(tableContainer);
        p.add(split, BorderLayout.CENTER);

        contentCards.add(p, "Manage Subjects");

        // Action loading
        Runnable loadAction = () -> {
            DatabaseManager db = DatabaseManager.getInstance();
            modelSubjects.setRowCount(0);
            
            // Populate dropdowns
            cbClass.removeAllItems();
            for (ClassGroup cg : db.getClassGroups()) cbClass.addItem(cg);

            cbTeacher.removeAllItems();
            for (Teacher t : db.getTeachers()) cbTeacher.addItem(t);

            for (Subject s : db.getSubjects()) {
                modelSubjects.addRow(new Object[]{
                    s.getSubId(), s.getSubName(), s.getHours(), s.getClassId(), s.getTeacherId()
                });
            }
        };
        loadAction.run();

        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = txtId.getText().trim();
                String name = txtName.getText().trim();
                String hrsStr = txtHours.getText().trim();
                ClassGroup selectedClass = (ClassGroup) cbClass.getSelectedItem();
                Teacher selectedTeacher = (Teacher) cbTeacher.getSelectedItem();

                if (id.isEmpty() || name.isEmpty() || hrsStr.isEmpty() || selectedClass == null || selectedTeacher == null) {
                    JOptionPane.showMessageDialog(null, "Please fill out all subject details completely!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int hours;
                try {
                    hours = Integer.parseInt(hrsStr);
                    if (hours <= 0 || hours > 10) throw new NumberFormatException();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Weekly hours must be a positive integer between 1 and 10!", "Invalid Hours", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Subject sub = new Subject(id, name, hours, selectedClass.getClassId(), selectedTeacher.getTeacherId());
                boolean saved = DatabaseManager.getInstance().saveSubject(sub);
                if (saved) {
                    JOptionPane.showMessageDialog(null, "Subject saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    txtId.setText("");
                    txtName.setText("");
                    txtHours.setText("");
                    loadAction.run();
                } else {
                    JOptionPane.showMessageDialog(null, "Unique Key Violation! ID '" + id + "' is already registered.", "Save Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnReset.addActionListener(e -> {
            txtId.setText("");
            txtName.setText("");
            txtHours.setText("");
        });

        btnDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = tblSubjects.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(null, "Please select a subject from the table to delete!", "Selection Empty", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String id = modelSubjects.getValueAt(row, 0).toString();
                int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete subject " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    DatabaseManager.getInstance().deleteSubject(id);
                    loadAction.run();
                }
            }
        });
    }

    // ==========================================
    // 4. CRUD PANEL: CLASSES
    // ==========================================

    private void createClassCrudCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(25, 25, 25, 25));

        p.add(createHeaderLabel("👥 Manage SemSecs", "Add, edit, or delete semester & sections."), BorderLayout.NORTH);

        JPanel split = new JPanel(new GridLayout(1, 2, 20, 0));
        split.setOpaque(false);
        split.setBorder(new EmptyBorder(15, 0, 0, 0));

        // Form Card Panel
        RoundedPanel formCard = new RoundedPanel(16, UIStyle.CARD_BG, UIStyle.BORDER_COLOR);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        JLabel lblFormHead = new JLabel("Class Registration Profile");
        lblFormHead.setFont(UIStyle.FONT_HEADER);
        lblFormHead.setForeground(UIStyle.ACCENT_GLOW);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 20, 0);
        formCard.add(lblFormHead, gbc);

        JLabel lblId = new JLabel("CLASS ID (UNIQUE SHORT CODE)");
        lblId.setFont(UIStyle.FONT_SUBTEXT);
        lblId.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 6, 0);
        formCard.add(lblId, gbc);

        ModernTextField txtId = new ModernTextField("e.g. CSE5A");
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        formCard.add(txtId, gbc);

        JLabel lblName = new JLabel("DEPARTMENT NAME / BRANCH");
        lblName.setFont(UIStyle.FONT_SUBTEXT);
        lblName.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        formCard.add(lblName, gbc);

        ModernTextField txtName = new ModernTextField("e.g. Computer Science");
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 20, 0);
        formCard.add(txtName, gbc);

        JLabel lblSem = new JLabel("SEMESTER TERM");
        lblSem.setFont(UIStyle.FONT_SUBTEXT);
        lblSem.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 6, 0);
        formCard.add(lblSem, gbc);

        ModernTextField txtSem = new ModernTextField("e.g. 5");
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 25, 0);
        formCard.add(txtSem, gbc);

        JPanel btns = new JPanel(new GridLayout(1, 2, 10, 0));
        btns.setOpaque(false);
        ModernButton btnSave = new ModernButton("SAVE RECORD");
        ModernButton btnReset = new ModernButton("RESET FORM", UIStyle.CARD_HOVER, UIStyle.BORDER_COLOR);
        btns.add(btnReset);
        btns.add(btnSave);

        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 10, 0);
        formCard.add(btns, gbc);

        split.add(formCard);

        // Table Panel (Right)
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);

        modelClasses = new DefaultTableModel(new String[]{"Class ID", "Branch Name", "Semester"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblClasses = new JTable(modelClasses);
        ModernTableHelper.styleTable(tblClasses);
        
        JScrollPane scroll = new JScrollPane(tblClasses);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1));
        tableContainer.add(scroll, BorderLayout.CENTER);

        ModernButton btnDelete = new ModernButton("DELETE SELECTED CLASS", UIStyle.ACCENT_RED.darker(), UIStyle.ACCENT_RED);
        btnDelete.setPreferredSize(new Dimension(0, 36));
        tableContainer.add(btnDelete, BorderLayout.SOUTH);

        split.add(tableContainer);
        p.add(split, BorderLayout.CENTER);

        contentCards.add(p, "Manage Classes");

        // Action actions
        Runnable loadAction = () -> {
            modelClasses.setRowCount(0);
            for (ClassGroup cg : DatabaseManager.getInstance().getClassGroups()) {
                modelClasses.addRow(new Object[]{cg.getClassId(), cg.getClassName(), cg.getSemester()});
            }
        };
        loadAction.run();

        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = txtId.getText().trim();
                String name = txtName.getText().trim();
                String semStr = txtSem.getText().trim();

                if (id.isEmpty() || name.isEmpty() || semStr.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Please enter all SemSec details completely!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int sem;
                try {
                    sem = Integer.parseInt(semStr);
                    if (sem <= 0 || sem > 8) throw new NumberFormatException();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Semester must be a positive integer between 1 and 8!", "Invalid Semester", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ClassGroup cg = new ClassGroup(id, name, sem);
                boolean saved = DatabaseManager.getInstance().saveClassGroup(cg);
                if (saved) {
                    JOptionPane.showMessageDialog(null, "Class saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    txtId.setText("");
                    txtName.setText("");
                    txtSem.setText("");
                    loadAction.run();
                } else {
                    JOptionPane.showMessageDialog(null, "Unique Key Violation! ID '" + id + "' is already registered.", "Save Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnReset.addActionListener(e -> {
            txtId.setText("");
            txtName.setText("");
            txtSem.setText("");
        });

        btnDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = tblClasses.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(null, "Please select a class from the table to delete!", "Selection Empty", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String id = modelClasses.getValueAt(row, 0).toString();
                int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete class " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    DatabaseManager.getInstance().deleteClassGroup(id);
                    loadAction.run();
                }
            }
        });
    }

    // ==========================================
    // 5. CRUD PANEL: ROOMS
    // ==========================================

    private void createRoomCrudCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(25, 25, 25, 25));

        p.add(createHeaderLabel("🏫 Manage Lecture Rooms", "Add, edit, or delete classrooms and laboratories."), BorderLayout.NORTH);

        JPanel split = new JPanel(new GridLayout(1, 2, 20, 0));
        split.setOpaque(false);
        split.setBorder(new EmptyBorder(15, 0, 0, 0));

        RoundedPanel formCard = new RoundedPanel(16, UIStyle.CARD_BG, UIStyle.BORDER_COLOR);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        JLabel lblFormHead = new JLabel("Classroom Configuration");
        lblFormHead.setFont(UIStyle.FONT_HEADER);
        lblFormHead.setForeground(UIStyle.ACCENT_GLOW);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 20, 0);
        formCard.add(lblFormHead, gbc);

        JLabel lblId = new JLabel("ROOM ID (SYSTEM SERIAL)");
        lblId.setFont(UIStyle.FONT_SUBTEXT);
        lblId.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 6, 0);
        formCard.add(lblId, gbc);

        ModernTextField txtId = new ModernTextField("e.g. R104");
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        formCard.add(txtId, gbc);

        JLabel lblNo = new JLabel("PHYSICAL ROOM NUMBER / NAME");
        lblNo.setFont(UIStyle.FONT_SUBTEXT);
        lblNo.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        formCard.add(lblNo, gbc);

        ModernTextField txtNo = new ModernTextField("e.g. 104 (Physics Lab)");
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 30, 0);
        formCard.add(txtNo, gbc);

        JPanel btns = new JPanel(new GridLayout(1, 2, 10, 0));
        btns.setOpaque(false);
        ModernButton btnSave = new ModernButton("SAVE RECORD");
        ModernButton btnReset = new ModernButton("RESET FORM", UIStyle.CARD_HOVER, UIStyle.BORDER_COLOR);
        btns.add(btnReset);
        btns.add(btnSave);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 10, 0);
        formCard.add(btns, gbc);

        split.add(formCard);

        // Table Panel (Right)
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);

        modelRooms = new DefaultTableModel(new String[]{"Room ID", "Room Number / Name"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRooms = new JTable(modelRooms);
        ModernTableHelper.styleTable(tblRooms);
        
        JScrollPane scroll = new JScrollPane(tblRooms);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1));
        tableContainer.add(scroll, BorderLayout.CENTER);

        ModernButton btnDelete = new ModernButton("DELETE SELECTED ROOM", UIStyle.ACCENT_RED.darker(), UIStyle.ACCENT_RED);
        btnDelete.setPreferredSize(new Dimension(0, 36));
        tableContainer.add(btnDelete, BorderLayout.SOUTH);

        split.add(tableContainer);
        p.add(split, BorderLayout.CENTER);

        contentCards.add(p, "Manage Rooms");

        // Action actions
        Runnable loadAction = () -> {
            modelRooms.setRowCount(0);
            for (Room r : DatabaseManager.getInstance().getRooms()) {
                modelRooms.addRow(new Object[]{r.getRoomId(), r.getRoomNo()});
            }
        };
        loadAction.run();

        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = txtId.getText().trim();
                String no = txtNo.getText().trim();

                if (id.isEmpty() || no.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Please enter both Room ID and Room Number!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Room rm = new Room(id, no);
                boolean saved = DatabaseManager.getInstance().saveRoom(rm);
                if (saved) {
                    JOptionPane.showMessageDialog(null, "Room saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    txtId.setText("");
                    txtNo.setText("");
                    loadAction.run();
                } else {
                    JOptionPane.showMessageDialog(null, "Unique Key Violation! ID '" + id + "' is already registered.", "Save Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnReset.addActionListener(e -> {
            txtId.setText("");
            txtNo.setText("");
        });

        btnDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = tblRooms.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(null, "Please select a room from the table to delete!", "Selection Empty", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String id = modelRooms.getValueAt(row, 0).toString();
                int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete room " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    DatabaseManager.getInstance().deleteRoom(id);
                    loadAction.run();
                }
            }
        });
    }

    // ==========================================
    // 6. CRUD PANEL: TIMESLOTS
    // ==========================================

    private void createTimeslotCrudCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(25, 25, 25, 25));

        p.add(createHeaderLabel("⏱️ Manage Active Time Slots", "Define weekly educational hour spans."), BorderLayout.NORTH);

        JPanel split = new JPanel(new GridLayout(1, 2, 20, 0));
        split.setOpaque(false);
        split.setBorder(new EmptyBorder(15, 0, 0, 0));

        RoundedPanel formCard = new RoundedPanel(16, UIStyle.CARD_BG, UIStyle.BORDER_COLOR);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        JLabel lblFormHead = new JLabel("Time Slot Registration");
        lblFormHead.setFont(UIStyle.FONT_HEADER);
        lblFormHead.setForeground(UIStyle.ACCENT_GLOW);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 15, 0);
        formCard.add(lblFormHead, gbc);

        JLabel lblId = new JLabel("TIME SLOT ID");
        lblId.setFont(UIStyle.FONT_SUBTEXT);
        lblId.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 6, 0);
        formCard.add(lblId, gbc);

        ModernTextField txtId = new ModernTextField("e.g. SL36");
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 12, 0);
        formCard.add(txtId, gbc);

        JLabel lblDay = new JLabel("WEEK DAY");
        lblDay.setFont(UIStyle.FONT_SUBTEXT);
        lblDay.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        formCard.add(lblDay, gbc);

        JComboBox<String> cbDay = new JComboBox<>(new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"});
        cbDay.setBackground(UIStyle.CARD_BG);
        cbDay.setForeground(UIStyle.TXT_WHITE);
        cbDay.setFont(UIStyle.FONT_BODY);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 12, 0);
        formCard.add(cbDay, gbc);

        JLabel lblTime = new JLabel("HOUR TIME SPAN");
        lblTime.setFont(UIStyle.FONT_SUBTEXT);
        lblTime.setForeground(UIStyle.TXT_LIGHT);
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 6, 0);
        formCard.add(lblTime, gbc);

        ModernTextField txtTime = new ModernTextField("e.g. 04:00 PM - 05:00 PM");
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 15, 0);
        formCard.add(txtTime, gbc);

        JCheckBox chkLunch = new JCheckBox("Mark as Lunch Break period (Locked slot)");
        chkLunch.setOpaque(false);
        chkLunch.setForeground(UIStyle.TXT_LIGHT);
        chkLunch.setFont(UIStyle.FONT_BODY);
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 20, 0);
        formCard.add(chkLunch, gbc);

        JPanel btns = new JPanel(new GridLayout(1, 2, 10, 0));
        btns.setOpaque(false);
        ModernButton btnSave = new ModernButton("SAVE RECORD");
        ModernButton btnReset = new ModernButton("RESET FORM", UIStyle.CARD_HOVER, UIStyle.BORDER_COLOR);
        btns.add(btnReset);
        btns.add(btnSave);

        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 10, 0);
        formCard.add(btns, gbc);

        split.add(formCard);

        // Table Panel (Right)
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);

        modelSlots = new DefaultTableModel(new String[]{"Slot ID", "Day", "Time Span", "Lunch Break?"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblSlots = new JTable(modelSlots);
        ModernTableHelper.styleTable(tblSlots);
        
        JScrollPane scroll = new JScrollPane(tblSlots);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1));
        tableContainer.add(scroll, BorderLayout.CENTER);

        ModernButton btnDelete = new ModernButton("DELETE SELECTED SLOT", UIStyle.ACCENT_RED.darker(), UIStyle.ACCENT_RED);
        btnDelete.setPreferredSize(new Dimension(0, 36));
        tableContainer.add(btnDelete, BorderLayout.SOUTH);

        split.add(tableContainer);
        p.add(split, BorderLayout.CENTER);

        contentCards.add(p, "Manage Timeslots");

        // Action actions
        Runnable loadAction = () -> {
            modelSlots.setRowCount(0);
            for (Timeslot s : DatabaseManager.getInstance().getTimeslots()) {
                modelSlots.addRow(new Object[]{s.getSlotId(), s.getDay(), s.getTime(), s.isLunchBreak() ? "Yes" : "No"});
            }
        };
        loadAction.run();

        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = txtId.getText().trim();
                String day = cbDay.getSelectedItem().toString();
                String time = txtTime.getText().trim();
                boolean isLunch = chkLunch.isSelected();

                if (id.isEmpty() || time.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Please enter all time slot details completely!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Timeslot s = new Timeslot(id, day, time, isLunch);
                boolean saved = DatabaseManager.getInstance().saveTimeslot(s);
                if (saved) {
                    JOptionPane.showMessageDialog(null, "Time slot saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    txtId.setText("");
                    txtTime.setText("");
                    chkLunch.setSelected(false);
                    loadAction.run();
                } else {
                    JOptionPane.showMessageDialog(null, "Unique Key Violation! ID '" + id + "' is already registered.", "Save Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnReset.addActionListener(e -> {
            txtId.setText("");
            txtTime.setText("");
            chkLunch.setSelected(false);
        });

        btnDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = tblSlots.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(null, "Please select a time slot from the table to delete!", "Selection Empty", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String id = modelSlots.getValueAt(row, 0).toString();
                int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete slot " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    DatabaseManager.getInstance().deleteTimeslot(id);
                    loadAction.run();
                }
            }
        });
    }

    // ==========================================
    // 7. TIMETABLE AUTOMATIC GENERATOR PANEL
    // ==========================================

    private void createGeneratorCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(25, 25, 25, 25));

        p.add(createHeaderLabel("⚡ Algorithmic Timetable Generator", "Execute the backtracking constraint solver engine."), BorderLayout.NORTH);

        // Splitting into controls card + metrics list
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        // Left control action panel
        RoundedPanel controlCard = new RoundedPanel(18, UIStyle.CARD_BG, UIStyle.BORDER_COLOR);
        controlCard.setLayout(new GridBagLayout());
        controlCard.setBorder(new EmptyBorder(25, 30, 25, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        JLabel lblGHead = new JLabel("Constraint-Satisfaction Scheduler");
        lblGHead.setFont(UIStyle.FONT_HEADER);
        lblGHead.setForeground(UIStyle.TXT_WHITE);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 15, 0);
        controlCard.add(lblGHead, gbc);

        JLabel lblInfo = new JLabel("<html>The scheduler reads all teachers, rooms, SemSecs, timeslots, and subject specifications to generate a conflict-free time table. All constraints are strictly verified in <i>O(1)</i> time:</html>");
        lblInfo.setFont(UIStyle.FONT_BODY);
        lblInfo.setForeground(UIStyle.TXT_GRAY);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 30, 0);
        controlCard.add(lblInfo, gbc);

        ModernButton btnGenerate = new ModernButton("🚀 GENERATE OPTIMIZED TIMETABLE");
        btnGenerate.setFont(UIStyle.FONT_BODY_BOLD);
        btnGenerate.setPreferredSize(new Dimension(0, 50));
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        controlCard.add(btnGenerate, gbc);

        ModernButton btnClear = new ModernButton("🧹 CLEAR CURRENT TIMETABLE", UIStyle.ACCENT_RED.darker(), UIStyle.ACCENT_RED);
        btnClear.setFont(UIStyle.FONT_BODY_BOLD);
        btnClear.setPreferredSize(new Dimension(0, 45));
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        controlCard.add(btnClear, gbc);

        centerPanel.add(controlCard);

        // Right metrics card panel
        RoundedPanel metricsCard = new RoundedPanel(18, UIStyle.CARD_BG, UIStyle.BORDER_COLOR);
        metricsCard.setLayout(new GridBagLayout());
        metricsCard.setBorder(new EmptyBorder(25, 30, 25, 30));
        GridBagConstraints mGbc = new GridBagConstraints();
        mGbc.fill = GridBagConstraints.HORIZONTAL;
        mGbc.weightx = 1.0;
        mGbc.gridx = 0;

        JLabel lblMHead = new JLabel("Solver Output Metrics");
        lblMHead.setFont(UIStyle.FONT_HEADER);
        lblMHead.setForeground(UIStyle.ACCENT_INDIGO);
        mGbc.gridy = 0;
        mGbc.insets = new Insets(0, 0, 20, 0);
        metricsCard.add(lblMHead, mGbc);

        lblGenStatus = new JLabel("STATUS: TIMETABLE NOT INJECTED", JLabel.LEFT);
        lblGenStatus.setFont(UIStyle.FONT_BODY_BOLD);
        lblGenStatus.setForeground(UIStyle.ACCENT_RED);
        mGbc.gridy = 1;
        mGbc.insets = new Insets(0, 0, 15, 0);
        metricsCard.add(lblGenStatus, mGbc);

        lblGenTime = new JLabel("⏱️ Execution Speed: 0 ms");
        lblGenTime.setFont(UIStyle.FONT_BODY);
        lblGenTime.setForeground(UIStyle.TXT_LIGHT);
        mGbc.gridy = 2;
        mGbc.insets = new Insets(0, 0, 10, 0);
        metricsCard.add(lblGenTime, mGbc);

        lblGenBacktracks = new JLabel("🔄 Backtrack Recurrence Retries: 0");
        lblGenBacktracks.setFont(UIStyle.FONT_BODY);
        lblGenBacktracks.setForeground(UIStyle.TXT_LIGHT);
        mGbc.gridy = 3;
        mGbc.insets = new Insets(0, 0, 10, 0);
        metricsCard.add(lblGenBacktracks, mGbc);

        lblGenTotal = new JLabel("📋 Total Schedules Formulated: 0");
        lblGenTotal.setFont(UIStyle.FONT_BODY);
        lblGenTotal.setForeground(UIStyle.TXT_LIGHT);
        mGbc.gridy = 4;
        mGbc.insets = new Insets(0, 0, 0, 0);
        metricsCard.add(lblGenTotal, mGbc);

        centerPanel.add(metricsCard);
        p.add(centerPanel, BorderLayout.CENTER);

        contentCards.add(p, "Timetable Generator");

        // Action handlers
        btnGenerate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Confirm there is data to schedule
                DatabaseManager db = DatabaseManager.getInstance();
                if (db.getSubjects().isEmpty() || db.getRooms().isEmpty() || db.getTimeslots().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Cannot generate schedule! Subjects, Rooms, and Timeslots databases cannot be empty.", "Engine Aborted", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                btnGenerate.setEnabled(false);
                btnGenerate.setText("SOLVING CONSTRAINTS...");
                
                SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                    private TimetableGenerator generator;

                    @Override
                    protected Boolean doInBackground() throws Exception {
                        generator = new TimetableGenerator();
                        return generator.generate();
                    }

                    @Override
                    protected void done() {
                        try {
                            boolean completed = get();
                            btnGenerate.setEnabled(true);
                            btnGenerate.setText("🚀 GENERATE OPTIMIZED TIMETABLE");
                            
                            if (completed) {
                                lblGenStatus.setText("STATUS: SOLVER COMPLETED CONFLICT-FREE");
                                lblGenStatus.setForeground(UIStyle.ACCENT_GREEN);
                                lblGenTime.setText("⏱️ Execution Speed: " + generator.getExecutionTimeMs() + " ms");
                                lblGenBacktracks.setText("🔄 Backtrack Recurrence Retries: " + generator.getBacktrackCount());
                                lblGenTotal.setText("📋 Total Schedules Formulated: " + generator.getTotalAllocations());
                                
                                JOptionPane.showMessageDialog(null, "Optimal schedule formulated successfully with zero clashes!", "Scheduling Success", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                lblGenStatus.setText("STATUS: SOLVER ABORTED (INCOMPLETE)");
                                lblGenStatus.setForeground(UIStyle.ACCENT_RED);
                                JOptionPane.showMessageDialog(null, "Constraint boundary limit exceeded! No conflict-free solution is mathematically possible with current datasets. Please add more rooms/timeslots or reduce subject hours.", "Solver Overload", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ex) {
                            btnGenerate.setEnabled(true);
                            btnGenerate.setText("🚀 GENERATE OPTIMIZED TIMETABLE");
                            JOptionPane.showMessageDialog(null, "Scheduling critical runtime error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();
            }
        });

        btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to completely wipe the current timetable schedule?", "Confirm Wipe", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    DatabaseManager.getInstance().clearTimetable();
                    lblGenStatus.setText("STATUS: TIMETABLE WIPED");
                    lblGenStatus.setForeground(UIStyle.ACCENT_RED);
                    lblGenTime.setText("⏱️ Execution Speed: 0 ms");
                    lblGenBacktracks.setText("🔄 Backtrack Recurrence Retries: 0");
                    lblGenTotal.setText("📋 Total Schedules Formulated: 0");
                    JOptionPane.showMessageDialog(null, "Timetable cleared successfully!", "Timetable Wiped", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
    }

    // ==========================================
    // 8. ADVANCED SEARCH & FILTERING PANEL
    // ==========================================

    private void createSearchQueryCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(25, 25, 25, 25));

        p.add(createHeaderLabel("🔍 Analytical Search and Queries", "Query and filter solved schedules dynamically."), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(15, 0, 0, 0));

        // Filters card
        RoundedPanel filterCard = new RoundedPanel(16, UIStyle.CARD_BG, UIStyle.BORDER_COLOR);
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 15));
        
        JLabel lblSearch = new JLabel("Search Keyword:");
        lblSearch.setFont(UIStyle.FONT_BODY_BOLD);
        lblSearch.setForeground(UIStyle.TXT_LIGHT);
        filterCard.add(lblSearch);

        ModernTextField txtSearch = new ModernTextField("Search class, teacher, room, or subject...");
        txtSearch.setPreferredSize(new Dimension(350, 30));
        filterCard.add(txtSearch);

        ModernButton btnSearch = new ModernButton("EXECUTE QUERY");
        btnSearch.setPreferredSize(new Dimension(140, 30));
        btnSearch.setFont(UIStyle.FONT_SUBTEXT);
        filterCard.add(btnSearch);

        content.add(filterCard, BorderLayout.NORTH);

        // Query JTable Results
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);
        tableContainer.setBorder(new EmptyBorder(15, 0, 0, 0));

        DefaultTableModel modelSearch = new DefaultTableModel(new String[]{"Schedule ID", "Class Group", "Course Subject", "Teacher Name", "Classroom", "Timeslot Period"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblSearch = new JTable(modelSearch);
        ModernTableHelper.styleTable(tblSearch);

        JScrollPane scroll = new JScrollPane(tblSearch);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1));
        tableContainer.add(scroll, BorderLayout.CENTER);
        content.add(tableContainer, BorderLayout.CENTER);

        p.add(content, BorderLayout.CENTER);
        contentCards.add(p, "Search & Queries");

        // Action behavior
        Runnable executeQuery = () -> {
            String keyword = txtSearch.getText().trim().toLowerCase();
            modelSearch.setRowCount(0);

            DatabaseManager db = DatabaseManager.getInstance();
            List<TimetableEntry> tt = db.getTimetable();

            for (TimetableEntry entry : tt) {
                // Fetch detail values
                String subName = "";
                for (Subject s : db.getSubjects()) {
                    if (s.getSubId().equalsIgnoreCase(entry.getSubId())) {
                        subName = s.getSubName();
                        break;
                    }
                }
                String teacherName = "";
                for (Teacher t : db.getTeachers()) {
                    if (t.getTeacherId().equalsIgnoreCase(entry.getTeacherId())) {
                        teacherName = t.getName();
                        break;
                    }
                }
                String roomNo = "";
                for (Room rm : db.getRooms()) {
                    if (rm.getRoomId().equalsIgnoreCase(entry.getRoomId())) {
                        roomNo = rm.getRoomNo();
                        break;
                    }
                }
                String slotText = "";
                for (Timeslot sl : db.getTimeslots()) {
                    if (sl.getSlotId().equalsIgnoreCase(entry.getSlotId())) {
                        slotText = sl.getDay() + " " + sl.getTime();
                        break;
                    }
                }

                // Match query terms
                if (keyword.isEmpty() ||
                        entry.getTtId().toLowerCase().contains(keyword) ||
                        entry.getClassId().toLowerCase().contains(keyword) ||
                        subName.toLowerCase().contains(keyword) ||
                        teacherName.toLowerCase().contains(keyword) ||
                        roomNo.toLowerCase().contains(keyword) ||
                        slotText.toLowerCase().contains(keyword)) {
                    
                    modelSearch.addRow(new Object[]{
                        entry.getTtId(), entry.getClassId(), subName, teacherName, "Room " + roomNo, slotText
                    });
                }
            }
        };

        btnSearch.addActionListener(e -> executeQuery.run());
        txtSearch.addActionListener(e -> executeQuery.run());
        
        // Populate initially
        executeQuery.run();
    }

    // ==========================================
    // DECORATIVE / LAYOUT HELPER METHODS
    // ==========================================

    private JPanel createHeaderLabel(String titleText, String subtitleText) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        
        JLabel t = new JLabel(titleText);
        t.setFont(UIStyle.FONT_TITLE);
        t.setForeground(UIStyle.TXT_WHITE);
        h.add(t, BorderLayout.NORTH);

        JLabel s = new JLabel(subtitleText);
        s.setFont(UIStyle.FONT_SUBTEXT);
        s.setForeground(UIStyle.TXT_GRAY);
        s.setBorder(new EmptyBorder(4, 0, 10, 0));
        h.add(s, BorderLayout.SOUTH);

        return h;
    }
}

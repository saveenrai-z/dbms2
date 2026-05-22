package com.timetable.ui;

import com.timetable.db.DatabaseManager;
import com.timetable.model.*;
import com.timetable.ui.components.ModernButton;
import com.timetable.ui.components.UIStyle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

/**
 * Modern soft-copy grid panel representing the generated timetable matrix.
 * Supports viewing by Class, Teacher, or Room, and features PDF/Print and CSV export actions.
 */
public class TimetableGridView extends JPanel {
    private JTable gridTable;
    private TimetableModel tableModel;

    private JComboBox<String> cbFilterType;
    private JComboBox<Object> cbFilterValue;

    private final String[] columns = {"Time Slot", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private final String[] timeRanges = {
        "08:30 AM - 09:30 AM", // Period 1
        "09:30 AM - 10:30 AM", // Period 2
        "10:30 AM - 10:45 AM", // Short Break
        "10:45 AM - 11:45 AM", // Period 3
        "11:45 AM - 12:45 PM", // Period 4
        "12:45 PM - 01:45 PM", // Lunch Break
        "01:45 PM - 02:45 PM", // Period 5
        "02:45 PM - 03:45 PM", // Period 6
        "03:45 PM - 04:45 PM"  // Period 7
    };

    // Filter modes
    private enum ViewMode { CLASS, TEACHER, ROOM }
    private ViewMode currentMode = ViewMode.CLASS;
    private String selectedId = "";

    public TimetableGridView() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // 1. Top Panel: Title and Filter Controls
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("📅 Interactive Soft-Copy Timetable");
        lblTitle.setFont(UIStyle.FONT_TITLE);
        lblTitle.setForeground(UIStyle.TXT_WHITE);
        topPanel.add(lblTitle, BorderLayout.NORTH);

        // Control Panel
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        controls.setOpaque(false);
        controls.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel lblFilter = new JLabel("View By:");
        lblFilter.setFont(UIStyle.FONT_BODY_BOLD);
        lblFilter.setForeground(UIStyle.TXT_LIGHT);
        controls.add(lblFilter);

        cbFilterType = new JComboBox<>(new String[]{"Class Group", "Teacher / Faculty", "Classroom"});
        cbFilterType.setFont(UIStyle.FONT_BODY);
        cbFilterType.setBackground(UIStyle.CARD_BG);
        cbFilterType.setForeground(UIStyle.TXT_WHITE);
        controls.add(cbFilterType);

        cbFilterValue = new JComboBox<>();
        cbFilterValue.setFont(UIStyle.FONT_BODY);
        cbFilterValue.setBackground(UIStyle.CARD_BG);
        cbFilterValue.setForeground(UIStyle.TXT_WHITE);
        cbFilterValue.setPreferredSize(new Dimension(200, 28));
        controls.add(cbFilterValue);

        // Print/PDF Button
        ModernButton btnPrint = new ModernButton("Export PDF / Print");
        btnPrint.setPreferredSize(new Dimension(160, 30));
        btnPrint.setFont(UIStyle.FONT_SUBTEXT);
        controls.add(btnPrint);

        // CSV Button
        ModernButton btnCSV = new ModernButton("Export CSV", UIStyle.ACCENT_CYAN, UIStyle.ACCENT_CYAN.brighter());
        btnCSV.setPreferredSize(new Dimension(120, 30));
        btnCSV.setFont(UIStyle.FONT_SUBTEXT);
        controls.add(btnCSV);

        topPanel.add(controls, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // 2. Table Initialization
        tableModel = new TimetableModel();
        gridTable = new JTable(tableModel);
        gridTable.setRowHeight(72); // Extra spacious for visual multi-line cards
        gridTable.setOpaque(false);
        gridTable.setBackground(UIStyle.CARD_BG);
        gridTable.setGridColor(new Color(71, 85, 105, 80));
        gridTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gridTable.setCellSelectionEnabled(true);
        gridTable.getTableHeader().setReorderingAllowed(false);

        // Style the Table Header
        gridTable.getTableHeader().setPreferredSize(new Dimension(gridTable.getTableHeader().getWidth(), 42));
        gridTable.getTableHeader().setBackground(UIStyle.BG_DARK);
        gridTable.getTableHeader().setForeground(UIStyle.ACCENT_GLOW);
        gridTable.getTableHeader().setFont(UIStyle.FONT_BODY_BOLD);

        // Cell Custom Styling Renderer
        gridTable.setDefaultRenderer(Object.class, new TimetableCellRenderer());

        JScrollPane scrollPane = new JScrollPane(gridTable);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Event Handling for Interactive Repainting
        cbFilterType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateFilterValues();
            }
        });

        cbFilterValue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selected = cbFilterValue.getSelectedItem();
                if (selected instanceof ClassGroup) {
                    currentMode = ViewMode.CLASS;
                    selectedId = ((ClassGroup) selected).getClassId();
                } else if (selected instanceof Teacher) {
                    currentMode = ViewMode.TEACHER;
                    selectedId = ((Teacher) selected).getTeacherId();
                } else if (selected instanceof Room) {
                    currentMode = ViewMode.ROOM;
                    selectedId = ((Room) selected).getRoomId();
                } else {
                    selectedId = "";
                }
                tableModel.reload();
            }
        });

        // PDF Print Action
        btnPrint.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String headerText = "Automated Timetable - " + cbFilterType.getSelectedItem().toString() + ": " + cbFilterValue.getSelectedItem().toString();
                    MessageFormat header = new MessageFormat(headerText);
                    MessageFormat footer = new MessageFormat("Page {0} | Powered by Time Table Management");
                    
                    // Trigger vector print layout
                    boolean complete = gridTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
                    if (complete) {
                        JOptionPane.showMessageDialog(null, "Printing/PDF completed successfully!", "Print Action", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Printing error: " + ex.getMessage(), "Print Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // CSV Export Action
        btnCSV.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportToCSV();
            }
        });

        // Initialize drop-down data on panel open
        updateFilterValues();
    }

    /**
     * Refreshes the second combobox depending on whether we filter by Class, Teacher, or Room.
     */
    public void updateFilterValues() {
        cbFilterValue.removeAllItems();
        DatabaseManager db = DatabaseManager.getInstance();

        int selectedIndex = cbFilterType.getSelectedIndex();
        if (selectedIndex == 0) { // Class
            List<ClassGroup> list = db.getClassGroups();
            for (ClassGroup c : list) cbFilterValue.addItem(c);
        } else if (selectedIndex == 1) { // Teacher
            List<Teacher> list = db.getTeachers();
            for (Teacher t : list) cbFilterValue.addItem(t);
        } else if (selectedIndex == 2) { // Room
            List<Room> list = db.getRooms();
            for (Room r : list) cbFilterValue.addItem(r);
        }
        tableModel.reload();
    }

    /**
     * Re-loads table model grid values from the database.
     */
    public void refreshGrid() {
        updateFilterValues();
    }

    /**
     * Formulates and saves the CSV spreadsheet file.
     */
    private void exportToCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(selectedId + "_timetable.csv"));
        int retVal = chooser.showSaveDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(target)) {
                // Write headers
                pw.println("Time Slot,Monday,Tuesday,Wednesday,Thursday,Friday,Saturday");
                
                // Write rows
                for (int r = 0; r < timeRanges.length; r++) {
                    StringBuilder line = new StringBuilder();
                    line.append("\"").append(timeRanges[r]).append("\"");
                    
                    for (int c = 1; c < columns.length; c++) {
                        String day = columns[c];
                        String time = timeRanges[r];
                        
                        boolean isShortBreak = time.equals("10:30 AM - 10:45 AM");
                        boolean isLunchBreak = time.equals("12:45 PM - 01:45 PM");
                        
                        if (isShortBreak || isLunchBreak) {
                            if (day.equalsIgnoreCase("Saturday") && isLunchBreak) {
                                line.append(",\"INACTIVE\"");
                            } else if (isShortBreak) {
                                line.append(",\"TEA BREAK\"");
                            } else {
                                line.append(",\"LUNCH BREAK\"");
                            }
                            continue;
                        }
                        
                        if (day.equalsIgnoreCase("Saturday") && r >= 5) {
                            line.append(",\"INACTIVE\"");
                            continue;
                        }
                        
                        TimetableEntry entry = tableModel.getEntryAt(day, time);
                        if (entry != null) {
                            DatabaseManager db = DatabaseManager.getInstance();
                            // Fetch names
                            String subName = "Course";
                            for (Subject s : db.getSubjects()) {
                                if (s.getSubId().equalsIgnoreCase(entry.getSubId())) {
                                    subName = s.getSubName();
                                    break;
                                }
                            }
                            String teacherName = "Teacher";
                            for (Teacher t : db.getTeachers()) {
                                if (t.getTeacherId().equalsIgnoreCase(entry.getTeacherId())) {
                                    teacherName = t.getName();
                                    break;
                                }
                            }
                            String roomNo = "Room";
                            for (Room rm : db.getRooms()) {
                                if (rm.getRoomId().equalsIgnoreCase(entry.getRoomId())) {
                                    roomNo = rm.getRoomNo();
                                    break;
                                }
                            }
                            
                            line.append(",\"").append(subName).append(" (").append(teacherName).append(" in Rm ").append(roomNo).append(")\"");
                        } else {
                            line.append(",\"-\"");
                        }
                    }
                    pw.println(line.toString());
                }
                
                JOptionPane.showMessageDialog(this, "CSV timetable exported successfully!", "Export Completed", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save CSV: " + ex.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==========================================
    // CUSTOM TABLE MODEL FOR TIME TABLE MATRIX
    // ==========================================

    private class TimetableModel extends AbstractTableModel {
        private final List<TimetableEntry> activeTimetable = new ArrayList<>();

        public void reload() {
            activeTimetable.clear();
            activeTimetable.addAll(DatabaseManager.getInstance().getTimetable());
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return timeRanges.length;
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return timeRanges[rowIndex];
            }

            // Identify Day and Time slot
            String day = columns[columnIndex];
            String time = timeRanges[rowIndex];

            return getEntryAt(day, time);
        }

        public TimetableEntry getEntryAt(String day, String time) {
            DatabaseManager db = DatabaseManager.getInstance();
            
            // Resolve Slot ID
            String slotId = "";
            for (Timeslot s : db.getTimeslots()) {
                if (s.getDay().equalsIgnoreCase(day) && s.getTime().equalsIgnoreCase(time)) {
                    slotId = s.getSlotId();
                    break;
                }
            }

            if (slotId.isEmpty()) return null;

            // Search timetable
            for (TimetableEntry entry : activeTimetable) {
                if (entry.getSlotId().equalsIgnoreCase(slotId)) {
                    if (currentMode == ViewMode.CLASS && entry.getClassId().equalsIgnoreCase(selectedId)) {
                        return entry;
                    }
                    if (currentMode == ViewMode.TEACHER && entry.getTeacherId().equalsIgnoreCase(selectedId)) {
                        return entry;
                    }
                    if (currentMode == ViewMode.ROOM && entry.getRoomId().equalsIgnoreCase(selectedId)) {
                        return entry;
                    }
                }
            }
            return null;
        }
    }

    // ==========================================
    // PREMIUM CUSTOM MATRIX CARD CELL RENDERER
    // ==========================================

    private class TimetableCellRenderer extends DefaultTableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            setHorizontalAlignment(JLabel.CENTER);
            setFont(UIStyle.FONT_CARD);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(71, 85, 105, 80)));

            // 1. Column 0 contains the time periods
            if (column == 0) {
                setBackground(UIStyle.BG_DARK);
                setForeground(UIStyle.ACCENT_GLOW);
                setFont(UIStyle.FONT_BODY_BOLD);
                setText("<html><center>" + value.toString().replace(" - ", "<br>to<br>") + "</center></html>");
                return this;
            }

            // 2. Row 2 is Short Break
            if (row == 2) {
                setBackground(new Color(21, 94, 117, 90)); // Soft cyan/teal
                setForeground(new Color(165, 243, 252)); // Light cyan text
                setFont(UIStyle.FONT_BODY_BOLD);
                setText("☕ TEA BREAK / REST");
                return this;
            }

            // 3. Row 5 is Lunch Break
            if (row == 5) {
                if (column == 6) {
                    setBackground(new Color(31, 41, 55, 120)); // Inactive on Saturday
                    setForeground(UIStyle.TXT_GRAY);
                    setFont(UIStyle.FONT_SUBTEXT);
                    setText("Inactive");
                } else {
                    setBackground(new Color(51, 65, 85, 90)); // Soft grey
                    setForeground(UIStyle.TXT_GRAY);
                    setFont(UIStyle.FONT_BODY_BOLD);
                    setText("🍽️ LUNCH BREAK / RECESS");
                }
                return this;
            }

            // 4. Saturday after 12:45 PM is inactive (row >= 6, as row 5 is already handled)
            if (column == 6 && row >= 6) {
                setBackground(new Color(31, 41, 55, 120)); // Inactive on Saturday
                setForeground(UIStyle.TXT_GRAY);
                setFont(UIStyle.FONT_SUBTEXT);
                setText("Inactive");
                return this;
            }

            // Alternate normal backgrounds for empty blocks
            if (row % 2 == 0) {
                setBackground(UIStyle.CARD_BG);
            } else {
                setBackground(new Color(25, 33, 49)); // Lighter card space
            }
            setForeground(UIStyle.TXT_GRAY);
            setText("-");

            // 3. Render loaded Timetable Entry
            if (value instanceof TimetableEntry) {
                TimetableEntry entry = (TimetableEntry) value;
                DatabaseManager db = DatabaseManager.getInstance();

                // Fetch details
                String subName = "N/A";
                for (Subject s : db.getSubjects()) {
                    if (s.getSubId().equalsIgnoreCase(entry.getSubId())) {
                        subName = s.getSubName();
                        break;
                    }
                }

                String teacherName = "N/A";
                for (Teacher t : db.getTeachers()) {
                    if (t.getTeacherId().equalsIgnoreCase(entry.getTeacherId())) {
                        teacherName = t.getName();
                        break;
                    }
                }

                String roomNo = "N/A";
                for (Room rm : db.getRooms()) {
                    if (rm.getRoomId().equalsIgnoreCase(entry.getRoomId())) {
                        roomNo = rm.getRoomNo();
                        break;
                    }
                }

                // Give card colored accent backgrounds depending on the subject type
                if (subName.toLowerCase().contains("lab")) {
                    setBackground(new Color(22, 78, 99)); // Soft Teal Card
                } else if (subName.toLowerCase().contains("dbms")) {
                    setBackground(new Color(30, 58, 138)); // Deep Royal Blue Card
                } else {
                    setBackground(new Color(49, 46, 129)); // Deep Indigo Card
                }
                
                // Formulate beautiful multi-line HTML card
                String cardText = "<html><center style='margin: 4px;'>" +
                        "<b style='color:#ffffff; font-size:11px;'>" + subName + "</b><br>" +
                        "<span style='color:#cbd5e1; font-size:10px;'>" + teacherName + "</span><br>";
                
                if (currentMode == ViewMode.CLASS) {
                    cardText += "<span style='color:#a5f3fc; font-size:9px; background-color:rgba(6,182,212,0.3); padding:1px 4px; border-radius:3px;'>Room " + roomNo + "</span>";
                } else if (currentMode == ViewMode.TEACHER) {
                    cardText += "<span style='color:#fbcfe8; font-size:9px; background-color:rgba(236,72,153,0.3); padding:1px 4px; border-radius:3px;'>" + entry.getClassId() + " (Room " + roomNo + ")</span>";
                } else if (currentMode == ViewMode.ROOM) {
                    cardText += "<span style='color:#c7d2fe; font-size:9px; background-color:rgba(99,102,241,0.3); padding:1px 4px; border-radius:3px;'>" + entry.getClassId() + "</span>";
                }
                
                cardText += "</center></html>";
                setText(cardText);
            }

            return this;
        }
    }
}

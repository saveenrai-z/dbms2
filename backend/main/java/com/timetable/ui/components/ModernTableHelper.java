package com.timetable.ui.components;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Styling helper utility to paint modern premium Slate/Indigo style on standard JTables.
 */
public class ModernTableHelper {

    public static void styleTable(JTable table) {
        table.setOpaque(false);
        table.setBackground(UIStyle.CARD_BG);
        table.setForeground(UIStyle.TXT_LIGHT);
        table.setGridColor(new Color(51, 65, 85, 100)); // Slate 700 transparent
        table.setRowHeight(38);
        table.setFont(UIStyle.FONT_BODY);
        table.setSelectionBackground(new Color(99, 102, 241, 180)); // Semi-transparent Indigo
        table.setSelectionForeground(UIStyle.TXT_WHITE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        
        // Remove standard cell borders
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                
                // Add horizontal padding and alignment
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                
                // Alternate row backgrounds for readability
                if (!isSelected) {
                    if (row % 2 == 0) {
                        setBackground(UIStyle.CARD_BG);
                    } else {
                        setBackground(new Color(38, 50, 70)); // Slightly lighter Slate card
                    }
                }
                
                return this;
            }
        });

        // Style the Header
        JTableHeader header = table.getTableHeader();
        header.setOpaque(false);
        header.setBackground(UIStyle.BG_DARK);
        header.setForeground(UIStyle.ACCENT_INDIGO);
        header.setFont(UIStyle.FONT_BODY_BOLD);
        header.setPreferredSize(new Dimension(header.getWidth(), 42));
        
        // Custom header renderer
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(UIStyle.BG_DARK);
                setForeground(UIStyle.ACCENT_GLOW);
                setFont(UIStyle.FONT_BODY_BOLD);
                setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIStyle.BORDER_COLOR));
                setHorizontalAlignment(JLabel.LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                    getBorder(),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)
                ));
                return this;
            }
        });
        
        // Disable header reordering/resizing
        header.setReorderingAllowed(false);
    }
}

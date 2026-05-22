package com.timetable.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Custom modern password field with rounded corners, placeholders, and glowing borders on focus.
 */
public class ModernPasswordField extends JPasswordField {
    private String placeholder = "";
    private boolean isFocused = false;
    private int cornerRadius = 12;
    private Color bgNormal = UIStyle.CARD_BG;
    private Color borderNormal = UIStyle.BORDER_COLOR;
    private Color borderFocused = UIStyle.ACCENT_INDIGO;

    public ModernPasswordField() {
        initComponent();
    }

    public ModernPasswordField(String placeholder) {
        this.placeholder = placeholder;
        initComponent();
    }

    private void initComponent() {
        setOpaque(false);
        setBackground(bgNormal);
        setForeground(UIStyle.TXT_WHITE);
        setCaretColor(UIStyle.TXT_WHITE);
        setFont(UIStyle.FONT_BODY);
        setBorder(new EmptyBorder(8, 12, 8, 12));

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UIStyle.enableAntialiasing(g2);

        int width = getWidth();
        int height = getHeight();

        // 1. Draw rounded body background
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);

        // 2. Draw modern glow border
        if (isFocused) {
            g2.setColor(borderFocused);
            g2.setStroke(new BasicStroke(1.8f));
        } else {
            g2.setColor(borderNormal);
            g2.setStroke(new BasicStroke(1.0f));
        }
        g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);

        g2.dispose();

        // 3. Draw password dots
        super.paintComponent(g);

        char[] pass = getPassword();
        if (pass.length == 0 && !placeholder.isEmpty() && !isFocused) {
            Graphics2D gPlaceholder = (Graphics2D) g.create();
            UIStyle.enableAntialiasing(gPlaceholder);
            gPlaceholder.setFont(getFont());
            gPlaceholder.setColor(UIStyle.TXT_GRAY);
            
            Insets insets = getInsets();
            FontMetrics metrics = gPlaceholder.getFontMetrics();
            int y = (height - metrics.getHeight()) / 2 + metrics.getAscent();
            
            gPlaceholder.drawString(placeholder, insets.left, y);
            gPlaceholder.dispose();
        }
    }
}

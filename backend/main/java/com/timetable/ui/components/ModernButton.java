package com.timetable.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Custom modern button with anti-aliasing, rounded corners, hover/press micro-animations, and styled fonts.
 */
public class ModernButton extends JButton {
    private Color bgNormal = UIStyle.ACCENT_INDIGO;
    private Color bgHover = UIStyle.ACCENT_GLOW;
    private Color bgPress = UIStyle.CARD_HOVER;
    private Color fgColor = UIStyle.TXT_WHITE;
    
    private boolean isHovered = false;
    private boolean isPressed = false;
    private int cornerRadius = 12;

    public ModernButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(fgColor);
        setFont(UIStyle.FONT_BODY_BOLD);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Add mouse listeners for hover and click micro-animations
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public ModernButton(String text, Color bgNormal, Color bgHover) {
        this(text);
        this.bgNormal = bgNormal;
        this.bgHover = bgHover;
    }

    public void setColors(Color normal, Color hover, Color press) {
        this.bgNormal = normal;
        this.bgHover = hover;
        this.bgPress = press;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UIStyle.enableAntialiasing(g2);

        int width = getWidth();
        int height = getHeight();

        // Determine active background color based on mouse state (micro-animation fallback)
        Color activeBg;
        if (isPressed) {
            activeBg = bgPress;
        } else if (isHovered) {
            activeBg = bgHover;
        } else {
            activeBg = bgNormal;
        }

        // 1. Draw rounded button body
        g2.setColor(activeBg);
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        // 2. Add a very subtle drop shadow effect at the bottom when hovered
        if (isHovered) {
            g2.setColor(new Color(255, 255, 255, 30));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        }

        // 3. Draw text label centered
        FontMetrics metrics = g2.getFontMetrics(getFont());
        int x = (width - metrics.stringWidth(getText())) / 2;
        int y = ((height - metrics.getHeight()) / 2) + metrics.getAscent();

        g2.setFont(getFont());
        g2.setColor(getForeground());
        g2.drawString(getText(), x, y);

        g2.dispose();
    }
}

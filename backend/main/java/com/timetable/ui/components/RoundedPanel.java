package com.timetable.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Custom JPanel with modern rounded corners, high-quality rendering, and custom border/shadow options.
 */
public class RoundedPanel extends JPanel {
    private int cornerRadius = 16;
    private Color backgroundColor = UIStyle.CARD_BG;
    private Color borderColor = UIStyle.BORDER_COLOR;
    private boolean showBorder = false;
    private boolean isGradient = false;
    private Color gradientEndColor = UIStyle.BG_DARK;

    public RoundedPanel() {
        setOpaque(false);
    }

    public RoundedPanel(int radius) {
        this.cornerRadius = radius;
        setOpaque(false);
    }

    public RoundedPanel(int radius, Color bg) {
        this.cornerRadius = radius;
        this.backgroundColor = bg;
        setOpaque(false);
    }

    public RoundedPanel(int radius, Color bg, Color border) {
        this.cornerRadius = radius;
        this.backgroundColor = bg;
        this.borderColor = border;
        this.showBorder = true;
        setOpaque(false);
    }

    public void setBackgroundColor(Color bg) {
        this.backgroundColor = bg;
        repaint();
    }

    public void setBorderColor(Color border) {
        this.borderColor = border;
        this.showBorder = true;
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setGradient(boolean isGradient, Color endColor) {
        this.isGradient = isGradient;
        this.gradientEndColor = endColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        UIStyle.enableAntialiasing(g2);

        int width = getWidth();
        int height = getHeight();

        // 1. Paint Background
        if (isGradient) {
            GradientPaint gp = new GradientPaint(0, 0, backgroundColor, 0, height, gradientEndColor);
            g2.setPaint(gp);
        } else {
            g2.setColor(backgroundColor);
        }
        g2.fillRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);

        // 2. Paint Border if enabled
        if (showBorder) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        }

        g2.dispose();
    }
}

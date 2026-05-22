package com.timetable.ui.components;

import java.awt.*;
import java.io.InputStream;

/**
 * Global Styling Constants and Tokens for a modern Premium Slate/Indigo theme.
 */
public class UIStyle {
    // Theme Colors
    public static final Color BG_DARK = new Color(15, 23, 42);      // Slate 900
    public static final Color CARD_BG = new Color(30, 41, 59);      // Slate 800
    public static final Color CARD_HOVER = new Color(51, 65, 85);   // Slate 700
    public static final Color ACCENT_INDIGO = new Color(99, 102, 241); // Indigo 500
    public static final Color ACCENT_GLOW = new Color(129, 140, 248);  // Indigo 400
    public static final Color ACCENT_CYAN = new Color(6, 182, 212);   // Cyan 500
    public static final Color ACCENT_GREEN = new Color(16, 185, 129); // Emerald 500
    public static final Color ACCENT_RED = new Color(239, 68, 68);    // Red 500
    
    // Text Colors
    public static final Color TXT_WHITE = new Color(248, 250, 252);  // Slate 50
    public static final Color TXT_LIGHT = new Color(226, 232, 240);  // Slate 200
    public static final Color TXT_GRAY = new Color(148, 163, 184);   // Slate 400
    public static final Color BORDER_COLOR = new Color(71, 85, 105); // Slate 600

    // Unified Modern Fonts
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 26);
    public static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 18);
    public static final Font FONT_BODY_BOLD = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_SUBTEXT = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_CARD = new Font("SansSerif", Font.BOLD, 13);

    /**
     * Enables premium text rendering qualities (antialiasing) on standard graphics contexts.
     */
    public static void enableAntialiasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
}

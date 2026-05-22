package com.timetable.ui;

import com.timetable.db.DatabaseManager;
import com.timetable.ui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Modern glassmorphic Login screen for the Admin panel.
 */
public class LoginFrame extends JFrame {
    private ModernTextField txtUsername;
    private ModernPasswordField txtPassword;
    private JLabel lblStatusBadge;

    public LoginFrame() {
        super("Automated Time Table Generation System - Admin Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 550);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main Layout Container
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw a beautiful dark space gradient
                Graphics2D g2 = (Graphics2D) g.create();
                UIStyle.enableAntialiasing(g2);
                GradientPaint gp = new GradientPaint(0, 0, UIStyle.BG_DARK, getWidth(), getHeight(), new Color(15, 10, 35));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
        setContentPane(mainPanel);

        // Center Split Panel (Sidebar + Login Form Card)
        JPanel contentCard = new JPanel(new GridLayout(1, 2, 0, 0));
        contentCard.setOpaque(false);
        mainPanel.add(contentCard, BorderLayout.CENTER);

        // Left Panel - Decorative Brand Sidebar with gradient accent
        RoundedPanel leftPanel = new RoundedPanel(24, new Color(25, 30, 50), UIStyle.BORDER_COLOR);
        leftPanel.setGradient(true, new Color(49, 46, 129)); // Glowing Indigo Gradient
        leftPanel.setLayout(new GridBagLayout());
        leftPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        JLabel lblBrandIcon = new JLabel("📅", JLabel.CENTER);
        lblBrandIcon.setFont(new Font("SansSerif", Font.PLAIN, 64));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        leftPanel.add(lblBrandIcon, gbc);

        JLabel lblBrandTitle = new JLabel("Time Table Management", JLabel.CENTER);
        lblBrandTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblBrandTitle.setForeground(UIStyle.TXT_WHITE);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 8, 0);
        leftPanel.add(lblBrandTitle, gbc);

        JLabel lblBrandSubtitle = new JLabel("DBMS Conflict-Free System", JLabel.CENTER);
        lblBrandSubtitle.setFont(UIStyle.FONT_SUBTEXT);
        lblBrandSubtitle.setForeground(UIStyle.TXT_GRAY);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        leftPanel.add(lblBrandSubtitle, gbc);

        // Add separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 40));
        gbc.gridy = 3;
        gbc.insets = new Insets(10, 30, 15, 30);
        leftPanel.add(sep, gbc);

        // Database status badge
        DatabaseManager db = DatabaseManager.getInstance();
        lblStatusBadge = new JLabel("", JLabel.CENTER);
        lblStatusBadge.setOpaque(true);
        lblStatusBadge.setFont(UIStyle.FONT_SUBTEXT);
        lblStatusBadge.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        
        if (db.isMockMode()) {
            lblStatusBadge.setText("⚠️ DATABASE: LOCAL MOCK MODE");
            lblStatusBadge.setBackground(new Color(245, 158, 11, 40)); // Transparent Amber
            lblStatusBadge.setForeground(new Color(251, 191, 36));     // Amber Text
        } else {
            lblStatusBadge.setText("🟢 DATABASE: MONGODB ACTIVE");
            lblStatusBadge.setBackground(new Color(16, 185, 129, 40)); // Transparent Green
            lblStatusBadge.setForeground(new Color(52, 211, 153));     // Green Text
        }
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 20, 0, 20);
        leftPanel.add(lblStatusBadge, gbc);

        contentCard.add(leftPanel);

        // Right Panel - Login Fields Form
        RoundedPanel rightPanel = new RoundedPanel(24, UIStyle.CARD_BG, UIStyle.BORDER_COLOR);
        rightPanel.setLayout(new GridBagLayout());
        rightPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.fill = GridBagConstraints.HORIZONTAL;
        rGbc.gridx = 0;
        rGbc.weightx = 1.0;

        JLabel lblLoginHeader = new JLabel("Welcome Back");
        lblLoginHeader.setFont(UIStyle.FONT_TITLE);
        lblLoginHeader.setForeground(UIStyle.TXT_WHITE);
        rGbc.gridy = 0;
        rGbc.insets = new Insets(0, 0, 4, 0);
        rightPanel.add(lblLoginHeader, gbc);

        JLabel lblLoginSub = new JLabel("Log in with administrator credentials.");
        lblLoginSub.setFont(UIStyle.FONT_SUBTEXT);
        lblLoginSub.setForeground(UIStyle.TXT_GRAY);
        rGbc.gridy = 1;
        rGbc.insets = new Insets(0, 0, 30, 0);
        rightPanel.add(lblLoginSub, rGbc);

        // Username Field
        JLabel lblUser = new JLabel("USERNAME");
        lblUser.setFont(UIStyle.FONT_SUBTEXT);
        lblUser.setForeground(UIStyle.TXT_LIGHT);
        rGbc.gridy = 2;
        rGbc.insets = new Insets(0, 0, 6, 0);
        rightPanel.add(lblUser, rGbc);

        txtUsername = new ModernTextField("Enter username...");
        txtUsername.setText("admin"); // Auto-fill default username
        rGbc.gridy = 3;
        rGbc.insets = new Insets(0, 0, 20, 0);
        rightPanel.add(txtUsername, rGbc);

        // Password Field
        JLabel lblPass = new JLabel("PASSWORD");
        lblPass.setFont(UIStyle.FONT_SUBTEXT);
        lblPass.setForeground(UIStyle.TXT_LIGHT);
        rGbc.gridy = 4;
        rGbc.insets = new Insets(0, 0, 6, 0);
        rightPanel.add(lblPass, rGbc);

        txtPassword = new ModernPasswordField("Enter password...");
        txtPassword.setText("admin123"); // Auto-fill default password
        rGbc.gridy = 5;
        rGbc.insets = new Insets(0, 0, 30, 0);
        rightPanel.add(txtPassword, rGbc);

        // Login Button
        ModernButton btnLogin = new ModernButton("SIGN IN");
        btnLogin.setFont(UIStyle.FONT_BODY_BOLD);
        btnLogin.setPreferredSize(new Dimension(0, 45));
        
        rGbc.gridy = 6;
        rGbc.insets = new Insets(10, 0, 10, 0);
        rightPanel.add(btnLogin, rGbc);

        // Bottom help label
        JLabel lblHelp = new JLabel("Default Credentials: admin / admin123", JLabel.CENTER);
        lblHelp.setFont(UIStyle.FONT_SUBTEXT);
        lblHelp.setForeground(UIStyle.TXT_GRAY);
        rGbc.gridy = 7;
        rGbc.insets = new Insets(15, 0, 0, 0);
        rightPanel.add(lblHelp, rGbc);

        contentCard.add(rightPanel);

        // Authentication Handler Action
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });
        
        // Allow pressing Enter key to login
        ActionListener enterAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        };
        txtUsername.addActionListener(enterAction);
        txtPassword.addActionListener(enterAction);
    }

    private void performLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.equalsIgnoreCase("admin") && password.equals("admin123")) {
            // Success: Close Login screen and transition to Dashboard
            dispose();
            
            // Show custom pop-up informing the user about the database state
            DatabaseManager db = DatabaseManager.getInstance();
            if (db.isMockMode()) {
                JOptionPane.showMessageDialog(null,
                        "MongoDB server not found. Connecting in OFFLINE MOCK MODE!\nAll data changes are saved in 'local_database.json'.",
                        "Database Fallback", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null,
                        "Successfully connected to MongoDB server!",
                        "Database Connected", JOptionPane.INFORMATION_MESSAGE);
            }

            // Launch Dashboard Frame
            SwingUtilities.invokeLater(() -> {
                DashboardFrame dashboard = new DashboardFrame();
                dashboard.setVisible(true);
            });
        } else {
            // Error handling
            JOptionPane.showMessageDialog(this,
                    "Invalid Administrator Credentials! Please try again.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // Apply System Look and Feel if possible, but keep dark rendering intact
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}

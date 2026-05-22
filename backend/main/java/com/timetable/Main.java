package com.timetable;

import com.timetable.db.DatabaseManager;
import com.timetable.web.WebServer;

import java.awt.Desktop;
import java.net.URI;

/**
 * Entry point / launcher class for the Automated Time Table Generation System.
 * Hosts a native lightweight web server and opens the browser dynamically.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   Time Table Management: Starting Academic System v2.0   ");
        System.out.println("=================================================");

        // 1. Load Configurations from .env
        loadEnv();

        // 2. Initialize Core Database manager and pre-populate sample data
        System.out.println("Time Table Management Core: Connecting database services...");
        DatabaseManager.getInstance();

        // 3. Start native WebServer node
        String portStr = System.getProperty("SERVER_PORT", "8080");
        int port = Integer.parseInt(portStr);
        System.out.println("Time Table Management Core: Booting embedded Web Server on port " + port + "...");
        WebServer.start();

        // 4. Automatically open local dashboard in default browser
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    System.out.println("Time Table Management Launcher: Launching default web browser...");
                    desktop.browse(new URI("http://localhost:" + port));
                } else {
                    System.out.println("Time Table Management Launcher: Browser action unsupported. Please visit: http://localhost:" + port);
                }
            } else {
                System.out.println("Time Table Management Launcher: Desktop not supported. Please visit: http://localhost:" + port);
            }
        } catch (Exception e) {
            System.out.println("Time Table Management Launcher: Headless system detected. Please access web panel at: http://localhost:" + port);
        }

        // Add shutdown hook to shut down WebServer cleanly when process is terminated
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Time Table Management Shutdown Hook: Terminating active servers...");
            WebServer.stop();
        }));
    }

    /**
     * Helper to read .env file and load key-value configurations into System Properties.
     */
    public static void loadEnv() {
        try {
            java.io.File file = new java.io.File(".env");
            if (!file.exists()) {
                System.out.println("Time Table Management Config: .env file not found. Using system defaults.");
                return;
            }
            java.util.List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(".env"));
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf("=");
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    // Strip quotes if any
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    System.setProperty(key, value);
                }
            }
            System.out.println("Time Table Management Config: Loaded configurations from .env file successfully.");
        } catch (Exception e) {
            System.err.println("Time Table Management Config: Failed to load .env file: " + e.getMessage());
        }
    }
}

package com.timetable.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.timetable.algorithm.TimetableGenerator;
import com.timetable.db.DatabaseManager;
import com.timetable.model.*;
import org.bson.Document;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;

/**
 * Embedded Lightweight Localhost Web Server using JDK's HttpServer.
 * Acts as the REST API engine and serves static files from the 'web/' directory.
 */
public class WebServer {
    private static HttpServer server;

    /**
     * Initializes and starts the Web Server.
     */
    public static void start() {
        try {
            int port = Integer.parseInt(System.getProperty("SERVER_PORT", "8080"));
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Set up routes
            server.createContext("/", new StaticFileHandler());
            server.createContext("/api/status", new StatusHandler());
            server.createContext("/api/teachers", new TeacherHandler());
            server.createContext("/api/subjects", new SubjectHandler());
            server.createContext("/api/classes", new ClassHandler());
            server.createContext("/api/rooms", new RoomHandler());
            server.createContext("/api/timeslots", new TimeslotHandler());
            server.createContext("/api/generate", new GenerateHandler());
            server.createContext("/api/timetable", new TimetableHandler());
            server.createContext("/api/reset", new ResetHandler());
            server.createContext("/api/purge", new PurgeHandler());

            // Use a multi-threaded executor for handling parallel REST transactions
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("Time Table Management Server: Successfully booted at http://localhost:" + port);
        } catch (Exception e) {
            System.err.println("Time Table Management Server: Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stops the Web Server.
     */
    public static void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Time Table Management Server: Server stopped.");
        }
    }

    // ==========================================
    // UTILITY UTILS FOR REQUESTS & RESPONSES
    // ==========================================

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             Scanner s = new Scanner(is, StandardCharsets.UTF_8.name())) {
            return s.useDelimiter("\\A").hasNext() ? s.next() : "";
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String response) throws IOException {
        // Set CORS and headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void handleOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1); // No content for preflight OPTIONS
    }

    private static String getQueryParam(HttpExchange exchange, String name) {
        try {
            String query = exchange.getRequestURI().getQuery();
            if (query == null) return null;
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    if (key.equalsIgnoreCase(name)) {
                        return URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing query parameter: " + e.getMessage());
        }
        return null;
    }

    // ==========================================
    // STATIC FILE SERVER
    // ==========================================

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String pathStr = exchange.getRequestURI().getPath();
            
            // Map default path to index.html
            if (pathStr.equals("/")) {
                pathStr = "/index.html";
            }

            // Clean path to prevent directory traversal
            pathStr = pathStr.replace("..", "");
            Path filePath = Paths.get("front end", pathStr.substring(1));

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendResponse(exchange, 404, "text/plain", "404 Not Found");
                return;
            }

            // Detect MIME Content-Type
            String contentType = "text/plain";
            if (pathStr.endsWith(".html")) contentType = "text/html";
            else if (pathStr.endsWith(".css")) contentType = "text/css";
            else if (pathStr.endsWith(".js")) contentType = "application/javascript";
            else if (pathStr.endsWith(".png")) contentType = "image/png";
            else if (pathStr.endsWith(".jpg") || pathStr.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (pathStr.endsWith(".svg")) contentType = "image/svg+xml";
            else if (pathStr.endsWith(".ico")) contentType = "image/x-icon";

            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", contentType);

            byte[] fileBytes = Files.readAllBytes(filePath);
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }
    }

    // ==========================================
    // REST ROUTE HANDLERS
    // ==========================================

    private static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            DatabaseManager db = DatabaseManager.getInstance();
            Document statusDoc = new Document()
                    .append("is_mock", db.isMockMode())
                    .append("database", db.isMockMode() ? "local_database.json" : "MongoDB (automated_timetable_system)")
                    .append("teachers_count", db.getTeachers().size())
                    .append("subjects_count", db.getSubjects().size())
                    .append("classes_count", db.getClassGroups().size())
                    .append("rooms_count", db.getRooms().size())
                    .append("timeslots_count", db.getTimeslots().size())
                    .append("timetable_entries_count", db.getTimetable().size());

            sendResponse(exchange, 200, "application/json", statusDoc.toJson());
        }
    }

    private static class TeacherHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            DatabaseManager db = DatabaseManager.getInstance();

            try {
                if (method.equalsIgnoreCase("GET")) {
                    List<Teacher> list = db.getTeachers();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        sb.append(list.get(i).toDocument().toJson());
                        if (i < list.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    sendResponse(exchange, 200, "application/json", sb.toString());

                } else if (method.equalsIgnoreCase("POST")) {
                    String body = readRequestBody(exchange);
                    Document doc = Document.parse(body);
                    
                    String teacherId = doc.getString("teacherId");
                    String name = doc.getString("name");
                    String department = doc.getString("department");

                    if (teacherId == null || teacherId.trim().isEmpty() || name == null || name.trim().isEmpty()) {
                        sendResponse(exchange, 400, "application/json", "{\"error\": \"Missing required fields: teacherId, name\"}");
                        return;
                    }

                    if (department == null || department.trim().isEmpty()) {
                        department = "cse-aiml";
                    }

                    Teacher teacher = new Teacher(teacherId.trim(), name.trim(), department.trim());
                    boolean success = db.saveTeacher(teacher);

                    if (success) {
                        sendResponse(exchange, 201, "application/json", "{\"message\": \"Teacher saved successfully!\"}");
                    } else {
                        sendResponse(exchange, 409, "application/json", "{\"error\": \"Duplicate ID Constraint Violation\"}");
                    }

                } else if (method.equalsIgnoreCase("DELETE")) {
                    String id = getQueryParam(exchange, "id");
                    if (id == null || id.trim().isEmpty()) {
                        sendResponse(exchange, 400, "application/json", "{\"error\": \"Missing parameter: id\"}");
                        return;
                    }

                    boolean success = db.deleteTeacher(id.trim());
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"message\": \"Teacher deleted successfully!\"}");
                    } else {
                        sendResponse(exchange, 404, "application/json", "{\"error\": \"Teacher not found\"}");
                    }
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    private static class SubjectHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            DatabaseManager db = DatabaseManager.getInstance();

            try {
                if (method.equalsIgnoreCase("GET")) {
                    List<Subject> list = db.getSubjects();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        sb.append(list.get(i).toDocument().toJson());
                        if (i < list.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    sendResponse(exchange, 200, "application/json", sb.toString());

                } else if (method.equalsIgnoreCase("POST")) {
                    String body = readRequestBody(exchange);
                    Document doc = Document.parse(body);

                    String subId = doc.getString("subId");
                    String subName = doc.getString("subName");
                    Object hrsObj = doc.get("hours");
                    int hours = hrsObj instanceof Number ? ((Number) hrsObj).intValue() : Integer.parseInt(hrsObj.toString());
                    String classId = doc.getString("classId");
                    String teacherId = doc.getString("teacherId");

                    if (subId == null || subId.trim().isEmpty() || subName == null || subName.trim().isEmpty() ||
                            classId == null || classId.trim().isEmpty() || teacherId == null || teacherId.trim().isEmpty()) {
                        sendResponse(exchange, 400, "application/json", "{\"error\": \"Missing required fields: subId, subName, hours, classId, teacherId\"}");
                        return;
                    }

                    Subject subject = new Subject(subId.trim(), subName.trim(), hours, classId.trim(), teacherId.trim());
                    boolean success = db.saveSubject(subject);

                    if (success) {
                        sendResponse(exchange, 201, "application/json", "{\"message\": \"Subject saved successfully!\"}");
                    } else {
                        sendResponse(exchange, 409, "application/json", "{\"error\": \"Duplicate ID Constraint Violation\"}");
                    }

                } else if (method.equalsIgnoreCase("DELETE")) {
                    String id = getQueryParam(exchange, "id");
                    String classId = getQueryParam(exchange, "classId");
                    if (id == null || id.trim().isEmpty()) {
                        sendResponse(exchange, 400, "application/json", "{\"error\": \"Missing parameter: id\"}");
                        return;
                    }

                    boolean success;
                    if (classId != null && !classId.trim().isEmpty()) {
                        success = db.deleteSubject(id.trim(), classId.trim());
                    } else {
                        success = db.deleteSubject(id.trim());
                    }

                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"message\": \"Subject deleted successfully!\"}");
                    } else {
                        sendResponse(exchange, 404, "application/json", "{\"error\": \"Subject not found\"}");
                    }
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    private static class ClassHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            DatabaseManager db = DatabaseManager.getInstance();

            try {
                if (method.equalsIgnoreCase("GET")) {
                    List<ClassGroup> list = db.getClassGroups();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        sb.append(list.get(i).toDocument().toJson());
                        if (i < list.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    sendResponse(exchange, 200, "application/json", sb.toString());

                } else if (method.equalsIgnoreCase("POST")) {
                    String body = readRequestBody(exchange);
                    Document doc = Document.parse(body);

                    String classId = doc.getString("classId");
                    String className = doc.getString("className");
                    Object semObj = doc.get("semester");
                    int semester = semObj instanceof Number ? ((Number) semObj).intValue() : Integer.parseInt(semObj.toString());

                    if (classId == null || classId.trim().isEmpty() || className == null || className.trim().isEmpty()) {
                        sendResponse(exchange, 400, "application/json", "{\"error\": \"Missing required fields: classId, className, semester\"}");
                        return;
                    }

                    ClassGroup classGroup = new ClassGroup(classId.trim(), className.trim(), semester);
                    boolean success = db.saveClassGroup(classGroup);

                    if (success) {
                        sendResponse(exchange, 201, "application/json", "{\"message\": \"Class group saved successfully!\"}");
                    } else {
                        sendResponse(exchange, 409, "application/json", "{\"error\": \"Duplicate ID Constraint Violation\"}");
                    }

                } else if (method.equalsIgnoreCase("DELETE")) {
                    String id = getQueryParam(exchange, "id");
                    if (id == null || id.trim().isEmpty()) {
                        sendResponse(exchange, 400, "application/json", "{\"error\": \"Missing parameter: id\"}");
                        return;
                    }

                    boolean success = db.deleteClassGroup(id.trim());
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"message\": \"Class group deleted successfully!\"}");
                    } else {
                        sendResponse(exchange, 404, "application/json", "{\"error\": \"Class group not found\"}");
                    }
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    private static class RoomHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            DatabaseManager db = DatabaseManager.getInstance();

            try {
                if (method.equalsIgnoreCase("GET")) {
                    List<Room> list = db.getRooms();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        sb.append(list.get(i).toDocument().toJson());
                        if (i < list.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    sendResponse(exchange, 200, "application/json", sb.toString());

                } else if (method.equalsIgnoreCase("POST")) {
                    String body = readRequestBody(exchange);
                    Document doc = Document.parse(body);

                    String roomId = doc.getString("roomId");
                    String roomNo = doc.getString("roomNo");
                    String department = doc.getString("department");

                    if (roomId == null || roomId.trim().isEmpty() || roomNo == null || roomNo.trim().isEmpty()) {
                        sendResponse(exchange, 400, "application/json", "{\"error\": \"Missing required fields: roomId, roomNo\"}");
                        return;
                    }

                    if (department == null || department.trim().isEmpty()) {
                        department = "cse-aiml";
                    }

                    Room room = new Room(roomId.trim(), roomNo.trim(), department.trim());
                    boolean success = db.saveRoom(room);

                    if (success) {
                        sendResponse(exchange, 201, "application/json", "{\"message\": \"Room saved successfully!\"}");
                    } else {
                        sendResponse(exchange, 409, "application/json", "{\"error\": \"Duplicate ID Constraint Violation\"}");
                    }

                } else if (method.equalsIgnoreCase("DELETE")) {
                    String id = getQueryParam(exchange, "id");
                    if (id == null || id.trim().isEmpty()) {
                        sendResponse(exchange, 400, "application/json", "{\"error\": \"Missing parameter: id\"}");
                        return;
                    }

                    boolean success = db.deleteRoom(id.trim());
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"message\": \"Room deleted successfully!\"}");
                    } else {
                        sendResponse(exchange, 404, "application/json", "{\"error\": \"Room not found\"}");
                    }
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    private static class TimeslotHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            DatabaseManager db = DatabaseManager.getInstance();

            try {
                if (method.equalsIgnoreCase("GET")) {
                    List<Timeslot> list = db.getTimeslots();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        sb.append(list.get(i).toDocument().toJson());
                        if (i < list.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    sendResponse(exchange, 200, "application/json", sb.toString());

                } else if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("DELETE")) {
                    sendResponse(exchange, 405, "application/json", "{\"error\": \"Method not allowed - timeslots are static\"}");
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    private static class GenerateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "application/json", "{\"error\": \"Method not allowed\"}");
                return;
            }

            try {
                System.out.println("Time Table Management Core: Triggering AI Backtracking Scheduler solver...");
                TimetableGenerator generator = new TimetableGenerator();
                boolean success = generator.generate();

                Document responseDoc = new Document()
                        .append("success", success)
                        .append("executionTimeMs", generator.getExecutionTimeMs())
                        .append("backtrackCount", generator.getBacktrackCount())
                        .append("totalAllocations", generator.getTotalAllocations());

                if (success) {
                    sendResponse(exchange, 200, "application/json", responseDoc.toJson());
                } else {
                    sendResponse(exchange, 422, "application/json", responseDoc.toJson());
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    private static class TimetableHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            DatabaseManager db = DatabaseManager.getInstance();

            try {
                if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    List<TimetableEntry> list = db.getTimetable();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        sb.append(list.get(i).toDocument().toJson());
                        if (i < list.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    sendResponse(exchange, 200, "application/json", sb.toString());
                } else {
                    sendResponse(exchange, 405, "application/json", "{\"error\": \"Method not allowed\"}");
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    private static class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "application/json", "{\"error\": \"Method not allowed\"}");
                return;
            }

            try {
                DatabaseManager db = DatabaseManager.getInstance();
                db.populateSampleData();
                sendResponse(exchange, 200, "application/json", "{\"message\": \"Database reset and sample data populated successfully!\"}");
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    private static class PurgeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "application/json", "{\"error\": \"Method not allowed\"}");
                return;
            }

            try {
                DatabaseManager db = DatabaseManager.getInstance();
                db.purgeAllData();
                sendResponse(exchange, 200, "application/json", "{\"message\": \"All data has been purged from the database successfully!\"}");
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }
}

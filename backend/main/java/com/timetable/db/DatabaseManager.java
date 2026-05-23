package com.timetable.db;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.timetable.model.*;
import org.bson.Document;

import javax.swing.*;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller class managing Database connectivity and CRUD operations.
 * Implements Singleton pattern and supports automatic Mock fallback using local JSON.
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    
    private boolean isMockMode = false;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private String dbName = "automated_timetable_system";

    // In-memory collections for Mock Mode
    private final List<Teacher> teachers = new ArrayList<>();
    private final List<Subject> subjects = new ArrayList<>();
    private final List<ClassGroup> classes = new ArrayList<>();
    private final List<Room> rooms = new ArrayList<>();
    private final List<Timeslot> timeslots = new ArrayList<>();
    private final List<TimetableEntry> timetable = new ArrayList<>();

    private final String localDbFile = "local_database.json";

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public boolean isMockMode() {
        return isMockMode;
    }

    /**
     * Attempts to connect to local/configured MongoDB.
     * Appends client-side timeout options so it fails fast (2 seconds) if service is offline.
     */
    private void initializeDatabase() {
        try {
            String envUri = System.getProperty("MONGO_URI", "mongodb://localhost:27017");
            dbName = System.getProperty("MONGO_DB_NAME", "automated_timetable_system");

            // Build the connection URI with fail-fast timeout options
            String connectionURI = envUri;
            if (!connectionURI.contains("connectTimeoutMS")) {
                if (connectionURI.contains("?")) {
                    connectionURI += "&connectTimeoutMS=15000&socketTimeoutMS=15000&serverSelectionTimeoutMS=15000";
                } else {
                    if (connectionURI.endsWith("/")) {
                        connectionURI += "?connectTimeoutMS=15000&socketTimeoutMS=15000&serverSelectionTimeoutMS=15000";
                    } else {
                        connectionURI += "/?connectTimeoutMS=15000&socketTimeoutMS=15000&serverSelectionTimeoutMS=15000";
                    }
                }
            }

            MongoClientURI uri = new MongoClientURI(connectionURI);
            mongoClient = new MongoClient(uri);
            
            // Check if URI specifies a database name; fallback to dbName otherwise
            String targetDbName = uri.getDatabase() != null ? uri.getDatabase() : dbName;
            database = mongoClient.getDatabase(targetDbName);
            
            // Force connection test by querying database names
            mongoClient.listDatabaseNames().first();
            isMockMode = false;
            System.out.println("Successfully connected to MongoDB (" + targetDbName + ")!");
            
            // Check if db collections are empty
            if (getTeachers().isEmpty() && getRooms().isEmpty() && getTimeslots().isEmpty()) {
                System.out.println("MongoDB is empty. Ready for custom data entries.");
            }
        } catch (Exception e) {
            System.err.println("MongoDB connection failed: " + e.getMessage());
            System.out.println("Switching to persistent Local JSON Database Mode...");
            isMockMode = true;
            
            File file = new File(localDbFile);
            if (file.exists()) {
                System.out.println("Loading existing Local JSON Database from " + localDbFile + "...");
                loadLocalDatabase();
            } else {
                System.out.println("Local database file not found. Ready for custom data entries.");
                saveLocalDatabase(); // Save empty structure
            }
        }
    }

    // ==========================================
    // LOCAL JSON PERSISTENCE (MOCK MODE ENGINE)
    // ==========================================

    private void saveLocalDatabase() {
        if (!isMockMode) return;
        try {
            Document root = new Document();
            
            List<Document> teacherDocs = new ArrayList<>();
            for (Teacher t : teachers) teacherDocs.add(t.toDocument());
            root.append("teachers", teacherDocs);

            List<Document> subjectDocs = new ArrayList<>();
            for (Subject s : subjects) subjectDocs.add(s.toDocument());
            root.append("subjects", subjectDocs);

            List<Document> classDocs = new ArrayList<>();
            for (ClassGroup c : classes) classDocs.add(c.toDocument());
            root.append("classes", classDocs);

            List<Document> roomDocs = new ArrayList<>();
            for (Room r : rooms) roomDocs.add(r.toDocument());
            root.append("rooms", roomDocs);

            List<Document> slotDocs = new ArrayList<>();
            for (Timeslot s : timeslots) slotDocs.add(s.toDocument());
            root.append("timeslots", slotDocs);

            List<Document> ttDocs = new ArrayList<>();
            for (TimetableEntry e : timetable) ttDocs.add(e.toDocument());
            root.append("timetable", ttDocs);

            String prettyJson = root.toJson(); // BSON document to JSON string representation
            try (PrintWriter out = new PrintWriter(localDbFile)) {
                out.println(prettyJson);
            }
        } catch (Exception e) {
            System.err.println("Failed to save local database: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadLocalDatabase() {
        try {
            File file = new File(localDbFile);
            if (!file.exists()) return;

            String content = new String(Files.readAllBytes(Paths.get(localDbFile)));
            if (content.trim().isEmpty()) return;

            Document root = Document.parse(content);

            teachers.clear();
            List<Document> teacherDocs = (List<Document>) root.get("teachers");
            if (teacherDocs != null) {
                for (Document doc : teacherDocs) teachers.add(Teacher.fromDocument(doc));
            }

            subjects.clear();
            List<Document> subjectDocs = (List<Document>) root.get("subjects");
            if (subjectDocs != null) {
                for (Document doc : subjectDocs) subjects.add(Subject.fromDocument(doc));
            }

            classes.clear();
            List<Document> classDocs = (List<Document>) root.get("classes");
            if (classDocs != null) {
                for (Document doc : classDocs) classes.add(ClassGroup.fromDocument(doc));
            }

            rooms.clear();
            List<Document> roomDocs = (List<Document>) root.get("rooms");
            if (roomDocs != null) {
                for (Document doc : roomDocs) rooms.add(Room.fromDocument(doc));
            }

            timeslots.clear();
            List<Document> slotDocs = (List<Document>) root.get("timeslots");
            if (slotDocs != null) {
                for (Document doc : slotDocs) timeslots.add(Timeslot.fromDocument(doc));
            }

            timetable.clear();
            List<Document> ttDocs = (List<Document>) root.get("timetable");
            if (ttDocs != null) {
                for (Document doc : ttDocs) timetable.add(TimetableEntry.fromDocument(doc));
            }
        } catch (Exception e) {
            System.err.println("Failed to load local database: " + e.getMessage());
        }
    }

    // ==========================================
    // CRUD OPERATIONS: TEACHERS
    // ==========================================

    public boolean saveTeacher(Teacher teacher) {
        if (isMockMode) {
            // Validate Unique PK Constraints
            for (Teacher t : teachers) {
                if (t.getTeacherId().equalsIgnoreCase(teacher.getTeacherId())) {
                    return false; // Unique key violation
                }
            }
            teachers.add(teacher);
            saveLocalDatabase();
            return true;
        } else {
            try {
                MongoCollection<Document> col = database.getCollection("teachers");
                if (col.find(new Document("teacher_id", teacher.getTeacherId())).first() != null) {
                    return false; // Unique key violation
                }
                col.insertOne(teacher.toDocument());
                return true;
            } catch (Exception e) {
                System.err.println("Error saving teacher: " + e.getMessage());
                return false;
            }
        }
    }

    public List<Teacher> getTeachers() {
        if (isMockMode) {
            return new ArrayList<>(teachers);
        } else {
            List<Teacher> list = new ArrayList<>();
            try {
                MongoCollection<Document> col = database.getCollection("teachers");
                for (Document doc : col.find()) {
                    list.add(Teacher.fromDocument(doc));
                }
            } catch (Exception e) {
                System.err.println("Error getting teachers: " + e.getMessage());
            }
            return list;
        }
    }

    public boolean deleteTeacher(String teacherId) {
        if (isMockMode) {
            boolean removed = teachers.removeIf(t -> t.getTeacherId().equalsIgnoreCase(teacherId));
            if (removed) saveLocalDatabase();
            return removed;
        } else {
            try {
                MongoCollection<Document> col = database.getCollection("teachers");
                long deleted = col.deleteOne(new Document("teacher_id", teacherId)).getDeletedCount();
                return deleted > 0;
            } catch (Exception e) {
                System.err.println("Error deleting teacher: " + e.getMessage());
                return false;
            }
        }
    }

    // ==========================================
    // CRUD OPERATIONS: SUBJECTS
    // ==========================================

    public boolean saveSubject(Subject subject) {
        if (isMockMode) {
            for (Subject s : subjects) {
                if (s.getSubId().equalsIgnoreCase(subject.getSubId()) && s.getClassId().equalsIgnoreCase(subject.getClassId())) {
                    return false; // Unique composite key constraint: sub_id + class_id
                }
            }
            subjects.add(subject);
            saveLocalDatabase();
            return true;
        } else {
            try {
                MongoCollection<Document> col = database.getCollection("subjects");
                if (col.find(new Document("sub_id", subject.getSubId()).append("class_id", subject.getClassId())).first() != null) {
                    return false;
                }
                col.insertOne(subject.toDocument());
                return true;
            } catch (Exception e) {
                System.err.println("Error saving subject: " + e.getMessage());
                return false;
            }
        }
    }

    public List<Subject> getSubjects() {
        if (isMockMode) {
            return new ArrayList<>(subjects);
        } else {
            List<Subject> list = new ArrayList<>();
            try {
                MongoCollection<Document> col = database.getCollection("subjects");
                for (Document doc : col.find()) {
                    list.add(Subject.fromDocument(doc));
                }
            } catch (Exception e) {
                System.err.println("Error getting subjects: " + e.getMessage());
            }
            return list;
        }
    }

    public boolean deleteSubject(String subId) {
        if (isMockMode) {
            boolean removed = subjects.removeIf(s -> s.getSubId().equalsIgnoreCase(subId));
            if (removed) saveLocalDatabase();
            return removed;
        } else {
            try {
                MongoCollection<Document> col = database.getCollection("subjects");
                return col.deleteMany(new Document("sub_id", subId)).getDeletedCount() > 0;
            } catch (Exception e) {
                System.err.println("Error deleting subject: " + e.getMessage());
                return false;
            }
        }
    }

    public boolean deleteSubject(String subId, String classId) {
        if (isMockMode) {
            boolean removed = subjects.removeIf(s -> s.getSubId().equalsIgnoreCase(subId) && s.getClassId().equalsIgnoreCase(classId));
            if (removed) saveLocalDatabase();
            return removed;
        } else {
            try {
                MongoCollection<Document> col = database.getCollection("subjects");
                return col.deleteOne(new Document("sub_id", subId).append("class_id", classId)).getDeletedCount() > 0;
            } catch (Exception e) {
                System.err.println("Error deleting subject: " + e.getMessage());
                return false;
            }
        }
    }

    // ==========================================
    // CRUD OPERATIONS: CLASSES
    // ==========================================

    public boolean saveClassGroup(ClassGroup classGroup) {
        if (isMockMode) {
            for (ClassGroup c : classes) {
                if (c.getClassId().equalsIgnoreCase(classGroup.getClassId())) {
                    return false; // Unique PK constraint
                }
            }
            classes.add(classGroup);
            saveLocalDatabase();
            return true;
        } else {
            try {
                MongoCollection<Document> col = database.getCollection("classes");
                if (col.find(new Document("class_id", classGroup.getClassId())).first() != null) {
                    return false;
                }
                col.insertOne(classGroup.toDocument());
                return true;
            } catch (Exception e) {
                System.err.println("Error saving class: " + e.getMessage());
                return false;
            }
        }
    }

    public List<ClassGroup> getClassGroups() {
        if (isMockMode) {
            return new ArrayList<>(classes);
        } else {
            List<ClassGroup> list = new ArrayList<>();
            try {
                MongoCollection<Document> col = database.getCollection("classes");
                for (Document doc : col.find()) {
                    list.add(ClassGroup.fromDocument(doc));
                }
            } catch (Exception e) {
                System.err.println("Error getting classes: " + e.getMessage());
            }
            return list;
        }
    }

    public boolean deleteClassGroup(String classId) {
        if (isMockMode) {
            boolean removed = classes.removeIf(c -> c.getClassId().equalsIgnoreCase(classId));
            if (removed) saveLocalDatabase();
            return removed;
        } else {
            try {
                MongoCollection<Document> col = database.getCollection("classes");
                return col.deleteOne(new Document("class_id", classId)).getDeletedCount() > 0;
            } catch (Exception e) {
                System.err.println("Error deleting class: " + e.getMessage());
                return false;
            }
        }
    }

    // ==========================================
    // CRUD OPERATIONS: ROOMS
    // ==========================================

    public boolean saveRoom(Room room) {
        if (isMockMode) {
            for (Room r : rooms) {
                if (r.getRoomId().equalsIgnoreCase(room.getRoomId())) {
                    return false; // Unique PK constraint
                }
            }
            rooms.add(room);
            saveLocalDatabase();
            return true;
        } else {
            try {
                MongoCollection<Document> col = database.getCollection("rooms");
                if (col.find(new Document("room_id", room.getRoomId())).first() != null) {
                    return false;
                }
                col.insertOne(room.toDocument());
                return true;
            } catch (Exception e) {
                System.err.println("Error saving room: " + e.getMessage());
                return false;
            }
        }
    }

    public List<Room> getRooms() {
        if (isMockMode) {
            return new ArrayList<>(rooms);
        } else {
            List<Room> list = new ArrayList<>();
            try {
                MongoCollection<Document> col = database.getCollection("rooms");
                for (Document doc : col.find()) {
                    list.add(Room.fromDocument(doc));
                }
            } catch (Exception e) {
                System.err.println("Error getting rooms: " + e.getMessage());
            }
            return list;
        }
    }

    public boolean deleteRoom(String roomId) {
        if (isMockMode) {
            boolean removed = rooms.removeIf(r -> r.getRoomId().equalsIgnoreCase(roomId));
            if (removed) saveLocalDatabase();
            return removed;
        } else {
            try {
                MongoCollection<Document> col = database.getCollection("rooms");
                return col.deleteOne(new Document("room_id", roomId)).getDeletedCount() > 0;
            } catch (Exception e) {
                System.err.println("Error deleting room: " + e.getMessage());
                return false;
            }
        }
    }

    // ==========================================
    // CRUD OPERATIONS: TIMESLOTS
    // ==========================================

    public boolean saveTimeslot(Timeslot timeslot) {
        return true;
    }

    public List<Timeslot> getTimeslots() {
        List<Timeslot> list = new ArrayList<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String[] hours = {
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

        int slotCount = 1;
        for (String day : days) {
            int limit = day.equals("Saturday") ? 5 : hours.length;
            for (int h = 0; h < limit; h++) {
                String slotId = "SL" + String.format("%02d", slotCount++);
                boolean isBreak = hours[h].equals("10:30 AM - 10:45 AM") || hours[h].equals("12:45 PM - 01:45 PM");
                list.add(new Timeslot(slotId, day, hours[h], isBreak));
            }
        }
        return list;
    }

    public boolean deleteTimeslot(String slotId) {
        return true;
    }

    // ==========================================
    // CRUD OPERATIONS: TIMETABLE
    // ==========================================

    public void clearTimetable() {
        if (isMockMode) {
            timetable.clear();
            saveLocalDatabase();
        } else {
            try {
                database.getCollection("timetable").deleteMany(new Document());
            } catch (Exception e) {
                System.err.println("Error clearing timetable: " + e.getMessage());
            }
        }
    }

    public void saveTimetable(List<TimetableEntry> entries) {
        clearTimetable();
        if (isMockMode) {
            timetable.addAll(entries);
            saveLocalDatabase();
        } else {
            try {
                MongoCollection<Document> col = database.getCollection("timetable");
                List<Document> docs = new ArrayList<>();
                for (TimetableEntry e : entries) {
                    docs.add(e.toDocument());
                }
                if (!docs.isEmpty()) {
                    col.insertMany(docs);
                }
            } catch (Exception e) {
                System.err.println("Error saving timetable entries: " + e.getMessage());
            }
        }
    }

    public List<TimetableEntry> getTimetable() {
        if (isMockMode) {
            return new ArrayList<>(timetable);
        } else {
            List<TimetableEntry> list = new ArrayList<>();
            try {
                MongoCollection<Document> col = database.getCollection("timetable");
                for (Document doc : col.find()) {
                    list.add(TimetableEntry.fromDocument(doc));
                }
            } catch (Exception e) {
                System.err.println("Error getting timetable: " + e.getMessage());
            }
            return list;
        }
    }

    // ==========================================
    // PRE-POPULATE HIGH-QUALITY SAMPLE DATA
    // ==========================================

    public void populateSampleData() {
        // Clear all lists if in mock mode
        if (isMockMode) {
            teachers.clear();
            subjects.clear();
            classes.clear();
            rooms.clear();
            timeslots.clear();
            timetable.clear();
        } else {
            try {
                database.getCollection("teachers").deleteMany(new Document());
                database.getCollection("subjects").deleteMany(new Document());
                database.getCollection("classes").deleteMany(new Document());
                database.getCollection("rooms").deleteMany(new Document());
                database.getCollection("timeslots").deleteMany(new Document());
                database.getCollection("timetable").deleteMany(new Document());
            } catch (Exception e) {
                System.err.println("Error clearing MongoDB collections: " + e.getMessage());
            }
        }

        // 1. Save Teachers
        saveTeacher(new Teacher("AI101", "Dr. Jayashree T R", "cse-aiml"));
        saveTeacher(new Teacher("AI102", "Dr. Pushpalatha K", "cse-aiml"));
        saveTeacher(new Teacher("AI103", "Mrs. Sangeetha M S", "cse-aiml"));
        saveTeacher(new Teacher("AI104", "Ms. Deeksha J S", "cse-aiml"));
        saveTeacher(new Teacher("AI105", "Mrs. Shruthi Vishwajeeth", "cse-aiml"));
        saveTeacher(new Teacher("AI106", "Mrs. Suchetha Sheka", "cse-aiml"));
        saveTeacher(new Teacher("AI107", "Ms. Anujna K Y", "cse-aiml"));
        saveTeacher(new Teacher("AI108", "Ms. Monisha", "cse-aiml"));
        saveTeacher(new Teacher("AI109", "Dr. Sadhana Rai", "cse-aiml"));
        saveTeacher(new Teacher("AI110", "Ms. Soumya", "cse-aiml"));
        saveTeacher(new Teacher("AI111", "Ms. Shweta S Naik", "cse-aiml"));
        saveTeacher(new Teacher("AI112", "Mrs. Rajeshwari Shettigar", "cse-aiml"));
        saveTeacher(new Teacher("AI113", "Dr. Duddela Sai Prashanth", "cse-aiml"));
        saveTeacher(new Teacher("AI114", "Mr. Ganaraj K", "cse-aiml"));
        saveTeacher(new Teacher("AI115", "Ms. Shreekshitha", "cse-aiml"));
        saveTeacher(new Teacher("AI116", "Mr. Sharathchandra N R", "cse-aiml"));
        saveTeacher(new Teacher("AI117", "Dr. Gurusiddayya Hiremath", "cse-aiml"));
        saveTeacher(new Teacher("AI118", "Mrs. Chaithrika Aditya", "cse-aiml"));

        // 2. Save Classes (SemSecs)
        saveClassGroup(new ClassGroup("4A", "cse-aiml", 4));
        saveClassGroup(new ClassGroup("4B", "cse-aiml", 4));
        saveClassGroup(new ClassGroup("4C", "cse-aiml", 4));
        saveClassGroup(new ClassGroup("6A", "cse-aiml", 6));
        saveClassGroup(new ClassGroup("6B", "cse-aiml", 6));
        saveClassGroup(new ClassGroup("6C", "cse-aiml", 6));

        // 3. Save Rooms
        saveRoom(new Room("4A", "404", "cse-aiml"));
        saveRoom(new Room("4B", "405", "cse-aiml"));
        saveRoom(new Room("4C", "406", "cse-aiml"));
        saveRoom(new Room("6A", "312", "cse-aiml"));
        saveRoom(new Room("6B", "313", "cse-aiml"));
        saveRoom(new Room("6C", "314", "cse-aiml"));

        // 4. Save Timeslots
        // Define timeslots for Monday through Saturday
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String[] hours = {
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

        int slotCount = 1;
        for (String day : days) {
            int limit = day.equals("Saturday") ? 5 : hours.length;
            for (int h = 0; h < limit; h++) {
                String slotId = "SL" + String.format("%02d", slotCount++);
                boolean isBreak = hours[h].equals("10:30 AM - 10:45 AM") || hours[h].equals("12:45 PM - 01:45 PM");
                saveTimeslot(new Timeslot(slotId, day, hours[h], isBreak));
            }
        }

        // 5. Save Subjects (Course Code as code, Class ID identifies SemSec)
        // 4A
        saveSubject(new Subject("BAI401G", "Design and Analysis of Algorithms", 4, "4A", "AI101"));
        saveSubject(new Subject("BAI402G", "Database Management System", 4, "4A", "AI103"));
        saveSubject(new Subject("BAI403T", "Principles of Operating Systems", 3, "4A", "AI104"));
        saveSubject(new Subject("BAI404T", "Artificial Intelligence", 4, "4A", "AI105"));
        saveSubject(new Subject("BAI405L", "Artificial Intelligence Laboratory", 2, "4A", "AI105"));
        saveSubject(new Subject("BAI406W1", "Mathematical Foundations for Machine Learning", 3, "4A", "AI106"));
        saveSubject(new Subject("BAI401G_LAB", "Design and Analysis of Algorithms Laboratory", 2, "4A", "AI101"));
        saveSubject(new Subject("BAI402G_LAB", "Database Management System Laboratory", 2, "4A", "AI103"));
        saveSubject(new Subject("BHU407TK", "UHV-2: Understanding Harmony and Ethical Conduct", 2, "4A", "AI108"));
        saveSubject(new Subject("BAI408A2", "Data Visualization Using R", 2, "4A", "AI104"));
        saveSubject(new Subject("BBS409TC", "Biology for Engineers", 2, "4A", "AI108"));
 
        // 4B
        saveSubject(new Subject("BAI401G", "Design and Analysis of Algorithms", 4, "4B", "AI109"));
        saveSubject(new Subject("BAI402G", "Database Management System", 4, "4B", "AI110"));
        saveSubject(new Subject("BAI403T", "Principles of Operating Systems", 4, "4B", "AI111"));
        saveSubject(new Subject("BAI404T", "Artificial Intelligence", 4, "4B", "AI112"));
        saveSubject(new Subject("BAI405L", "Artificial Intelligence Laboratory", 2, "4B", "AI112"));
        saveSubject(new Subject("BAI406W1", "Mathematical Foundations for Machine Learning", 3, "4B", "AI106"));
        saveSubject(new Subject("BAI401G_LAB", "Design and Analysis of Algorithms Laboratory", 2, "4B", "AI109"));
        saveSubject(new Subject("BAI402G_LAB", "Database Management System Laboratory", 2, "4B", "AI110"));
        saveSubject(new Subject("BHU407TK", "UHV-2: Understanding Harmony and Ethical Conduct", 2, "4B", "AI110"));
        saveSubject(new Subject("BAI408A2", "Data Visualization Using R", 2, "4B", "AI107"));
        saveSubject(new Subject("BBS409TC", "Biology for Engineers", 2, "4B", "AI108"));
 
        // 4C
        saveSubject(new Subject("BAI401G", "Design and Analysis of Algorithms", 4, "4C", "AI113"));
        saveSubject(new Subject("BAI402G", "Database Management System", 4, "4C", "AI114"));
        saveSubject(new Subject("BAI403T", "Principles of Operating Systems", 4, "4C", "AI115"));
        saveSubject(new Subject("BAI404T", "Artificial Intelligence", 4, "4C", "AI116"));
        saveSubject(new Subject("BAI405L", "Artificial Intelligence Laboratory", 2, "4C", "AI116"));
        saveSubject(new Subject("BAI406W1", "Mathematical Foundations for Machine Learning", 3, "4C", "AI106"));
        saveSubject(new Subject("BAI401G_LAB", "Design and Analysis of Algorithms Laboratory", 2, "4C", "AI113"));
        saveSubject(new Subject("BAI402G_LAB", "Database Management System Laboratory", 2, "4C", "AI114"));
        saveSubject(new Subject("BHU407TK", "UHV-2: Understanding Harmony and Ethical Conduct", 2, "4C", "AI114"));
        saveSubject(new Subject("BAI408A4", "Python Libraries", 1, "4C", "AI117"));
        saveSubject(new Subject("BBS409TC", "Biology for Engineers", 2, "4C", "AI108"));
 
        // 6A
        saveSubject(new Subject("CS622T1C", "Software Engineering and Project Management", 3, "6A", "AI110"));
        saveSubject(new Subject("AM622I2A", "Natural Language Processing", 3, "6A", "AI111"));
        saveSubject(new Subject("AM622T3A", "Image Processing and Computer Vision", 3, "6A", "AI115"));
        saveSubject(new Subject("AM62214AC", "Cloud Computing and Applications", 3, "6A", "AI112"));
        saveSubject(new Subject("AM62214AD", "Data Science and Big Data Analytics", 3, "6A", "AI105"));
        saveSubject(new Subject("AM622L7A", "Image Processing and Computer Vision Laboratory", 2, "6A", "AI115"));
        saveSubject(new Subject("AM622I2A_LAB", "Natural Language Processing Laboratory", 2, "6A", "AI111"));
        saveSubject(new Subject("AM62299AD", "Network Security", 2, "6A", "AI101"));
        saveSubject(new Subject("AM622P6A", "Project Work Phase I", 8, "6A", "AI117"));
 
        // 6B
        saveSubject(new Subject("CS622T1C", "Software Engineering and Project Management", 3, "6B", "AI113"));
        saveSubject(new Subject("AM622I2A", "Natural Language Processing", 3, "6B", "AI102"));
        saveSubject(new Subject("AM622T3A", "Image Processing and Computer Vision", 3, "6B", "AI117"));
        saveSubject(new Subject("AM62214AC", "Cloud Computing and Applications", 3, "6B", "AI112"));
        saveSubject(new Subject("AM62214AD", "Data Science and Big Data Analytics", 3, "6B", "AI105"));
        saveSubject(new Subject("AM622L7A", "Image Processing and Computer Vision Laboratory", 2, "6B", "AI117"));
        saveSubject(new Subject("AM622I2A_LAB", "Natural Language Processing Laboratory", 2, "6B", "AI109"));
        saveSubject(new Subject("AM62299AA", "3-D Animation using Blender", 2, "6B", "AI107"));
        saveSubject(new Subject("AM622P6A", "Project Work Phase I", 8, "6B", "AI109"));
 
        // 6C
        saveSubject(new Subject("CS622T1C", "Software Engineering and Project Management", 3, "6C", "AI104"));
        saveSubject(new Subject("AM622I2A", "Natural Language Processing", 3, "6C", "AI103"));
        saveSubject(new Subject("AM622T3A", "Image Processing and Computer Vision", 3, "6C", "AI118"));
        saveSubject(new Subject("AM62214AC", "Cloud Computing and Applications", 3, "6C", "AI112"));
        saveSubject(new Subject("AM62214AD", "Data Science and Big Data Analytics", 3, "6C", "AI105"));
        saveSubject(new Subject("AM622L7A", "Image Processing and Computer Vision Laboratory", 2, "6C", "AI118"));
        saveSubject(new Subject("AM622I2A_LAB", "Natural Language Processing Laboratory", 2, "6C", "AI103"));
        saveSubject(new Subject("AM62299AA", "3-D Animation using Blender", 2, "6C", "AI104"));
        saveSubject(new Subject("AM622P6A", "Project Work Phase I", 8, "6C", "AI101"));

        if (isMockMode) {
            saveLocalDatabase();
        }
        System.out.println("Database initialization completed with sample records!");
    }

    public void purgeAllData() {
        if (isMockMode) {
            teachers.clear();
            subjects.clear();
            classes.clear();
            rooms.clear();
            timeslots.clear();
            timetable.clear();
            saveLocalDatabase();
        } else {
            try {
                database.getCollection("teachers").deleteMany(new Document());
                database.getCollection("subjects").deleteMany(new Document());
                database.getCollection("classes").deleteMany(new Document());
                database.getCollection("rooms").deleteMany(new Document());
                database.getCollection("timeslots").deleteMany(new Document());
                database.getCollection("timetable").deleteMany(new Document());
                System.out.println("All data successfully purged from MongoDB Atlas!");
            } catch (Exception e) {
                System.err.println("Error purging MongoDB collections: " + e.getMessage());
                throw new RuntimeException("Failed to purge data from MongoDB Atlas", e);
            }
        }
    }
}

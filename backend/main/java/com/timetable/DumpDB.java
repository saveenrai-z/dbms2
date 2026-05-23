package com.timetable;

import com.timetable.db.DatabaseManager;
import com.timetable.model.*;
import java.util.List;

public class DumpDB {
    public static void main(String[] args) {
        Main.loadEnv();
        DatabaseManager db = DatabaseManager.getInstance();
        
        System.out.println("--- CLASSES ---");
        List<ClassGroup> classes = db.getClassGroups();
        for (ClassGroup c : classes) {
            System.out.println(c.getClassId() + " (" + c.getClassName() + ", Sem " + c.getSemester() + ")");
        }
        
        System.out.println("--- ROOMS ---");
        List<Room> rooms = db.getRooms();
        for (Room r : rooms) {
            System.out.println(r.getRoomId() + " - No: " + r.getRoomNo() + " (Dept: " + r.getDepartment() + ")");
        }
        
        System.out.println("--- SUBJECTS ---");
        List<Subject> subjects = db.getSubjects();
        for (Subject s : subjects) {
            System.out.println(s.getSubId() + ": " + s.getSubName() + " (" + s.getHours() + " hours, Class: " + s.getClassId() + ", Teacher: " + s.getTeacherId() + ")");
        }
    }
}

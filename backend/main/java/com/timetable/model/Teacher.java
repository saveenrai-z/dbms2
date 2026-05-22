package com.timetable.model;

import org.bson.Document;

/**
 * Model representing a Teacher/Faculty member.
 */
public class Teacher {
    private String teacherId;
    private String name;
    private String department;

    public Teacher() {}

    public Teacher(String teacherId, String name) {
        this(teacherId, name, "cse-aiml");
    }

    public Teacher(String teacherId, String name, String department) {
        this.teacherId = teacherId;
        this.name = name;
        this.department = department;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * Converts Teacher object to a MongoDB Document.
     */
    public Document toDocument() {
        return new Document("teacher_id", teacherId)
                .append("name", name)
                .append("department", department);
    }

    /**
     * Creates a Teacher object from a MongoDB Document.
     */
    public static Teacher fromDocument(Document doc) {
        if (doc == null) return null;
        String dept = doc.getString("department");
        if (dept == null || dept.trim().isEmpty()) {
            dept = "cse-aiml";
        }
        return new Teacher(
            doc.getString("teacher_id"),
            doc.getString("name"),
            dept
        );
    }

    @Override
    public String toString() {
        return name + " (" + teacherId + ")";
    }
}

package com.timetable.model;

import org.bson.Document;

/**
 * Model representing a Class / student SemSec.
 * Named ClassGroup to avoid name collision with Java's built-in java.lang.Class.
 */
public class ClassGroup {
    private String classId;   // e.g., "CSE5A"
    private String className; // e.g., "Computer Science"
    private int semester;     // e.g., 5

    public ClassGroup() {}

    public ClassGroup(String classId, String className, int semester) {
        this.classId = classId;
        this.className = className != null ? className.replaceAll("\\s+[0-9]+[a-zA-Z]+$", "") : null;
        this.semester = semester;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className != null ? className.replaceAll("\\s+[0-9]+[a-zA-Z]+$", "") : null;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    /**
     * Converts ClassGroup object to a MongoDB Document.
     */
    public Document toDocument() {
        return new Document("class_id", classId)
                .append("class_name", className)
                .append("semester", semester);
    }

    /**
     * Creates a ClassGroup object from a MongoDB Document.
     */
    public static ClassGroup fromDocument(Document doc) {
        if (doc == null) return null;
        return new ClassGroup(
            doc.getString("class_id"),
            doc.getString("class_name"),
            doc.getInteger("semester", 1)
        );
    }

    @Override
    public String toString() {
        return classId + " (Sem " + semester + ")";
    }
}

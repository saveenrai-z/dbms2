package com.timetable.model;

import org.bson.Document;

/**
 * Model representing a Subject/Course.
 * Captures "Teacher teaches Subject" and "Subject belongs to Class" relationships.
 */
public class Subject {
    private String subId;
    private String subName;
    private int hours;
    private String classId;   // Foreign Key to Class Group
    private String teacherId; // Foreign Key to Teacher

    public Subject() {}

    public Subject(String subId, String subName, int hours, String classId, String teacherId) {
        this.subId = subId != null ? subId.replaceAll("-[0-9]+[a-zA-Z]+$", "") : null;
        this.subName = subName;
        this.hours = hours;
        this.classId = classId;
        this.teacherId = teacherId;
    }

    public String getSubId() {
        return subId;
    }

    public void setSubId(String subId) {
        this.subId = subId != null ? subId.replaceAll("-[0-9]+[a-zA-Z]+$", "") : null;
    }

    public String getSubName() {
        return subName;
    }

    public void setSubName(String subName) {
        this.subName = subName;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    /**
     * Converts Subject object to a MongoDB Document.
     */
    public Document toDocument() {
        return new Document("sub_id", subId)
                .append("sub_name", subName)
                .append("hours", hours)
                .append("class_id", classId)
                .append("teacher_id", teacherId);
    }

    /**
     * Creates a Subject object from a MongoDB Document.
     */
    public static Subject fromDocument(Document doc) {
        if (doc == null) return null;
        return new Subject(
            doc.getString("sub_id"),
            doc.getString("sub_name"),
            doc.getInteger("hours", 0),
            doc.getString("class_id"),
            doc.getString("teacher_id")
        );
    }

    @Override
    public String toString() {
        return subName + " (" + subId + ", " + hours + " hrs)";
    }
}

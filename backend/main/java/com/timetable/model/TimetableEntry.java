package com.timetable.model;

import org.bson.Document;

/**
 * Model representing a generated / scheduled Timetable cell.
 * Integrates multiple entities as foreign keys, satisfying standard relational/SQL constraints.
 */
public class TimetableEntry {
    private String ttId;      // Primary Key
    private String teacherId; // Foreign Key to Teacher
    private String subId;     // Foreign Key to Subject
    private String classId;   // Foreign Key to ClassGroup
    private String roomId;    // Foreign Key to Room
    private String slotId;    // Foreign Key to Timeslot

    public TimetableEntry() {}

    public TimetableEntry(String ttId, String teacherId, String subId, String classId, String roomId, String slotId) {
        this.ttId = ttId;
        this.teacherId = teacherId;
        this.subId = subId;
        this.classId = classId;
        this.roomId = roomId;
        this.slotId = slotId;
    }

    public String getTtId() {
        return ttId;
    }

    public void setTtId(String ttId) {
        this.ttId = ttId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getSubId() {
        return subId;
    }

    public void setSubId(String subId) {
        this.subId = subId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    /**
     * Converts TimetableEntry to a MongoDB Document.
     */
    public Document toDocument() {
        return new Document("tt_id", ttId)
                .append("teacher_id", teacherId)
                .append("sub_id", subId)
                .append("class_id", classId)
                .append("room_id", roomId)
                .append("slot_id", slotId);
    }

    /**
     * Creates a TimetableEntry from a MongoDB Document.
     */
    public static TimetableEntry fromDocument(Document doc) {
        if (doc == null) return null;
        return new TimetableEntry(
            doc.getString("tt_id"),
            doc.getString("teacher_id"),
            doc.getString("sub_id"),
            doc.getString("class_id"),
            doc.getString("room_id"),
            doc.getString("slot_id")
        );
    }

    @Override
    public String toString() {
        return "TimetableEntry{" +
                "ttId='" + ttId + '\'' +
                ", teacherId='" + teacherId + '\'' +
                ", subId='" + subId + '\'' +
                ", classId='" + classId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", slotId='" + slotId + '\'' +
                '}';
    }
}

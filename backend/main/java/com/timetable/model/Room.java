package com.timetable.model;

import org.bson.Document;

/**
 * Model representing a physical Room/Classroom.
 */
public class Room {
    private String roomId; // e.g., "R201"
    private String roomNo; // e.g., "201"
    private String department; // e.g., "cse-aiml"

    public Room() {}

    public Room(String roomId, String roomNo) {
        this.roomId = roomId;
        this.roomNo = roomNo;
        this.department = "cse-aiml";
    }

    public Room(String roomId, String roomNo, String department) {
        this.roomId = roomId;
        this.roomNo = roomNo;
        this.department = department != null ? department : "cse-aiml";
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public String getDepartment() {
        return department != null ? department : "cse-aiml";
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * Converts Room object to a MongoDB Document.
     */
    public Document toDocument() {
        return new Document("room_id", roomId)
                .append("room_no", roomNo)
                .append("department", getDepartment());
    }

    /**
     * Creates a Room object from a MongoDB Document.
     */
    public static Room fromDocument(Document doc) {
        if (doc == null) return null;
        return new Room(
            doc.getString("room_id"),
            doc.getString("room_no"),
            doc.getString("department")
        );
    }

    @Override
    public String toString() {
        return "Room " + roomNo;
    }
}

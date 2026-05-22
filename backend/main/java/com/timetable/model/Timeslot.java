package com.timetable.model;

import org.bson.Document;

/**
 * Model representing a specific Time Slot in a day.
 */
public class Timeslot {
    private String slotId; // e.g., "SL01"
    private String day;    // e.g., "Monday"
    private String time;   // e.g., "10:00 AM - 11:00 AM"
    private boolean isLunchBreak; // Flag for Lunch Break Constraint

    public Timeslot() {}

    public Timeslot(String slotId, String day, String time) {
        this(slotId, day, time, false);
    }

    public Timeslot(String slotId, String day, String time, boolean isLunchBreak) {
        this.slotId = slotId;
        this.day = day;
        this.time = time;
        this.isLunchBreak = isLunchBreak;
    }

    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isLunchBreak() {
        return isLunchBreak;
    }

    public void setLunchBreak(boolean lunchBreak) {
        isLunchBreak = lunchBreak;
    }

    /**
     * Converts Timeslot object to a MongoDB Document.
     */
    public Document toDocument() {
        return new Document("slot_id", slotId)
                .append("day", day)
                .append("time", time)
                .append("is_lunch_break", isLunchBreak);
    }

    /**
     * Creates a Timeslot object from a MongoDB Document.
     */
    public static Timeslot fromDocument(Document doc) {
        if (doc == null) return null;
        Boolean lunch = doc.getBoolean("is_lunch_break");
        return new Timeslot(
            doc.getString("slot_id"),
            doc.getString("day"),
            doc.getString("time"),
            lunch != null ? lunch : false
        );
    }

    @Override
    public String toString() {
        return day + " " + time + (isLunchBreak ? " (Lunch Break)" : "");
    }
}

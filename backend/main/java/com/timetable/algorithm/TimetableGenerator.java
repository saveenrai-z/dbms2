package com.timetable.algorithm;

import com.timetable.db.DatabaseManager;
import com.timetable.model.*;

import java.util.*;

/**
 * Backtracking algorithm engine that automatically generates a conflict-free timetable.
 */
public class TimetableGenerator {
    
    // Schedule trackers: Map<EntityID, Map<SlotID, TimetableEntry>>
    private final Map<String, Map<String, TimetableEntry>> teacherSchedule = new HashMap<>();
    private final Map<String, Map<String, TimetableEntry>> roomSchedule = new HashMap<>();
    private final Map<String, Map<String, TimetableEntry>> classSchedule = new HashMap<>();

    private List<Timeslot> timeslots;
    private List<Room> rooms;
    private List<Subject> subjects;

    // Execution metrics
    private long executionTimeMs = 0;
    private int backtrackCount = 0;
    private int totalAllocations = 0;

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public int getBacktrackCount() {
        return backtrackCount;
    }

    public int getTotalAllocations() {
        return totalAllocations;
    }

    /**
     * Entry point to trigger the automatic timetable generation.
     * @return true if a complete conflict-free schedule was generated, false otherwise.
     */
    public boolean generate() {
        long startTime = System.currentTimeMillis();
        backtrackCount = 0;
        totalAllocations = 0;

        DatabaseManager db = DatabaseManager.getInstance();
        
        // Fetch fresh copies of active records
        this.timeslots = db.getTimeslots();
        
        // Sort timeslots by period index first (distributes classes evenly across days and considers Saturday early)
        this.timeslots.sort((a, b) -> {
            int pA = getPeriodIndex(a.getTime());
            int pB = getPeriodIndex(b.getTime());
            if (pA != pB) return Integer.compare(pA, pB);
            return a.getDay().compareTo(b.getDay());
        });

        this.rooms = db.getRooms();
        this.subjects = db.getSubjects();

        // Initialize empty tracking maps
        teacherSchedule.clear();
        roomSchedule.clear();
        classSchedule.clear();

        for (Teacher t : db.getTeachers()) {
            teacherSchedule.put(t.getTeacherId(), new HashMap<>());
        }
        for (Room r : rooms) {
            roomSchedule.put(r.getRoomId(), new HashMap<>());
        }
        for (ClassGroup c : db.getClassGroups()) {
            classSchedule.put(c.getClassId(), new HashMap<>());
        }

        // Expand subjects into separate weekly lecture units (1 unit = 1 hour session)
        List<Subject> lectureUnits = new ArrayList<>();
        for (Subject sub : subjects) {
            for (int i = 0; i < sub.getHours(); i++) {
                lectureUnits.add(sub);
            }
        }

        // Group by classId first, then by weekly hours descending.
        // This keeps class groups contiguous in the list and enables immediate eager validation.
        lectureUnits.sort((a, b) -> {
            int classComp = a.getClassId().compareTo(b.getClassId());
            if (classComp != 0) return classComp;
            return Integer.compare(b.getHours(), a.getHours());
        });

        // Trigger backtracking recursion
        boolean success = backtrack(lectureUnits, 0);

        executionTimeMs = System.currentTimeMillis() - startTime;

        if (success) {
            // Save the resulting generated timetable to the database
            List<TimetableEntry> entries = new ArrayList<>();
            for (Map<String, TimetableEntry> slots : classSchedule.values()) {
                entries.addAll(slots.values());
            }
            db.saveTimetable(entries);
            System.out.println("Timetable generated successfully in " + executionTimeMs + "ms! Backtracks: " + backtrackCount);
            return true;
        } else {
            System.err.println("Failed to generate conflict-free timetable! Incomplete constraints.");
            return false;
        }
    }

    /**
     * Recursive backtracking method to assign Timeslot and Room for each lecture unit.
     */
    private boolean backtrack(List<Subject> lectureUnits, int index) {
        if (index >= lectureUnits.size()) {
            return validateOptimalTimetable(); // Validate density and end time constraints at the leaf node
        }

        Subject subject = lectureUnits.get(index);
        String teacherId = subject.getTeacherId();
        String classId = subject.getClassId();

        // Try every combination of Timeslots and Rooms
        for (Timeslot slot : timeslots) {
            // Rule 1: Lunch Break Constraint
            if (slot.isLunchBreak()) {
                continue; // Skip lunch breaks for everyone
            }

            String slotId = slot.getSlotId();

            // Rule 2: Class Collision - Class cannot have multiple subjects at the same time
            if (isClassBusy(classId, slotId)) {
                continue;
            }

            // Rule 3: Faculty Collision - Teacher cannot teach two classes at the same time
            if (isTeacherBusy(teacherId, slotId)) {
                continue;
            }

            // Rule 4: Faculty Workload Balance (Max 3 consecutive classes per day)
            if (isTeacherOverloaded(teacherId, slot)) {
                continue;
            }

            // Rule 5: Class Student Congestion (Max 3 consecutive classes, Max 5 classes per day)
            if (isClassCongested(classId, slot, subject)) {
                continue;
            }

            // Rule 8: Subject Time Diversity - Subject cannot occupy the same time interval too many times per week for this class
            int maxAllowed = subject.getHours() > 4 ? 2 : 1;
            if (isSubjectScheduledAtTime(classId, subject.getSubId(), slot.getTime(), maxAllowed)) {
                continue;
            }

            for (Room room : rooms) {
                String roomId = room.getRoomId();

                // Rule 7 (Fixed Room): Enforce that each class is scheduled in its own fixed room
                if (!roomId.equalsIgnoreCase(classId)) {
                    continue;
                }

                // Rule 6: Room Collision - Room cannot be allocated to multiple classes simultaneously
                if (isRoomBusy(roomId, slotId)) {
                    continue;
                }

                // If all constraints pass, perform allocation
                String ttId = "TT" + String.format("%04d", ++totalAllocations);
                TimetableEntry entry = new TimetableEntry(ttId, teacherId, subject.getSubId(), classId, roomId, slotId);

                allocate(entry);

                // Eager Early Pruning Check: If this is the last lecture unit for the current class,
                // validate the entire schedule for this class group before recursing to the next class group.
                boolean isValid = true;
                boolean isLastOfClass = (index == lectureUnits.size() - 1) || 
                        !lectureUnits.get(index + 1).getClassId().equalsIgnoreCase(classId);
                if (isLastOfClass) {
                    if (!validateClassSchedule(classId)) {
                        isValid = false;
                    }
                }

                if (isValid) {
                    // Recurse to next lecture unit
                    if (backtrack(lectureUnits, index + 1)) {
                        return true;
                    }
                }

                // Backtrack if allocation leads to a future collision or validation failure
                deallocate(entry);
                backtrackCount++;
            }
        }

        return false; // No valid slot found for this course in the current branch
    }

    // ==========================================
    // UTILITY ALLOCATION / DEALLOCATION METHODS
    // ==========================================

    private void allocate(TimetableEntry entry) {
        // Class mapping
        classSchedule.computeIfAbsent(entry.getClassId(), k -> new HashMap<>())
                .put(entry.getSlotId(), entry);

        // Teacher mapping
        teacherSchedule.computeIfAbsent(entry.getTeacherId(), k -> new HashMap<>())
                .put(entry.getSlotId(), entry);

        // Room mapping
        roomSchedule.computeIfAbsent(entry.getRoomId(), k -> new HashMap<>())
                .put(entry.getSlotId(), entry);
    }

    private void deallocate(TimetableEntry entry) {
        Map<String, TimetableEntry> classMap = classSchedule.get(entry.getClassId());
        if (classMap != null) classMap.remove(entry.getSlotId());

        Map<String, TimetableEntry> teacherMap = teacherSchedule.get(entry.getTeacherId());
        if (teacherMap != null) teacherMap.remove(entry.getSlotId());

        Map<String, TimetableEntry> roomMap = roomSchedule.get(entry.getRoomId());
        if (roomMap != null) roomMap.remove(entry.getSlotId());
    }

    // ==========================================
    // COLLISION & CONSTRAINT VERIFICATION METHODS
    // ==========================================

    private boolean isClassBusy(String classId, String slotId) {
        Map<String, TimetableEntry> schedule = classSchedule.get(classId);
        return schedule != null && schedule.containsKey(slotId);
    }

    private boolean isTeacherBusy(String teacherId, String slotId) {
        Map<String, TimetableEntry> schedule = teacherSchedule.get(teacherId);
        return schedule != null && schedule.containsKey(slotId);
    }

    private boolean isRoomBusy(String roomId, String slotId) {
        Map<String, TimetableEntry> schedule = roomSchedule.get(roomId);
        return schedule != null && schedule.containsKey(slotId);
    }

    private boolean isSubjectScheduledAtTime(String classId, String subId, String timeStr, int maxAllowed) {
        Map<String, TimetableEntry> schedule = classSchedule.get(classId);
        if (schedule == null || schedule.isEmpty()) return false;

        int count = 0;
        for (TimetableEntry entry : schedule.values()) {
            if (entry.getSubId().equalsIgnoreCase(subId)) {
                Timeslot s = findTimeslotById(entry.getSlotId());
                if (s != null && s.getTime().equalsIgnoreCase(timeStr)) {
                    count++;
                }
            }
        }
        return count >= maxAllowed;
    }

    /**
     * Faculty Workload Balance: Ensures teacher does not teach:
     * 1. More than 4 total periods on a single day.
     * 2. More than 3 consecutive classes in a day.
     */
    private boolean isTeacherOverloaded(String teacherId, Timeslot targetSlot) {
        Map<String, TimetableEntry> schedule = teacherSchedule.get(teacherId);
        if (schedule == null || schedule.isEmpty()) return false;

        // Count daily scheduled slots for this teacher
        int dailyCount = 0;
        for (String slotId : schedule.keySet()) {
            Timeslot s = findTimeslotById(slotId);
            if (s != null && s.getDay().equalsIgnoreCase(targetSlot.getDay())) {
                dailyCount++;
            }
        }
        if (dailyCount >= 4) {
            return true;
        }

        return hasConsecutiveBlocks(schedule, targetSlot);
    }

    /**
     * Student Congestion Avoidance: Ensures a class does not have:
     * 1. More than 3 classes of the same subject per day.
     * 2. More than 6 total periods on a single day.
     * 3. More than 3 consecutive periods in a row on a single day.
     */
    private boolean isClassCongested(String classId, Timeslot targetSlot, Subject subject) {
        Map<String, TimetableEntry> schedule = classSchedule.get(classId);
        if (schedule == null || schedule.isEmpty()) return false;

        // Constraint 1: Max 3 classes of the same subject per day
        int subjectDayCount = 0;
        for (TimetableEntry entry : schedule.values()) {
            Timeslot s = findTimeslotById(entry.getSlotId());
            if (s != null && s.getDay().equalsIgnoreCase(targetSlot.getDay())) {
                if (entry.getSubId().equalsIgnoreCase(subject.getSubId())) {
                    subjectDayCount++;
                }
            }
        }
        if (subjectDayCount >= 3) {
            return true;
        }

        // Constraint 2: Max 6 periods per day
        int dayCount = 0;
        for (String slotId : schedule.keySet()) {
            Timeslot s = findTimeslotById(slotId);
            if (s != null && s.getDay().equalsIgnoreCase(targetSlot.getDay())) {
                dayCount++;
            }
        }
        if (dayCount >= 6) {
            return true;
        }

        // Constraint 3: Max 3 consecutive periods in a row
        return hasConsecutiveBlocks(schedule, targetSlot);
    }

    /**
     * Checks if allocating a session in the targetSlot will result in > 3 consecutive periods in a day.
     */
    private boolean hasConsecutiveBlocks(Map<String, TimetableEntry> schedule, Timeslot targetSlot) {
        List<Timeslot> daySlots = new ArrayList<>();
        // Gather all timeslots scheduled on the same day for this schedule
        for (String slotId : schedule.keySet()) {
            Timeslot s = findTimeslotById(slotId);
            if (s != null && s.getDay().equalsIgnoreCase(targetSlot.getDay())) {
                daySlots.add(s);
            }
        }
        daySlots.add(targetSlot);

        // Sort day slots chronologically by parsing their slot ID or index
        daySlots.sort(Comparator.comparing(Timeslot::getSlotId));

        // Find consecutive run-length
        int maxConsecutive = 1;
        int currentConsecutive = 1;

        for (int i = 0; i < daySlots.size() - 1; i++) {
            Timeslot s1 = daySlots.get(i);
            Timeslot s2 = daySlots.get(i + 1);

            int id1 = Integer.parseInt(s1.getSlotId().replaceAll("\\D+", ""));
            int id2 = Integer.parseInt(s2.getSlotId().replaceAll("\\D+", ""));

            // If slot IDs are adjacent (e.g. SL01 and SL02), they are consecutive
            if (id2 - id1 == 1) {
                currentConsecutive++;
                if (currentConsecutive > maxConsecutive) {
                    maxConsecutive = currentConsecutive;
                }
            } else {
                currentConsecutive = 1;
            }
        }

        return maxConsecutive > 3; // Block if consecutive period length exceeds 3
    }

    private Timeslot findTimeslotById(String slotId) {
        for (Timeslot s : timeslots) {
            if (s.getSlotId().equalsIgnoreCase(slotId)) {
                return s;
            }
        }
        return null;
    }

    private int getPeriodIndex(String timeStr) {
        if (timeStr.contains("08:30") && timeStr.contains("09:30")) return 1;
        if (timeStr.contains("09:30") && timeStr.contains("10:30")) return 2;
        if (timeStr.contains("10:45") && timeStr.contains("11:45")) return 3;
        if (timeStr.contains("11:45") && timeStr.contains("12:45")) return 4;
        if (timeStr.contains("01:45") && timeStr.contains("02:45")) return 5;
        if (timeStr.contains("02:45") && timeStr.contains("03:45")) return 6;
        if (timeStr.contains("03:45") && timeStr.contains("04:45")) return 7;
        return -1; // Not an active period (breaks)
    }

    private boolean validateTeacherSchedules() {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (Map.Entry<String, Map<String, TimetableEntry>> teacherEntry : teacherSchedule.entrySet()) {
            Map<String, TimetableEntry> schedule = teacherEntry.getValue();
            if (schedule == null || schedule.isEmpty()) continue;

            for (String day : days) {
                List<Integer> activeScheduled = new ArrayList<>();
                for (TimetableEntry entry : schedule.values()) {
                    Timeslot s = findTimeslotById(entry.getSlotId());
                    if (s != null && s.getDay().equalsIgnoreCase(day)) {
                        int periodIdx = getPeriodIndex(s.getTime());
                        if (periodIdx != -1) {
                            activeScheduled.add(periodIdx);
                        }
                    }
                }

                if (activeScheduled.size() >= 2) {
                    Collections.sort(activeScheduled);
                    for (int i = 0; i < activeScheduled.size() - 1; i++) {
                        int p1 = activeScheduled.get(i);
                        int p2 = activeScheduled.get(i + 1);
                        if (p2 - p1 > 3) { // gap >= 3 active periods (more than 2 consecutive hours)
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean validateOptimalTimetable() {
        if (!validateTeacherSchedules()) {
            return false;
        }

        DatabaseManager db = DatabaseManager.getInstance();
        List<ClassGroup> classGroups = db.getClassGroups();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

        for (ClassGroup cg : classGroups) {
            String classId = cg.getClassId();
            Map<String, TimetableEntry> schedule = classSchedule.get(classId);
            if (schedule == null) continue;

            // Ensure Saturday has at least 2 active periods scheduled for this class
            int satCount = 0;
            for (TimetableEntry entry : schedule.values()) {
                Timeslot s = findTimeslotById(entry.getSlotId());
                if (s != null && s.getDay().equalsIgnoreCase("Saturday")) {
                    int periodIdx = getPeriodIndex(s.getTime());
                    if (periodIdx != -1) {
                        satCount++;
                    }
                }
            }
            if (satCount < 2) {
                return false;
            }

            for (String day : days) {
                List<Integer> activeScheduled = new ArrayList<>();
                for (TimetableEntry entry : schedule.values()) {
                    Timeslot s = findTimeslotById(entry.getSlotId());
                    if (s != null && s.getDay().equalsIgnoreCase(day)) {
                        int periodIdx = getPeriodIndex(s.getTime());
                        if (periodIdx != -1) {
                            activeScheduled.add(periodIdx);
                        }
                    }
                }

                // Check gaps
                if (!activeScheduled.isEmpty()) {
                    int min = Collections.min(activeScheduled);
                    int max = Collections.max(activeScheduled);
                    int gaps = 0;
                    for (int p = min + 1; p < max; p++) {
                        if (!activeScheduled.contains(p)) {
                            gaps++;
                        }
                    }
                    if (gaps > 1) {
                        return false; // More than 1 gap hour daily is forbidden
                    }

                    // Check weekday minimum working hour (must extend till Period 5 or later)
                    if (!day.equalsIgnoreCase("Saturday")) {
                        if (max < 5) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean validateClassSchedule(String classId) {
        Map<String, TimetableEntry> schedule = classSchedule.get(classId);
        if (schedule == null || schedule.isEmpty()) return true;

        // Ensure Saturday has at least 2 active periods scheduled for this class
        int satCount = 0;
        for (TimetableEntry entry : schedule.values()) {
            Timeslot s = findTimeslotById(entry.getSlotId());
            if (s != null && s.getDay().equalsIgnoreCase("Saturday")) {
                int periodIdx = getPeriodIndex(s.getTime());
                if (periodIdx != -1) {
                    satCount++;
                }
            }
        }
        if (satCount < 2) {
            return false;
        }

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

        for (String day : days) {
            List<Integer> activeScheduled = new ArrayList<>();
            for (TimetableEntry entry : schedule.values()) {
                Timeslot s = findTimeslotById(entry.getSlotId());
                if (s != null && s.getDay().equalsIgnoreCase(day)) {
                    int periodIdx = getPeriodIndex(s.getTime());
                    if (periodIdx != -1) {
                        activeScheduled.add(periodIdx);
                    }
                }
            }

            if (!activeScheduled.isEmpty()) {
                int min = Collections.min(activeScheduled);
                int max = Collections.max(activeScheduled);
                int gaps = 0;
                for (int p = min + 1; p < max; p++) {
                    if (!activeScheduled.contains(p)) {
                        gaps++;
                    }
                }
                if (gaps > 1) {
                    return false; // Forbidden: More than 1 gap hour daily
                }

                // Check weekday minimum working hour (must extend till Period 5 or later)
                if (!day.equalsIgnoreCase("Saturday")) {
                    if (max < 5) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}

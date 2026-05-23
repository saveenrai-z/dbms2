package com.timetable;
import com.timetable.algorithm.TimetableGenerator;

public class TestGenerator {
    public static void main(String[] args) {
        Main.loadEnv();
        System.out.println("Starting timetable generation...");
        TimetableGenerator generator = new TimetableGenerator();
        boolean success = generator.generate();
        System.out.println("Success: " + success);
        System.out.println("Time: " + generator.getExecutionTimeMs() + "ms");
        System.out.println("Backtracks: " + generator.getBacktrackCount());
    }
}

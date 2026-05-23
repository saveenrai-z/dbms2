-- =========================================================================
-- DBMS PROJECT: AUTOMATED TIME TABLE GENERATION SYSTEM
-- SQL DDL SCHEMA, MOCK SEED DATA, AND RELATION TRRIGERS
-- =========================================================================
-- Designed for: PostgreSQL / PL-pgSQL Dialect
-- Target Database: Relational RDBMS Mapping
-- =========================================================================

-- -----------------------------------------------------
-- 1. CLEAN EXISTING SCHEMAS (DROP TABLE IF EXISTS)
-- -----------------------------------------------------
DROP TABLE IF EXISTS Timetable CASCADE;
DROP TABLE IF EXISTS Subject CASCADE;
DROP TABLE IF EXISTS Classroom CASCADE;
DROP TABLE IF EXISTS ClassGroup CASCADE;
DROP TABLE IF EXISTS Teacher CASCADE;
DROP TABLE IF EXISTS Timeslot CASCADE;
DROP TABLE IF EXISTS Audit_Logs CASCADE;

-- -----------------------------------------------------
-- 2. CREATE ENTITY RELATION TABLES
-- -----------------------------------------------------

-- Table A: Timeslots
CREATE TABLE Timeslot (
    Slot_ID VARCHAR(10) PRIMARY KEY,
    Day VARCHAR(20) NOT NULL,
    Time_Interval VARCHAR(50) NOT NULL,
    Is_Lunch_Break BOOLEAN DEFAULT FALSE
);

-- Table B: Teachers (Faculty Staff)
CREATE TABLE Teacher (
    Teacher_ID VARCHAR(10) PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Department VARCHAR(50) NOT NULL
);

-- Table C: Class Groups (SemSec Roster)
CREATE TABLE ClassGroup (
    Class_ID VARCHAR(10) PRIMARY KEY,
    Class_Name VARCHAR(100) NOT NULL,
    Semester INT NOT NULL
);

-- Table D: Classrooms (Locations)
CREATE TABLE Classroom (
    Room_ID VARCHAR(10) PRIMARY KEY,
    Room_No VARCHAR(20) NOT NULL UNIQUE,
    Department VARCHAR(50) NOT NULL
);

-- Table E: Subjects (Course Curriculums)
CREATE TABLE Subject (
    Sub_ID VARCHAR(10) PRIMARY KEY,
    Sub_Name VARCHAR(100) NOT NULL,
    Hours INT NOT NULL CHECK (Hours > 0),
    Class_ID VARCHAR(10) REFERENCES ClassGroup(Class_ID) ON DELETE CASCADE,
    Teacher_ID VARCHAR(10) REFERENCES Teacher(Teacher_ID) ON DELETE CASCADE
);

-- Table F: Timetable (Junction Bridge table enforcing FK integrity)
CREATE TABLE Timetable (
    TT_ID VARCHAR(10) PRIMARY KEY,
    Teacher_ID VARCHAR(10) REFERENCES Teacher(Teacher_ID) ON DELETE CASCADE,
    Sub_ID VARCHAR(10) REFERENCES Subject(Sub_ID) ON DELETE CASCADE,
    Class_ID VARCHAR(10) REFERENCES ClassGroup(Class_ID) ON DELETE CASCADE,
    Room_ID VARCHAR(10) REFERENCES Classroom(Room_ID) ON DELETE CASCADE,
    Slot_ID VARCHAR(10) REFERENCES Timeslot(Slot_ID) ON DELETE CASCADE,
    CONSTRAINT unique_timetable_slot UNIQUE (Class_ID, Slot_ID)
);

-- Table G: Audit Logs (Central Auditing Hub)
CREATE TABLE Audit_Logs (
    Log_ID SERIAL PRIMARY KEY,
    Action_Type VARCHAR(20) NOT NULL,       -- INSERT, UPDATE, DELETE
    Table_Name VARCHAR(30) NOT NULL,        -- Teacher, Subject, Timetable, etc.
    Description TEXT NOT NULL,              -- Full details of the changes
    Fired_At TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- -----------------------------------------------------
-- 3. INITIAL MOCK DATA SEED
-- -----------------------------------------------------

-- Seed Timeslots
INSERT INTO Timeslot (Slot_ID, Day, Time_Interval, Is_Lunch_Break) VALUES 
('SL01', 'Monday', '08:30 AM - 09:30 AM', FALSE),
('SL02', 'Monday', '09:30 AM - 10:30 AM', FALSE),
('SL03', 'Monday', '10:30 AM - 10:45 AM', TRUE), -- Tea Break
('SL04', 'Monday', '10:45 AM - 11:45 AM', FALSE),
('SL05', 'Monday', '11:45 AM - 12:45 PM', FALSE),
('SL06', 'Monday', '12:45 PM - 01:45 PM', TRUE); -- Lunch Break

-- Seed Faculty
INSERT INTO Teacher (Teacher_ID, Name, Department) VALUES 
('AI101', 'Dr. Jayashree T R', 'cse-aiml'),
('AI103', 'Mrs. Sangeetha M S', 'cse-aiml'),
('AI104', 'Ms. Deeksha J S', 'cse-aiml');

-- Seed Classes
INSERT INTO ClassGroup (Class_ID, Class_Name, Semester) VALUES 
('4A', 'cse-aiml', 4),
('6A', 'cse-aiml', 6);

-- Seed Classrooms
INSERT INTO Classroom (Room_ID, Room_No, Department) VALUES 
('4A', '404', 'cse-aiml'),
('6A', '312', 'cse-aiml');

-- Seed Subjects (Seed DAA, DBMS, and OS with +1 hour updates from PDF)
INSERT INTO Subject (Sub_ID, Sub_Name, Hours, Class_ID, Teacher_ID) VALUES 
('BAI401G', 'Design and Analysis of Algorithms', 4, '4A', 'AI101'),
('BAI402G', 'Database Management System', 4, '4A', 'AI103'),
('BAI403T', 'Principles of Operating Systems', 3, '4A', 'AI104');


-- -----------------------------------------------------
-- 4. DATABASE TRIGGER DESIGNS (ACTIVE DBMS INTELLIGENCE)
-- -----------------------------------------------------

-- =====================================================
-- TRIGGER 1: CONFLICT PREVENTION (BEFORE TIMETABLE INSERT)
-- =====================================================
-- Description: Ensures that before any schedule cell is occupied, the RDBMS checks for:
--              1. Teacher Busy: Faculty member cannot teach two courses simultaneously.
--              2. Room Occupied: Classroom cannot hold two groups simultaneously.
--              3. Lunch Break constraint: No lectures during lunch/tea breaks.
-- =====================================================

CREATE OR REPLACE FUNCTION check_timetable_conflicts()
RETURNS TRIGGER AS $$
DECLARE
    slot_desc VARCHAR(100);
    teacher_name VARCHAR(100);
    room_no VARCHAR(20);
    is_break BOOLEAN;
BEGIN
    -- A. Validate Timeslot is not a Lunch/Tea Break
    SELECT Is_Lunch_Break, Day || ' ' || Time_Interval INTO is_break, slot_desc
    FROM Timeslot WHERE Slot_ID = NEW.Slot_ID;

    IF is_break THEN
        RAISE EXCEPTION 'SQL CONSTRAINT VIOLATION: Cannot allocate lectures in Timeslot % because it is a fixed break interval.', slot_desc;
    END IF;

    -- B. Verify Teacher Availability (Teacher Collision Prevention)
    IF EXISTS (
        SELECT 1 FROM Timetable 
        WHERE Slot_ID = NEW.Slot_ID AND Teacher_ID = NEW.Teacher_ID AND TT_ID <> NEW.TT_ID
    ) THEN
        SELECT Name INTO teacher_name FROM Teacher WHERE Teacher_ID = NEW.Teacher_ID;
        RAISE EXCEPTION 'SQL CLASH DETECTED: Faculty member % (%) is already scheduled in another classroom in Timeslot %.', 
            teacher_name, NEW.Teacher_ID, slot_desc;
    END IF;

    -- C. Verify Room Availability (Room Collision Prevention)
    IF EXISTS (
        SELECT 1 FROM Timetable 
        WHERE Slot_ID = NEW.Slot_ID AND Room_ID = NEW.Room_ID AND TT_ID <> NEW.TT_ID
    ) THEN
        SELECT Room_No INTO room_no FROM Classroom WHERE Room_ID = NEW.Room_ID;
        RAISE EXCEPTION 'SQL CLASH DETECTED: Classroom Room % is already allocated to another SemSec group in Timeslot %.', 
            room_no, slot_desc;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Bind Trigger 1
CREATE TRIGGER trg_before_timetable_insert
BEFORE INSERT OR UPDATE ON Timetable
FOR EACH ROW
EXECUTE FUNCTION check_timetable_conflicts();


-- =====================================================
-- TRIGGER 2: AUDITING LOGS (AFTER FACULTY OR SUBJECT INSERT/UPDATE/DELETE)
-- =====================================================
-- Description: Automatically logs all administrative CRUD operations to the central
--              Audit_Logs table, creating a solid audit trail for DBMS reporting.
-- =====================================================

-- Trigger function for Faculty Changes
CREATE OR REPLACE FUNCTION audit_teacher_actions()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO Audit_Logs (Action_Type, Table_Name, Description)
        VALUES ('INSERT', 'Teacher', 'Registered new faculty member: ' || NEW.Name || ' (' || NEW.Teacher_ID || ') for ' || UPPER(NEW.Department) || ' department.');
        RETURN NEW;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO Audit_Logs (Action_Type, Table_Name, Description)
        VALUES ('UPDATE', 'Teacher', 'Updated faculty credentials for ID: ' || NEW.Teacher_ID || '. Old Name: ' || OLD.Name || ' -> New Name: ' || NEW.Name);
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        INSERT INTO Audit_Logs (Action_Type, Table_Name, Description)
        VALUES ('DELETE', 'Teacher', 'Wiped teacher account: ' || OLD.Name || ' (' || OLD.Teacher_ID || ') from registries.');
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Bind Trigger 2
CREATE TRIGGER trg_after_teacher_crud
AFTER INSERT OR UPDATE OR DELETE ON Teacher
FOR EACH ROW
EXECUTE FUNCTION audit_teacher_actions();


-- =====================================================
-- TRIGGER 3: AUDITING LOGS (AFTER SUBJECT INSERT/UPDATE/DELETE)
-- =====================================================
CREATE OR REPLACE FUNCTION audit_subject_actions()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO Audit_Logs (Action_Type, Table_Name, Description)
        VALUES ('INSERT', 'Subject', 'Added new course: ' || NEW.Sub_Name || ' (' || NEW.Sub_ID || ') for Class ' || NEW.Class_ID);
        RETURN NEW;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO Audit_Logs (Action_Type, Table_Name, Description)
        VALUES ('UPDATE', 'Subject', 'Modified course details for ID: ' || NEW.Sub_ID || '. Old Name: ' || OLD.Sub_Name || ' -> New Name: ' || NEW.Sub_Name);
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        INSERT INTO Audit_Logs (Action_Type, Table_Name, Description)
        VALUES ('DELETE', 'Subject', 'Removed course: ' || OLD.Sub_Name || ' (' || OLD.Sub_ID || ') from Class ' || OLD.Class_ID);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_after_subject_crud
AFTER INSERT OR UPDATE OR DELETE ON Subject
FOR EACH ROW
EXECUTE FUNCTION audit_subject_actions();

-- =====================================================
-- TRIGGER 4: AUDITING LOGS (AFTER TIMETABLE INSERT/DELETE)
-- =====================================================
CREATE OR REPLACE FUNCTION audit_timetable_actions()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO Audit_Logs (Action_Type, Table_Name, Description)
        VALUES ('INSERT', 'Timetable', 'Allocated Class ' || NEW.Class_ID || ' to Slot ' || NEW.Slot_ID || ' in Room ' || NEW.Room_ID || ' for Subject ' || NEW.Sub_ID);
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        INSERT INTO Audit_Logs (Action_Type, Table_Name, Description)
        VALUES ('DELETE', 'Timetable', 'Cancelled lecture allocation for Class ' || OLD.Class_ID || ' in Slot ' || OLD.Slot_ID);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_after_timetable_crud
AFTER INSERT OR DELETE ON Timetable
FOR EACH ROW
EXECUTE FUNCTION audit_timetable_actions();

-- =====================================================
-- 5. VERIFICATION AND SEED TEST RUNS
-- =====================================================

-- Test Trigger 2: Insert new faculty members and check logs
INSERT INTO Teacher (Teacher_ID, Name, Department) VALUES ('AI105', 'Mrs. Shruthi Vishwajeeth', 'cse-aiml');
INSERT INTO Teacher (Teacher_ID, Name, Department) VALUES ('AI106', 'Mrs. Suchetha Sheka', 'cse-aiml');

-- Test Trigger 3: Insert new subject and check logs
INSERT INTO Subject (Sub_ID, Sub_Name, Hours, Class_ID, Teacher_ID) VALUES ('BAI405L', 'Artificial Intelligence Laboratory', 2, '4A', 'AI104');

-- Display Audit Logs to verify Triggers fired successfully
SELECT * FROM Audit_Logs;

-- Test Trigger 1 & 4: Successfully allocate a class
INSERT INTO Timetable (TT_ID, Teacher_ID, Sub_ID, Class_ID, Room_ID, Slot_ID)
VALUES ('TT0001', 'AI101', 'BAI401G', '4A', '4A', 'SL01');

-- Test Trigger 1 (Collision Exception): Attempt to allocate another class in the occupied Room 4A in the same Timeslot SL01
-- THIS INSERT WILL FAIL AND ROLLBACK AUTOMATICALLY DUE TO TRG_BEFORE_TIMETABLE_INSERT!
-- INSERT INTO Timetable (TT_ID, Teacher_ID, Sub_ID, Class_ID, Room_ID, Slot_ID)
-- VALUES ('TT0002', 'AI103', 'BAI402G', '6A', '4A', 'SL01');

-- Display Audit Logs again to verify Timetable Allocation Log
SELECT * FROM Audit_Logs;

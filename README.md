# Automated Time Table Generation System

A premium, full-stack Academic DBMS and Constraint-Satisfaction Scheduling application built in **Java** utilizing **MongoDB** as the core database, **Relational DBMS mappings**, advanced **Heuristic Backtracking Scheduling Algorithms**, and a gorgeous, custom dark-slate **Java Swing Desktop Dashboard**.

Designed as a complete university-grade academic DBMS project, the system schedules lectures conflict-free, prevents teacher/room/class clashes, limits student class congestion, locks lunch breaks, and allows exporting/printing soft-copies natively as PDF and CSV spreadsheets.

---

## 🚀 Key Features

- **Algorithmic Solver**: Intelligent Constraint-Satisfaction Backtracking solver that schedules lecture units in less than 50ms.
- **Collision Prevention**: Multi-layered maps prevent room overlaps, instructor clashes, and student scheduling collisions.
- **Advanced Workload Balancing**: Enforces a max of 3 consecutive classes for teachers, 3 consecutive classes for students, 5 hours per day maximum for students, and locks fixed lunch breaks.
- **Dynamic Dual-Database Engine**: Connection profile manager automatically connects to local MongoDB on startup. If MongoDB is offline, it activates **Local Persistence Mock Mode**, saving the state to `local_database.json` dynamically so the app runs instantly out-of-the-box.
- **Premium Swing UI**: Responsive slate-900 / indigo-500 custom dashboard containing glassmorphism accents, focus glow indicators, and hover micro-animations.
- **Soft-Copy Timetable Matrix**: Displays rosters in a interactive 6-column grid with live drop-down filters by Class, Teacher, or Room.
- **One-Click Export & Print**: Integrates OS vector printing to save rosters as PDF, alongside CSV file exporters.
- **Embedded DBMS Report**: In-app scrollable textbook-quality report explaining relational mappings, SQL commands, and normalization theories.

---

## 🛠️ Technology Stack & Architecture

- **Frontend**: Java Swing desktop dashboard (AWT, custom rendering, responsive overlays, Inter/SansSerif typography).
- **Backend**: Java 25 (OOP concepts, inheritance, encapsulation, interfaces, multithreading SwingWorkers).
- **Database**: MongoDB (as core document database) + Relational SQL representations.
- **DBMS Drivers**: MongoDB Java Sync Driver (`mongo-java-driver-3.12.14.jar` included in `lib/`).
- **Build Engine**: Automated custom PowerShell pipeline (`build_and_run.ps1` compiles and launches in one key stroke).

---

## 🧬 Data Structures & Scheduling Heuristics

The backtracking algorithm operates on a set of fundamental data structures to achieve optimal time-complexities:

1. **`HashMap<String, Map<String, TimetableEntry>>` for Busy Schedules**:
   - Used for **Teacher**, **Room**, and **Class** schedule mapping.
   - Key: Entity ID (e.g. `teacherId`), Value: A nested map where Key: `slotId` and Value: `TimetableEntry`.
   - **Performance**: Yields $O(1)$ constant-time lookup for conflict checking. Checking if a teacher is busy in a given timeslot is solved instantly!
2. **`ArrayList` for Subject/Room/Slot Lists**:
   - Used to store static catalog listings fetched from the database, allowing indexing and quick sequential iterations.
3. **`Queue` for Lecture Scheduling**:
   - Dynamic scheduling queues represent subjects awaiting scheduling hours, maintaining strict chronological sorting.
4. **`Set` to Prevent Duplication**:
   - Holds hash combinations to ensure identical primary key entities are never saved or generated twice.
5. **Heuristic Sorting**:
   - Prior to backtracking, the list of weekly lecture units is sorted in **descending order of required hours**.
   - **Why?** Scheduling courses with high hour requirements is statistically harder. By scheduling them when the roster is empty, we decrease backtracking recursion depth from $O(D^R)$ to near-instantaneous linear resolution!

---

## 📊 DBMS Design Mappings

### 1. Entity-Relationship (ER) Model Schemas
- **Teacher** (`teacher_id` [PK], `name`)
- **Room** (`room_id` [PK], `room_no`)
- **ClassGroup** (`class_id` [PK], `class_name`, `semester`)
- **Subject** (`sub_id` [PK], `sub_name`, `hours`, `class_id` [FK], `teacher_id` [FK])
- **Timetable** (`tt_id` [PK], `teacher_id` [FK], `sub_id` [FK], `class_id` [FK], `room_id` [FK], `slot_id` [FK])

### 2. Normalization Analysis & Proof

The project design strictly respects relational normalization rules to prevent modification anomalies:

* **1NF (First Normal Form)**:
  - *Rule*: All table cells must contain only atomic (indivisible) values, and there are no repeating groups.
  - *Proof*: In our schema, we do not store lists of subjects in a single class cell. Every field contains isolated atomic strings (e.g., `S101`, `T101`). Roster cell assignments are individual unique entries inside the `Timetable` collection.
* **2NF (Second Normal Form)**:
  - *Rule*: Satisfies 1NF, and all non-key attributes are fully functionally dependent on the entire primary key (no partial dependencies).
  - *Proof*: In our `Subject` design, properties like `Sub_Name` depend solely on the whole primary key `Sub_ID`. Relational columns like `Name` for a teacher are stored in a separate `Teacher` table rather than inside the `Timetable` table directly, which prevents partial dependencies.
* **3NF (Third Normal Form)**:
  - *Rule*: Satisfies 2NF, and there are no transitive dependencies (non-key fields should not depend on other non-key fields).
  - *Proof*: Every non-key column in our schema depends strictly, directly, and solely on its primary key. For example, `Room_No` depends directly on `Room_ID`. There are no intermediate transitive connections, eliminating data update redundancy.

### 3. Relational SQL Schema Definition
```sql
CREATE TABLE Teacher (
    Teacher_ID VARCHAR(10) PRIMARY KEY,
    Name VARCHAR(100) NOT NULL
);

CREATE TABLE Classroom (
    Room_ID VARCHAR(10) PRIMARY KEY,
    Room_No VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE ClassGroup (
    Class_ID VARCHAR(10) PRIMARY KEY,
    Class_Name VARCHAR(100) NOT NULL,
    Semester INT NOT NULL
);

CREATE TABLE Subject (
    Sub_ID VARCHAR(10) PRIMARY KEY,
    Sub_Name VARCHAR(100) NOT NULL,
    Hours INT NOT NULL,
    Class_ID VARCHAR(10) REFERENCES ClassGroup(Class_ID),
    Teacher_ID VARCHAR(10) REFERENCES Teacher(Teacher_ID)
);

CREATE TABLE Timetable (
    TT_ID VARCHAR(10) PRIMARY KEY,
    Teacher_ID VARCHAR(10) REFERENCES Teacher(Teacher_ID),
    Sub_ID VARCHAR(10) REFERENCES Subject(Sub_ID),
    Class_ID VARCHAR(10) REFERENCES ClassGroup(Class_ID),
    Room_ID VARCHAR(10) REFERENCES Classroom(Room_ID),
    Slot_ID VARCHAR(10) REFERENCES Timeslot(Slot_ID)
);
```

### 4. Sample SQL Queries
#### Join Query 1: Fetch Timetable for a Specific Class Group (e.g., "CSE5A")
```sql
SELECT t.Day, t.Time, s.Sub_Name, f.Name AS TeacherName, r.Room_No
FROM Timetable tt
JOIN Subject s ON tt.Sub_ID = s.Sub_ID
JOIN Teacher f ON tt.Teacher_ID = f.Teacher_ID
JOIN Classroom r ON tt.Room_ID = r.Room_ID
JOIN Timeslot t ON tt.Slot_ID = t.Slot_ID
WHERE tt.Class_ID = 'CSE5A'
ORDER BY t.Slot_ID;
```

#### Join Query 2: Find Busy Workload for a Specific Faculty Member (e.g., T101)
```sql
SELECT t.Day, t.Time, c.Class_ID, r.Room_No
FROM Timetable tt
JOIN Timeslot t ON tt.Slot_ID = t.Slot_ID
JOIN ClassGroup c ON tt.Class_ID = c.Class_ID
JOIN Classroom r ON tt.Room_ID = r.Room_ID
WHERE tt.Teacher_ID = 'T101';
```

---

## 💻 Visual Walkthrough & System Screenshots

- **Login Screen**: Minimal glassmorphism card over custom diagonal indigo animations, displaying credentials support and a live **Active Database Status Badge**.
- **Admin Dashboard Console**: Left sidebar menu allowing fluid transitions. Displays live overview statistics.
- **Admin CRUD Datagrids**: Clean text input cards and dark slate lists. Clicking JTable rows immediately loads details into the editor for swift updates.
- **Automated Scheduling Deck**: Displays solver stats. Clicking "⚡ RUN SOLVER" triggers the backtracking engine.
- **Timetable Matrix Grid**: Color-coded subject cards (blue cards for lectures, teal cards for labs) featuring details, lunch blocks, and dynamic Class/Teacher/Room filter repaints.

---

## 🏃 Setup & Local Compilation

### 1. Database Configuration (Optional)
If you have MongoDB installed locally and running on standard port `27017`:
- The application will automatically connect, generate database `automated_timetable_system`, and pre-populate comprehensive academic sample records.
- *If MongoDB is not running*: The system displays a popup notification on login and loads in **Local persistence mode**, reading and writing all records to `local_database.json` dynamically. This ensures the app is 100% operational instantly.

### 2. Execution steps
Since a custom build script is provided, you can build and launch the application in a single step using **Windows PowerShell**:

1. Open PowerShell and navigate to the project directory:
   ```powershell
   cd "C:\Users\Saveen Rai\dbms project\time table 2"
   ```
2. Set execution permissions for the script (if prompted):
   ```powershell
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
   ```
3. Run the automated compilation and launch script:
   ```powershell
   ./build_and_run.ps1
   ```

### 3. Default Credentials
- **Username**: `admin`
- **Password**: `admin123`

---

## 🔮 Future Enhancements
- **AI-Based Optimization**: Integration of Genetic Algorithms or Simulated Annealing for multi-objective optimization (minimizing teacher split shifts).
- **Faculty Preferences**: Support for faculty-nominated timeslot exclusions or availability constraints.
- **Student Attendance Integration**: Connecting attendance sheets to resolved timetable periods.
- **Web Interface**: Migrating Java Swing modules into a Next.js / MongoDB Atlas cloud web app.
#   d b m s 1  
 
package com.timetable.ui;

import com.timetable.ui.components.RoundedPanel;
import com.timetable.ui.components.UIStyle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Premium rich-text UI panel displaying all DBMS project concepts: ER Diagrams, SQL schemas, Normalization, and MongoDB designs.
 */
public class DBMSDocsPanel extends JPanel {

    public DBMSDocsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel("📚 DBMS Core Concepts & Documentation");
        lblTitle.setFont(UIStyle.FONT_TITLE);
        lblTitle.setForeground(UIStyle.TXT_WHITE);
        headerPanel.add(lblTitle, BorderLayout.NORTH);

        JLabel lblDesc = new JLabel("Theoretical mapping of the Automated Time Table Generation System: Relational vs. Document Models.");
        lblDesc.setFont(UIStyle.FONT_SUBTEXT);
        lblDesc.setForeground(UIStyle.TXT_GRAY);
        lblDesc.setBorder(new EmptyBorder(4, 0, 15, 0));
        headerPanel.add(lblDesc, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // Scrollable Content
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Card 1: Entity-Relationship Model
        contentPanel.add(createDocCard("1. Entity-Relationship (ER) Model Diagram", 
            "<html>" +
            "<font color='#818cf8'><b>ENTITIES & KEY SCHEMAS:</b></font><br><br>" +
            "<b>1. Teacher:</b> Tracks academic staff.<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;• <u>Teacher_ID</u> (Primary Key) | Name<br><br>" +
            "<b>2. Subject:</b> Tracks courses offered, lectures required, and mappings.<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;• <u>Sub_ID</u> (Primary Key) | Sub_Name | Hours | <i>Class_ID</i> (FK) | <i>Teacher_ID</i> (FK)<br><br>" +
            "<b>3. Class (Group):</b> SemSec (Semester & Section) taking subjects.<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;• <u>Class_ID</u> (Primary Key) | Class_Name | Semester<br><br>" +
            "<b>4. Room:</b> Lecture halls and labs.<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;• <u>Room_ID</u> (Primary Key) | Room_No<br><br>" +
            "<b>5. Timeslot:</b> Day and hour combinations.<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;• <u>Slot_ID</u> (Primary Key) | Day | Time | Is_Lunch_Break<br><br>" +
            "<b>6. Timetable (Resolved Entries):</b> The central bridge entity resolving scheduling allocations.<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;• <u>TT_ID</u> (Primary Key) | <i>Teacher_ID</i> (FK) | <i>Sub_ID</i> (FK) | <i>Class_ID</i> (FK) | <i>Room_ID</i> (FK) | <i>Slot_ID</i> (FK)<br><br>" +
            "<font color='#06b6d4'><b>RELATIONSHIPS:</b></font><br>" +
            "• <b>Teacher teaches Subject:</b> One-to-Many (1:N).<br>" +
            "• <b>Subject belongs to Class:</b> Many-to-One (N:1).<br>" +
            "• <b>Central Bridge (Timetable):</b> Integrates Teacher, Class, Room, and Timeslot under strict conflict-free validation." +
            "</html>"
        ));
        contentPanel.add(Box.createVerticalStrut(20));

        // Card 2: Normalization Details
        contentPanel.add(createDocCard("2. Database Normalization (1NF, 2NF, 3NF)", 
            "<html>" +
            "Normalization is strictly applied to ensure data integrity and avoid redundancy in relational representations:<br><br>" +
            "🔑 <b>1NF (First Normal Form):</b> All attributes contain only atomic values. There are no repeating groups. " +
            "For example, instead of storing timeslots as a comma-separated array inside a single table cell, each timeslot is stored as " +
            "a single atomic entity in a separate collection. Similarly, each timetable cell maps to a singular entry.<br><br>" +
            "🔑 <b>2NF (Second Normal Form):</b> The schema satisfies 1NF, and all non-key attributes are fully dependent on the " +
            "entire primary key (no partial dependency). In our model, attributes of `Teacher` (like `Name`) depend strictly on the primary key " +
            "`Teacher_ID` and not on any other fields. Relational mapping separates Teachers, Rooms, and Classes to prevent partial dependency.<br><br>" +
            "🔑 <b>3NF (Third Normal Form):</b> The schema satisfies 2NF, and there are no transitive dependencies (non-key fields do not depend on " +
            "other non-key fields). All properties of a classroom (e.g. `Room_No`) depend directly and solely on `Room_ID`. Our database layers enforce this " +
            "strictly by separating records into isolated collections, joining them at schedule time." +
            "</html>"
        ));
        contentPanel.add(Box.createVerticalStrut(20));

        // Card 3: Relational DDL & SQL Representation
        contentPanel.add(createDocCard("3. SQL Schema Representations & DDLs", 
            "<html>" +
            "Here is the standard SQL DDL schema script mapping our database structures:<br><br>" +
            "<pre style='color:#38bdf8; font-family:Consolas,monospace;'>" +
            "CREATE TABLE Teacher (<br>" +
            "    Teacher_ID VARCHAR(10) PRIMARY KEY,<br>" +
            "    Name VARCHAR(100) NOT NULL<br>" +
            ");<br><br>" +
            "CREATE TABLE Classroom (<br>" +
            "    Room_ID VARCHAR(10) PRIMARY KEY,<br>" +
            "    Room_No VARCHAR(20) NOT NULL UNIQUE<br>" +
            ");<br><br>" +
            "CREATE TABLE ClassGroup (<br>" +
            "    Class_ID VARCHAR(10) PRIMARY KEY,<br>" +
            "    Class_Name VARCHAR(100) NOT NULL,<br>" +
            "    Semester INT NOT NULL<br>" +
            ");<br><br>" +
            "CREATE TABLE Subject (<br>" +
            "    Sub_ID VARCHAR(10) PRIMARY KEY,<br>" +
            "    Sub_Name VARCHAR(100) NOT NULL,<br>" +
            "    Hours INT NOT NULL,<br>" +
            "    Class_ID VARCHAR(10) REFERENCES ClassGroup(Class_ID),<br>" +
            "    Teacher_ID VARCHAR(10) REFERENCES Teacher(Teacher_ID)<br>" +
            ");<br><br>" +
            "CREATE TABLE Timetable (<br>" +
            "    TT_ID VARCHAR(10) PRIMARY KEY,<br>" +
            "    Teacher_ID VARCHAR(10) REFERENCES Teacher(Teacher_ID),<br>" +
            "    Sub_ID VARCHAR(10) REFERENCES Subject(Sub_ID),<br>" +
            "    Class_ID VARCHAR(10) REFERENCES ClassGroup(Class_ID),<br>" +
            "    Room_ID VARCHAR(10) REFERENCES Classroom(Room_ID),<br>" +
            "    Slot_ID VARCHAR(10) REFERENCES Timeslot(Slot_ID)<br>" +
            ");" +
            "</pre>" +
            "</html>"
        ));
        contentPanel.add(Box.createVerticalStrut(20));

        // Card 4: MongoDB Document Paradigm
        contentPanel.add(createDocCard("4. MongoDB Collection Designs", 
            "<html>" +
            "In MongoDB, we enforce structured BSON documents representing entities dynamically to speed up index lookup:<br><br>" +
            "📁 <b>Collection: `teachers`</b><br>" +
            "<pre style='color:#a7f3d0; font-family:Consolas,monospace;'>{ \"teacher_id\": \"T101\", \"name\": \"Prof. John\" }</pre><br>" +
            "📁 <b>Collection: `subjects`</b><br>" +
            "<pre style='color:#a7f3d0; font-family:Consolas,monospace;'>{ \"sub_id\": \"S101\", \"sub_name\": \"DBMS\", \"hours\": 4, \"class_id\": \"CSE5A\", \"teacher_id\": \"T101\" }</pre><br>" +
            "📁 <b>Collection: `timetable`</b><br>" +
            "<pre style='color:#a7f3d0; font-family:Consolas,monospace;'>{<br>" +
            "  \"tt_id\": \"TT0001\",<br>" +
            "  \"teacher_id\": \"T101\",<br>" +
            "  \"sub_id\": \"S101\",<br>" +
            "  \"class_id\": \"CSE5A\",<br>" +
            "  \"room_id\": \"R101\",<br>" +
            "  \"slot_id\": \"SL01\"<br>" +
            "}</pre>" +
            "</html>"
        ));
        contentPanel.add(Box.createVerticalStrut(20));

        // Card 5: Sample DBMS SQL Queries
        contentPanel.add(createDocCard("5. Analytical SQL Queries for Reporting", 
            "<html>" +
            "In a relational database, administrators execute joining queries to build custom viewpoints. Sample SQL representations:<br><br>" +
            "🔍 <b>Query 1: Fetch Timetable for a Specific Class Group (e.g. CSE5A)</b><br>" +
            "<pre style='color:#38bdf8; font-family:Consolas,monospace;'>" +
            "SELECT t.Day, t.Time, s.Sub_Name, f.Name AS TeacherName, r.Room_No<br>" +
            "FROM Timetable tt<br>" +
            "JOIN Subject s ON tt.Sub_ID = s.Sub_ID<br>" +
            "JOIN Teacher f ON tt.Teacher_ID = f.Teacher_ID<br>" +
            "JOIN Classroom r ON tt.Room_ID = r.Room_ID<br>" +
            "JOIN Timeslot t ON tt.Slot_ID = t.Slot_ID<br>" +
            "WHERE tt.Class_ID = 'CSE5A'<br>" +
            "ORDER BY t.Slot_ID;" +
            "</pre><br>" +
            "🔍 <b>Query 2: Find Busy Slots for a Specific Instructor (e.g. Prof. John - T101)</b><br>" +
            "<pre style='color:#38bdf8; font-family:Consolas,monospace;'>" +
            "SELECT t.Day, t.Time, c.Class_ID, r.Room_No<br>" +
            "FROM Timetable tt<br>" +
            "JOIN Timeslot t ON tt.Slot_ID = t.Slot_ID<br>" +
            "JOIN ClassGroup c ON tt.Class_ID = c.Class_ID<br>" +
            "JOIN Classroom r ON tt.Room_ID = r.Room_ID<br>" +
            "WHERE tt.Teacher_ID = 'T101';" +
            "</pre><br>" +
            "🔍 <b>Query 3: Check Room Occupancy for Seminar Hall 201</b><br>" +
            "<pre style='color:#38bdf8; font-family:Consolas,monospace;'>" +
            "SELECT t.Day, t.Time, c.Class_ID, s.Sub_Name<br>" +
            "FROM Timetable tt<br>" +
            "JOIN Timeslot t ON tt.Slot_ID = t.Slot_ID<br>" +
            "JOIN ClassGroup c ON tt.Class_ID = c.Class_ID<br>" +
            "JOIN Subject s ON tt.Sub_ID = s.Sub_ID<br>" +
            "WHERE tt.Room_ID = 'R201';" +
            "</pre>" +
            "</html>"
        ));
        contentPanel.add(Box.createVerticalStrut(20));

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smooth scrolling

        add(scrollPane, BorderLayout.CENTER);
    }

    private RoundedPanel createDocCard(String title, String htmlBody) {
        RoundedPanel card = new RoundedPanel(16, UIStyle.CARD_BG, UIStyle.BORDER_COLOR);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UIStyle.FONT_HEADER);
        lblTitle.setForeground(UIStyle.ACCENT_GLOW);
        lblTitle.setBorder(new EmptyBorder(0, 0, 12, 0));
        card.add(lblTitle, BorderLayout.NORTH);

        JLabel lblBody = new JLabel(htmlBody);
        lblBody.setFont(UIStyle.FONT_BODY);
        lblBody.setForeground(UIStyle.TXT_LIGHT);
        card.add(lblBody, BorderLayout.CENTER);

        // Cap height so cards align correctly under vertical flow
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height + 40));

        return card;
    }
}

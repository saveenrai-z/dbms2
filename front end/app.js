/* Frontend Controller Logic - Time Table Management */

// Global State
let teachers = [];
let subjects = [];
let classes = [];
let rooms = [];
let timeslots = [];
let timetableEntries = [];

// Base API URL (relative to localhost:8080 host)
const API_BASE = '/api';

// On Document Load
document.addEventListener('DOMContentLoaded', () => {
    initSPA();
    initDatabaseStatus();
    loadAllData();
    setupFormListeners();
    setupResetButton();
    setupPurgeButton();
    setupSolverButton();
    setupTimetableFilters();
    setupExportButtons();
});

// ==========================================
// SPA TAB NAVIGATION
// ==========================================
function initSPA() {
    const navItems = document.querySelectorAll('.nav-item');
    const tabPanels = document.querySelectorAll('.tab-panel');

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            
            const targetTab = item.getAttribute('data-tab');
            
            // Update active sidebar state
            navItems.forEach(n => n.classList.remove('active'));
            item.classList.add('active');
            
            // Switch panel displays
            tabPanels.forEach(p => p.classList.remove('active'));
            const activePanel = document.getElementById(`panel-${targetTab}`);
            if (activePanel) {
                activePanel.classList.add('active');
            }
            
            // Update panel header title
            const titleElement = document.getElementById('current-panel-title');
            titleElement.textContent = item.querySelector('span').textContent;

            // If entering timetable tab, refresh filters and render
            if (targetTab === 'timetable') {
                refreshTimetableTab();
            }
        });
    });
}

// ==========================================
// NOTIFICATIONS SYSTEM
// ==========================================
function showNotification(message, type = 'info') {
    const area = document.getElementById('notification-area');
    const notif = document.createElement('div');
    notif.className = `notification ${type}`;
    
    let icon = 'fa-circle-info';
    if (type === 'success') icon = 'fa-circle-check';
    if (type === 'error') icon = 'fa-triangle-exclamation';

    notif.innerHTML = `
        <i class="fa-solid ${icon}"></i>
        <span>${message}</span>
    `;

    area.appendChild(notif);

    // Auto-remove after 4 seconds with animation fade-out
    setTimeout(() => {
        notif.style.opacity = '0';
        notif.style.transform = 'translateY(-10px)';
        notif.style.transition = 'all 0.4s ease';
        setTimeout(() => notif.remove(), 400);
    }, 4000);
}

// ==========================================
// CORE DATABASE METRICS & HEALTH
// ==========================================
async function initDatabaseStatus() {
    try {
        const res = await fetch(`${API_BASE}/status`);
        if (!res.ok) throw new Error("Status API returned bad response");
        const status = await res.json();
        
        // Update connection badges
        const modeSpan = document.getElementById('connection-mode');
        const dbBadge = document.getElementById('db-type-badge');
        const solverStatusBadge = document.getElementById('solver-status-badge');
        
        if (status.is_mock) {
            if (modeSpan) {
                modeSpan.textContent = "JSON Database (Offline Cache)";
                modeSpan.previousElementSibling.className = "status-dot offline";
            }
            if (dbBadge) {
                dbBadge.textContent = "Mock Cache Mode";
                dbBadge.style.background = "rgba(245, 158, 11, 0.1)";
                dbBadge.style.color = "var(--color-warning)";
                dbBadge.style.borderColor = "rgba(245, 158, 11, 0.25)";
            }
        } else {
            if (modeSpan) {
                modeSpan.textContent = "MongoDB Server Online";
                modeSpan.previousElementSibling.className = "status-dot online";
            }
            if (dbBadge) {
                dbBadge.textContent = "MongoDB Connected";
                dbBadge.style.background = "rgba(16, 185, 129, 0.1)";
                dbBadge.style.color = "var(--color-success)";
                dbBadge.style.borderColor = "rgba(16, 185, 129, 0.25)";
            }
        }

        // Update counts on Dashboard
        document.getElementById('stat-teachers').textContent = status.teachers_count;
        document.getElementById('stat-subjects').textContent = status.subjects_count;
        document.getElementById('stat-classes').textContent = status.classes_count;
        document.getElementById('stat-rooms').textContent = status.rooms_count;

        // Timetable status
        if (status.timetable_entries_count > 0) {
            if (solverStatusBadge) {
                solverStatusBadge.innerHTML = `
                    <i class="fa-solid fa-circle-check"></i>
                    <span>Time Table Ready</span>
                `;
                solverStatusBadge.style.background = "rgba(16, 185, 129, 0.1)";
                solverStatusBadge.style.color = "var(--color-success)";
                solverStatusBadge.style.borderColor = "rgba(16, 185, 129, 0.2)";
            }
        } else {
            if (solverStatusBadge) {
                solverStatusBadge.innerHTML = `
                    <i class="fa-solid fa-triangle-exclamation"></i>
                    <span>No Time Table</span>
                `;
                solverStatusBadge.style.background = "rgba(245, 158, 11, 0.1)";
                solverStatusBadge.style.color = "var(--color-warning)";
                solverStatusBadge.style.borderColor = "rgba(245, 158, 11, 0.2)";
            }
        }

    } catch (e) {
        console.error("Failed to connect to backend WebServer: ", e);
        showNotification("Server Offline: Unable to query database status.", "error");
    }
}

// ==========================================
// LOAD ALL DATA REGISTERS
// ==========================================
async function loadAllData() {
    try {
        await Promise.all([
            fetchTeachers(),
            fetchClasses(),
            fetchRooms(),
            fetchTimeslots(),
            fetchSubjects()
        ]);
        
        // Refresh selectors inside forms
        populateFormSelectors();
        
        // Populate UI tables
        renderTeachersTable();
        renderSubjectsTable();
        renderClassesTable();
        renderRoomsTable();
        
        // Refresh dashboard status stats counts
        initDatabaseStatus();
        
    } catch (e) {
        console.error("Error batch fetching academic registries: ", e);
    }
}

async function fetchTeachers() {
    try {
        const res = await fetch(`${API_BASE}/teachers`);
        if (!res.ok) throw new Error("Teachers API failed");
        teachers = await res.json();
    } catch (e) {
        console.error("fetchTeachers failed: ", e);
        teachers = [];
    }
}

async function fetchSubjects() {
    try {
        const res = await fetch(`${API_BASE}/subjects`);
        if (!res.ok) throw new Error("Subjects API failed");
        subjects = await res.json();
    } catch (e) {
        console.error("fetchSubjects failed: ", e);
        subjects = [];
    }
}

async function fetchClasses() {
    try {
        const res = await fetch(`${API_BASE}/classes`);
        if (!res.ok) throw new Error("Classes API failed");
        classes = await res.json();
    } catch (e) {
        console.error("fetchClasses failed: ", e);
        classes = [];
    }
}

async function fetchRooms() {
    try {
        const res = await fetch(`${API_BASE}/rooms`);
        if (!res.ok) throw new Error("Rooms API failed");
        rooms = await res.json();
    } catch (e) {
        console.error("fetchRooms failed: ", e);
        rooms = [];
    }
}

async function fetchTimeslots() {
    try {
        const res = await fetch(`${API_BASE}/timeslots`);
        if (!res.ok) throw new Error("Timeslots API failed");
        timeslots = await res.json();
    } catch (e) {
        console.error("fetchTimeslots failed: ", e);
        timeslots = [];
    }
}

async function fetchTimetable() {
    try {
        const res = await fetch(`${API_BASE}/timetable`);
        if (!res.ok) throw new Error("Timetable API failed");
        timetableEntries = await res.json();
    } catch (e) {
        console.error("fetchTimetable failed: ", e);
        timetableEntries = [];
    }
}

// ==========================================
// FORM SELECTORS DYNAMIC INJECTION
// ==========================================
function populateFormSelectors() {
    const classSelect = document.getElementById('subject-class');
    const teacherSelect = document.getElementById('subject-teacher');

    // Reset selectors
    classSelect.innerHTML = '<option value="">-- Choose Class Group --</option>';
    teacherSelect.innerHTML = '<option value="">-- Choose Teacher --</option>';

    classes.forEach(c => {
        classSelect.innerHTML += `<option value="${c.class_id}">${c.class_id} - ${c.class_name}</option>`;
    });

    teachers.forEach(t => {
        teacherSelect.innerHTML += `<option value="${t.teacher_id}">${t.name} (${t.teacher_id})</option>`;
    });
}

// ==========================================
// TABLE RENDERERS
// ==========================================
function renderTeachersTable() {
    const tbody = document.querySelector('#table-teachers tbody');
    tbody.innerHTML = '';
    
    if (teachers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="empty-cell">No teachers registered yet.</td></tr>';
        return;
    }

    teachers.forEach(t => {
        const dept = t.department || 'cse-aiml';
        let bgStyle = 'rgba(6, 182, 212, 0.1)';
        let textColor = 'var(--color-secondary)';
        let borderCol = 'rgba(6, 182, 212, 0.25)';

        if (dept === 'cse') {
            bgStyle = 'rgba(16, 185, 129, 0.1)';
            textColor = 'var(--color-success)';
            borderCol = 'rgba(16, 185, 129, 0.25)';
        } else if (dept === 'ise') {
            bgStyle = 'rgba(245, 158, 11, 0.1)';
            textColor = 'var(--color-warning)';
            borderCol = 'rgba(245, 158, 11, 0.25)';
        }

        tbody.innerHTML += `
            <tr>
                <td><strong>${t.teacher_id}</strong></td>
                <td>${t.name}</td>
                <td><span class="badge-db" style="background: ${bgStyle}; color: ${textColor}; border-color: ${borderCol};">${dept.toUpperCase()}</span></td>
                <td class="actions-col">
                    <button class="btn btn-danger btn-sm" onclick="deleteRecord('teachers', '${t.teacher_id}')" title="Delete Teacher">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
}

function renderSubjectsTable() {
    const tbody = document.querySelector('#table-subjects tbody');
    tbody.innerHTML = '';

    if (subjects.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-cell">No subjects registered yet.</td></tr>';
        return;
    }

    subjects.forEach(s => {
        tbody.innerHTML += `
            <tr>
                <td><strong>${s.sub_id}</strong></td>
                <td>${s.sub_name}</td>
                <td>${s.hours} hours</td>
                <td><span class="badge-db" style="background: rgba(6,182,212,0.1); color: var(--color-secondary); border-color: rgba(6,182,212,0.2);">${s.class_id}</span></td>
                <td>${getTeacherName(s.teacher_id)}</td>
                <td class="actions-col">
                    <button class="btn btn-danger btn-sm" onclick="deleteRecord('subjects', '${s.sub_id}', { classId: '${s.class_id}' })" title="Delete Subject">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
}

function renderClassesTable() {
    const tbody = document.querySelector('#table-classes tbody');
    tbody.innerHTML = '';

    if (classes.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="empty-cell">No classes registered yet.</td></tr>';
        return;
    }

    classes.forEach(c => {
        const cleanDeptName = c.class_name.replace(/\s+[0-9]+[a-zA-Z]+$/, '').toUpperCase();
        tbody.innerHTML += `
            <tr>
                <td><strong>${c.class_id}</strong></td>
                <td>${cleanDeptName}</td>
                <td>Semester ${c.semester}</td>
                <td class="actions-col">
                    <button class="btn btn-danger btn-sm" onclick="deleteRecord('classes', '${c.class_id}')" title="Delete Class">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
}

function renderRoomsTable() {
    const tbody = document.querySelector('#table-rooms tbody');
    tbody.innerHTML = '';

    if (rooms.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="empty-cell">No physical rooms registered yet.</td></tr>';
        return;
    }

    rooms.forEach(r => {
        const dept = r.department || 'cse-aiml';
        let bgStyle = 'rgba(6, 182, 212, 0.1)';
        let textColor = 'var(--color-secondary)';
        let borderCol = 'rgba(6, 182, 212, 0.25)';

        if (dept === 'cse') {
            bgStyle = 'rgba(16, 185, 129, 0.1)';
            textColor = 'var(--color-success)';
            borderCol = 'rgba(16, 185, 129, 0.25)';
        } else if (dept === 'ise') {
            bgStyle = 'rgba(245, 158, 11, 0.1)';
            textColor = 'var(--color-warning)';
            borderCol = 'rgba(245, 158, 11, 0.25)';
        }

        tbody.innerHTML += `
            <tr>
                <td><strong>${r.room_id}</strong></td>
                <td><span class="badge-db" style="background: ${bgStyle}; color: ${textColor}; border-color: ${borderCol};">${dept.toUpperCase()}</span></td>
                <td>Room ${r.room_no}</td>
                <td class="actions-col">
                    <button class="btn btn-danger btn-sm" onclick="deleteRecord('rooms', '${r.room_id}')" title="Delete Room">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
}



// Helpers to resolve foreign names
function getTeacherName(id) {
    const t = teachers.find(x => x.teacher_id === id);
    return t ? t.name : id;
}

function getSubjectName(id) {
    const s = subjects.find(x => x.sub_id === id);
    return s ? s.sub_name : id;
}

// ==========================================
// FORM SUBMISSIONS & POST APIS
// ==========================================
function setupFormListeners() {
    // Add Teacher
    document.getElementById('form-teacher').addEventListener('submit', async (e) => {
        e.preventDefault();
        const rawTeacherId = document.getElementById('teacher-id').value.trim();
        const name = document.getElementById('teacher-name').value.trim();
        const department = document.getElementById('teacher-dept').value;

        // Validation of ID prefixes based on department
        let expectedPrefix = 'ai';
        if (department === 'cse') expectedPrefix = 'cs';
        else if (department === 'ise') expectedPrefix = 'is';

        const lowerId = rawTeacherId.toLowerCase();
        if (!lowerId.startsWith(expectedPrefix)) {
            showNotification(`Teacher ID for ${department.toUpperCase()} department must start with '${expectedPrefix}' prefix (e.g. ${expectedPrefix}101)`, "error");
            return;
        }

        // Standardize to uppercase IDs (e.g., AI101)
        const teacherId = rawTeacherId.toUpperCase();

        try {
            const res = await fetch(`${API_BASE}/teachers`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ teacherId, name, department })
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.error || "Failed to save teacher");

            showNotification("Teacher registered successfully!", "success");
            document.getElementById('form-teacher').reset();
            loadAllData();
        } catch (err) {
            showNotification(err.message, "error");
        }
    });

    // Add Subject
    document.getElementById('form-subject').addEventListener('submit', async (e) => {
        e.preventDefault();
        const rawSubId = document.getElementById('subject-id').value.trim();
        // Remove semsec suffix like -4A, -6A, etc. case-insensitively
        const subId = rawSubId.replace(/-[0-9]+[a-zA-Z]+$/, '');
        const subName = document.getElementById('subject-name').value.trim();
        const hours = parseInt(document.getElementById('subject-hours').value);
        const classId = document.getElementById('subject-class').value;
        const teacherId = document.getElementById('subject-teacher').value;

        try {
            const res = await fetch(`${API_BASE}/subjects`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ subId, subName, hours, classId, teacherId })
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.error || "Failed to save subject");

            showNotification("Subject course registered successfully!", "success");
            document.getElementById('form-subject').reset();
            loadAllData();
        } catch (err) {
            showNotification(err.message, "error");
        }
    });

    // Add Class
    document.getElementById('form-class').addEventListener('submit', async (e) => {
        e.preventDefault();
        const classId = document.getElementById('class-id').value;
        const className = document.getElementById('class-name').value;
        const semester = parseInt(document.getElementById('class-semester').value);

        try {
            const res = await fetch(`${API_BASE}/classes`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ classId, className, semester })
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.error || "Failed to save class group");

            showNotification("Class group saved successfully!", "success");
            document.getElementById('form-class').reset();
            loadAllData();
        } catch (err) {
            showNotification(err.message, "error");
        }
    });

    // Add Room
    document.getElementById('form-room').addEventListener('submit', async (e) => {
        e.preventDefault();
        const roomId = document.getElementById('room-id').value.trim();
        const roomNo = document.getElementById('room-no').value.trim();
        const department = document.getElementById('room-dept').value;

        try {
            const res = await fetch(`${API_BASE}/rooms`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ roomId, roomNo, department })
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.error || "Failed to save room");

            showNotification("Classroom details saved successfully!", "success");
            document.getElementById('form-room').reset();
            loadAllData();
        } catch (err) {
            showNotification(err.message, "error");
        }
    });


}

// Global record deletion mapping
window.deleteRecord = async function(resource, id, extraParams = {}) {
    if (!confirm(`Are you sure you want to delete this ${resource.slice(0, -1)} registry?`)) return;
    
    try {
        let url = `${API_BASE}/${resource}?id=${encodeURIComponent(id)}`;
        for (const [key, val] of Object.entries(extraParams)) {
            if (val !== undefined && val !== null) {
                url += `&${key}=${encodeURIComponent(val)}`;
            }
        }
        const res = await fetch(url, {
            method: 'DELETE'
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "Deletion operation failed");

        showNotification("Record deleted successfully.", "success");
        loadAllData();
    } catch (err) {
        showNotification(err.message, "error");
    }
};

// ==========================================
// RESET DATABASE CONTROLS
// ==========================================
function setupResetButton() {
    const btn = document.getElementById('btn-reset-db');
    btn.addEventListener('click', async () => {
        if (!confirm("Caution: This will clear your custom entries and populate default database tables. Proceed?")) return;
        
        try {
            btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Resetting...';
            const res = await fetch(`${API_BASE}/reset`, { method: 'POST' });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error);

            showNotification("Database reset to academic sample records successfully!", "success");
            loadAllData();
        } catch (err) {
            showNotification(err.message, "error");
        } finally {
            btn.innerHTML = '<i class="fa-solid fa-rotate-left"></i> Reset Database';
        }
    });
}

// ==========================================
// PURGE DATABASE CONTROLS
// ==========================================
function setupPurgeButton() {
    const btn = document.getElementById('btn-purge-db');
    if (!btn) return;
    btn.addEventListener('click', async () => {
        if (!confirm("WARNING: This will permanently wipe ALL data (Teachers, Subjects, Class Groups, Classrooms, and Timetables) from this project. This action cannot be undone! Are you absolutely sure?")) return;
        
        try {
            btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Purging...';
            const res = await fetch(`${API_BASE}/purge`, { method: 'POST' });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error);

            showNotification("All database records successfully purged from the project!", "success");
            loadAllData();
        } catch (err) {
            showNotification(err.message, "error");
        } finally {
            btn.innerHTML = '<i class="fa-solid fa-trash-can"></i> Purge Database';
        }
    });
}

// ==========================================
// SOLVER ENGINE INVOKER
// ==========================================
function setupSolverButton() {
    const btn = document.getElementById('btn-trigger-solver');
    if (!btn) return;

    const resultStatus = document.getElementById('result-status');
    const resultTime = document.getElementById('result-time');
    const resultBacktracks = document.getElementById('result-backtracks');
    const resultAllocations = document.getElementById('result-allocations');
    const lastRunText = document.getElementById('solver-last-run');

    btn.addEventListener('click', async () => {
        try {
            btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Generating...';
            btn.disabled = true;
            
            if (resultStatus) {
                resultStatus.textContent = "Computing...";
                resultStatus.style.color = "var(--color-warning)";
            }

            const startTime = Date.now();
            const res = await fetch(`${API_BASE}/generate`, { method: 'POST' });
            const data = await res.json();

            const duration = Date.now() - startTime;

            if (res.ok && data.success) {
                if (resultStatus) {
                    resultStatus.textContent = "Success";
                    resultStatus.style.color = "var(--color-success)";
                }
                if (resultTime) resultTime.textContent = `${data.executionTimeMs} ms`;
                if (resultBacktracks) resultBacktracks.textContent = data.backtrackCount;
                if (resultAllocations) resultAllocations.textContent = data.totalAllocations;
                
                if (lastRunText) {
                    lastRunText.textContent = `Completed successfully at ${new Date().toLocaleTimeString()}!`;
                }
                showNotification("Optimal Timetable compiled successfully!", "success");
                
                // Refresh health and statistics
                initDatabaseStatus();
                // Dynamically redraw timetable matrix to show new schedule instantly
                refreshTimetableTab();
            } else {
                if (resultStatus) {
                    resultStatus.textContent = "Collision Fail";
                    resultStatus.style.color = "var(--color-danger)";
                }
                if (resultTime) resultTime.textContent = `${duration} ms`;
                if (resultBacktracks) resultBacktracks.textContent = data ? data.backtrackCount : "--";
                if (resultAllocations) resultAllocations.textContent = "0";
                
                if (lastRunText) {
                    lastRunText.textContent = `Constraint Clashes Found: Timetable Generation Failed!`;
                }
                showNotification("Could not compile conflict-free schedules! Please modify constraints.", "error");
            }

        } catch (e) {
            console.error(e);
            if (resultStatus) {
                resultStatus.textContent = "Error";
                resultStatus.style.color = "var(--color-danger)";
            }
            showNotification("Backend server crash or timeout during scheduling recursions.", "error");
        } finally {
            btn.innerHTML = '<i class="fa-solid fa-star"></i> Generate Time Table';
            btn.disabled = false;
        }
    });
}

// ==========================================
// TIMETABLE MATRIX GRID CONTROLLER
// ==========================================
function setupTimetableFilters() {
    const filterType = document.getElementById('filter-type');
    const filterValue = document.getElementById('filter-value');
    const labelFilter = document.getElementById('label-filter-value');

    filterType.addEventListener('change', () => {
        const type = filterType.value;
        filterValue.innerHTML = '';

        if (type === 'class') {
            labelFilter.textContent = "Select Class Group:";
            classes.forEach(c => {
                filterValue.innerHTML += `<option value="${c.class_id}">${c.class_id}</option>`;
            });
        } else if (type === 'teacher') {
            labelFilter.textContent = "Select Teacher Profile:";
            teachers.forEach(t => {
                filterValue.innerHTML += `<option value="${t.teacher_id}">${t.name}</option>`;
            });
        } else if (type === 'room') {
            labelFilter.textContent = "Select Room:";
            rooms.forEach(r => {
                filterValue.innerHTML += `<option value="${r.room_id}">Room ${r.room_no}</option>`;
            });
        }

        // Draw roster
        drawTimetableMatrix();
    });

    filterValue.addEventListener('change', () => {
        drawTimetableMatrix();
    });
}

async function refreshTimetableTab() {
    await fetchTimetable();
    
    // Refresh the selectors with initial values
    const filterType = document.getElementById('filter-type');
    const filterValue = document.getElementById('filter-value');
    const labelFilter = document.getElementById('label-filter-value');

    // Trigger initial filter populate if empty
    if (filterValue.options.length === 0) {
        filterType.value = 'class';
        labelFilter.textContent = "Select Class Group:";
        classes.forEach(c => {
            filterValue.innerHTML += `<option value="${c.class_id}">${c.class_id}</option>`;
        });
    }

    drawTimetableMatrix();
}

function drawTimetableMatrix() {
    const tbody = document.querySelector('#matrix-timetable tbody');
    tbody.innerHTML = '';

    const filterType = document.getElementById('filter-type').value;
    const selectedValue = document.getElementById('filter-value').value;
    
    // Update Print view meta labels
    const printTitle = document.getElementById('print-title');
    if (filterType === 'class') {
        printTitle.textContent = `Class Schedule: SemSec ${selectedValue}`;
    } else if (filterType === 'teacher') {
        printTitle.textContent = `Faculty Time Table: ${getTeacherName(selectedValue)}`;
    } else if (filterType === 'room') {
        printTitle.textContent = `Room Allocation: Room ${selectedValue.replace("R", "")}`;
    }

    if (!selectedValue) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-cell">Add records and trigger the AI Solver to display grids.</td></tr>';
        return;
    }

    // Process timeslots chronologically
    // Extract unique time intervals sorted chronologically
    const uniqueHours = [...new Set(timeslots.map(s => s.time))].sort((a, b) => {
        // Simple hour parser, e.g. "09:00 AM" vs "10:00 AM"
        const timeVal = (tStr) => {
            const m = tStr.match(/(\d+):(\d+)\s*(AM|PM)/i);
            if (!m) return 0;
            let hrs = parseInt(m[1]);
            const mins = parseInt(m[2]);
            const isPM = m[3].toUpperCase() === 'PM';
            if (isPM && hrs !== 12) hrs += 12;
            if (!isPM && hrs === 12) hrs = 0;
            return hrs * 60 + mins;
        };
        return timeVal(a) - timeVal(b);
    });

    const weekdays = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];

    if (uniqueHours.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-cell">No timeslots configured. Please configure in Timeslots tab.</td></tr>';
        return;
    }

    uniqueHours.forEach(hour => {
        const matchingSlots = timeslots.filter(t => t.time === hour);
        const isLunchBreakRow = matchingSlots.some(t => t.is_lunch_break);
        let rowHtml = `<tr><td><strong>${hour}</strong></td>`;
        
        if (isLunchBreakRow) {
            if (hour === "10:30 AM - 10:45 AM") {
                // Render beautiful merged Short Break Row
                rowHtml += `
                    <td colspan="6" class="short-break-cell">
                        <span class="short-break-badge">
                            <i class="fa-solid fa-mug-hot"></i> TEA BREAK / REST
                        </span>
                    </td>
                `;
            } else {
                // Render beautiful merged Lunch Row
                rowHtml += `
                    <td colspan="6" class="lunch-break-cell">
                        <span class="lunch-break-badge">
                            <i class="fa-solid fa-utensils"></i> LUNCH BREAK / RECESS
                        </span>
                    </td>
                `;
            }
        } else {
            // Render ordinary periods per weekday column
            weekdays.forEach(day => {
                const slot = timeslots.find(s => s.day === day && s.time === hour);
                
                if (!slot) {
                    if (day === "Saturday") {
                        rowHtml += '<td><div class="inactive-slot">Inactive</div></td>';
                    } else {
                        rowHtml += '<td><span class="empty-cell">--</span></td>';
                    }
                    return;
                }

                // Search for matching timetable cells
                const match = timetableEntries.find(e => {
                    if (e.slot_id !== slot.slot_id) return false;
                    if (filterType === 'class' && e.class_id.toLowerCase() === selectedValue.toLowerCase()) return true;
                    if (filterType === 'teacher' && e.teacher_id.toLowerCase() === selectedValue.toLowerCase()) return true;
                    if (filterType === 'room' && e.room_id.toLowerCase() === selectedValue.toLowerCase()) return true;
                    return false;
                });

                if (match) {
                    // Resolve course codes
                    const subject = subjects.find(s => s.sub_id === match.sub_id);
                    const subName = subject ? subject.sub_name : match.sub_id;
                    const teacherName = getTeacherName(match.teacher_id);
                    const roomNo = rooms.find(r => r.room_id === match.room_id)?.room_no || match.room_id;

                    rowHtml += `
                        <td>
                            <div class="schedule-card animate-fade-in">
                                ${filterType !== 'class' ? `<span class="cell-class">${match.class_id}</span>` : ''}
                                <span class="cell-subject">${subName}</span>
                                ${filterType !== 'teacher' ? `<span class="cell-teacher"><i class="fa-solid fa-user-tie"></i> ${teacherName}</span>` : ''}
                                ${filterType !== 'room' ? `<span class="cell-room"><i class="fa-solid fa-building"></i> R ${roomNo}</span>` : ''}
                            </div>
                        </td>
                    `;
                } else {
                    rowHtml += '<td><span class="empty-cell">--</span></td>';
                }
            });
        }

        rowHtml += '</tr>';
        tbody.innerHTML += rowHtml;
    });
}

// ==========================================
// EXPORTS DOWNLOAD CONTROLLERS
// ==========================================
function setupExportButtons() {
    // Print PDF Trigger
    document.getElementById('btn-print-pdf').addEventListener('click', () => {
        window.print();
    });

    // CSV Download Timetable Trigger
    document.getElementById('btn-export-csv').addEventListener('click', () => {
        const filterType = document.getElementById('filter-type').value;
        const selectedValue = document.getElementById('filter-value').value;

        if (!selectedValue || timetableEntries.length === 0) {
            showNotification("No timetable entries to export! Run Solver first.", "error");
            return;
        }

        const weekdays = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
        const uniqueHours = [...new Set(timeslots.map(s => s.time))];

        // Compile CSV Rows
        let csvContent = `Time Table,${filterType.toUpperCase()}:${selectedValue}\n`;
        csvContent += `Period / Hour,${weekdays.join(',')}\n`;

        uniqueHours.forEach(hour => {
            let row = [`"${hour}"`];
            
            const matchingSlots = timeslots.filter(t => t.time === hour);
            const isLunchBreakRow = matchingSlots.some(t => t.is_lunch_break);

            weekdays.forEach(day => {
                if (isLunchBreakRow) {
                    if (day === "Saturday" && hour === "12:45 PM - 01:45 PM") {
                        row.push('"INACTIVE"');
                    } else if (hour === "10:30 AM - 10:45 AM") {
                        row.push('"TEA BREAK"');
                    } else {
                        row.push('"LUNCH BREAK"');
                    }
                    return;
                }

                const slot = timeslots.find(s => s.day === day && s.time === hour);
                if (!slot) {
                    if (day === "Saturday") {
                        row.push('"INACTIVE"');
                    } else {
                        row.push('"--"');
                    }
                    return;
                }

                const match = timetableEntries.find(e => {
                    if (e.slot_id !== slot.slot_id) return false;
                    if (filterType === 'class' && e.class_id.toLowerCase() === selectedValue.toLowerCase()) return true;
                    if (filterType === 'teacher' && e.teacher_id.toLowerCase() === selectedValue.toLowerCase()) return true;
                    if (filterType === 'room' && e.room_id.toLowerCase() === selectedValue.toLowerCase()) return true;
                    return false;
                });

                if (match) {
                    const subject = subjects.find(s => s.sub_id === match.sub_id)?.sub_name || match.sub_id;
                    const teacher = getTeacherName(match.teacher_id);
                    const room = rooms.find(r => r.room_id === match.room_id)?.room_no || match.room_id;
                    row.push(`"${subject} (${teacher} in Room ${room})"`);
                } else {
                    row.push('"--"');
                }
            });

            csvContent += row.join(',') + '\n';
        });

        // Trigger file download block
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.setAttribute("href", url);
        link.setAttribute("download", `timetable_generation_${selectedValue}.csv`);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        showNotification("CSV Spreadsheet compiled and downloaded!", "success");
    });
}



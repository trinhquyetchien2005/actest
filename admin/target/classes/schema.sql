CREATE TABLE IF NOT EXISTS exams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    duration INTEGER NOT NULL,
    creator_id INTEGER,
    status TEXT NOT NULL,
    allow_review BOOLEAN DEFAULT 0
);

CREATE TABLE IF NOT EXISTS questions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    exam_id INTEGER,
    content TEXT NOT NULL,
    options TEXT NOT NULL,
    correct_option_index INTEGER NOT NULL,
    correct_answer TEXT,
    wrong_answer1 TEXT,
    wrong_answer2 TEXT,
    wrong_answer3 TEXT
);



CREATE TABLE IF NOT EXISTS results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_name TEXT NOT NULL,
    focus_lost_count INTEGER DEFAULT 0,
    score REAL DEFAULT 0.0,
    exam_id INTEGER REFERENCES exams(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS student_answers (
    student_name TEXT NOT NULL,
    exam_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    answer TEXT,
    PRIMARY KEY (student_name, exam_id, question_id)
);

CREATE TABLE IF NOT EXISTS exam_violations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_name TEXT NOT NULL,
    exam_id INTEGER NOT NULL,
    violation_type TEXT NOT NULL,
    violation_time TEXT NOT NULL,
    image_data TEXT
);

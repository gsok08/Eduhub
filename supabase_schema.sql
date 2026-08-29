-- =============================================================================
-- EduHub Complete Supabase SQL Database Schema & PostgREST Setup
-- Run this entire script in your Supabase Dashboard -> SQL Editor -> Run (▶️)
-- =============================================================================

-- 1. Profiles Table (Tracks User Display Name and Role)
CREATE TABLE IF NOT EXISTS public.profiles (
    id TEXT PRIMARY KEY,
    full_name TEXT NOT NULL,
    role TEXT DEFAULT 'STUDENT', -- 'STUDENT' or 'LECTURER'
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public full access on profiles" ON public.profiles;
CREATE POLICY "Public full access on profiles" ON public.profiles FOR ALL USING (true) WITH CHECK (true);

-- 2. Courses Table
CREATE TABLE IF NOT EXISTS public.courses (
    id TEXT PRIMARY KEY,
    code TEXT NOT NULL,
    title TEXT NOT NULL,
    lecturer_name TEXT NOT NULL,
    join_code TEXT NOT NULL,
    icon_category TEXT DEFAULT 'CODE',
    exam_days_left INT DEFAULT 30,
    progress REAL DEFAULT 0.0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.courses ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public full access on courses" ON public.courses;
CREATE POLICY "Public full access on courses" ON public.courses FOR ALL USING (true) WITH CHECK (true);

-- 3. Course Enrollments Table (Tracks Which Students Joined Which Courses)
CREATE TABLE IF NOT EXISTS public.course_enrollments (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id TEXT NOT NULL,
    course_id TEXT NOT NULL,
    enrolled_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, course_id)
);

ALTER TABLE public.course_enrollments ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public full access on course_enrollments" ON public.course_enrollments;
CREATE POLICY "Public full access on course_enrollments" ON public.course_enrollments FOR ALL USING (true) WITH CHECK (true);

-- 4. Announcements Table
CREATE TABLE IF NOT EXISTS public.announcements (
    id TEXT PRIMARY KEY,
    course_id TEXT NOT NULL,
    lecturer_name TEXT NOT NULL,
    date TEXT NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.announcements ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public full access on announcements" ON public.announcements;
CREATE POLICY "Public full access on announcements" ON public.announcements FOR ALL USING (true) WITH CHECK (true);

-- 5. Lecture Notes Table (with PDF Storage Support)
CREATE TABLE IF NOT EXISTS public.lecture_notes (
    id TEXT PRIMARY KEY,
    course_code TEXT NOT NULL,
    course_title TEXT NOT NULL,
    semester_period TEXT NOT NULL,
    chapter_title TEXT NOT NULL,
    raw_content TEXT NOT NULL,
    pdf_file_name TEXT,
    pdf_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.lecture_notes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public full access on lecture_notes" ON public.lecture_notes;
CREATE POLICY "Public full access on lecture_notes" ON public.lecture_notes FOR ALL USING (true) WITH CHECK (true);

-- 6. Study Groups Table
CREATE TABLE IF NOT EXISTS public.study_groups (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    host TEXT NOT NULL,
    details TEXT DEFAULT '',
    current_members INT DEFAULT 1,
    max_members INT DEFAULT 6,
    category TEXT DEFAULT 'GROUP',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.study_groups ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public full access on study_groups" ON public.study_groups;
CREATE POLICY "Public full access on study_groups" ON public.study_groups FOR ALL USING (true) WITH CHECK (true);

-- 7. Chat Messages Table
CREATE TABLE IF NOT EXISTS public.chat_messages (
    id TEXT PRIMARY KEY,
    group_id TEXT NOT NULL,
    sender_name TEXT NOT NULL,
    sender_role TEXT NOT NULL,
    message TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    is_from_me BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public full access on chat_messages" ON public.chat_messages;
CREATE POLICY "Public full access on chat_messages" ON public.chat_messages FOR ALL USING (true) WITH CHECK (true);

-- 8. Supabase Storage Bucket for Lecture PDFs
INSERT INTO storage.buckets (id, name, public) 
VALUES ('lecture-notes', 'lecture-notes', true)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "Public Access to lecture-notes bucket" ON storage.objects;
CREATE POLICY "Public Access to lecture-notes bucket" ON storage.objects FOR ALL USING (bucket_id = 'lecture-notes') WITH CHECK (bucket_id = 'lecture-notes');

-- 9. Grant full API permissions to public schema tables
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL ROUTINES IN SCHEMA public TO anon, authenticated, service_role;

-- 10. Enable Realtime Replication
ALTER PUBLICATION supabase_realtime ADD TABLE public.study_groups;
ALTER PUBLICATION supabase_realtime ADD TABLE public.chat_messages;
ALTER PUBLICATION supabase_realtime ADD TABLE public.announcements;
ALTER PUBLICATION supabase_realtime ADD TABLE public.courses;
ALTER PUBLICATION supabase_realtime ADD TABLE public.lecture_notes;

-- 11. Force PostgREST schema cache to reload immediately
NOTIFY pgrst, 'reload schema';

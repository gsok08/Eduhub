-- =============================================================================
-- EduHub Complete Supabase SQL Database Schema
-- Run this SQL in your Supabase Dashboard -> SQL Editor -> New query -> Run
-- =============================================================================

-- 1. Profiles Table
CREATE TABLE IF NOT EXISTS public.profiles (
    id TEXT PRIMARY KEY,
    full_name TEXT NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public full access on profiles" ON public.profiles;
CREATE POLICY "Public full access on profiles" ON public.profiles FOR ALL USING (true) WITH CHECK (true);

-- 2. Study Groups Table
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

-- 3. Chat Messages Table
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

-- 4. Enable Realtime Replication for Chat Messages and Study Groups
ALTER PUBLICATION supabase_realtime ADD TABLE public.study_groups;
ALTER PUBLICATION supabase_realtime ADD TABLE public.chat_messages;

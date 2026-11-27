-- ScamShield Supabase schema
-- Apply in Supabase SQL Editor. Safe to re-run where practical.
-- Client apps use only the publishable key; never expose service_role/secret keys.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text not null unique check (username ~ '^[a-z0-9_]{3,24}$'),
  display_name text not null default '' check (char_length(display_name) <= 60),
  city text not null default '' check (char_length(city) <= 60),
  bio text not null default '' check (char_length(bio) <= 240),
  is_public boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.posts (
  id uuid primary key default gen_random_uuid(),
  author_id uuid not null references public.profiles(id) on delete cascade,
  title text not null check (char_length(title) between 1 and 120),
  body text not null check (char_length(body) between 1 and 5000),
  category text not null default 'community' check (char_length(category) <= 40),
  city text not null default '' check (char_length(city) <= 60),
  status text not null default 'published' check (status in ('published','hidden','removed')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.comments (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references public.posts(id) on delete cascade,
  author_id uuid not null references public.profiles(id) on delete cascade,
  body text not null check (char_length(body) between 1 and 1000),
  status text not null default 'published' check (status in ('published','hidden','removed')),
  created_at timestamptz not null default now()
);

create table if not exists public.post_votes (
  post_id uuid not null references public.posts(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  value smallint not null check (value in (-1,1)),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (post_id,user_id)
);

create table if not exists public.reposts (
  post_id uuid not null references public.posts(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (post_id,user_id)
);

create table if not exists public.scam_reports (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references public.profiles(id) on delete cascade,
  target_type text not null check (target_type in ('phone','email','url','message','social','marketplace','qr','other')),
  target_value text not null check (char_length(target_value) between 1 and 2048),
  category text not null default 'unspecified' check (char_length(category) <= 60),
  details text not null default '' check (char_length(details) <= 2000),
  status text not null default 'pending' check (status in ('pending','reviewed','rejected','confirmed')),
  created_at timestamptz not null default now(),
  unique (reporter_id,target_type,target_value)
);

create table if not exists public.rule_acceptances (
  user_id uuid not null references public.profiles(id) on delete cascade,
  rules_version text not null,
  accepted_at timestamptz not null default now(),
  primary key (user_id,rules_version)
);

create table if not exists public.moderation_actions (
  id uuid primary key default gen_random_uuid(),
  moderator_id uuid not null references auth.users(id),
  target_type text not null check (target_type in ('post','comment','profile','report')),
  target_id uuid not null,
  action text not null check (action in ('hide','restore','remove','warn','resolve')),
  reason text not null default '' check (char_length(reason) <= 1000),
  created_at timestamptz not null default now()
);

create index if not exists posts_created_at_idx on public.posts(created_at desc) where status='published';
create index if not exists posts_author_idx on public.posts(author_id,created_at desc);
create index if not exists comments_post_idx on public.comments(post_id,created_at);
create index if not exists scam_reports_target_idx on public.scam_reports(target_type,target_value);
create index if not exists scam_reports_created_idx on public.scam_reports(created_at desc);

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  wanted_username text;
begin
  wanted_username := lower(coalesce(new.raw_user_meta_data ->> 'username',''));

  if wanted_username !~ '^[a-z0-9_]{3,24}$'
     or exists (
       select 1
       from public.profiles p
       where p.username = wanted_username
     ) then
    wanted_username := 'user_' || substr(replace(new.id::text,'-',''),1,12);
  end if;

  insert into public.profiles(id,username,display_name)
  values(
    new.id,
    wanted_username,
    left(coalesce(new.raw_user_meta_data ->> 'display_name',''),60)
  )
  on conflict (id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created_scamshield on auth.users;
create trigger on_auth_user_created_scamshield
after insert on auth.users
for each row execute procedure public.handle_new_user();

alter table public.profiles enable row level security;
alter table public.posts enable row level security;
alter table public.comments enable row level security;
alter table public.post_votes enable row level security;
alter table public.reposts enable row level security;
alter table public.scam_reports enable row level security;
alter table public.rule_acceptances enable row level security;
alter table public.moderation_actions enable row level security;

drop policy if exists "Public profiles are visible" on public.profiles;
create policy "Public profiles are visible" on public.profiles
for select to anon, authenticated
using (is_public = true or id = (select auth.uid()));

drop policy if exists "Users update own profile" on public.profiles;
create policy "Users update own profile" on public.profiles
for update to authenticated
using (id = (select auth.uid()))
with check (id = (select auth.uid()));

drop policy if exists "Published posts are visible" on public.posts;
create policy "Published posts are visible" on public.posts
for select to anon, authenticated
using (status = 'published' or author_id = (select auth.uid()));

drop policy if exists "Signed-in users create own posts" on public.posts;
create policy "Signed-in users create own posts" on public.posts
for insert to authenticated
with check (author_id = (select auth.uid()));

drop policy if exists "Authors update own posts" on public.posts;
create policy "Authors update own posts" on public.posts
for update to authenticated
using (author_id = (select auth.uid()))
with check (author_id = (select auth.uid()));

drop policy if exists "Authors delete own posts" on public.posts;
create policy "Authors delete own posts" on public.posts
for delete to authenticated
using (author_id = (select auth.uid()));

drop policy if exists "Published comments are visible" on public.comments;
create policy "Published comments are visible" on public.comments
for select to anon, authenticated
using (
  status = 'published'
  and exists (select 1 from public.posts p where p.id = post_id and p.status = 'published')
);

drop policy if exists "Signed-in users create own comments" on public.comments;
create policy "Signed-in users create own comments" on public.comments
for insert to authenticated
with check (
  author_id = (select auth.uid())
  and exists (select 1 from public.posts p where p.id = post_id and p.status = 'published')
);

drop policy if exists "Authors delete own comments" on public.comments;
create policy "Authors delete own comments" on public.comments
for delete to authenticated
using (author_id = (select auth.uid()));

-- Vote rows are private to their owner; public totals should be exposed only
-- through a controlled aggregate RPC in a later integration step.
drop policy if exists "Users see only their votes" on public.post_votes;
create policy "Users see only their votes" on public.post_votes
for select to authenticated using (user_id = (select auth.uid()));

drop policy if exists "Users create own votes" on public.post_votes;
create policy "Users create own votes" on public.post_votes
for insert to authenticated with check (user_id = (select auth.uid()));

drop policy if exists "Users change own votes" on public.post_votes;
create policy "Users change own votes" on public.post_votes
for update to authenticated using (user_id = (select auth.uid()))
with check (user_id = (select auth.uid()));

drop policy if exists "Users remove own votes" on public.post_votes;
create policy "Users remove own votes" on public.post_votes
for delete to authenticated using (user_id = (select auth.uid()));

drop policy if exists "Reposts visible to owner" on public.reposts;
create policy "Reposts visible to owner" on public.reposts
for select to authenticated using (user_id = (select auth.uid()));

drop policy if exists "Users create own reposts" on public.reposts;
create policy "Users create own reposts" on public.reposts
for insert to authenticated with check (user_id = (select auth.uid()));

drop policy if exists "Users remove own reposts" on public.reposts;
create policy "Users remove own reposts" on public.reposts
for delete to authenticated using (user_id = (select auth.uid()));

drop policy if exists "Users submit reports as themselves" on public.scam_reports;
create policy "Users submit reports as themselves" on public.scam_reports
for insert to authenticated with check (reporter_id = (select auth.uid()));

drop policy if exists "Users see their own reports" on public.scam_reports;
create policy "Users see their own reports" on public.scam_reports
for select to authenticated using (reporter_id = (select auth.uid()));

drop policy if exists "Users accept rules for themselves" on public.rule_acceptances;
create policy "Users accept rules for themselves" on public.rule_acceptances
for all to authenticated using (user_id = (select auth.uid()))
with check (user_id = (select auth.uid()));

-- No client policies for moderation_actions: moderation writes must be done
-- by a trusted server-side function after moderator authorization.

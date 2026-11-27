-- ScamShield Supabase foundation
-- Apply once in Supabase SQL Editor. Designed for Supabase Auth + RLS.
-- No raw phone number, email address, or full URL is stored in scam_reports.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text not null unique check (username = lower(username) and username ~ '^[a-z0-9_.]{3,24}$'),
  display_name text not null default '' check (char_length(display_name) <= 60),
  bio text not null default '' check (char_length(bio) <= 280),
  city text not null default '' check (char_length(city) <= 80),
  avatar_path text,
  is_public boolean not null default true,
  show_email_verified boolean not null default true,
  email_verified boolean not null default false,
  community_verified boolean not null default false,
  role text not null default 'user' check (role in ('user','moderator','admin')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.posts (
  id uuid primary key default gen_random_uuid(),
  author_id uuid not null references public.profiles(id) on delete cascade,
  title text not null check (char_length(title) between 1 and 120),
  body text not null check (char_length(body) between 1 and 5000),
  category text not null default 'general' check (char_length(category) <= 40),
  language text not null default 'sr-Latn' check (language in ('sr-Latn','sr-Cyrl','en')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.comments (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references public.posts(id) on delete cascade,
  author_id uuid not null references public.profiles(id) on delete cascade,
  body text not null check (char_length(body) between 1 and 2000),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.post_votes (
  post_id uuid not null references public.posts(id) on delete cascade,
  voter_id uuid not null references public.profiles(id) on delete cascade,
  value smallint not null check (value in (-1, 1)),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (post_id, voter_id)
);

create table if not exists public.post_reposts (
  post_id uuid not null references public.posts(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (post_id, user_id)
);

create table if not exists public.author_endorsements (
  author_id uuid not null references public.profiles(id) on delete cascade,
  endorser_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (author_id, endorser_id),
  check (author_id <> endorser_id)
);

-- Store a one-way target fingerprint and masked display value, never the raw target.
create table if not exists public.scam_reports (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references public.profiles(id) on delete cascade,
  target_type text not null check (target_type in ('phone','email','url','sms','social','marketplace','other')),
  target_hash text not null check (target_hash ~ '^[a-f0-9]{64}$'),
  target_masked text not null check (char_length(target_masked) <= 160),
  category text not null check (char_length(category) between 1 and 60),
  description text not null default '' check (char_length(description) <= 2000),
  country_code text not null default '' check (char_length(country_code) <= 2),
  city text not null default '' check (char_length(city) <= 80),
  created_at timestamptz not null default now(),
  unique (reporter_id, target_type, target_hash)
);

create index if not exists posts_feed_idx on public.posts (created_at desc) where deleted_at is null;
create index if not exists comments_post_idx on public.comments (post_id, created_at) where deleted_at is null;
create index if not exists reports_target_idx on public.scam_reports (target_type, target_hash, created_at desc);

create or replace function public.set_updated_at()
returns trigger language plpgsql set search_path = public
as $$ begin new.updated_at = now(); return new; end; $$;

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at before update on public.profiles
for each row execute function public.set_updated_at();
drop trigger if exists posts_set_updated_at on public.posts;
create trigger posts_set_updated_at before update on public.posts
for each row execute function public.set_updated_at();
drop trigger if exists comments_set_updated_at on public.comments;
create trigger comments_set_updated_at before update on public.comments
for each row execute function public.set_updated_at();
drop trigger if exists votes_set_updated_at on public.post_votes;
create trigger votes_set_updated_at before update on public.post_votes
for each row execute function public.set_updated_at();

create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = ''
as $$
declare requested_username text;
begin
  requested_username := lower(coalesce(new.raw_user_meta_data ->> 'username', ''));
  if requested_username !~ '^[a-z0-9_.]{3,24}$' then
    requested_username := left('user_' || replace(new.id::text, '-', ''), 24);
  end if;
  insert into public.profiles (id, username, display_name, email_verified)
  values (new.id, requested_username,
    left(coalesce(new.raw_user_meta_data ->> 'display_name', ''), 60),
    (new.email_confirmed_at is not null))
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
for each row execute procedure public.handle_new_user();

create or replace function public.sync_email_verification()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
  if new.email_confirmed_at is distinct from old.email_confirmed_at then
    update public.profiles set email_verified = (new.email_confirmed_at is not null)
    where id = new.id;
  end if;
  return new;
end;
$$;

drop trigger if exists on_auth_user_email_verified on auth.users;
create trigger on_auth_user_email_verified after update of email_confirmed_at on auth.users
for each row execute procedure public.sync_email_verification();

alter table public.profiles enable row level security;
alter table public.posts enable row level security;
alter table public.comments enable row level security;
alter table public.post_votes enable row level security;
alter table public.post_reposts enable row level security;
alter table public.author_endorsements enable row level security;
alter table public.scam_reports enable row level security;

drop policy if exists "profiles_select_public_or_self" on public.profiles;
create policy "profiles_select_public_or_self" on public.profiles
for select to anon, authenticated using (is_public = true or id = (select auth.uid()));
drop policy if exists "profiles_update_self" on public.profiles;
create policy "profiles_update_self" on public.profiles
for update to authenticated using (id = (select auth.uid()))
with check (id = (select auth.uid()));

-- Column-level grants stop users changing roles or verification flags.
revoke update on public.profiles from anon, authenticated;
grant update (username, display_name, bio, city, avatar_path, is_public, show_email_verified)
on public.profiles to authenticated;

drop policy if exists "posts_select_visible" on public.posts;
create policy "posts_select_visible" on public.posts for select to anon, authenticated
using (deleted_at is null and exists (
  select 1 from public.profiles p where p.id = author_id
  and (p.is_public = true or p.id = (select auth.uid()))
));
drop policy if exists "posts_insert_own" on public.posts;
create policy "posts_insert_own" on public.posts for insert to authenticated
with check (author_id = (select auth.uid()));
drop policy if exists "posts_update_own" on public.posts;
create policy "posts_update_own" on public.posts for update to authenticated
using (author_id = (select auth.uid())) with check (author_id = (select auth.uid()));
drop policy if exists "posts_delete_own" on public.posts;
create policy "posts_delete_own" on public.posts for delete to authenticated
using (author_id = (select auth.uid()));

drop policy if exists "comments_select_visible_post" on public.comments;
create policy "comments_select_visible_post" on public.comments for select to anon, authenticated
using (deleted_at is null and exists (
  select 1 from public.posts p where p.id = post_id and p.deleted_at is null
  and exists (select 1 from public.profiles a where a.id = p.author_id
    and (a.is_public or a.id = (select auth.uid())))
  and exists (select 1 from public.profiles commenter where commenter.id = author_id
    and (commenter.is_public or commenter.id = (select auth.uid())))
));
drop policy if exists "comments_insert_own" on public.comments;
create policy "comments_insert_own" on public.comments for insert to authenticated
with check (author_id = (select auth.uid()) and exists (
  select 1 from public.posts p where p.id = post_id and p.deleted_at is null
));
drop policy if exists "comments_update_own" on public.comments;
create policy "comments_update_own" on public.comments for update to authenticated
using (author_id = (select auth.uid())) with check (author_id = (select auth.uid()));
drop policy if exists "comments_delete_own" on public.comments;
create policy "comments_delete_own" on public.comments for delete to authenticated
using (author_id = (select auth.uid()));

drop policy if exists "votes_read_visible_posts" on public.post_votes;
create policy "votes_read_visible_posts" on public.post_votes for select to anon, authenticated
using (exists (select 1 from public.posts p where p.id = post_id and p.deleted_at is null
  and exists (select 1 from public.profiles a where a.id = p.author_id
    and (a.is_public or a.id = (select auth.uid())))));
drop policy if exists "votes_insert_self" on public.post_votes;
create policy "votes_insert_self" on public.post_votes for insert to authenticated
with check (voter_id = (select auth.uid()) and exists (
  select 1 from public.posts p where p.id = post_id and p.author_id <> (select auth.uid()) and p.deleted_at is null
));
drop policy if exists "votes_update_self" on public.post_votes;
create policy "votes_update_self" on public.post_votes for update to authenticated
using (voter_id = (select auth.uid())) with check (voter_id = (select auth.uid()));
drop policy if exists "votes_delete_self" on public.post_votes;
create policy "votes_delete_self" on public.post_votes for delete to authenticated
using (voter_id = (select auth.uid()));

drop policy if exists "reposts_read_visible_posts" on public.post_reposts;
create policy "reposts_read_visible_posts" on public.post_reposts for select to anon, authenticated
using (exists (select 1 from public.posts p where p.id = post_id and p.deleted_at is null
  and exists (select 1 from public.profiles author where author.id = p.author_id
    and (author.is_public or author.id = (select auth.uid()))))
  and exists (select 1 from public.profiles actor where actor.id = user_id
    and (actor.is_public or actor.id = (select auth.uid()))));
drop policy if exists "reposts_insert_self" on public.post_reposts;
create policy "reposts_insert_self" on public.post_reposts for insert to authenticated
with check (user_id = (select auth.uid()));
drop policy if exists "reposts_delete_self" on public.post_reposts;
create policy "reposts_delete_self" on public.post_reposts for delete to authenticated
using (user_id = (select auth.uid()));

drop policy if exists "endorsements_read" on public.author_endorsements;
create policy "endorsements_read" on public.author_endorsements for select to anon, authenticated
using (exists (select 1 from public.profiles p where p.id = author_id
  and (p.is_public or p.id = (select auth.uid()))));
drop policy if exists "endorsements_insert_self" on public.author_endorsements;
create policy "endorsements_insert_self" on public.author_endorsements for insert to authenticated
with check (endorser_id = (select auth.uid()) and author_id <> (select auth.uid()));
drop policy if exists "endorsements_delete_self" on public.author_endorsements;
create policy "endorsements_delete_self" on public.author_endorsements for delete to authenticated
using (endorser_id = (select auth.uid()));

-- Reports are private to their submitter until an aggregate RPC is added.
drop policy if exists "reports_insert_self" on public.scam_reports;
create policy "reports_insert_self" on public.scam_reports for insert to authenticated
with check (reporter_id = (select auth.uid()));
drop policy if exists "reports_select_self" on public.scam_reports;
create policy "reports_select_self" on public.scam_reports for select to authenticated
using (reporter_id = (select auth.uid()));

grant select on public.profiles, public.posts, public.comments,
  public.post_reposts, public.author_endorsements to anon, authenticated;
revoke select on public.post_votes from anon, authenticated;

-- Return only aggregate vote totals. Individual voter IDs remain private.
create or replace function public.get_post_vote_totals(p_post_ids uuid[])
returns table (post_id uuid, upvotes bigint, downvotes bigint)
language sql
stable
security definer
set search_path = ''
as $
  select v.post_id,
    count(*) filter (where v.value = 1) as upvotes,
    count(*) filter (where v.value = -1) as downvotes
  from public.post_votes v
  join public.posts p on p.id = v.post_id
  join public.profiles author on author.id = p.author_id
  where v.post_id = any(p_post_ids)
    and p.deleted_at is null
    and (author.is_public or author.id = (select auth.uid()))
  group by v.post_id;
$;
revoke all on function public.get_post_vote_totals(uuid[]) from public;
grant execute on function public.get_post_vote_totals(uuid[]) to anon, authenticated;
grant insert, update, delete on public.posts, public.comments to authenticated;
grant insert, update, delete on public.post_votes to authenticated;
grant insert, delete on public.post_reposts, public.author_endorsements to authenticated;
grant select, insert on public.scam_reports to authenticated;

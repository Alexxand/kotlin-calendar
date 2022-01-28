create table users (
    id uuid primary key,
    name varchar(50) not null,
    lastname varchar(50) not null
);

create table meetings (
    id uuid primary key,
    meeting_organizer_id uuid references users(id),
    start_time timestamp,
    end_time timestamp
);

create index meeting_start_idx on meetings(start_time);

create table meeting_invitations (
    meeting_id uuid references meetings(id),
    invited_user_id uuid references users(id),
    accepted boolean default false,

    primary key (meeting_id, invited_user_id)
);


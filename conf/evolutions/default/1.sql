# --- !Ups

create sequence account_id_seq;
create table account
(
  id bigint not null default nextval('account_id_seq') constraint account_pkey primary key,
  email varchar(500) not null,
  password varchar(100) not null,
  created_at timestamp not null default current_timestamp,
  constraint account_mail_address_key unique (email)
);

insert into account(email, password) values ('bob@example.com', '48181acd22b3edaebc8a447868a7df7ce629920a'); -- password:bob
insert into account(email, password) values ('alice@example.com', '522b276a356bdf39013dfabea2cd43e141ecc9e8'); -- password:alice

create sequence oauth_client_id_seq;
create table oauth_client
(
  id bigint not null default nextval('oauth_client_id_seq') constraint oauth_client_pkey primary key,
  owner_id bigint not null,
  grant_type varchar(20) not null,
  client_id varchar(100) not null,
  client_secret varchar(100) not null,
  redirect_uri varchar(2000),
  created_at timestamp not null default current_timestamp,
  constraint oauth_client_owner_id_fkey foreign key (owner_id)
    references account (id) on delete cascade,
  constraint oauth_client_client_id_key unique (client_id)
);
create index oauth_client_owner_id_idx on oauth_client(owner_id);

insert into oauth_client(owner_id, grant_type, client_id, client_secret)
  values (1, 'client_credentials', 'bob_client_id', 'bob_client_secret');
insert into oauth_client(owner_id, grant_type, client_id, client_secret, redirect_uri)
  values (2, 'authorization_code', 'alice_client_id', 'alice_client_secret', 'http://localhost:3000/callback');
insert into oauth_client(owner_id, grant_type, client_id, client_secret)
  values (2, 'password', 'alice_client_id2', 'alice_client_secret2');

create sequence oauth_authorization_code_id_seq;
create table oauth_authorization_code
(
  id bigint not null default nextval('oauth_authorization_code_id_seq') constraint oauth_authorization_code_pkey primary key,
  account_id bigint not null,
  oauth_client_id bigint not null,
  code varchar(100) not null,
  redirect_uri varchar(2000) not null,
  created_at timestamp not null default current_timestamp,
  constraint oauth_authorization_code_account_id_fkey foreign key (account_id)
    references account (id) on delete cascade,
  constraint oauth_authorization_code_oauth_client_id_fkey foreign key (oauth_client_id)
    references oauth_client (id) on delete cascade
);

create index oauth_authorization_code_account_id_idx on oauth_authorization_code(account_id);
create unique index oauth_authorization_code_code_idx on oauth_authorization_code(code);
create index oauth_authorization_code_oauth_client_id_idx on oauth_authorization_code(oauth_client_id);

insert into oauth_authorization_code(account_id, oauth_client_id, code, redirect_uri)
  values (1, 2, 'bob_code', 'http://localhost:3000/callback');

create sequence oauth_access_token_id_seq;
create table oauth_access_token
(
  id bigint not null default nextval('oauth_access_token_id_seq') constraint oauth_access_token_pkey primary key,
  account_id bigint not null,
  oauth_client_id bigint not null,
  access_token varchar(100) not null,
  refresh_token varchar(100) not null,
  created_at timestamp not null default current_timestamp,
  constraint oauth_access_token_account_id_fkey foreign key (account_id)
    references account (id) on delete cascade,
  constraint oauth_access_token_oauth_client_id_fkey foreign key (oauth_client_id)
    references oauth_client (id) on delete cascade
);
create index oauth_access_token_account_id_idx on oauth_access_token(account_id);
create index oauth_access_token_oauth_client_id_idx on oauth_access_token(oauth_client_id);
create unique index oauth_access_token_access_token_idx on oauth_access_token(access_token);
create unique index oauth_access_token_refresh_token_idx on oauth_access_token(refresh_token);

# --- !Downs

drop index oauth_access_token_account_id_idx;
drop index oauth_access_token_oauth_client_id_idx;
drop index oauth_access_token_access_token_idx;
drop index oauth_access_token_refresh_token_idx;
drop table oauth_access_token;
drop sequence oauth_access_token_id_seq;

drop index oauth_authorization_code_account_id_idx;
drop index oauth_authorization_code_code_idx;
drop index oauth_authorization_code_oauth_client_id_idx;
drop table oauth_authorization_code;
drop sequence oauth_authorization_code_id_seq;

drop index oauth_client_owner_id_idx;
drop table oauth_client;
drop sequence oauth_client_id_seq;

drop table account;
drop sequence account_id_seq;

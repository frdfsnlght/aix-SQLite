/*
* This is a file used by the SQLite test app.
*/

create table if not exists months (id integer primary key, name text, updates integer default 0)
insert into months (name) values ('January')
insert into months (name) values ('February')
insert into months (name) values ('March')
insert into months (name) values ('April')
insert into months (name) values ('May')
insert into months (name) values ('June')   /* this is a single line comment */
insert into months (name) values ('July')
insert into months (name) values ('August')
insert into months (name) values ('September')
insert into months (name) values ('October')    -- this is a single line comment
insert into months (name) values ('November')
insert into months (name) values ('December')
update months set updates = updates + 1 where name like '%ry'
delete from months where name = 'July'

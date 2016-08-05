-- Postgres
CREATE TABLE datasource_info
(
  id SERIAL,
  name character varying(64),
  driver_class character varying(64),
  jndi character varying(64),
  url character varying(256),
  username character varying(64),
  password character varying(64),
  max_active smallint default 10,
  initial_size smallint default 1,
  active_flag smallint default 1,
  memo character varying(512),
  gmt_create timestamp,
  gmt_modified timestamp,
  CONSTRAINT ds_info_pkey PRIMARY KEY (id)
);

CREATE TABLE business_type
(
  id SERIAL,
  name character varying(64),
  ds_id character varying(128),
  active_flag smallint default 1,
  memo character varying(512),
  gmt_create timestamp,
  gmt_modified timestamp,
  CONSTRAINT ds_info_pkey PRIMARY KEY (id)
);

insert into datasource_info (name, driver_class, jndi, url, username, password) values ('udb1','', '', 'jdbc:postgresql://localhost:5432/udb1', 'postgreuser', 'aaa123+-*/');
insert into transaction_info(tx_id, parent_id, business_id, business_type, tx_type, tx_status, context, payload, retried_count) values (1, 1, 'business_id', 'test', 1, 1, 'context', 'payload', 3);
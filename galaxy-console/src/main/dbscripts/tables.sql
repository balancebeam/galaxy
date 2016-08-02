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
  active_flag smallint default 0,
  memo character varying(512),
  gmt_create timestamp,
  gmt_modified timestamp,
  CONSTRAINT ds_info_pkey PRIMARY KEY (id)
);

CREATE TABLE business_type
(
  id SERIAL,
  name character varying(64),
  ds_id integer,
  active_flag smallint default 0,
  memo character varying(512),
  gmt_create timestamp,
  gmt_modified timestamp,
  CONSTRAINT ds_info_pkey PRIMARY KEY (id)
);
CREATE TABLE t_repository
(
  id numeric NOT NULL,
  name character varying(50),
  stock numeric NOT NULL,
  unit_price numeric,
  CONSTRAINT t_repository_pkey PRIMARY KEY (id)
)
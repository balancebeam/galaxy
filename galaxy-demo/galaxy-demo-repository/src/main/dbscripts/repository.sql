CREATE TABLE t_repository
(
  id numeric NOT NULL,
  category character varying(50),
  amount numeric NOT NULL,
  price numeric,
  CONSTRAINT t_repository_pkey PRIMARY KEY (id)
)
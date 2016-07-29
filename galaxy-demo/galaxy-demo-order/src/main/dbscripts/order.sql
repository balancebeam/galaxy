CREATE TABLE t_order
(
  order_id numeric NOT NULL,
  item_id numeric NOT NULL,
  user_id numeric NOT NULL,
  status character varying(50),
  amout numeric NOT NULL,
  CONSTRAINT t_order_pkey PRIMARY KEY (order_id)
)

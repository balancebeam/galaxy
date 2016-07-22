-- Postgres
CREATE TABLE transaction_info
(
  tx_id bigint,
  parent_id  bigint,
  business_id bigint,
  business_type character varying(64),
  tx_type smallint,
  tx_status smallint,
  context text,
  payload text,
  retried_count smallint default 0,
  gmt_create timestamp,
  gmt_modified timestamp,
  CONSTRAINT tran_info_pkey PRIMARY KEY (tx_id)
);

create index idx_tran_info_pid on transaction_info(parent_id);

commit;
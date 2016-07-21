-- Postgres
CREATE TABLE transaction_status
(
  tx_id bigint,
  parent_id  bigint,
  business_id bigint,
  business_type character varying(64),
  tx_type smallint,
  tx_status smallint,
  context text,
  payload text,
  retried_count smallint,
  gmt_create timestamp,
  gmt_modified timestamp,
  CONSTRAINT transaction_status_pkey PRIMARY KEY (tx_id)
);

create index idx_tran_s_pid on transaction_status(parent_id);

commit;
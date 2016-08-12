-- Postgres
CREATE TABLE transaction_info
(
  tx_id bigint,
  parent_id bigint,
  module_id character varying(64),
  business_id character varying(64),
  business_type character varying(64),
  tx_type smallint,
  tx_status smallint,
  context text,
  retried_count character varying(128),
  next_retry_time timestamp without time zone,
  gmt_created timestamp without time zone,
  gmt_modified timestamp without time zone,
  CONSTRAINT tran_info_pkey PRIMARY KEY (tx_id)
);

create index idx_tran_info_pid on transaction_info(parent_id);
create index idx_tran_info_bType on transaction_info(business_type);
create index idx_tran_info_status on transaction_info(tx_status);

commit;
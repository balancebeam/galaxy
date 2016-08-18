-- Postgres
CREATE TABLE transaction_info
(
  parent_id bigint,
  tx_id bigint,
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
  CONSTRAINT tran_info_pkey PRIMARY KEY (parent_id, tx_id)
);
create index idx_tran_info_mid on transaction_info(module_id);
create index idx_tran_info_ts on transaction_info(tx_status);
create index idx_tran_info_nrt on transaction_info(next_retry_time);
create index idx_tran_info_gm on transaction_info(gmt_modified);

commit;
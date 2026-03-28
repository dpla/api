ALTER TABLE account ADD COLUMN last_used_at timestamp with time zone;
ALTER TABLE account ADD COLUMN request_count bigint NOT NULL DEFAULT 0;

CREATE INDEX account_last_used_at_idx ON account USING btree (last_used_at);

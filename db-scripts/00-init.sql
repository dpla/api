CREATE TABLE account (
                                id integer NOT NULL,
                                key character(32) NOT NULL,
                                email character varying(100) NOT NULL,
                                enabled boolean DEFAULT true,
                                staff boolean DEFAULT false,
                                created_at timestamp with time zone DEFAULT timezone('utc'::text, now()),
                                updated_at timestamp with time zone DEFAULT timezone('utc'::text, now())
);

CREATE SEQUENCE account_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE account_id_seq OWNED BY account.id;

ALTER TABLE ONLY account ALTER COLUMN id SET DEFAULT nextval('account_id_seq'::regclass);

ALTER TABLE ONLY account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id);

CREATE INDEX account_email_idx ON account USING btree (email);

CREATE UNIQUE INDEX account_key_idx ON account USING btree (key);




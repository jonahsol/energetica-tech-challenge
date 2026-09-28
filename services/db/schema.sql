-- Text search configuration: "simple".
-- It lowercases and splits tokens, and it does not stem or drop stop words.
-- Keyword lookup should keep the lexeme resolved from the vocabulary, including
-- proper nouns. English stemming would make those lexemes diverge from search_terms.
-- Misspellings are corrected with pg_trgm before the tsquery is built.
-- Subject is weight A and body is weight B, so a subject hit outranks a body-only hit.
-- to_tsvector rejects input longer than 1,048,575 bytes. bounded_search_text keeps the
-- indexed prefix under that limit. body and raw_message still store the full text.
--
-- The same message is stored once per mailbox folder, and each copy has its own
-- generated Message-ID. content_key hashes the mailbox, sender, date, subject, and
-- body so ingest can keep the first copy and skip the others.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE OR REPLACE FUNCTION bounded_search_text(value text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT CASE
        WHEN octet_length(coalesce(value, '')) <= 1000000 THEN coalesce(value, '')
        ELSE left(value, 250000)
    END
$$;

-- Mailbox is the first path segment of id. Date is microseconds since epoch so the
-- text does not depend on DateStyle. Message-ID and X-Folder are excluded because
-- those are what change between folder copies.
CREATE OR REPLACE FUNCTION email_content_key(
    mailbox_id text,
    sender text,
    sent_at timestamptz,
    subject text,
    body text
)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT md5(
        split_part(coalesce(mailbox_id, ''), '/', 1)
        || chr(10)
        || coalesce(sender, '')
        || chr(10)
        || CASE
            WHEN sent_at IS NULL THEN ''
            ELSE ((extract(epoch FROM sent_at) * 1000000)::bigint)::text
        END
        || chr(10)
        || coalesce(subject, '')
        || chr(10)
        || coalesce(body, '')
    )
$$;

CREATE TABLE IF NOT EXISTS emails (
    id TEXT PRIMARY KEY,
    message_id TEXT,
    sender TEXT,
    recipient_to TEXT,
    recipient_cc TEXT,
    recipient_bcc TEXT,
    x_to TEXT,
    x_cc TEXT,
    x_bcc TEXT,
    date TIMESTAMPTZ,
    subject TEXT,
    body TEXT,
    raw_message TEXT,
    content_key TEXT GENERATED ALWAYS AS (
        email_content_key(id, sender, date, subject, body)
    ) STORED NOT NULL,
    search_vector TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', bounded_search_text(subject)), 'A') ||
        setweight(to_tsvector('simple', bounded_search_text(body)), 'B')
    ) STORED NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_emails_content_key ON emails (content_key);

CREATE INDEX IF NOT EXISTS idx_emails_search_vector
    ON emails
    USING GIN (search_vector);

CREATE TABLE IF NOT EXISTS search_terms (
    term TEXT PRIMARY KEY
);

CREATE INDEX IF NOT EXISTS idx_search_terms_trgm
    ON search_terms
    USING GIN (term gin_trgm_ops);

-- Text search configuration: "simple".
-- It lowercases and splits tokens, and it does not stem or drop stop words.
-- Keyword lookup should keep the lexeme resolved from the vocabulary, including
-- proper nouns. English stemming would make those lexemes diverge from search_terms.
-- Misspellings are corrected with pg_trgm before the tsquery is built.
-- Subject is weight A and body is weight B, so a subject hit outranks a body-only hit.
-- to_tsvector rejects input longer than 1,048,575 bytes. bounded_search_text keeps the
-- indexed prefix under that limit. body and raw_message still store the full text.

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
    search_vector TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', bounded_search_text(subject)), 'A') ||
        setweight(to_tsvector('simple', bounded_search_text(body)), 'B')
    ) STORED NOT NULL
);

ALTER TABLE emails ADD COLUMN IF NOT EXISTS x_to TEXT;
ALTER TABLE emails ADD COLUMN IF NOT EXISTS x_cc TEXT;
ALTER TABLE emails ADD COLUMN IF NOT EXISTS x_bcc TEXT;

-- Replace an older generated column that indexed the full body.
DO $$
DECLARE
    expr text;
BEGIN
    SELECT pg_get_expr(definition.adbin, definition.adrelid)
    INTO expr
    FROM pg_attrdef AS definition
    JOIN pg_attribute AS attribute
      ON attribute.attrelid = definition.adrelid
     AND attribute.attnum = definition.adnum
    JOIN pg_class AS relation ON relation.oid = attribute.attrelid
    WHERE relation.relname = 'emails'
      AND attribute.attname = 'search_vector';

    IF expr IS NOT NULL AND position('bounded_search_text' IN expr) = 0 THEN
        DROP INDEX IF EXISTS idx_emails_search_vector;
        ALTER TABLE emails DROP COLUMN search_vector;
        ALTER TABLE emails ADD COLUMN search_vector tsvector
            GENERATED ALWAYS AS (
                setweight(to_tsvector('simple', bounded_search_text(subject)), 'A') ||
                setweight(to_tsvector('simple', bounded_search_text(body)), 'B')
            ) STORED NOT NULL;
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_emails_search_vector
    ON emails
    USING GIN (search_vector);

CREATE TABLE IF NOT EXISTS search_terms (
    term TEXT PRIMARY KEY
);

CREATE INDEX IF NOT EXISTS idx_search_terms_trgm
    ON search_terms
    USING GIN (term gin_trgm_ops);
